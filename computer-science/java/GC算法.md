## JVM中的GC算法

当前市面上商业JVM的垃圾收集器，大多数都遵循了“分代收集”的理论进行设计，所谓分代收集，实际上就是一套符合大多数程序运行实际情况的经验，它建立在两个分代假说上：  

1. 弱分代假说（Weak Generational Hypothesis）：绝大多数对象都是朝生夕灭的。
2. 强分代假说（Strong Generational Hypothesis）：熬过越多次数垃圾收集过程的对象就越难以消亡。

于是这两个假说就共同奠定了多款常用垃圾收集器的一致的设计原则：收集器应该将java堆划分出不同的区域，然后将回收对象依据年龄（即对象熬过垃圾回收过程的次数）分配到不同的区域中存储。java堆划分出不同的区域之后，垃圾收集器于是可以每次只回收其中某个或者某些部分区域，因此才有了“Minor GC” 、“Major GC”、“Full GC”这样不同的回收类型；

常见的垃圾收集算法有“标记清除法”、“标记复制法”、“标记整理法”这几种。

### 标记-清除法
最早出现也是最基础的垃圾收集算法是标记-清除（Mark-Sweep）算法，在1960年由lisp之父John McCarthy提出。它的工作过程可以分为“标记”和“清除”两个阶段：首先标记出所有需要回收的对象，在标记完成之后，统一回收掉所有被标记的对象。或者标记所有存活的对象，统一回收没有被标记过的对象。

它的缺点主要有两个：
1. 执行效率不稳定，如果Java堆中包含大量对象，而且其中大部分是需要被回收的，这时需要大量的标记和清除工作。因此工作效率随着对象数量的增加而降低。
2. 存在内存空间碎片化问题，标记清除之后回产生大量不连续的内存碎片，空间碎片太多可能会导致后续程序运行过程中需要分配较大对象是无法找到足够的连续内存而提前出发GC动作。


### 标记-复制算法
为了解决标记-清除算法内存碎片化以及面对大量对象时效率低的问题，1969年Fenichel提出了一种称为“半区复制”(Semispace Copying)的垃圾收集算法，它将可用内存划分成大小相等的两块区域，每次只是用其中的一块，当这一块用完了，就将还存活复制到另一块上面，再将这块用已使用过的内存空间一次清理掉。

但是如果内存中大多数对象都是存活的，这种算法将会产生大量的内存间复制的开销，但是对于多数对象都是可回收的情况，算法需要复制到额就时占少数的存活对象，并且不会产生内存碎片。但是缺点也非常明显，可用的内存只有实际内存的一半，内存空间比较浪费。但现在商用的JVM大多数都有限采用了这种收集算法回收新生代。

在1989年，Andrew Apple针对具备"朝生夕灭"特点的对象，提出了一种更优化的“半区复制”策略，成为apple式回收。在HotSpot虚拟机的Serial, ParNew等新生代收集器均采用了这种策略来设计新生代的内存布局。具体做法是将新生代分为一块比较大的Eden空间和两块比较小的Survivor空间，每次分配内存只使用Eden空间和其中一块Survivor空间。发生GC时，将Eden空间和Survivor空间中仍然存活的对象一次性复制到另一个Survivor空间上，然后直接清理掉Eden和GC前正在使用的Survivor空间。

HotSpot中默认Eden和Survivor的大小比例时8：1。因此新生代中的实际可用内存大小是新生代的90%，即80%的Eden加上10%的Survivor。当存活的对象占用的内存超过10%时，即Survivor空间不足以容纳一次Minor GC之后存活的对象时，需要依赖其它内存区域（通常是老年代）进行分配担保（Handle Promotion）.



### 标记-整理算法
标记-复制算分在对象存活率比较高时就要进行较多的复制操作，效率就会降低


## 实现细节

### 根节点枚举

### GC Roots

### OopMap
oopMap: 存储对象中引用信息（对象中什么偏移量对应是什么类型的数据）

### 安全点

### 安全区域
指的是在某一段代码片段中，能够确保引用关系不会发生变化，因此在这个区域中任意地方开始垃圾收集都是安全的。

