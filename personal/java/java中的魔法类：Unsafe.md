# Java魔法Unsafe类

## 介绍
Unsafe是java在sun.misc包下的一个类，此类主要提供了一些用于执行低级别、不安全操作的方法，比如直接访问系统内存资源，自主管理内存资源等等。Unsafe在许多并发编程场景中都有它的身影，例如ConcurrentHashMap，Netty的NIO库中等等。但是正如Unsafe名字本身那样，由于提供了类似C语言中指针一样操作内存空间的能力，这也使得随着它的使用会增加应用程序发生指针安全方面的风险，使用不当容易产生内存泄漏，指针异常等问题，使得java这种安全的语言变得不再“安全”，因此Unsafe的使用必须要谨慎。

如Unsafe中的源码所示，Unsafe类提供getInstance()获取单例对象，但仅当调用Unsafe方法的类为引导类加载器所加载时才合法，否则抛出异常。
```java
    private Unsafe() {}
    private static final Unsafe theUnsafe = new Unsafe();
    // .....
    @CallerSensitive
    public static Unsafe getUnsafe() {
        Class<?> caller = Reflection.getCallerClass();
        if (!VM.isSystemDomainLoader(caller.getClassLoader()))
            throw new SecurityException("Unsafe");
        return theUnsafe;
    }
```
因此可以通过以下两种方法获取Unsafe的对象实例，一个是通过反射，另一个是在java命令中加上```-Xbootclasspath/a```把调用Unsafe相关方法的类A所在jar的包路径追加到默认的bootstrap路径中，使A被引导类加载器加载，从而使得A可以通过`Unsafe.getInstance()`方法获取到Unsafe实例。

## Unsafe提供的API大致功能

Unsafe主要提供了系统相关、数组相关、内存屏障、对象操作、Class相关、cas操作相关、线程调度以及内存操作这几个部分的功能接口。

### 系统相关
- 返回内存页大小
- 返回系统指针的大小

### 数组相关
- 返回数组元素大小
- 返回数组首元素偏移量
  
### 内存屏障
- 禁止Load,store重排序

### 对象操作
- 获取对象成员属性在内存的偏移量
- 非常规对象实例化
- 存储，读取指定偏移地址的变量值（包含延迟生效，volatile语义）

```java
//返回对象成员属性在内存地址相对于此对象的内存地址的偏移量
public native long objectFieldOffset(Field f);
//获得给定对象的指定地址偏移量的值，与此类似操作还有：getInt，getDouble，getLong，getChar等
public native Object getObject(Object o, long offset);
//给定对象的指定地址偏移量设值，与此类似操作还有：putInt，putDouble，putLong，putChar等
public native void putObject(Object o, long offset, Object x);
//从对象的指定偏移量处获取变量的引用，使用volatile的加载语义
public native Object getObjectVolatile(Object o, long offset);
//存储变量的引用到对象的指定的偏移量处，使用volatile的存储语义
public native void putObjectVolatile(Object o, long offset, Object x);
//有序、延迟版本的putObjectVolatile方法，不保证值的改变被其他线程立即看到。只有在field被volatile修饰符修饰时有效
public native void putOrderedObject(Object o, long offset, Object x);
//绕过构造方法、初始化代码来创建对象
public native Object allocateInstance(Class<?> cls) throws InstantiationException;
```

### Class相关
- 动态创建类(普通类和匿名类)
- 获取field的内存地址偏移量
- 检测、确保类初始化


Unsafe在这一块主要提供了Class和它的静态字段相关操作的方法，包含静态字段内存定位、定义类、定义匿名类、检验&确保初始化等。
```java
// 获取给定静态字段的内存地址偏移量
public native long staticFieldOffset(Field var1);
// 获取给定的静态变量的地址
public native Object staticFieldBase(Field var1);
// 判断给定的类是否需要初始化
public native boolean shouldBeInitialized(Class<?> var1);
// 检测给定的类是否已经初始化
public native void ensureClassInitialized(Class<?> c);
// 定义一个类
public native Class<?> defineClass(String name, byte[] b, int off, int len, ClassLoader loader, ProtectionDomain protectionDomain);
// 定义匿名类
public native Class<?> defineAnonymousClass(Class<?> hostClass, byte[] data, Object[] cpPatches);
```


