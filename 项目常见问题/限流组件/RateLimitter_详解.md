# Lua 限流脚本（令牌桶算法）详细解析

## 整体概述

这是一个基于 Redis 的**令牌桶限流算法**实现。核心思想是：

- 系统以恒定速率向桶中放入令牌
- 请求需要获取令牌才能通过
- 如果令牌不足，请求被拒绝并告知等待时间

---

## 逐行详解

### 第 1-3 行：读取限流配置

```lua
local rate = redis.call('hget', KEYS[1], 'rate');
local interval = redis.call('hget', KEYS[1], 'interval');
local type = redis.call('hget', KEYS[1], 'type');
```

**含义**：

- `redis.call('hget', KEYS[1], 'rate')`：从 Redis Hash 类型键 `KEYS[1]` 中获取 `rate` 字段
  - `rate`：限流速率，即单位时间内允许的最大请求数（例如 100）
  
- `redis.call('hget', KEYS[1], 'interval')`：获取 `interval` 字段
  - `interval`：时间窗口大小，单位毫秒（例如 1000ms = 1秒）
  
- `redis.call('hget', KEYS[1], 'type')`：获取 `type` 字段
  - `type`：限流类型标记
    - `'0'`：普通模式（使用 KEYS[2] 和 KEYS[4]）
    - `'1'：高级模式（使用 KEYS[3] 和 KEYS[5]，可能支持不同的计费方式）

**例子**：

```
KEYS[1] 对应的 Redis Hash：
{
  "rate": "100",
  "interval": "1000",
  "type": "0"
}
```

---

### 第 4 行：配置校验

```lua
assert(rate ~= false and interval ~= false and type ~= false, 'RateLimiter is not initialized')
```

**含义**：

- 使用 `assert` 检查配置是否存在
- 如果任一配置为 `false`（即在 Redis 中不存在），抛出异常
- 异常信息：`'RateLimiter is not initialized'`
- **目的**：确保限流器已被正确初始化，防止后续脚本出错

---

### 第 5-10 行：根据类型选择键名

```lua
local valueName = KEYS[2];
local permitsName = KEYS[4];
if type == '1' then 
  valueName = KEYS[3];
  permitsName = KEYS[5];
end;
```

**含义**：

- 默认使用 `KEYS[2]` 和 `KEYS[4]` 作为存储键名
- `valueName`：存储当前可用令牌数的键（Redis String 类型）
- `permitsName`：存储已分配的许可证的键（Redis Sorted Set 类型）

**条件分支**：

- 如果 `type == '1'`（高级模式），改用 `KEYS[3]` 和 `KEYS[5]`
- **目的**：支持多种限流场景（可能不同的业务使用不同的键）

**KEYS 参数总结**：

| 参数 | 说明 | 类型 |
|------|------|------|
| KEYS[1] | 配置键 | Hash（存放 rate、interval、type） |
| KEYS[2] | 令牌数键（普通模式） | String（当前可用令牌） |
| KEYS[3] | 令牌数键（高级模式） | String（当前可用令牌） |
| KEYS[4] | 许可证集合键（普通模式） | Sorted Set（记录分配的许可证） |
| KEYS[5] | 许可证集合键（高级模式） | Sorted Set（记录分配的许可证） |

---

### 第 11 行：校验请求令牌数

```lua
assert(tonumber(rate) >= tonumber(ARGV[1]), 'Requested permits amount could not exceed defined rate');
```

**含义**：

- `ARGV[1]`：本次请求需要的令牌数（例如 1）
- `tonumber(rate)`：速率（例如 100）
- 断言：`rate >= ARGV[1]`，即**单次请求令牌数不能超过限流速率**
- **目的**：防止无意义的大批量请求（例如一次要 1000 个令牌但限流速率只有 100）

**示例**：

```
如果 rate = 100, ARGV[1] = 50
则 100 >= 50，检查通过

如果 ARGV[1] = 150
则 100 >= 150 为假，抛出异常
```

---

### 第 12 行：获取当前可用令牌数

```lua
local currentValue = redis.call('get', valueName);
```

**含义**：

- 从 Redis 中读取 `valueName` 键的值
- 这个值代表**当前瞬间可用的令牌数量**
- 返回值可能是：
  - 一个数字字符串（如 `"45"`）：当前还有 45 个可用令牌
  - `false`：键不存在，表示这是第一次请求

---

### 第 13-36 行：已有令牌数的场景（currentValue ~= false）

```lua
if currentValue ~= false then
  -- ... 处理逻辑 ...
