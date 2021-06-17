# XML配置
## 配置
MyBatis的配置文件的顶层结构如下
- configuration（配置）
  - properties（属性）
  - settings（设置）
  - typeAliases（类型别名）
  - typeHandlers（类型处理器）
  - objectFactory（对象工厂）
  - plugins（插件）
  - environments（配置环境）
    - environment（环境变量）
      - transactionManager（事务管理器）
      - dataSource（数据源）
  - databaseldProvider（数据库厂商表示）
  - mappers（映射器）

## 属性（properties）
可以在外部进行配置，并可以进行动态替换。既可以在典型的java属性文件中配置这些属性，也可以在XML中的properties元素的子元素中设置。例如

```xml
<properties resource="org/mybatis/example/config.properties">
  <property name="username" value="dev_user"/>
  <property name="password" value="F2Fa3!33TYyg"/>
</properties>
```

设置好的属性可以在整个配置文件中用来替换需要动态配置的属性值。例如：
```xml
<dataSource type="POOLED">
  <property name="driver" value="${driver}"/>
  <property name="url" value="${url}"/>
  <property name="username" value="${username}"/>
  <property name="password" value="${password}"/>
</dataSource>
```
在该例子中，dataSource中的username和password属性会被properties中的对应元素值替换，driver和url属性由config.properties文件中对应的值替换。  

亦可以在SqlSessionFactoryBuilder.build()方法中传入属性值，如：
```java
SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, props);

// ... 或者 ...

SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(reader, environment, props);
```

若一个属性在多个地方都进行了配置，呢么MyBaties将按照下面的顺序来加载
1. properties中的属性
2. properties中的source属性或URL属性指定的属性文件中的属性（覆盖之前的）
3. 最后读取作为方法参数传递的属性，并覆盖之前读取过的同名属性。

在 3.4.2版本之后，可以使用默认值，但是需要手动开启该功能：
```xml
<properties resource="org/mybatis/example/config.properties">
  <!-- ... -->
  <property name="org.apache.ibatis.parsing.PropertyParser.enable-default-value" value="true"/> <!-- 启用默认值特性 -->
</properties>
```
```xml
<dataSource type="POOLED">
  <!-- ... -->
  <property name="username" value="${username:ut_user}"/> <!-- 如果属性 'username' 没有被配置，'username' 属性的值将为 'ut_user' -->
</dataSource>
```
**注意**: 如果你在属性名中使用了 ":" 字符（如：db:username），或者在 SQL 映射中使用了 OGNL 表达式的三元运算符（如： ${tableName != null ? tableName : 'global_constants'}），就需要设置特定的属性来修改分隔属性名和默认值的字符。例如： 
```xml
<properties resource="org/mybatis/example/config.properties">
  <!-- ... -->
  <property name="org.apache.ibatis.parsing.PropertyParser.default-value-separator" value="?:"/> <!-- 修改默认值的分隔符 -->
</properties>
```
```xml
<dataSource type="POOLED">
  <!-- ... -->
  <property name="username" value="${db:username?:ut_user}"/>
</dataSource>
```