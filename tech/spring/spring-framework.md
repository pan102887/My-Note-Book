# 1. Spring框架
- [1. Spring框架](#1-spring框架)
  - [1.1. IoC容器](#11-ioc容器)
    - [1.1.1. 关于Spring IoC容器和Bean的介绍](#111-关于spring-ioc容器和bean的介绍)
    - [1.1.2. IoC容器总览](#112-ioc容器总览)
      - [配置元数据](#配置元数据)
## 1.1. IoC容器

### 1.1.1. 关于Spring IoC容器和Bean的介绍

`org.springframework.beans`和`org.springframework.context`这两个包是Spring框架 IoC(控制反转)的基础。  

`BeanFactory`接口提供了一个先进的配置机制，以管理任何类型的（Java）对象。`ApplicationContext`接口是`BeanFactory`的一个派生接口。它在`BeanFactory`的基础上增加了

- 更简便的Spring AOP功能集成
- 消息资源处理
- 事件发布
- 应用层特定`contexts`例如web应用的`WebApplicationContext`  

简的来说，`BeanFactory`提供了一个配置框架和基础功能。`ApplicationContext`则增加了更多领域特定的功能。`ApplicationContext`是`BeanFactory`的一个真超集。本章将只从`ApplicationContext`展开对Spring Ioc 容器的介绍。如果想了解更多关于`BeanFactory`的内容，请详见于[The BeanFactory](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-beanfactory)。

在Spring中，通过Spring IoC容器管理的组成程序主干的对象被称作为`beans`（复数）。一个`bean`是由Spring Ioc容器进行实例化、装配和管理的对象。除此之外，`bean`和程序中其他众多Java 对象并无区别。`bean`和它们之间的`依赖关系`通过容器使用的配置`元数据`表示。

### 1.1.2. IoC容器总览

`org.springframework.context.ApplicationContext`接口是Spring IoC容器的一个代表，它负责实例化、配置和装配`bean`。它从配置元数据中获得如何实例化、配置和如何装配bean的指令。配置元数据的表示形式有很多，分别有XML、Java注解或者Java代码。配置元数据可以表达组成应用程序的对象有哪些，及其它们之间丰富的依赖关系。

Spring提供了一些`ApplicationContext`接口的实现类。在独立的应用程序中，比较常见的有`ClassPathXmlApplicationContext`和`FileSystemXmlApplicationContext`。虽然传统上使用XML作为配置元数据的格式，但仍可以通过少量的XML配置，以指示容器使用Java注解或者Java代码作为配置元数据格式。

在大多数应用场景下，并不需要用户代码显示地去实例化一个或多个Spring IoC容器。例如在一个web程序场景中，一个简单模板网页的描述XML：`web.xml`，只用八行代码就足够了（详见[ Convenient ApplicationContext Instantiation for Web Applications](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#context-create)）。如果你使用[eclipse中的Spring工具](https://spring.io/tools)，你只需要通过一些简单的鼠标点击，就可以非常容易地创建这个模板网页的配置（web.xml。

下面这张图从整体上显示了Spring是如何运作的。配置元数据将你的业务类整合到一起（配置数据中描述了业务类之间的关系）。然后`ApplicationContext`初始化后，你的业务系统代码就被组装成一个完整的可以被执行的系统或应用。
![关系图](img\container-magic.png)

#### 配置元数据  

如上图所示，Spring IoC容器