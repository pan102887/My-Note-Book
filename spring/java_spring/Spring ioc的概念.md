# SPring IOC 的概念

## Spring 中Bean的声明周期

Spring中的Bean由IOC容器进行管理，它的生命周期主要包括以下几个阶段：

- 实例化（Instantiation）：IOC容器根据配置文件或注解创建Bean实例。
- 属性注入（Dependency Injection）：IOC容器将Bean所依赖的其他Bean注入到该Bean中。
- 初始化（Initialization）：如果Bean实现了InitializingBean接口，IOC容器会调用afterPropertiesSet()方法进行初始化操作。或者，如果在配置文件中指定了init-method属性，IOC容器会调用该方法。
- 使用（Usage）：Bean被应用程序使用。
- 销毁（Destruction）：当IOC容器关闭时，如果Bean实现了DisposableBean接口，IOC容器会调用destroy()方法进行清理操作。或者，如果在配置文件中指定了destroy-method属性，IOC容器会调用该方法。
  