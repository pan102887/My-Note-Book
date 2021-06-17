# Mockito简明教程
## Mock测试
Mock测试就是在测试中，对于某些不易构造（如HttpServletRequest必须在Servlet容器中构造）或者不容易获取比较复杂的对象（如JDBC中的ResultSet对象），用一个虚拟的对象（Mock对象）来创建以便于测试的测试方法 。  

## Mock对象的使用范畴
真实对象具有不可确定的行为，产生不可预测的效果：
- 真实对象很难被创建
- 真实对象的某些行为很难被触发
- 真实对象实际上还不存在

## 使用Mock对象测试的关键步骤
- 使用一个接口来描述这个对象
- 在产品代码中实现这个接口
- 在测试代码中实现这个接口
- 在被测试代码中只是通过接口来引用对象，因此它不知道这个引用的对象是真实对象，还是Mock对象。
  
## java Mock测试工具
目前的主要Mock测试工具由Mockito, JMock, EasyMock等。

## Mockito的特点
- 开源
- 更简单直观  
  大多java Mock库都是expect-run-verify（期望-运行-验证）方式，而Mockito则使用更加简单：在执行后的互动中提问。使用Mockito，可以验证任何你想要的。而erv方式，常常被迫查看无关的交互。  

  非erv方式意味着Mockito无需要昂贵的前期启动。让开发人员专注于测试选定的行为。  

  Mockito拥有非常少的API，所有开始使用Mockito，没有时间成本，只有一种创造Mock的方式：`在执行前stub，而后在交互中验证。`  

  类似EasyMock语法，方便重构，Mockito不需要"expectation"，只有stub和验证。

## 其他特点
- 可以mock具体类而不只是接口
- 语法糖`@Mock`
- 干净的验证错误是：点击堆栈跟踪，看看再测试中失败的验证；点击异常的原因来导航到代码中的实际互动。堆栈跟踪总是干净的。
- 允许灵活有序的验证
- 支持"详细的用户号码时间"以及"至少一次"验证
- 灵活的验证或使用参数匹配器stub（anyObject(),anyString(),refEq()用于基于反射的相等匹配）
- 允许创建自定义的参数匹配器或者使用现有的hamcrest匹配器

## Mockito入门
### 示例
1. 验证行为
   ```java
    import static org.mockito.Mockito*;
   ```
