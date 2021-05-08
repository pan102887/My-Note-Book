# 入门
## 使用
- maven
  ```xml
  <dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis</artifactId>
  <version>x.x.x</version>
</dependency>
```

## 从XML中构建SqlSessionFactory
MyBatis是以SqlSessionFactory实例（用于生成SqlSession）为核心的。SqlSessionFactory的实例通过SqlSessionFactoryBuilder获得，而SqlSessionFactoryBuilder通过XML配置文件或者一个预先配置的Configuration实例构建出SqlSessionFactory 实例。  

从XML中构建SqlSessionFactory的实例非常简单。建议使用类路径下的资源文件进行配置。也可以使用任意的输入流(InputStream)实例，如用文件路径字符串file://URL 构造的输入流。  

Mybatis包含一个叫做Resources的工具类，它包含一些使用的方法，使得从类路径或者其它位置加载资源文件更加容易。  

XML配置文件中包含了对MyBatis的核心设置，environment部分包含了事务管理和连接池的配置。mappers元素包含了一组映射器(mapper)，这些映射器的XML映射文件包含了SQL代码和映射定义信息。  

## 不使用XML

若不使用XML，也可以使用java配置类进行替换。但是在一些复杂场景下，java配置类不能完全胜任。还需要使用XML配置，如果存在一个同名的XML配置文件，mybatis会自动查找并加载它。  

## 从SqlSessionFactory中获取SqlSession
- 旧版
    ```java
    try(SqlSession session = sqlSessionFactory.openSession()){
        Blog blog = (Blog) session.selectOne("org.mybatis.example.BlogMapper.selectBlog", 101);
    }
    ```
- 新版
  ```java
  try(SqlSession session = sqlSessionFactory.openSession()){
      BlogMapper mapper = session.getMapper(BlogMapper.class);
      Blog blog = mapper.selectBlog(101);
  }
  ```

  ## 映射SQL语句
    ```xml
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE mapper
      PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
      "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <mapper namespace="org.mybatis.example.BlogMapper">
      <select id="selectBlog" resultType="Blog">
        select * from Blog where id = #{id}
      </select>
    </mapper>
    ```

    上文的查询语句和使用java的全限定名的方法十分类似。因此根据XML配置文件命名空间中的值，就可以将XML中的mapper规则映射到命名空间中的同名映射器（java类），并将已映射的select语句匹配到对应名称、参数和返回类型的方法中。因此可以直接调用对应映射器接口里的方法。  

    *提示*

- 命名空间（namespace）的作用：
    - 利用更长的全限定名来将不同的语句隔离开，实现接口的绑定  
    - 可以让代码更加整洁  

  命名解析：为了减少输入量，MyBatis对所有具有名称的配置元素（语句，结果映射，缓存等）使用了如下的命名解析规则。  
  - 全限定名:（如"com.mypackage.MyMapper.selectAllThings"）将被直接用于查找及使用。  
  - 短名称：（如"selectAllThings"）如果全局唯一的情况下，也可以作为一个单独引用，若全局不唯一则必须使用全限定名(短名称若全局唯一，则可以直接使用短名称，若存在重复，必须使用全限定名加以区分)。  

- 使用java注解类配置映射器(Mapper)
    ```java
    package org.mybatis.example;
    public interface BlogMapper {
      @Select("SELECT * FROM blog WHERE id = #{id}")
      Blog selectBlog(int id);
    }
    ```
    对于简单的语句，使用java注解来映射可以使代码显得更加简洁，但是对于复杂情况下，java注解则会显得力不从心，因此在复杂情况下最好还是使用XML进行映射。  

## 作用域（Scope）和生命周期  
*提示* 对象生命周期和依赖注入框架  
依赖注入框架可以创建线程安全的，基于事务的SqlSession和映射器，将其直接注入到bean中，就可以直接忽略他们的生命周期，对应两个MaBatis框架的两个子项目：MaBatis-Spring或MyBatis-Guice  

**SqlSessionFactoryBuilder**  
该类可以被实例化，使用和丢弃，在SqlSessionFactory创建之后，则不再需要它的实例。因此可以重用它的实例创建多个SqlSessionFactory实例，但通常不会一直保留它，以保证XML解析资源可以被释放给更重要的事情。

**SqlSessionFactory**  
SqlSessionFactory一旦被创建，就应该在应用的运行期间一直存在，没有任何理由丢弃它或者重建一个实例（重新创建SqlSessionFactory实例将会带来大量不必要的开销）。`因此SqlSessionFactory的最佳作用域就是应用作用域`。达到这个目的的方法：`单例模式；静态单例模式……`

**SqlSession**  
每一个线程都应该有它自己的SqlSession实例。SqlSession实例不是线程安全的，因此不能被共享，因此它的最佳作用域是请求或方法作用域。绝对不能将SqlSession实例的引用放在一个类的静态域，也不要放在类的实例变量中（什么时候需要，什么时候创建，使用完之后即使销毁。）以下是一个确保SqlSession关闭的标准模式：
```java
try (SqlSession session = sqlSessionFactory.openSession()) {
  // 你的应用逻辑代码
}
```
**映射器实例**
映射器一些绑定映射语句的接口。映射器的实例时从SqlSession中获取的。从理论上说，映射器实例的最大作用域与请求它们的SqlSession相同。但方法作用域才是映射器实例的最合适的作用域。 也就是说，映射器实例应该在调用它们的方法中被获取，使用完毕之后即可丢弃。映射器实例并不需要被显式地关闭。尽管在整个请求作用域保留映射器实例不会有什么问题，但是你很快会发现，在这个作用域上管理太多像 SqlSession 的资源会让你忙不过来。 因此，最好将映射器放在方法作用域内。就像下面的例子一样：
```java
try (SqlSession session = sqlSessionFactory.openSession()) {
  BlogMapper mapper = session.getMapper(BlogMapper.class);
  // 你的应用逻辑代码
}
```

