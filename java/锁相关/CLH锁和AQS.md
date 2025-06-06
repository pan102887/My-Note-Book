# 1. 摘要
CLH (Craig, Landin, and Hagersten) 锁是对自旋锁的一种改进，有效的解决了以上的两个缺点。首先它将线程组织成一个队列，保证先请求的线程先获得锁，避免了饥饿问题。其次锁状态去中心化，让每个线程在不同的状态变量中自旋，这样当一个线程释放它的锁时，只能使其后续线程的高速缓存失效，缩小了影响范围，从而减少了 CPU 的开销。

CLH 锁数据结构很简单，类似一个链表队列，所有请求获取锁的线程会排列在链表队列中，自旋访问队列中前一个节点的状态。当一个节点释放锁时，只有它的后一个节点才可以得到锁。CLH 锁本身有一个队尾指针 Tail，它是一个原子变量，指向队列最末端的 CLH 节点。每一个 CLH 节点有两个属性：所代表的线程和标识是否持有锁的状态变量。当一个线程要获取锁时，它会对 Tail 进行一个 getAndSet 的原子操作。该操作会返回 Tail 当前指向的节点，也就是当前队尾节点，然后使 Tail 指向这个线程对应的 CLH 节点，成为新的队尾节点。入队成功后，该线程会轮询上一个队尾节点的状态变量，当上一个节点释放锁后，它将得到这个锁。

# 2. 自旋锁

自旋锁是互斥锁的一种实现，在java中可以这样实现
```java
public class SpinLock {
    private AtomicReference<Thread> owner = new AtomicReference<Thread>();

    public void lock() {
        Thread currentThread = Thread.currentThread();
        // 如果锁未被占用，设置当前线程为锁的拥有者
        while (!owner.compareAndSet(null, currentThread));
    }

    public void unlock() {
        Thread currentThread = Thread.currentThread();
        // 只有锁的拥有者才能解锁
        owner.compareAndSet(currentThread, null);
    }
}
```
## 2.1. 自旋锁的优缺点
自旋锁实现简单，同时避免了操作系统进程调度和线程上下文切换的开销，但他有两个缺点：

第一个是锁饥饿问题。在锁竞争激烈的情况下，可能存在一个线程一直被其他线程”插队“而一直获取不到锁的情况。

第二是性能问题。在实际的多处理上运行的自旋锁在锁竞争激烈时性能较差。自旋锁锁状态中心化，在竞争激烈的情况下，锁状态变更会导致多个 CPU 的高速缓存的频繁同步，从而拖慢 CPU 效率(这里涉及到 CPU 底层的一些知识，这里不再展开)。

因此自旋锁适用


# 3. CLH锁
## 3.1. CLH锁的java实现

```java
public class CLH {
    private final ThreadLocal<Node> node = ThreadLocal.withInitial(Node::new);
    private final AtomicReference<Node> tail = new AtomicReference<>(new Node());
    private static class Node {
        private volatile boolean locked;
    }

    public void lock() {
        Node node = this.node.get();
        node.locked = true;
        Node pre = this.tail.getAndSet(node);
        while (pre.locked);
    }

    public void unlock() {
        final Node node = this.node.get();
        node.locked = false;
        this.node.set(new Node());
    }
}
```

1. Node类中修饰locked的volitale关键字的作用  
   使用 volatile 修饰状态变量不是为了利用 volatile 的内存可见性，因为这个状态变量只会被持有该状态变量的线程写入，只会被队列中该线程的后驱节点对应的线程读，而且后者会轮询读取。因此，可见性问题不会影响锁的正确性。以上面的例子为例，T2 会不断轮询T1的状态变量，T1 将它的状态变更为 False 时 T2 没有立即感知也没有关系。该状态变量最终会写回内存并被 T2 终感知到变更后的值。

    但要实现一个可以在多线程程序中正确执行的锁，还需要解决重排序问题。在《Java 并发编程实战》一书对于重排序问题是这么描述的：在没有同步的情况下，编译器、处理器以及运行时等都可能对操作的执行顺序进行一些意想不到的调整。在缺乏足够同步的多线程程序中，要想对内存操作的执行顺序进行判断，几乎无法得到正确的结论。对于 Java synchronized 关键字提供的内置锁(又叫监视器)，Java Memory Model（下称 JMM）规范中有一条 Happens-Before（先行发生）规则：“一个监视器锁上的解锁发生在该监视器锁的后续锁定之前”，因此 JVM 会保证这条规则成立。

    而自定义互斥锁就需要自己保证这一规则的成立，因此上述代码通过 volatile 的 Happens-Before（先行发生）规则来解决重排序问题。JMM 的 Happens-Before（先行发生）规则有一条针对 volatile 关键字的规则：“volatile 变量的写操作发生在该变量的后续读之前”。

## 3.2. CLH 锁的优缺点
CLH 锁作为自旋锁的改进，有以下几个优点：

1. 性能优异，获取和释放锁开销小。CLH 的锁状态不再是单一的原子变量，而是分散在每个节点的状态中，降低了自旋锁在竞争激烈时频繁同步的开销。在释放锁的开销也因为不需要使用 CAS 指令而降低了。

2. 公平锁。先入队的线程会先得到锁。

3. 实现简单，易于理解。

扩展性强。下面会提到 AQS 如何扩展 CLH 锁实现了 j.u.c 包下各类丰富的同步器。

当然，它也有两个缺点：第一是因为有自旋操作，当锁持有时间长时会带来较大的 CPU 开销。第二是基本的 CLH 锁功能单一，不改造不能支持复杂的功能。

# 4. AQS 对 CLH的改造
针对 CLH 的缺点，AQS 对 CLH 队列锁进行了一定的改造。针对第一个缺点，AQS 将自旋操作改为阻塞线程操作。针对第二个缺点，AQS 对 CLH 锁进行改造和扩展，原作者 Doug Lea 称之为“CLH 锁的变体”。下面将详细讲 AQS 底层细节以及对 CLH 锁的改进。AQS 中的对 CLH 锁数据结构的改进主要包括三方面：扩展每个节点的状态、显式的维护前驱节点和后继节点以及诸如出队节点显式设为 null 等辅助 GC 的优化。正是这些改进使 AQS 可以支撑 j.u.c 丰富多彩的同步器实现。



# 5. 相关文章
[AQS核心CLH锁](https://mp.weixin.qq.com/s/jEx-4XhNGOFdCo4Nou5tqg) 

