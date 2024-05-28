# volatile 关键字

## 摘要
当一个变量被声明为 volatile 时，编译器和运行时系统必须确保每次对该变量的读写操作都是直接与主内存交互的，而不是使用可能存在于CPU缓存中的值。

在没有使用 volatile 关键字的情况下，由于编译器优化和CPU缓存的存在，一个线程对变量的修改可能不会立即反映在其他线程中。这可能导致不一致的读取结果，因为其他线程可能读取到的是过时的缓存值。