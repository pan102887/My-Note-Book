# 是什么
ConcurrentHashMap是Java中提供的一种线程安全的HashMap实现，它在多线程环境下能够有效地支持并发的读写操作。它是Java 5引入的，并且在Java 8中得到了显著的改进和优化。
# 什么使用场景
# 什么核心机制
ConcurrentHashMap相较于使用Collections.synchronizedMap方法得到的map，ConcurrentHashMap通过分段锁降低了锁的粒度，使得多线程访问场景下，发生阻塞的概率大大减少。
## 分段锁的实现



# 解决什么问题
# 常见问题有哪些