### 记忆集与卡表
为了解决跨代引用的问题，垃圾收集器在新生代中建立了名为记忆集（Remenbered Set）的数据结构，用以避免把整个老年代加进GC Roots扫描范围。它是一种用于记录从非收集区域指向收集区域的指针集合的抽象数据结构。只需要收集器能通过记忆集判断出某一块非收集区域是否存在指向收集区的指针就可以。

卡表：目前最常用的一种记忆集实现形式。卡表最简单的形式可以只是一个字节数组，HotSpot中就是这样实现的，下面这段伪代码就是HotSpot中默认的卡表标记逻辑。
```java
    CARD_TABLE [this address >> 9] = 0;
```
字节数组CARD_TABLE的每一个元素都对应其标识的内存区域中一块特定大小的内存块，这个内存块被成为“卡页”（Card Page），通常卡页的大小都是2的N（N为整数）次方字节。所谓卡页，就是内存分页的概念，先将内存分成大小相等的N块，因此卡表中第m个元素的状态就代表第m页内存是否存在跨代引用。因此GC时就可以可以先通过卡表快速检查某块内存是否存在跨代引用，有的再进行进一步扫描。

### 并发的可达性分析
使用到了三色标记算法，三色标记法中有：黑白灰三种颜色的节点，黑色代表对象已经被垃圾收集访问过，且这个对象的所有引用都已经扫描过。白色：标识对象未被垃圾收集器访问过。灰色：表示对象已被垃圾收集器访问过，但改对象上至少存在一个引用没有被垃圾收集器扫描过。因此可能存在两种不好的情况：

1. 本来被标记成白色的对象，标记成了黑色
2. 本来被标记成黑色的对象，标记成了白色
   
对于第一种情况，问题不大，下一次回收就可以回收掉。对于第二种情况，问题比较严重。但是第二种情况发生的前提是同时满足以下两个条件：

1. 赋值器插入了一条或以上从黑色对象到白色对象的新引用。
2. 赋值器删除了全部从灰色对象到白色对象的引用或间接引用

```sh
Non-default VM flags: -XX:CICompilerCount=2 -XX:CMSInitiatingOccupancyFraction=75 -XX:+ExplicitGCInvokesConcurrent -XX:GCLogFileSize=10485760 -XX:InitialHeapSize=536870912 -XX:+ManagementServer -XX:MaxHeapSize=1073741824 -XX:MaxNewSize=536870912 -XX:MaxTenuringThreshold=6 -XX:MinHeapDeltaBytes=196608 -XX:NewRatio=1 -XX:NewSize=268435456 -XX:NumberOfGCLogFiles=10 -XX:OldPLABSize=16 -XX:OldSize=268435456 -XX:ParallelGCThreads=2 -XX:+PrintGC -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:ReservedCodeCacheSize=134217728 -XX:+UseCMSInitiatingOccupancyOnly -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseGCLogFileRotation -XX:+UseParNewGC 
```

```bash
Non-default VM flags: -XX:CICompilerCount=2 -XX:CMSInitiatingOccupancyFraction=75 -XX:+ExplicitGCInvokesConcurrent -XX:GCLogFileSize=10485760 -XX:InitialHeapSize=536870912 -XX:+ManagementServer -XX:MaxHeapSize=1073741824 -XX:MaxNewSize=536870912 -XX:MaxTenuringThreshold=6 -XX:MinHeapDeltaBytes=196608 -XX:NewRatio=1 -XX:NewSize=268435456 -XX:NumberOfGCLogFiles=10 -XX:OldPLABSize=16 -XX:OldSize=268435456 -XX:ParallelGCThreads=2 -XX:+PrintGC -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:ReservedCodeCacheSize=134217728 -XX:+UseCMSInitiatingOccupancyOnly -XX:+UseCompressedClassPointers -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:+UseGCLogFileRotation -XX:+UseParNewGC 

Command line:  -Denv=staging -Dstaging_meta=http://logan-namor-config.logan:8888 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.rmi.port=1099 -Dcom.sun.management.jmxremote.port=1099 -Xmx1024m -Xms512m -XX:NewRatio=1 -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -XX:ReservedCodeCacheSize=128M -XX:ParallelGCThreads=2 -XX:+ExplicitGCInvokesConcurrent -Duser.timezone=Asia/Shanghai -Xloggc:/tmp/logan-gc.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=10M -Djava.security.egd=file:/dev/./urandom -Dlogan.fluid.enabled=true
```
