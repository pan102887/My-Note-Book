# AbstractQueueSynchronizer (AQS)

## 关键词

- 无锁同步器
- CAS

## 介绍

AQS提供了一种用于实现依赖于等待队列(FIFO)的阻塞锁和相关同步器（如信号量，事件......）的框架。该类可以成为大多数依赖于用单个原子int值表示状态的同步器的基础（基类）。子类中必须定义一个线程安全的方法用于修改该状态变量的值，并且它需要定义该状态变量什么时候表示该对象被获取或释放。基于这些，该类中的其他方法负责实现（carry out）所有的队列管理和阻塞机制。子类中还可以维护其他状态变量，但只有这个被`getState`,`setState`和`compareAndSet`方法访问的这个原子更新的int状态变量才会被用于同步相关的跟踪。

子类应该定义为非公有的内部helper类，用于实现器外部类的同步属性。`AQS`类不实现任何同步接口，而是定义了例如`acquireInterruptibly`这样的方法，具体的锁和相关的同步器可以根据需求调用这些方法实现他们的公共方法。

该类支持默认的独占模式和共享模式中的一种或两种。当以独占模式获取时，其他线程尝试获取资源将无法成功。而以共享模式获取时，多个线程可能（但不一定）能够成功获取。该类并不‘理解’这些模式的差异，除了在机械层面上：当共享模式获取成功时，下一个等待线程（如果存在）也必须判断它是否能够获取资源。等待线程（无论是独占模式还是共享模式）共享同一个FIFO队列。通常，子类实现只支持其中一种模式，但在某些情况下，例如在ReadWriteLock中，两种模式都可能同时发挥作用。仅支持独占模式或仅支持共享模式的子类无需定义支持未使用模式的方法。

该类定义了一个嵌套类 AbstractQueuedSynchronizer.ConditionObject，它可以作为支持独占模式的子类的 Condition 实现。对于这种子类，方法 isHeldExclusively 用于报告当前线程是否独占同步状态，方法 release 使用当前的 getState 值完全释放该对象，而方法 acquire 在给定保存的状态值后，最终将该对象恢复到之前的获取状态。AbstractQueuedSynchronizer 的其他方法不会创建这样的条件对象，因此如果无法满足这些约束，请不要使用它。AbstractQueuedSynchronizer.ConditionObject 的行为当然取决于其同步器实现的语义。

## 设计解析

AbstractQueuedSynchronizer类中，通过一个队首引用（指针）head,与一个队尾引用tail来维护一个FIFO等待队列。每个节点都是一个Node类型的对象，Node对象中包含了线程的引用以及前驱和后继节点的引用。AQS通过CAS操作来实现对head和tail的原子更新。

AQS中的head与tail。

```java
public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable {
   /// ......
   
    /**
    * Head of the wait queue, lazily initialized.  Except for
    * initialization, it is modified only via method setHead.  Note:
    * If head exists, its waitStatus is guaranteed not to be
    * CANCELLED.
    */
   private transient volatile Node head;
   
   /**
    * Tail of the wait queue, lazily initialized.  Modified only via
    * method enq to add new wait node.
    */
   private transient volatile Node tail;

   /// ......
}
```

### AQS等待队列中的节点类型——Node

Node中，成员变量有一个用于表示当前节点状态的`waitStatus`, 一个指向前驱节点的`prev`, 一个指向后继节点的`next`, 一个指向当前线程的`thread`, 以及一个指向下一个等待条件的节点的`nextWaiter`。

这里可以看到，Node对象可以处于两个不同的队列中，一个是双向的AQS等待队列，节点之间的连接由`prev`与`next`维护，另一个是单向的条件队列，队列节点之间的连接由`nextWaiter`维护。AQS的等待队列用于管理线程获取锁的顺序，而条件队列用于管理线程在等待某个条件时的顺序。

下面是JDK 8中Node类的定义中的成员变量：