end;
```

**含义**：如果键存在，说明不是第一次请求，需要处理过期令牌的回收

---

#### 第 14-18 行：查询并统计过期的令牌

```lua
local expiredValues = redis.call('zrangebyscore', permitsName, 0, tonumber(ARGV[2]) - interval); 
local released = 0; 
for i, v in ipairs(expiredValues) do 
  local random, permits = struct.unpack('Lc0I', v);
  released = released + permits;
end;
```

**分项解析**：

**第 14 行：查询过期许可证**

```lua
local expiredValues = redis.call('zrangebyscore', permitsName, 0, tonumber(ARGV[2]) - interval);
```

- `ARGV[2]`：当前时间戳（毫秒）
- `ARGV[2] - interval`：`interval` 毫秒前的时间戳
- `zrangebyscore(permitsName, 0, ARGV[2] - interval)`：
  - 查询 Sorted Set 中分数 (score) 在 `[0, ARGV[2] - interval]` 范围内的元素
  - **分数代表许可证分配的时间戳**
  - **查出的是所有已过期的许可证**（时间戳早于当前时间 - interval）

**示例**：

```
假设 interval = 1000ms（1秒）
当前时间 ARGV[2] = 10000
查询范围：[0, 10000 - 1000] = [0, 9000]

如果许可证时间戳：
- 8000（在范围内）→ 已过期，可以回收
- 9500（不在范围内）→ 未过期，不可回收
- 10000（不在范围内）→ 刚分配，不可回收
```

**第 15-18 行：解包并累计过期许可证的令牌数**

```lua
local released = 0; 
for i, v in ipairs(expiredValues) do 
  local random, permits = struct.unpack('Lc0I', v);
  released = released + permits;
end;
```

- `expiredValues` 中的每个元素 `v` 是一个打包的二进制数据，包含：
  - `L`：4 字节无符号整数（请求 ID 的长度）
  - `c0`：可变长度字符串（请求 ID）
  - `I`：4 字节无符号整数（本次请求使用的令牌数）
  
- `struct.unpack('Lc0I', v)` 解包得到三个值，其中第二个被忽略，第三个是 `permits`
- 累加所有过期许可证的令牌数到 `released`

**示例**：

```
假设过期许可证有三个：
- 许可证 1：permits = 5
- 许可证 2：permits = 10
- 许可证 3：permits = 15

则 released = 5 + 10 + 15 = 30
```

---

#### 第 19-26 行：如果有过期许可证，执行回收

```lua
if released > 0 then 
  redis.call('zremrangebyscore', permitsName, 0, tonumber(ARGV[2]) - interval); 
  if tonumber(currentValue) + released > tonumber(rate) then 
    currentValue = tonumber(rate) - redis.call('zcard', permitsName); 
  else 
    currentValue = tonumber(currentValue) + released; 
  end; 
  redis.call('set', valueName, currentValue);
end;
```

**分项解析**：

**第 20 行：删除过期许可证**

```lua
redis.call('zremrangebyscore', permitsName, 0, tonumber(ARGV[2]) - interval);
```

- 从 Sorted Set 中删除分数在 `[0, ARGV[2] - interval]` 范围内的所有元素
- **目的**：清理已过期的许可证，释放 Redis 内存

**第 21-26 行：更新可用令牌数**

```lua
if tonumber(currentValue) + released > tonumber(rate) then 
  currentValue = tonumber(rate) - redis.call('zcard', permitsName); 
else 
  currentValue = tonumber(currentValue) + released; 
end;
```

**条件 1**：`currentValue + released > rate`（回收后令牌数会超过上限）

```lua
currentValue = tonumber(rate) - redis.call('zcard', permitsName);
```

- 令牌数受上限 `rate` 限制，不能超过
- `redis.call('zcard', permitsName)`：获取当前未过期许可证的数量
- 重新计算：`新令牌数 = 速率上限 - 未过期许可证数`
- **含义**：可用令牌 = 总容量 - 已分配（未过期）的许可证

**示例**：

```
rate = 100（限流速率）
currentValue = 50
released = 60（回收的）
zcard = 20（当前未过期的许可证数）

