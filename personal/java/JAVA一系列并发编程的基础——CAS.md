

CAS是一个强有力的原子操作，无需锁就可以对共享变量进行线程安全的更新。相比于通过传统的锁（sys_futex）来访问共享变量，CAS可以有更少的争用，更高的并发性。
CAS操作需要三个操作数，内存位置V，旧的预期值A，即将更新的新值B。CAS操作仅在(V)的值等于A时，才会将(V)的值更新为B，否则不会更新。整个过程是原子的。


## 底层实现
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


// Atomic::PlatformXchg 在不同CPU平台，不同操作系统以及不同操作数长度都有对应不同的实现
// 这里展示出X86架构下linux的4字节(因为int类型是4字节)的Cmpxchg操作的例子.
// 感兴趣可以自己去openJdk项目里，在src/hotspot/os_cpu 的径下找到atomix相关的实现代码
template<>
template<typename T>
inline T Atomic::PlatformCmpxchg<4>::operator()(T volatile* dest,
                                                T compare_value,
                                                T exchange_value,
                                                atomic_memory_order /* order */) const {
  STATIC_ASSERT(4 == sizeof(T));
  // %1是源操作数, (%3)是目标操作数，同时这里还蕴含了一个条件：使用eax寄存器中的值作为另一个操作数。详见下文CMPXCHG指令介绍
  __asm__ volatile ("lock cmpxchgl %1,(%3)"
                    : "=a" (exchange_value)
                    : "r" (exchange_value), "a" (compare_value), "r" (dest)
                    : "cc", "memory");
  return exchange_value;
}
```
从上面的源代码中可以看到，X86结构下"Compare and Swap"的实现是通过GCC扩展语法"__asm__ volatile"，在C++代码中内联了GCC汇编代码块，汇编代码调用cpu提供的**lock指令前缀**和**cmpxchg指令**实现的。由于是GCC的汇编模板语法，与一般见到的汇编语法有所区别，关于这部分，可以在这篇文章里了解更多。 --> [GCC内敛汇编](https://www.jianshu.com/p/1782e14a0766)

以intel处理器为代表的X86的LOCK指令前缀与CMPXCHG指令相关详细介绍，有兴趣可以可以在《Intel 64 and IA-32 Architectures Software Developer's Manual》中找到。AMD处理器同理。下面展示其中的一部分节选内容。

- CMPXCHG

> The CMPXCHG (compare and exchange) and CMPXCHG8B (compare and exchange 8 bytes) instructions are used to synchronize operations in systems that use multiple processors. The CMPXCHG instruction requires three operands: a source operand in a register, another source operand in the EAX register, and a destination operand. If the values contained in the destination operand and the EAX register are equal, the destination operand is replaced with the value of the other source operand (the value not in the EAX register). Otherwise, the originalvalue of the destination operand is loaded in the EAX register. The status flags in the EFLAGS register reflect the result that would have been obtained by subtracting the destination operand from the value in the EAX register.

> CMPXCHG（比较并交换）和CMPXCHG8B（比较并交换8个字节）指令用于在多处理器系统中同步操作。CMPXCHG指令需要三个操作数：一个在寄存器中的源操作数，另一个在EAX寄存器中的源操作数，以及一个目标操作数。如果目标操作数和EAX寄存器中包含的值相等，则目标操作数将被另一个源操作数的值（不在EAX寄存器中的值）替换。否则，目标操作数的原始值将被加载到EAX寄存器中。EFLAGS寄存器中的状态标志反映了通过从EAX寄存器中的值减去目标操作数的值所获得的结果。

> For multiple processor systems, CMPXCHG can be combined with the LOCK prefix to perform the compare and exchange operation atomically

> 对于多处理器系统，CMPXCHG 可以与 LOCK 前缀结合使用，以原子方式执行比较和交换操作

这里的LOCK指令前缀在CPU上的具体实现，涉及到总线锁，CPU缓存一致性协议等相关内容，这里也不再具体展开，感兴趣可以自行搜查相关资料。