### CAS操作
CAS操作为Java中一系列原子操作的实现提供了重要支持，在例如Atomic一系列相关的类，AQS以及ConcurrentHashMap等场景都有广泛使用，也是实现并发算法的常用技术。那什么是CAS，CAS的全称是Compare And Swap，即比较并替换，Unsafe这里提供的CAS操作有三个操作数：内存地址，预期原始值，新值。在执行CAS操作的时候，会比较此内存位置中的值是否等于预期原始值，如果是，则将位置赋予新值并返回true，否则直接返回false。CAS的底层实现其实是依赖CPU提供的cmpxchg原子操作指令，[CAS相关内容详见此处](./CAS.md)。

#### 使用案例

正如刚刚所提到的，Unsafe提供的CAS操作在java.util.concurrent包下atomic相关的的类、AQS以及ConcurrentHashMap等等都有广泛使用。下面以java 8中的AtomicInteger类为例子，在它的static代码块中可以看到，使用了Unsafe中的objectFieldOffset方法计算出了value成员变量在AtomicInteger类中的相对位置。并在AtomicInterger提供其他的原子操作方法里，

```java
private static final long valueOffset;

static {
    try {
        valueOffset = unsafe.objectFieldOffset
            (AtomicInteger.class.getDeclaredField("value"));
    } catch (Exception ex) { throw new Error(ex); }
}

private volatile int value;

// ....
/**
 * Atomically sets the value to the given updated value
 * if the current value {@code ==} the expected value.
 *
 * @param expect the expected value
 * @param update the new value
 * @return {@code true} if successful. False return indicates that
 * the actual value was not equal to the expected value.
 */
public final boolean compareAndSet(int expect, int update) {
    return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
}
```




### 线程调度
- 线程挂起、恢复
- 获取、释放锁

关于这部分，Unsafe提供了线程阻塞，取消阻塞以及锁相关这几块功能，正如下面代码示例中注释所示，spar与unspark方法分别提供了线程的的挂起与恢复这两个功能。调用park方法后，线程将一直阻塞到超时或者中断条件出现；unpark则可以终止挂起一个线程，使其恢复正常。
而关于对象锁这块内容，在新版本的JDK中已经被删除（但是目前我们广泛使用的JDK8仍然保留）

```java
//取消阻塞线程
public native void unpark(Object thread);
//阻塞线程
public native void park(boolean isAbsolute, long time);
//获得对象锁（可重入锁）(在最新版的JDK中已经移除)
@Deprecated
public native void monitorEnter(Object o);
//释放对象锁 (在最新版的JDK中已经移除)
@Deprecated
public native void monitorExit(Object o);
//尝试获取对象锁 (在最新版的JDK中已经移除)
@Deprecated
public native boolean tryMonitorEnter(Object o);
```
#### 使用案例
在LockSupport类中，通过调用Unsafe中的park和unpark方法来实现线程的阻塞与唤醒。而被广泛用于实现无锁并发机制的AQS，就是通过LockSupport来实现线程阻塞与唤醒的。

LockSupport中的unpark
```java
    /**
     * Makes available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * {@code park} then it will unblock.  Otherwise, its next call
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * @param thread the thread to unpark, or {@code null}, in which case
     *        this operation has no effect
     */
    public static void unpark(Thread thread) {
        if (thread != null) {
            if (thread.isVirtual()) {
                VirtualThreads.unpark(thread);
            } else {
                U.unpark(thread);
            }
        }
    }
```

AQS唤醒后继节点的操作
```java
    /**
     * Wakes up the successor of given node, if one exists, and unsets its
     * WAITING status to avoid park race. This may fail to wake up an
     * eligible thread when one or more have been cancelled, but
     * cancelAcquire ensures liveness.
     */
    private static void signalNext(Node h) {
        Node s;
        if (h != null && (s = h.next) != null && s.status != 0) {
            s.getAndUnsetStatus(WAITING);
            LockSupport.unpark(s.waiter);
        }
    }
```

