# 1. CPU缓存问题的分析

## CPU缓存的发展

Early examples of CPU caches include the Atlas 2[3] and the IBM System/360 Model 85[4][5] in the 1960s. The first CPUs that used a cache had only one level of cache; unlike later level 1 cache, it was not split into L1d (for data) and L1i (for instructions). Split L1 cache started in 1976 with the IBM 801 CPU,[6][7] became mainstream in the late 1980s, and in 1997 entered the embedded CPU market with the ARMv5TE. In 2015, even sub-dollar SoCs split the L1 cache. They also have L2 caches and, for larger processors, L3 caches as well. The L2 cache is usually not split, and acts as a common repository for the already split L1 cache. Every core of a multi-core processor has a dedicated L1 cache and is usually not shared between the cores. The L2 cache, and higher-level caches, may be shared between the cores. L4 cache is currently uncommon, and is generally dynamic random-access memory (DRAM) on a separate die or chip, rather than static random-access memory (SRAM). An exception to this is when eDRAM is used for all levels of cache, down to L1. Historically L1 was also on a separate die, however bigger die sizes have allowed integration of it as well as other cache levels, with the possible exception of the last level. Each extra level of cache tends to be bigger and optimized differently.

Caches (like for RAM historically) have generally been sized in powers of: 2, 4, 8, 16 etc. KiB; when up to MiB sizes (i.e. for larger non-L1), very early on the pattern broke down, to allow for larger caches without being forced into the doubling-in-size paradigm, with e.g. Intel Core 2 Duo with 3 MiB L2 cache in April 2008. This happened much later for L1 caches, as their size is generally still a small number of KiB. The IBM zEC12 from 2012 is an exception however, to gain unusually large 96 KiB L1 data cache for its time, and e.g. the IBM z13 having a 96 KiB L1 instruction cache (and 128 KiB L1 data cache),[8] and Intel Ice Lake-based processors from 2018, having 48 KiB L1 data cache and 48 KiB L1 instruction cache. In 2020, some Intel Atom CPUs (with up to 24 cores) have (multiple of) 4.5 MiB and 15 MiB cache sizes.[9][10]

## 不同CPU缓存架构带来的问题分析

1. **单核心，无缓存:** 直接从主存中读写数据，不存在一致性问题，但是速度较慢。

2. **单核心单级缓存:** CPU不会直接访问主存，而是先访问一级缓存（L1 Cache）。如果数据在L1 Cache中，则直接使用；否则，从主存中加载到L1 Cache。此时，缓存与主存之间可能会出现不一致的情况，

3. **多核心共享单级缓存** 不确定是否存在这种架构，

### 单核心多级缓存



1. **单核无缓存CPU** 
   - 直接从主存读取数据，速度较慢。
   - 适用于简单的计算任务。

2. **单核单级缓存CPU(one level of cache)**
   - 引入了一级缓存（L1 Cache），提高了数据访问速度。
   - L1 Cache通常分为指令缓存和数据缓存。

3. **单核多级缓存CPU**
   - 增加了二级缓存（L2 Cache）和可能的三级缓存（L3 Cache）。
   - 多级缓存结构可以更好地平衡速度和容量。

4. **多核多级缓存CPU**
    - 每个核心都有自己的L1和L2缓存，可能共享L3缓存。
    - 需要处理多核之间的缓存一致性问题。


## 多核心独立单级缓存

假设某个系统有两个CPU，每个CPU都有自己的L1缓存，并且`cache line`的大小为`64 bytes`。现在两个CPU都试图同时访问地址为`Ox40`的内存数据，并导致起始地址为`0x40`的64字节缓存行被加载到各自的L1缓存中。

|      | main |     |     |
| ---- | ---- | --- | --- |
| 0x00 | xx   | ... | 00  |
| 0x40 | xx   | ... | 00  |
| 0x80 | xx   | ... | 00  |

|      | CPU0 |     |     |
| ---- | ---- | --- | --- |
| 0x40 | xx   | ... | 00  |

|      | CPU1 |     |     |
| ---- | ---- | --- | --- |
| 0x40 | xx   | ... | 00  |

CPU0 现在修改了地址为 `0x40` 的数据，将其值改为 `0x01`。此时，CPU0与CPU1的L1缓存中的数据变为：
|      | CPU0 |     |     |
| ---- | ---- | --- | --- |
| 0x40 | 00   | ... | 01  |

|      | CPU1 |     |     |
| ---- | ---- | --- | --- |
| 0x40 | xx   | ... | 00  |

CPU1 仍然认为它的L1缓存中有地址为 `0x40` 的数据，并且它的值仍然是 `0x00`。如果CPU1现在尝试读取地址为 `0x40` 的数据，它将得到过时的数据，这就是缓存一致性问题。



## 总线监听，Bus-Snooping

总线监听在1983年被Ravishankar与 Goodman提出。它的基本思想是每个CPU都监听总线上的数据传输，并根据需要更新自己的缓存。每当一个CPU修改了缓存中的数据，它会在总线上广播这个修改的消息，其他CPU监听到这个消息后可以根据这个消息来更新自己的缓存。

### Bus-Snooping的两种类型

1. **Write-Update**: 当某个CPU向某个cache block执行写入操作时，其他cache监听到该消息后，如果存在相同地址的cache block，则更新该cache block中内容的值.这种方式需要将更新的值在bus上广播，这通常会导致更大的BUS带宽占用，所以这种方式不怎么常用。

    `dragon`和`Firefly`协议都属于这种类型。

2. **Write-Invalidate**: 这是最常见的总线监听方案，当处理器向cache bolck执行写入操作时，其他的cache监听到该消息后，将相同地址的cache block标记为无效（invalid）

    `Write-through`, `Write-once`, `MSI`, `MESI`, `MOSI`, `MOESI`以及`MESIF`协议都属于这种类型。

### MESI协议

MESI协议也成为`Illinois`协议，MESI中的四个字母代表四种cache line的状态，分别是：

1. **Modified:** cache line 只存在于当前CPU的cache中，并且已经被修改（是Dirty的），主存中的数据是过时的。
2. **Exclusive:** cache line只存在于当前CPU的cache中，并且与主存中的数据一致（是Clean的）。
3. **Shared:** cache line 可能存在于多个CPU的cache中，并且与主存中的数据一致（是Clean的）。
4. **Invalid:** cache line 在当前CPU的cache中是无效的，必须从其他cache或主存中获取数据。

    |     | M   | E   | S   | I   |
    | --- | --- | --- | --- | --- |
    | M   | ×   | ×   | ×   | √   |
    | E   | ×   | ×   | ×   | √   |
    | S   | ×   | ×   | √   | √   |
    | I   | √   | √   | √   | √   |