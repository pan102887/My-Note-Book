# ziplist源码分析
## 1. ziplist的结构
ziplist是redis的一种数据结构，用来存储列表和哈希的数据，ziplist是一种压缩列表，它将多个元素紧密地放在一起，节省了内存空间，ziplist的结构如下：
```c
typedef struct {
    /* When string is used, it is provided with the length (slen). */
    unsigned char *sval;
    unsigned int slen;
    /* When integer is used, 'sval' is NULL, and lval holds the value. */
    long long lval;
} ziplistEntry;
```

ziplist的头部和尾部都有一个特殊的entry，这个entry的sval指向NULL，slen指向0，lval指向0，这个entry的作用是标识ziplist的头部和尾部，ziplist的结构如下：
```c
typedef struct ziplist {
    // ziplist的长度，不包括zlend
    unsigned int zlbytes;
    // ziplist的entry个数，不包括zlend
    unsigned int zllength;
    // ziplist的尾部entry的偏移量
    unsigned char tail[1];
    // ziplist的头部entry的偏移量
    unsigned char head[1];
    // ziplist的末尾标识
    unsigned char zlend;
} ziplist;
```
