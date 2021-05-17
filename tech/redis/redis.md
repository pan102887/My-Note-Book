# redis 启停
## 启动：
  - 一般启动：redis-server
  - 指定端口：redis-server --port xxxx
  - 脚本启动：  
    在linux中，可以在Redis源码目录的utils文件夹下，找到一个名为redis_init_script的初始化脚本文件。可以通过脚本配置Redis的运行方式，持久化，日志文件存储位置等。方法如下：  

    1. 将以上脚本复制一份到/etc/init.d，目录中，重命名为redis_端口号，端口号指的是Redis监听的端口号。然后将脚本开头的REDISPORT变量值修改为想要监听的端口号（与文件名中的端口号相同）。
    2. 建立需要的文件夹  

        >|目录名|说明|
        >|:---|:---|
        >|/etc/redis|存放Redis的配置文件|
        >|/var/redis/\<port_number>|存放Redis的持久化文件|
    3. 修改配置文件。先将配置文件模板（上文中提到的utils目录下的redis_init_script）复制到/etc/redis/目录中，并以端口号命名，如"6379.conf",然后按照对照表 2-3中的参数进行编辑。  
   
        >|参数|值|说明|
        >|:---|:---|:---:|
        >|daemonize|yes|使Redis以守护进程模式运行|
        >|pidfile|/var/run/redis_端口号.pid|设置Redis的PID文件位置|
        >|port|端口号|设置Redis的监听端口号|
        >|dir|/var/redis/端口号|设置持久化文件存放位置|
        
        现在就可以使用/etc/init.d/redis_端口号 start来启动Redis了。可以通过执行以下命令实现Redis随系统自启动：
        ```bash
        $ sudo update-rc.d redis_端口号 defaults
        ```

## Redis停止  
- 正确停止Redis的方法是向Redis发送SHUTDOWN指令，具体为：
  ```bash
  $ redis-cli SHUTDOWN
  ```  
  Redis收到SHUTDOWN指令后，会先断开所有的客户端连接，然后根据配置执行持久化，完成后才退出。Redis能接受SIGTERM信号，所以使用kill Redis进程的PID也可以正常结束Redis，效果与SHUTDOWN命令一样。

  # Redis命令行客户端  
  Redis的命令行客户端即为redis-cli（Redis Command Line Interface），在客户端环境下可以与Redis-server交互。  

  ## 发送命令  
  使用redis-cli发送命令的方式有两种方式，第一种是将命令作为redis-cli的参数执行，比如 `redis-cli SHUTDOWN`。reids-cli执行时会自动按照默认配置(服务器地址为127.0.0.1, 端口号为6379)连接redis，可以通过-H和-P参数指定IP和端口。  
  Redis提供了PING命令来测试客户端与Redis的连接是否正常，如果连接正常会收到回复的`PONG`；  
  ```bash
  $ jupeter@XP-PC-pansf:~/download/redis/utils$ redis-cli ping
    PONG
  ```  

  第二种方式是不附带参数运行redis-cli，这时会进入交互模式，可以自由输入命令（可以理解为Reids系统的控制台）  

## 命令返回值  
1. 状态回复  
   - 状态回复：ok,pong...
   - 错误回复：如:(error) ERR unknown command 'ERRORCOMMEND'
   - 整数回复：如：`incr value` 的回复为：`1`

## 配置  
- 以某配置文件启动：  
  在启动redis-server时，将配置文件的路径作为参数:  
  ```bash
  $ redis-server <path>
  ```

  Redis提供了一个配置文件的模板，redis.conf，位于源码目录的根目录中。  
  在Redis运行时可以通过CONFIG SET 命令在不重新启动Redis的情况下动态修改Redis配置，如：
  ```bash
  redis> CONFIG SET loglevel warning
  ```
  但并不是所有的配置都可以通过CONFIG SET命令来修改，在运行时也可以通过CONFIG GET来获取配置情况。  

# 多数据库

# 控制台交互
***提示：*** redis中每一个单一命令都是原子操作，包括对复杂类型的操作。  
## 数据类型
- 普通变量
  - 声明: `set <key> <value>`、`incr <key>`（创建并设值为1）
  - 获取: `get <key>`
  - 检查是否存在: `exists <key>`： 返回0为不存在，1为存在。
  - 对于数字类型操作：
    - 加一：`incr <key>`
    - 删除变量：`del <key>`
    - 加某个值：`incrby <key> <value>`
    - 减某个值：`decrby <key> <value>`   

- 列表（list）
  - 声明：  
    `RPUSH <list_name> <value>`：从右边插入value，若\<list_name>不存在则自动创建。  
    `LPUSH <list_name> <value>`：从左边插入插入\value,与`RPUSH`操作类似。
  
  - 列出列表：`LRANGE <list_name> <start> <end>`：end为-1表示从start开始列出包括start在内及其右边的所有元素。  
  - 弹出：  
    弹出列表右边第一个元素，并返回：`RPOP <list_name>`  
    弹出列表左边第一个元素，并返回：`LPOP <list_name>`  

  - 求列表长度：`LLEN <list_name>`  

- 集合（set）
  - 添加元素（声明）：`SADD <set_name> <value1> <value2> ...` 
  - 删除集合元素：`SREM <set_name> <value>`，返回1表示删除成功，0则表示删除失败。  
  - 检测元素是否存在：`SISMEMBER <set_name> <value>`,返回1表示存在，0则相反。  
  - 合并两个集合：`SUNION <setA_name> <setB_name>`  
  - 随机弹出集合内元素：`SPOP <set_name> <弹出元素的数量>`
  - 列出集合内元素：`SMEMBERS <set_name>`

- 有序集合（sorted set）  
  和普通集合类似，但是每个元素都有一个序号，这个序号用来给元素进行排序。  
  - 添加或创建添加：`ZADD <set_name> <weight> <value1> <value2> ...`  
  - 列出集合元素（有序）：`ZRANGE <set_name> <start> <end>`  
  - ......
- 哈希表（hashes）
  哈希表