50 + 60 = 110 > 100，触发第一条件
新的 currentValue = 100 - 20 = 80
含义：还有 80 个可用令牌
```

**条件 2**：`currentValue + released <= rate`（回收后令牌数不超上限）

```lua
currentValue = tonumber(currentValue) + released;
```

- 直接加上回收的令牌数

**第 27 行：持久化更新**

```lua
redis.call('set', valueName, currentValue);
```

- 将更新后的令牌数写回 Redis

---

#### 第 28-33 行：判断是否有足够令牌

```lua
if tonumber(currentValue) < tonumber(ARGV[1]) then 
  local firstValue = redis.call('zrange', permitsName, 0, 0, 'withscores'); 
  return 3 + interval - (tonumber(ARGV[2]) - tonumber(firstValue[2]));
else 
  redis.call('zadd', permitsName, ARGV[2], struct.pack('Lc0I', string.len(ARGV[3]), ARGV[3], ARGV[1])); 
  redis.call('decrby', valueName, ARGV[1]); 
  return nil; 
end;
```

**第 28 行：检查令牌是否不足**

```lua
if tonumber(currentValue) < tonumber(ARGV[1]) then
```

- 可用令牌数 < 请求需要的令牌数
- 限流被触发，无法满足本次请求

**第 29-30 行：计算需要等待的时间**

```lua
local firstValue = redis.call('zrange', permitsName, 0, 0, 'withscores'); 
return 3 + interval - (tonumber(ARGV[2]) - tonumber(firstValue[2]));
```

- `redis.call('zrange', permitsName, 0, 0, 'withscores')`：获取 Sorted Set 中分数最小的元素及其分数
  - `firstValue[1]`：最早分配的许可证数据
  - `firstValue[2]`：该许可证的时间戳
  
- 返回值计算：
  - `tonumber(ARGV[2]) - tonumber(firstValue[2])`：从最早许可证到现在已过的时间
  - `interval - (...)`：该许可证还需要多久才会过期
  - `3 +`：加上 3ms 的缓冲（确保过期）
  
- **含义**：告知客户端需要等待多少毫秒后重试

**示例**：

```
interval = 1000ms
最早许可证时间戳 = 5000ms
当前时间 = 5500ms

已过时间 = 5500 - 5000 = 500ms
还需等待 = 1000 - 500 = 500ms
返回值 = 3 + 500 = 503ms
```

**第 31-32 行：令牌充足，发放许可证**

```lua
redis.call('zadd', permitsName, ARGV[2], struct.pack('Lc0I', string.len(ARGV[3]), ARGV[3], ARGV[1])); 
redis.call('decrby', valueName, ARGV[1]);
```

- `redis.call('zadd', permitsName, ARGV[2], ...)`：
  - 添加一个新许可证到 Sorted Set
  - 分数 = `ARGV[2]`（当前时间戳）
  - 值 = 打包的数据（包含请求 ID 和令牌数）
  
- `redis.call('decrby', valueName, ARGV[1])`：
  - 减少可用令牌数
  - 减少量 = 本次请求需要的令牌数

**第 33 行：返回成功标记**

```lua
return nil;
```

- 返回 `nil`（在 Java 中通常被解析为 `null`）表示**限流通过**

---

### 第 34-39 行：第一次请求的场景（currentValue == false）

```lua
else 
  redis.call('set', valueName, rate); 
  redis.call('zadd', permitsName, ARGV[2], struct.pack('Lc0I', string.len(ARGV[3]), ARGV[3], ARGV[1])); 
  redis.call('decrby', valueName, ARGV[1]); 
  return nil; 
end;
```

**含义**：如果是第一次请求（`currentValue` 不存在）

**第 35 行：初始化可用令牌数**

```lua
redis.call('set', valueName, rate);
```

- 将可用令牌数初始化为 `rate`（满桶状态）

**第 36-38 行：发放许可证并扣减令牌**

```lua
redis.call('zadd', permitsName, ARGV[2], struct.pack('Lc0I', string.len(ARGV[3]), ARGV[3], ARGV[1])); 
redis.call('decrby', valueName, ARGV[1]);
```

- 同第 31-32 行的逻辑，添加许可证并扣减令牌

**第 39 行：返回成功标记**

```lua
return nil;
```

- 首次请求成功通过

---

## ARGV 参数总结

| 参数 | 说明 | 类型 | 示例 |
|------|------|------|------|
| ARGV[1] | 本次请求需要的令牌数 | 数字 | 1、5、10 |
| ARGV[2] | 当前时间戳 | 毫秒数 | 1698000000000 |
| ARGV[3] | 请求 ID（唯一标识） | 字符串 | "user-123-req-456" |

---

## 脚本执行流程图

```
开始
  ↓
1. 读取限流配置 (rate, interval, type)
  ↓