```java
static final class Node {
   /**
    * Status field, taking on only the values:
    *   SIGNAL:     The successor of this node is (or will soon be)
    *               blocked (via park), so the current node must
    *               unpark its successor when it releases or
    *               cancels. To avoid races, acquire methods must
    *               first indicate they need a signal,
    *               then retry the atomic acquire, and then,
    *               on failure, block.
    *   CANCELLED:  This node is cancelled due to timeout or interrupt.
    *               Nodes never leave this state. In particular,
    *               a thread with cancelled node never again blocks.
    *   CONDITION:  This node is currently on a condition queue.
    *               It will not be used as a sync queue node
    *               until transferred, at which time the status
    *               will be set to 0. (Use of this value here has
    *               nothing to do with the other uses of the
    *               field, but simplifies mechanics.)
    *   PROPAGATE:  A releaseShared should be propagated to other
    *               nodes. This is set (for head node only) in
    *               doReleaseShared to ensure propagation
    *               continues, even if other operations have
    *               since intervened.
    *   0:          None of the above
    *
    * The values are arranged numerically to simplify use.
    * Non-negative values mean that a node doesn't need to
    * signal. So, most code doesn't need to check for particular
    * values, just for sign.
    *
    * The field is initialized to 0 for normal sync nodes, and
    * CONDITION for condition nodes.  It is modified using CAS
    * (or when possible, unconditional volatile writes).
    */
   volatile int waitStatus;
   /**
    * Link to predecessor node that current node/thread relies on
    * for checking waitStatus. Assigned during enqueuing, and nulled
    * out (for sake of GC) only upon dequeuing.  Also, upon
    * cancellation of a predecessor, we short-circuit while
    * finding a non-cancelled one, which will always exist
    * because the head node is never cancelled: A node becomes
    * head only as a result of successful acquire. A
    * cancelled thread never succeeds in acquiring, and a thread only
    * cancels itself, not any other node.
    */
   volatile Node prev;
   
   /**
    * Link to the successor node that the current node/thread
    * unparks upon release. Assigned during enqueuing, adjusted
    * when bypassing cancelled predecessors, and nulled out (for
    * sake of GC) when dequeued.  The enq operation does not
    * assign next field of a predecessor until after attachment,
    * so seeing a null next field does not necessarily mean that
    * node is at end of queue. However, if a next field appears
    * to be null, we can scan prev's from the tail to
    * double-check.  The next field of cancelled nodes is set to
    * point to the node itself instead of null, to make life
    * easier for isOnSyncQueue.
    */
   volatile Node next;
   /**
    * The thread that enqueued this node.  Initialized on
    * construction and nulled out after use.
    */
   volatile Thread thread;
   /**
    * Link to next node waiting on condition, or the special
    * value SHARED.  Because condition queues are accessed only
    * when holding in exclusive mode, we just need a simple
    * linked queue to hold nodes while they are waiting on
    * conditions. They are then transferred to the queue to
    * re-acquire. And because conditions can only be exclusive,
    * we save a field by using special value to indicate shared
    * mode.
    */
   Node nextWaiter;
}
```

#### Node的状态变量waitStatus

AQS中的等待队列中的节点类型是定义在AQS中的一个静态内部类Node。Node有一个int类型的状态变量`waitStatus`用于表示当前节点的状态。在JDK 8中，`waitStatus`的状态值有以下几种：

1. **SIGNAL (-1)**
   - **含义**：表示当前节点的后继节点已被（或即将被）阻塞，因此当前节点在释放或取消时必须唤醒其后继节点
   - **使用场景**：当一个线程获取锁失败后，会将其前驱节点的waitStatus设置为SIGNAL，表示前驱节点释放锁时需要唤醒自己

2. **CANCELLED (1)**
   - **含义**：表示当前节点已被取消，由于超时或中断导致
   - **使用场景**：当线程等待锁的过程中被中断或超时，该节点会被标记为CANCELLED状态
   - **特点**：处于此状态的节点不会再参与同步，会被从队列中移除

3. **CONDITION (-2)**
   - **含义**：表示当前节点在条件队列中等待
   - **使用场景**：当线程调用`condition.await()`时，节点会被加入到条件队列中，并标记为CONDITION状态
   - **特点**：只有在条件队列中的节点才会有此状态

