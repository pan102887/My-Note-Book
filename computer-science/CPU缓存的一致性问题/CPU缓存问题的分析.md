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
4. **Invalid:** cache line 在当前CPU的cache中是无效的。

    |     | M   | E   | S   | I   |
    | --- | --- | --- | --- | --- |
    | M   | ×   | ×   | ×   | √   |
    | E   | ×   | ×   | ×   | √   |
    | S   | ×   | ×   | √   | √   |
    | I   | √   | √   | √   | √   |


在多核心CPU中，由于每个核心都有自己的缓存（如L1、L2缓存），当多个核心访问同一内存地址时，可能会导致缓存中数据不一致的问题（Cache Coherence）。为了解决这一问题，各大厂商采用了不同的缓存一致性协议和硬件机制。以下是主要厂商的解决方案：

---
## 不同厂商对CPU缓存一致性问题的解决方案

### **1. Intel 的解决方案**
#### **MESIF 协议**
- Intel 在其多核处理器中使用了改进的 MESI 协议，称为 **MESIF**（Modified, Exclusive, Shared, Invalid, Forward）。
- **特点**：
  - 增加了 `Forward` 状态，用于优化共享数据的读取操作。
  - 当多个核心共享同一缓存行时，指定一个核心为“Forward”状态，负责向其他核心提供数据，减少总线流量。
- **实现机制**：
  - 使用 **Snoop-based Cache Coherence**（基于嗅探的缓存一致性）：
    - 每个核心监听总线上的内存操作（如读、写、失效请求）。
    - 当一个核心修改缓存行时，其他核心会将对应的缓存行标记为无效（Invalid）。

#### **Intel QuickPath Interconnect (QPI)**
- 在多插槽系统中，Intel 使用 QPI 协议在不同处理器之间维护缓存一致性。
- QPI 提供点对点高速互连，支持跨插槽的缓存一致性。

---

### **2. AMD 的解决方案**
#### **MOESI 协议**
- AMD 使用 **MOESI**（Modified, Owned, Exclusive, Shared, Invalid）协议。
- **特点**：
  - 增加了 `Owned` 状态，用于优化共享数据的写操作。
  - 当多个核心共享同一缓存行时，`Owned` 状态允许一个核心持有最新数据，同时其他核心保持共享状态，减少写回主存的开销。
- **实现机制**：
  - 使用 **Directory-based Cache Coherence**（基于目录的缓存一致性）：
    - 每个缓存行的状态由一个全局目录管理，记录哪些核心持有该缓存行及其状态。
    - 目录减少了广播失效消息的需求，提高了扩展性。

#### **Infinity Fabric**
- AMD 的 Infinity Fabric 是一种高带宽互连技术，用于在多核心和多插槽系统中维护缓存一致性。
- 它支持动态路由和高效的数据传输，适合大规模多核系统。

---

### **3. ARM 的解决方案**
#### **MESI 和 CCI（Cache Coherent Interconnect）**
- ARM 在其多核处理器中使用 MESI 协议，并通过 **CCI（Cache Coherent Interconnect）** 实现缓存一致性。
- **特点**：
  - CCI 是一种硬件互连组件，负责在多个核心之间维护缓存一致性。
  - ARM 的 CCI 支持多种一致性协议（如 MESI、MOESI），具体取决于处理器设计。

#### **ARM Coherent Mesh Network (CMN)**
- 在高性能处理器（如 ARM Neoverse 系列）中，ARM 使用 **CMN**（一致性网格网络）来实现缓存一致性。
- CMN 提供高扩展性，适合大规模多核系统。

---

### **4. IBM 的解决方案**
#### **Snoopy 和 Directory 混合机制**
- IBM 在其 Power 系列处理器中使用了基于嗅探和目录的混合机制。
- **特点**：
  - 在小规模多核系统中使用嗅探机制。
  - 在大规模多核系统中使用目录机制，减少广播流量。

#### **IBM Power 系列的创新**
- IBM 的 Power 系列处理器支持硬件事务内存（HTM），通过事务机制进一步优化缓存一致性。

---

### **5. NVIDIA 的解决方案**
#### **GPU 的缓存一致性**
- NVIDIA 的 GPU 通常不使用传统的 MESI/MOESI 协议，而是通过软件和硬件协同维护一致性。
- **特点**：
  - GPU 的缓存一致性通常由编程模型（如 CUDA）管理。
  - 在多 GPU 系统中，使用 NVLink 提供高带宽互连，并支持缓存一致性。

---

### **6. 通用解决方案：缓存一致性协议**
#### **MESI 协议**
- Modified, Exclusive, Shared, Invalid。
- 是最基础的缓存一致性协议，广泛应用于多核处理器。

#### **MOESI 协议**
- 增加了 `Owned` 状态，优化了共享数据的写操作。

#### **MESIF 协议**
- 增加了 `Forward` 状态，优化了共享数据的读取操作。

#### **Directory-based Cache Coherence**
- 使用全局目录记录缓存行的状态，减少广播流量，适合大规模多核系统。

#### **Snoop-based Cache Coherence**
- 每个核心监听总线上的内存操作，适合小规模多核系统。

---

### **总结**
- **Intel**：使用 MESIF 协议和 QPI 技术，适合高性能多核和多插槽系统。
- **AMD**：使用 MOESI 协议和 Infinity Fabric，优化共享数据的写操作。
- **ARM**：通过 CCI 和 CMN 提供高效的缓存一致性，适合嵌入式和高性能计算。
- **IBM**：结合嗅探和目录机制，适合大规模多核系统。
- **NVIDIA**：通过 NVLink 和编程模型管理 GPU 的缓存一致性。

该部分内容由AI总结生成，可能存在错误或不准确之处，请读者自行查证。