2. 校验配置是否存在
  ↓
3. 根据 type 选择存储键名
  ↓
4. 校验请求令牌数是否超限
  ↓
5. 读取当前可用令牌数
  ↓
     ↙ 键不存在         键存在 ↘
     ↓                  ↓
   首次请求           回收过期令牌
   初始化为 rate      统计过期许可证
   ↓                  ↓
   发放许可证        更新可用令牌
   扣减令牌          ↓
   ↓                检查令牌是否充足？
   ↓                ↙ 不足        充足 ↘
   ↓                ↓              ↓
   返回 nil      计算等待时间    发放许可证
   (成功)        返回等待时间    扣减令牌
                 (限流被触发)    返回 nil
                                (成功)
```

---

## 实际工作示例

**场景**：限流速率 100 个令牌/秒，当前请求需要 5 个令牌

### 首次请求

```
时间：10000ms
请求 ID：req-1
需要令牌：5

执行流程：
1. 读取：rate=100, interval=1000, type=0
2. 校验：100 >= 5 ✓
3. 读取当前令牌：false（不存在）
4. 初始化：set valueName = 100
5. 添加许可证：zadd permitsName 10000 (req-1, 5)
6. 扣减令牌：decrby valueName 5 → 95
7. 返回：nil（成功）

结果：还剩 95 个令牌
```

### 第二次请求（同一时间窗口内）

```
时间：10100ms
请求 ID：req-2
需要令牌：30

执行流程：
1. 读取当前令牌：95
2. 查询过期许可证：范围 [0, 10100-1000] = [0, 9100]
   → 没有过期许可证
3. 检查令牌：95 >= 30 ✓
4. 添加许可证：zadd permitsName 10100 (req-2, 30)
5. 扣减令牌：decrby valueName 30 → 65
6. 返回：nil（成功）

结果：还剩 65 个令牌
```

### 第三次请求（令牌不足）

```
时间：10200ms
请求 ID：req-3
需要令牌：100

执行流程：
1. 读取当前令牌：65
2. 检查过期许可证：没有
3. 检查令牌：65 < 100 ✗（不足）
4. 查询最早许可证时间戳：10000ms（req-1）
5. 计算等待时间：
   已过时间 = 10200 - 10000 = 200ms
   还需等待 = 1000 - 200 = 800ms
   返回值 = 3 + 800 = 803ms
6. 返回：803（限流被触发，需等待 803ms）
```

### 第四次请求（过期许可证被回收）

```
时间：11200ms（过了 1 秒）
请求 ID：req-4
需要令牌：50

执行流程：
1. 读取当前令牌：65
2. 查询过期许可证：范围 [0, 11200-1000] = [0, 10200]
   → req-1 (10000), req-2 (10100) 都过期
3. 回收令牌：5 + 30 = 35
4. 删除过期许可证
5. 更新令牌数：
   65 + 35 = 100 <= 100 ✓
   currentValue = 100
6. 检查令牌：100 >= 50 ✓
7. 添加许可证：zadd permitsName 11200 (req-4, 50)
8. 扣减令牌：decrby valueName 50 → 50
9. 返回：nil（成功）

结果：还剩 50 个令牌，可用容量恢复到 100
```

---

## 总结：限流算法核心

| 阶段 | 操作 | 目的 |
|------|------|------|
| **初始化** | 令牌数 = rate | 令牌桶满装 |
| **回收过期** | 删除时间戳早的许可证 | 释放已过期的令牌分配 |
| **检查充足** | currentValue >= ARGV[1] | 判断是否有足够令牌 |
| **发放许可** | 添加许可证 + 扣减令牌 | 记录分配并减少可用令牌 |
| **拒绝限流** | 返回等待时间 | 告知客户端何时重试 |

---

## 在项目中的应用

这个脚本通常在 Java 中通过 Redisson 库的 `RRateLimiter` 使用：

```java
RRateLimiter rateLimiter = redissonClient.getRateLimiter("order-limiter");
// 每秒 100 个请求
rateLimiter.trySetRate(RateType.OVERALL, 100, 1, RateIntervalUnit.SECONDS);

// 获取 5 个令牌，超时等待 1 秒
boolean acquired = rateLimiter.tryAcquire(5, 1, TimeUnit.SECONDS);
if (acquired) {
    // 请求被允许，处理业务逻辑
} else {
    // 被限流，返回 429 或其他错误
}
```

脚本在幕后自动执行，保证了分布式环境下的精确限流控制。
