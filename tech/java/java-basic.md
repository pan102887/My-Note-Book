- [1. 基础部分](#1-基础部分)
  - [1.1. 泛型](#11-泛型)
  - [1.2. 接口](#12-接口)
  - [1.3. 异常](#13-异常)
    - [1.3.1. 自定义异常](#131-自定义异常)
    - [1.3.2.](#132)
- [2. 高级部分](#2-高级部分)
  - [泛型](#泛型)
    - [特性](#特性)
    - [泛型的使用](#泛型的使用)
  - [2.1. java类加载器以及类的加载](#21-java类加载器以及类的加载)
    - [2.1.1. 类加载器](#211-类加载器)
    - [2.1.2. 类的加载](#212-类的加载)
  - [2.2. java Class类对象和类的加载](#22-java-class类对象和类的加载)
    - [2.2.1. Class类以及Class对象](#221-class类以及class对象)
    - [2.2.2. 获取Class对象的三种方法](#222-获取class对象的三种方法)
      - [2.2.2.1. 类字面常量](#2221-类字面常量)
  - [2.3. 反射机制的原理及用法](#23-反射机制的原理及用法)
    - [2.3.1. 通过类的Class对象来创建类的实例](#231-通过类的class对象来创建类的实例)
  - [2.4. 注解](#24-注解)
    - [2.4.1. 元注解](#241-元注解)
# 1. 基础部分  
## 1.1. 泛型  
## 1.2. 接口
## 1.3. 异常
### 1.3.1. 自定义异常
### 1.3.2. 

<!-- TODO: Java中this的用法 -->
<!-- TODO: java中super的用法 -->
<!-- TODO: java.io.SeriaLizable -->
<!-- TODO: @java.io.Serial -->
<!-- TODO: lambok.Data -->

---
---

# 2. 高级部分  

## 泛型
### 特性  
java的泛型信息，其实只保留到编译阶段，在编译过程中，会去泛型化，将泛型中的信息确定，将泛型的相关信息擦除。

### 泛型的使用
Java中泛型有三种使用方式，分别为：泛型类，泛型接口，泛型方法  
- 泛型类  
  ```java
  class 类名 <泛型标识：可以使用任意标识符>{
    //成员变量……
    private 泛型标识符 变量名称;
    //成员函数……
    public function(泛型标识符 形参){
      ……
    }
  }
  ```
  如
  ```java
  public class GenericCLass<T>{
    private T key;
    public GenericCLass(T key) {
      this.key = key;
    }
    public void setKey(T key) {
      this.key = key;
    }
    public T getKey() {
      return this.key;
    }
  }
  ```
  泛型的类型只能是类类型，不能是基本类型，如int,double等，但可以使用基本类型的包装类，如Integer,String等。在泛型类初始化为具体类型后，传入的参数必须要与泛型的类型参数类型相同。如：
  ```java
  GenericClass<String> generic = new GenericClass<String>();
  //generic被初始化为String类型后，类中的泛型方法参数类型也确定为String类型。
  generic.setKey(Integer.valueOf(12));
  ```
  上述代码会在编译时报错：`java: incompatible types: java.lang.Integer cannot be converted to java.lang.String`。  

  在声明泛型类对象的时候，不一定要传入泛型类型，若使用有参构造函数声明泛型对象，会根据构造函数中参数类型对泛型中的泛型类型进行初始化，如：
  ```java
  GenericClass genericCLass = new GenericClass("key");
  //这里会将泛型类型声明为String类型。
  ```

  *注意：* 不能对确切的泛型类型使用instanceof操作，如
  ```java
  //下面这行代码会在编译时报错
  boolean obj instanceof GenericClass<String>;
  ```
- 泛型接口  
  泛型接口的使用方法和泛型类大致相同  

- 泛型方法
  <!-- TODO: 补充泛型方法的用例 -->
## 2.1. java类加载器以及类的加载
### 2.1.1. 类加载器
在java中，类通常情况下只会被类加载器加载一次。java中的类加载器默认有三种（用户可以自定义类加载器），分别为：  
  1. 引导类加载器（Bootstrap class loader）
  2. 扩展类加载器（extensions class loader）  
  3. 系统类加载器(system class loader)  

引导类加载器用来加载java的核心库，由原生代码实现，不能通过getClassLoader方法以及getClassLoader.getParent方法得到。  

扩展类加载器用于加载java的扩展库。JVM的实现会提供一个扩展库目录。该类加载器在此目录里面查找并加载java类。扩展库目录可以通过`System.getProperty("java.class.path");`获取。  

系统类加载器：根据应用类的路径来加载java类。通常情况下，java应用的类都是由它来完成加载的。

- 自定义类加载器  
  
  除了系统提供的类加载器，开发人员可以通过继承java.lang.ClassLoader类的方式实现自己的类加载器，用于满足一些特殊需求。

- 父类加载器  
  
  除了引导类加载器，所有的类加载器都有一个父类加载器，可以通过`<某个类加载器>.getParent()`方法得到。系统默认的三个类加载器的继承关系为：`引导类加载器 <--- 扩展类加载器 <--- 系统类加载器`（箭头指向父类）

### 2.1.2. 类的加载
一个类的被加载到被使用有三个阶段：  
  1. **加载：** 由类加载器（ClassLoader）执行，通过类的`全限定名`来获取其定义的二进制字节流（Class字节码），将这个字节流所代表的静态存储结构转化为方法区的运行时数据接口,根据字节码在java堆中生成一个代表这个类的`java.lang.Class`对象。  
   
  2. **链接：** 验证Class文件中的字节流包含的信息是否符合当前虚拟机的需求，为静态域分配存储空间并设置类变量的初始值(默认的零)，若必需的话，将常量池中的符号引用转化为直接引用。  
  
  3. **初始化：** 在这个阶段执行类中定义的java程序代码。用于执行该类的静态初始器和静态初始块，如果该类有父类的话，则优先对其父类进行初始化。  
   
  **所有的类都是在对其第一次使用时，动态加载到JVM中的（懒加载），其各个类都是在需要时才加载的**  

  类中的静态方法块只在类被加载时执行。类通常只会被加载一次，因此静态代码块中的方法通常也就执行一次。

  在使用类中经过static final修饰的成员变量时，并不会加载该类。如下：
  ```java
  public class User{
    public static final String s = "静态成员变量";
    static{
      System.out.println("User类被加载")
    }
  }
  public Test
  ```
  在上述的例子中，User并不会被加载，因此静态方法块中的方法也因此不会被执行。

## 2.2. java Class类对象和类的加载
### 2.2.1. Class类以及Class对象  

java中有两种对象：实例对象和Class对象。  
- CLass对象  
  用于表示每个类的运行时的`类型信息`，包含了与类有关的信息。  

  每个类都有一个Class对象，每当编译一个新的类就产生一个Class对象，`保存在同名的.class文件中`（基本类型int, double……有CLass对象，数组有Class对象，关键字void……有Class对象）。

  Class对象对应java.lang.Class类。若类是抽象的集合，则Class类就是对类的抽象和集合。  

  Class类没有公共构造函数,Class对象是类在加载的时候，由`JVM`通过调用类加载器中的`defineCLass`方法自动构造的。不能显式地声明Class对象。  

  在类加载阶段，类加载器首先检查类的Class对象是否已经被加载，如果尚未被加载，默认的类加载器会根据类的全限定名查找.class文件。在这个类的字节码被加载时，它们会接受验证，以确保其没有被破坏，不包含不良java代码。一旦某个类的CLass对象被载入内存，就可以用它来创建这个类的所有对象。  

### 2.2.2. 获取Class对象的三种方法  
1. Class.forName("类的全限定名")
2. 实例对象.getClass()
3. 类名.class  `——(类字面常量)`

Class.forName可以不需要为了获得Class引用而持有该类型的对象，只要通过全限定名就可以返回该类型的`Class引用`  

若执行了new操作后，类就已经装在到内存中了，此时再去执行getClass()或者Class.forName()就不会再去执行加载的操作了，而是直接从java堆中返回该类型的Class引用。  

#### 2.2.2.1. 类字面常量  
java中提供了另一种方法来生成Class对象的引用，即为（类名.class）.这样做的好处是会更加安全，也更加高效。可以用于普通的类，接口，数组以及基本数据类型。  

基本数据类型和包装类的Class对象是不同的。  

**使用.class来创建Class对象的引用时，不会自动初始化Class对象**。类对象的初始化阶段被延迟到了对静态方法或者非常数静态域首次引用时才执行，如：  
```java
public User{
  public static final String s = "静态成员";
  static{
    System.out.println("User类被加载")
  }
}
public Starter{
  public static void main(String[] args){
    System.out.println(User.s);
  }
}
```
在以上例子中，User的static方法块并不会被执行。
## 2.3. 反射机制的原理及用法  
### 2.3.1. 通过类的Class对象来创建类的实例
1. 获取Class对象
   1. Class c1 = Class.forName("全限定名")
   2. Class c1 = <实例名>.getClass()
   3. Class c1 = <类名>.getCLass()

2. 创建实例
   
   ```java
   //-------------------无参构造器创建方法----------------------
   // jdk 8及其之前版本中
   Object object = c1.newInstance();
   //jdk 9及其之后版本，先通过getDeclaredConstructor获取构造函数，在通过构造函数newInstance。
   //getDeclaredConstructor中的参数即为构造器中的参数类型，不填就代表获取无参构造器，
   Object object = c1.getDeclaredConstructor().newInstance()

   //------------分割线----------------------

   //

   //将object 强转为目标类型即可得到类的实例
   ```
## 2.4. 注解  
### 2.4.1. 元注解
- java中定义了4个标准的meta-annotation类型（元注解），用于对其他的注解类型作说明  

- 这些类型和它们所支持的类在java.lang.annotation包中可以找到，分别为（`@Target, @Retention, @Documented, @Inherited`）
  1. **@Target:**  用于描述注解的使用范围
  2. **@Retention:** 用于说明在什么级别保存该注释信息，用于描述注解的生命周期
      - `SOURCE < CLASS < RUNTIME`
  3. **@Documented:** 表明该注解将被包含在javadoc中
  4. **@Inherited:** 说明子类可以继承父类中的注解
