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

## 功能介绍
Unsafe所提供的功能大致如下

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
- 存储，读取指定偏移地址的变量值（包含延迟生效，volatile语义）、

### Class相关
- 动态创建类(普通类和匿名类)
- 获取field的内存地址偏移量
- 检测、确保类初始化

关于这部分，Unsafe主要提供Class和它的静态字段的操作相关方法，包含静态字段内存定位、定义类、定义匿名类、检验&确保初始化等。

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

openJDK中关于CAS操作实现的相关JNI C++代码
```c++
#define UNSAFE_ENTRY_SCOPED(result_type, header) \
  JVM_ENTRY(static result_type, header) \
  if (thread->has_async_exception_condition()) {return (result_type)0;}


/**
 * 根据对象地址以及成员变量内存在类中偏移量计算成员变量实际内存地址
 * @param p             java对象地址（指针）
 * @param field_offset  成员变量偏移量
 */
static inline void* index_oop_from_field_offset_long(oop p, jlong field_offset) {
  assert_field_offset_sane(p, field_offset);
  uintptr_t base_address = cast_from_oop<uintptr_t>(p);
  uintptr_t byte_offset  = (uintptr_t)field_offset_to_byte_offset(field_offset);
  // 成员变量实际地址 = java对象地址 + 此成员变量的偏移量
  return (void*)(base_address + byte_offset);
}


// Unsafe中的    public final native boolean compareAndSetInt(Object o, long offset, int expected, int x) 方法的入口
UNSAFE_ENTRY_SCOPED(jboolean, Unsafe_CompareAndSetInt(JNIEnv *env, jobject unsafe, jobject obj, jlong offset, jint e, jint x)) {
  oop p = JNIHandles::resolve(obj);
  // 根据对象地址以及偏移量计算得到 将要进行CAS操作的int变量地址
  volatile jint* addr = (volatile jint*)index_oop_from_field_offset_long(p, offset);
  // 调用Atomic类下的cmpxchg方法
  return Atomic::cmpxchg(addr, e, x) == e;
} UNSAFE_END

/* --------- Atomic类下面的cmpxchg实现 ----------*/
class Atomic : AllStatic {
public:
  // Performs atomic compare of *dest and compare_value, and exchanges
  // *dest with exchange_value if the comparison succeeded. Returns prior
  // value of *dest. cmpxchg*() provide:
  // <fence> compare-and-exchange <membar StoreLoad|StoreStore>

  /* 这里对cmpxchg方法进行了定义，可以看到，是一个模板方法 */
  template<typename D, typename U, typename T>
  inline static D cmpxchg(D volatile* dest,
                          U compare_value,
                          T exchange_value,
                          atomic_memory_order order = memory_order_conservative);

private:

  // Dispatch handler for cmpxchg.  Provides type-based validity
  // checking and limited conversions around calls to the
  // platform-specific implementation layer provided by
  // PlatformCmpxchg.
  /* 这里插个眼，后面的cmpxchg实现代码中会用到这个CmpxchgImpl */
  template<typename D, typename U, typename T, typename Enable = void>
  struct CmpxchgImpl;
};

template<typename D, typename U, typename T>
inline D Atomic::cmpxchg(D volatile* dest,
                         U compare_value,
                         T exchange_value,
                         atomic_memory_order order) {
  /* 这里可以看到cmpxchg方法这里调用到了刚刚提到的CmpxchgImpl */
  return CmpxchgImpl<D, U, T>()(dest, compare_value, exchange_value, order);
}

// Handle cmpxchg for pointer types.
//
// The destination's type and the compare_value type must be the same,
// ignoring cv-qualifiers; we don't care about the cv-qualifiers of
// the compare_value.
//
// The exchange_value must be implicitly convertible to the
// destination's type; it must be type-correct to store the
// exchange_value in the destination.
template<typename D, typename U, typename T>
struct Atomic::CmpxchgImpl<
  D*, U*, T*,
  typename EnableIf<Atomic::IsPointerConvertible<T*, D*>::value &&
                    std::is_same<std::remove_cv_t<D>,
                                 std::remove_cv_t<U>>::value>::type>
{
  D* operator()(D* volatile* dest, U* compare_value, T* exchange_value,
               atomic_memory_order order) const {
    // Allow derived to base conversion, and adding cv-qualifiers.
    D* new_value = exchange_value;
    // Don't care what the CV qualifiers for compare_value are,
    // but we need to match D* when calling platform support.
    D* old_value = const_cast<D*>(compare_value);
    // 这里
    return PlatformCmpxchg<sizeof(D*)>()(dest, old_value, new_value, order);
  }
};

// Define the class before including platform file, which may specialize
// the operator definition.  No generic definition of specializations
// of the operator template are provided, nor are there any generic
// specializations of the class.  The platform file is responsible for
// providing those.
template<size_t byte_size>
struct Atomic::PlatformCmpxchg {
  template<typename T>
  T operator()(T volatile* dest,
               T compare_value,
               T exchange_value,
               atomic_memory_order order) const;
};


// Atomic::PlatformXchg 在不同CPU平台，不同操作系统，不同操作数长度都有对应不同的实现
// 这里只找出以最常见的X86架构下linux系统32位(因为int类型是32位)为例子
// 感兴趣可以自己去openJdk项目里，在src/hotspot/op_cpu 的包路径下找到想查阅的实现代码
template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order /* order */) const {
  STATIC_ASSERT(4 == sizeof(T));
  __asm__ volatile ("lock cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest)
                    : "cc", "memory");
  return exchange_value;
}

```

### 线程调度
- 线程挂起、恢复
- 获取、释放锁

关于这部分，Unsafe提供的方法主要有以下内容，正如代码注释中所示，spar与unspark方法可以实现现成的挂起与恢复。调用park方法后，线程将一直阻塞到超时或者中断条件出现；unpark则可以终止挂起一个线程，使其恢复正常。
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
在LockSupport类中，实现线程的阻塞(park)和唤醒(unpark)就是通过调用Unsafe中的park和unpark实现的。而LockSupport也被JAVA的锁和同步核心类AQS中用于实现线程的阻塞和唤醒。

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