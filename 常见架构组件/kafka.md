# kafka

## 为何kafka使用磁盘对数据进行持久化处理依然能很快

核心在于kafka磁盘写入优化为顺序写入即顺序I/O模式，从而充分利用了磁盘顺序写入大吞吐量的优势，避免了寻道延迟。

在其官方文档中也有提到，在一个由六块7200转/分的磁盘组成的raid-5系统中，顺序写入性能能达到 600MB/sec, 但是随机写入只有 100kB/sec

> The key fact about disk performance is that the throughput of hard drives has been diverging from the latency of a disk seek for the last decade.As a result the performance of linear writes on a JBOD configuration with six 7200rpm SATA RAID-5 array is about 600MB/sec but the performance of random writes is only about 100k/sec—a difference of over 6000X