4. **PROPAGATE (-3)**
   - **含义**：表示下一次共享模式的acquire操作应该无条件传播
   - **使用场景**：在共享模式下，当一个线程成功获取资源后，可能会唤醒多个后继节点
   - **特点**：主要用于共享锁的场景，如读写锁的读锁、信号量等

5. **0（初始状态）**
   - **含义**：默认状态，表示节点刚创建时的初始状态
   - **使用场景**：新创建的节点默认为此状态
   - **特点**：节点在加入队列后会根据具体情况转换为其他状态

#### Node的prev与next

Node类中，`prev`和`next`分别表示当前节点的前驱节点和后继节点。它们用于维护AQS的等待队列的双向链表结构。
`prev`指向当前节点的前一个节点，而`next`指向当前节点的后一个节点。通过这两个引用，AQS可以在等待队列中高效地插入和删除节点。

#### Node的nextWaiter

Node类中还有一个`nextWaiter`成员变量，用于表示当前节点在条件队列中的下一个等待节点。它是一个单向链表，用于管理线程在等待某个条件时的顺序。

### 从ReentrantLock中的FairLock模式的lock方法分析AQS的设计

在ReentrantLock的公平锁模式中，`lock()`方法会调用AQS的`acquire()`方法来获取锁。AQS的`acquire()`方法调用`tryAcquire()`方法尝试获取锁，如果获取失败，则会将当前线程加入到等待队列中，并阻塞当前线程。

在JDK 8的AQS中，acquire()及其相关的方法如下：

```java
static final Node EXCLUSIVE = null;

/// ......

public final void acquire(int arg) {
   if (!tryAcquire(arg) &&
         acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
      selfInterrupt();
}


/**
 * Inserts node into queue, initializing if necessary. See picture above.
 * @param node the node to insert
 * @return node's predecessor
 */
private Node enq(final Node node) {
    for (;;) {
        Node t = tail;
        if (t == null) { // Must initialize
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}

/**
 * Creates and enqueues node for current thread and given mode.
 *
 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
 * @return the new node
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode);
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) {
        node.prev = pred;
        if (compareAndSetTail(pred, node)) {
            pred.next = node;
            return node;
        }
    }
    enq(node);
    return node;
}

/**
 * Acquires in exclusive uninterruptible mode for thread already in
 * queue. Used by condition wait methods as well as acquire.
 *
 * @param node the node
 * @param arg the acquire argument
 * @return {@code true} if interrupted while waiting
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            final Node p = node.predecessor();
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

在`acquire()`这个方法中，首先尝试通过`tryAcquire(arg)`方法获取锁，如果获取失败，则调用`addWaiter(Node.EXCLUSIVE)`将当前线程加入到等待队列中，并返回一个Node节点。接着调用`acquireQueued()`方法来处理当前线程的阻塞和唤醒逻辑。如果`acquireQueued()`方法返回true，则表示当前线程被中断，需要调用`selfInterrupt()`方法来设置当前线程的中断状态。

其中`tryAcquire(arg)`方法是一个抽象方法，需要子类实现该方法来定义如何尝试获取锁。对于ReentrantLock来说，这个方法会检查当前线程是否可以获取锁，如果可以，则返回true，否则返回false。

```java
/**
 * Fair version of tryAcquire.  Don't grant access unless
 * recursive call or no waiters or is first.
 */
protected final boolean tryAcquire(int acquires) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (!hasQueuedPredecessors() &&
            compareAndSetState(0, acquires)) {
            setExclusiveOwnerThread(current);
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) {
        int nextc = c + acquires;
        if (nextc < 0)
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

## 待解决问题

1. TODO 在ReentrantLock中，如何实现公平锁与非公平锁两种模式。
2. TODO 在AQS中的Node类中，有表示节点前驱和后继的prev与next成员变量，但除此之外，还有nextWaiter, nextWaiter的作用是什么？与next有何不同
3. 在AQS中
