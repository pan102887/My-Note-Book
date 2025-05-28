# spring 框架的核心——IOC 和 AOP
spring 的核心是 IOC(Inversion of Controll) 和 AOP(Aspect Oriented Programming)。



## IoC的基础——BeanFactory
依赖注入(Dependency Injection)则是 IOC 的一种实现方式。

在Spring中，BeanFactory API 作为IoC容器为 Spring 的 IoC 功能提供了底层基础。为

BeanFactory 是Spring应用程序组件的中央注册表

## Spring的依赖注入方式
### 构造器注入
通过对象的构造器注入依赖。这种方式更加推荐，因为它保证了对象在创建时就拥有了所有必需的依赖，从而可以在对象创建后立即使用。
```java
public class MyService {
    private final MyDependency dep;

    public MyService(MyDependency dep) {
        this.dep = dep;
    }
}
// ...
```
在xml配置文件中可以这样
```xml
<bean id="myService" class="com.example.MyService">
    <constructor-arg ref="myDependencyBean"/>
</bean>
<bean id="myDependencyBean" class="com.example.MyDependency"/>
```
使用注解的方式
```java
@Service
public class MyService {
    @Autowired
    private final MyDependency dep;

    // ...
}
```

### Setter注入
在Setter方法上使用`@Autowired`注解，Spring会自动调用这个方法，并将依赖注入到这个方法中。
```java
@Service
public class MyService {
    private MyDependency dep;

    @Autowired
    public void setDep(MyDependency dep) {
        this.dep = dep;
    }
    // ...
}
```

### 方法注入
与Setter注入没有什么本质区别
### 字段注入