### 内存操作
主要有堆外内存的分配、拷贝、释放、对给定地址进行读/写操作等方法。在JAVA中创建的对象通常都是出于堆内存中，堆内存由JVM管控，遵循JVM的GC内存管理机制。相对的，堆外内存则不属于JVM的管控范围，java对于堆外内存的管理依赖于Unsafe所提供的一系列操作堆外内存的native方法。

- 分配、拷贝、释放**堆外**内存
- 设置、获取给定地址中的值

在目前我们普遍使用的java 8版本中，Unsafe关于内存操作的方法主要如下
```java
//分配内存, 类似于C中的malloc函数
public native long allocateMemory(long bytes);
//重新分配内存到bytes大小，并将原address内存块上的内容拷贝到新的内存块中，并返回新内存块起始地址
public native long reallocateMemory(long address, long bytes);
//释放已分配的内存，类似于C中的free
public native void freeMemory(long address);
//在给定的内存块中设置值，类似C中的memset
public native void setMemory(Object o, long offset, long bytes, byte value);
//内存拷贝，类似于C中stdlib里的memcpy函数，
public native void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes);
//获取给定地址值，忽略修饰限定符的访问限制。与此类似操作还有: getInt，getDouble，getLong，getChar等
public native Object getObject(Object o, long offset);
//为给定地址设置值，忽略修饰限定符的访问限制，与此类似操作还有: putInt,putDouble，putLong，putChar等
public native void putObject(Object o, long offset, Object x);
//获取给定地址的byte类型的值（当且仅当该内存地址为allocateMemory分配时，此方法结果为确定的）
public native byte getByte(long address);
//为给定地址设置byte类型的值（当且仅当该内存地址为allocateMemory分配时，此方法结果才是确定的）
public native void putByte(long address, byte x);
// ... 等等一些列put/get操作
```

#### 什么使用会使用到堆外内存
- 提升程序I/O性能：在常见的的I/O通信过程中，会存在将堆内内存拷贝到堆外内存的操作，而对于需要频繁进行内存数据拷贝并且生命周期比较短的暂存数据，直接存放到堆外内存。
- 避免GC的影响：如果有存在生命周期很长，不需要GC管理的对象，使用堆外内存可以避免GC带来的性能影响。
- 性能优化：在性能敏感的应用中，使用堆外内存可以减少GC的频率和时间，从而提高应用性能。
- ....

#### 典型案例

被广泛用于通信缓冲池的DirectByteBuffer中，其里面用于堆外内存的创建、使用、销毁等逻辑均通过Unsafe提供的api来实现。如其构造函数中给出的代码所示，在创建DirectByteBuffer时，通过Unsafe中的allocateMemory来进行内存的分配，并通过setMemory来进行内存的初始化。并在最后创建Cleaner对象，用于跟踪此DirectByteBuffer对象的垃圾回收，当此DirectByteBuffer对象被回收时，分配的堆外内存将被Deallocator释放。Cleaner的机制[详情请见](Cleaner的工作原理.md)

```java
// Primary constructor
//
DirectByteBuffer(int cap) {                   // package-private

        super(-1, 0, cap, cap, null);
        boolean pa = VM.isDirectMemoryPageAligned();
        int ps = Bits.pageSize();
        long size = Math.max(1L, (long)cap + (pa ? ps : 0));
        Bits.reserveMemory(size, cap);

        long base = 0;
        try {
            base = UNSAFE.allocateMemory(size);
        } catch (OutOfMemoryError x) {
            Bits.unreserveMemory(size, cap);
            throw x;
        }
        UNSAFE.setMemory(base, size, (byte) 0);
        if (pa && (base % ps != 0)) {
            // Round up to page boundary
            address = base + ps - (base & (ps - 1));
        } else {
            address = base;
        }
        try {
            cleaner = Cleaner.create(this, new Deallocator(base, size, cap));
        } catch (Throwable t) {
            // Prevent leak if the Deallocator or Cleaner fail for any reason
            UNSAFE.freeMemory(base);
            Bits.unreserveMemory(size, cap);
            throw t;
        }
        att = null;
    }
```