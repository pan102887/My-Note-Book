# mysql 

## 事务的隔离级别

Mysql的事务有如下隔离级别

- 读未提交
- 读已提交
- 可重复读
- 串行化

### 读未提交

事务中的修改即使未提交，对其他事务也是可见的。事务可以读取未提交的数据。

### 读已提交


## InnoDB 多版本控制

InnoDB是一个多版本并发控制的存储引擎。它会保存被修改行的旧版本信息，以支持事务的并发和回滚。这信息会保存在undo 表空间(undo tablespaces)中的一个称为“rollback segment”的数据结构中。详见第[17.6.3.4章 “Undo Tablespaces”](https://dev.mysql.com/doc/refman/8.4/en/innodb-undo-tablespaces.html)。InnoDB使用这些旧版本信息来实现一致性读（consistent reads），允许事务读取在其开始之前已经提交的数据版本，从而避免了读写冲突。也用于执行事务回滚所需要的撤销操作。详见[17.7.2.3章节 “非阻塞一致性读”](https://dev.mysql.com/doc/refman/8.4/en/innodb-consistent-read.html)

在InnoDB内部，对于数据库中的每行数据都增加了三个字段：

- `DB_TRX_ID`（6字节）, 该字段记录了最后修改或插入改行数据的事务ID。此外，删除操作在InnoDB内部也被看做为一种修改操作。
- `DB_ROLL_PTR`（7字节）, 该字段指向rollback segment中的一条undo log记录。如果该行数据已经被更新，该记录包含了将这条数据恢复到更新前状态所需的必要信息。
- `DB_ROW_ID`（6字节），行ID随着新行的插入单调递增。如果InnoDB自动生成聚簇索引，则聚簇索引中将包含该字段，否则字段不会出现在任何索引中。

rollback segment中的undo log被分为 insert 和 update 两个部分：

- insert undo log: 仅用于事务回滚，事务提交后即可丢弃。
- update undo log: 用于一致性读，只有在没有事务需要使用快照进行一致性读时才能丢弃。

关于更多undo log的细节，请参见[17.6.6章节 “Undo Logs”](https://dev.mysql.com/doc/refman/8.4/en/innodb-undo-logs.html)。

**mysql文档中的重要建议**：建议你定期提交事务，包括那些用于一致性读的只读事务。否则InnoDB不能将update undo log丢弃，从而导致 rollback segment过大，以至于填满整个undo tablespace。更多关于undo tablespace的信息，请参见[17.6.3.4章节 “Undo Tablespaces”](https://dev.mysql.com/doc/refman/8.4/en/innodb-undo-tablespaces.html)。

rollback segment中的一个 undo log记录的物理大小，通常小于对应被插入或者更新的行。

在InnoDB的多版本机制中，使用SQL DELETE语句时，行不会立即被物理删除。只有当delete操作对应的update undo log被丢弃后，行以及其对应的索引才会被物理删除。这种删除操作被称为purge，它非常快，通常与执行删除操作的SQL语句花费的时间相当。

如果你以同样的速率在表中同时插入或删除小批量的数据，purge线程可能滞后，由于这些未能及时删除的dead row会导致表不断增大，会导致所有的操作都变成磁盘绑定，并且非常缓慢。这种情况下，限制新行操作，并且通过调整`innodb_max_purge_lag`系统变量为purge thread分配更多资源。更多内容请参见[17.8.9章节 “Purge Configuration”](https://dev.mysql.com/doc/refman/8.4/en/innodb-purge-configuration.html)。

### 多版本控制和二级索引

InnoDB的多版本并发控制（MVCC）处理二级索引的方式与处理聚簇索引的方式不同。

- 聚簇索引：聚簇索引中的记录是就地更新的，并且它们的隐藏系统列（hidden system columns）指向 undo log记录，可以从中筹建旧版本的数据。
- 二级索引：二级索引中不包含隐藏系统列，因此无法直接指向 undo log记录，不会被就地更新。相反，二级索引中的每条记录包含了对应聚簇索引的主键值。要获取旧版本的数据，InnoDB首先使用二级索引查找对应的聚簇索引记录，然后通过聚簇索引记录中的隐藏系统列找到相应的 undo log记录。

当一个二级索引列被更新时，旧的二级索引记录会被标记为 'delete-marked'，当新的记录插入后，这些被标记为'delete-marked'的记录才被最终删除。当二级索引被标记为'delete-marked'或者二级索引页被另一个更新的事务更新时，InnoDB会在聚簇索引中寻找对应的记录。如果记录在读事务发起后被修改，InnoDB会在聚簇索引中检查记录的`DB_TRX_ID`字段，并且在undo log中检索出正确版本的记录。

如果二级索引被标记为删除或二级索引页被另一个更新的事务更新时，则不使用覆盖索引技术。InnoDB不会从二级索引结构中返回值，而是从聚簇索引中查询记录的位置然后查找返回。（回表）

但是如果 index condition pushdown（ICP） 优化被打开，并且`WHERE`条件中的部分可以仅使用索引字段评估，MySQL server 仍然会将`WHERE`条件中使用索引字段这部分下推到存储引擎层。如果没有找到匹配的记录，则不会访问聚簇索引。如果找到匹配的记录，即使是标记删除的记录，InnoDB仍会查找聚簇索引。

