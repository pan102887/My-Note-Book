# Maven 与 Gradle 依赖范围（Scope）详解

## Maven 依赖范围

Maven 定义了 6 种依赖范围，控制依赖在不同生命周期阶段的可用性。

### 1. compile（编译范围）- 默认

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>5.3.21</version>
    <scope>compile</scope> <!-- 可以省略，默认值 -->
</dependency>
```

**特点：**

- 编译、测试、运行时都可用
- 会传递给依赖该项目的其他项目
- 打包时会包含在最终的 jar/war 中

**适用场景：** 核心业务依赖，如 Spring Framework、MyBatis 等

### 2. provided（已提供范围）

```xml
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>
```

**特点：**
- 编译和测试时可用
- 运行时不可用（假设运行环境已提供）
- 不会传递给依赖项目
- 打包时不包含在最终文件中

#### "编译和测试可用，运行时不可用" 的核心场景分析

这种依赖范围的设计理念是：**编译时需要这些依赖来通过编译检查和单元测试，但运行时由外部环境提供或者根本不需要**。

**适用场景：**

- Servlet API（Tomcat 已提供）
- Lombok（编译时工具）
- JSP API

#### "编译和测试可用，运行时不可用" 的核心场景分析

这种依赖范围的设计理念是：**编译时需要这些依赖来通过编译检查和单元测试，但运行时由外部环境提供或者根本不需要**。

##### 核心场景类型分析

###### 1. 容器/平台已提供的API类库
```xml
<!-- Web容器场景 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- Java EE 应用服务器场景 -->
<dependency>
    <groupId>javax.ejb</groupId>
    <artifactId>javax.ejb-api</artifactId>
    <version>3.2.2</version>
    <scope>provided</scope>
</dependency>

<!-- Spring Boot 外部容器部署场景 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <version>2.7.0</version>
    <scope>provided</scope>
</dependency>
```

**代码示例和测试场景：**
```java
// 编译时需要 - 代码可以正常编译
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 编译时可以使用 HttpServletRequest 和 HttpServletResponse
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Hello World\"}");
    }
}

// 测试时需要 - 单元测试可以正常运行
@Test
public void testUserServlet() throws Exception {
    // 测试时可以创建 Mock 对象
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);
    StringWriter stringWriter = new StringWriter();
    PrintWriter writer = new PrintWriter(stringWriter);
    
    when(response.getWriter()).thenReturn(writer);
    
    UserServlet servlet = new UserServlet();
    servlet.doGet(request, response);
    
    assertTrue(stringWriter.toString().contains("Hello World"));
}
```

**为什么运行时不需要？**
- Tomcat 容器已经提供了 Servlet API 的实现
- 如果应用包含这些 jar，会导致类加载冲突
- 运行时由容器的类加载器负责加载这些类

###### 2. 编译时代码生成和转换工具
```xml
<!-- Lombok - 编译时代码生成 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.24</version>
    <scope>provided</scope>
</dependency>

<!-- MapStruct - 编译时映射代码生成 -->
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.3.Final</version>
    <scope>provided</scope>
</dependency>
```

**代码示例：**
```java
// 编译前的源代码
@Data
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
}

// 编译后生成的字节码中包含：
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    
    // Lombok 自动生成的方法
    public Long getId() { return this.id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return this.username; }
    // ... 其他 getter/setter
    public boolean equals(Object o) { /* 生成的equals方法 */ }
    public int hashCode() { /* 生成的hashCode方法 */ }
    // ... Builder 相关方法
}
```

**测试时的使用：**
```java
@Test
public void testUserDTO() {
    // 测试时可以使用 Lombok 生成的方法
    UserDTO user = UserDTO.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .build();
    
    assertEquals("testuser", user.getUsername());
    assertNotNull(user.hashCode());
    
    UserDTO user2 = UserDTO.builder()
        .id(1L)
        .username("testuser")
        .email("test@example.com")
        .build();
    
    assertEquals(user, user2); // 测试生成的equals方法
}
```

**为什么运行时不需要？**
- 生成的代码直接编译到字节码中
- 运行时不需要访问 Lombok 库
- 所有功能都在编译期完成

###### 3. 静态分析和编译期检查工具
```xml
<!-- JSR-305 空值检查注解 -->
<dependency>
    <groupId>com.google.code.findbugs</groupId>
    <artifactId>jsr305</artifactId>
    <version>3.0.2</version>
    <scope>provided</scope>
</dependency>

<!-- SpotBugs 静态分析注解 -->
<dependency>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-annotations</artifactId>
    <version>4.7.1</version>
    <scope>provided</scope>
</dependency>
```

**代码示例：**
```java
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import edu.umd.cs.findbugs.annotations.CheckForNull;

public class UserService {
    
    // 编译时和IDE会检查空值
    public User createUser(@Nonnull String username, @Nullable String email) {
        // 编译器会警告如果直接使用可能为null的参数
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        
        User user = new User();
        user.setUsername(username);
        
        // email 可能为 null，IDE会提示检查
        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }
        
        return user;
    }
    
    @CheckForNull
    public User findUserById(Long id) {
        // 返回可能为null，调用者需要检查
        return userRepository.findById(id);
    }
}
```

**测试中的验证：**
```java
@Test
public void testUserServiceNullChecks() {
    UserService service = new UserService();
    
    // 测试非空参数
    User user = service.createUser("testuser", "test@example.com");
    assertNotNull(user);
    assertEquals("testuser", user.getUsername());
    
    // 测试空值处理
    User userWithoutEmail = service.createUser("testuser2", null);
    assertNotNull(userWithoutEmail);
    assertNull(userWithoutEmail.getEmail());
    
    // 测试异常情况
    assertThrows(IllegalArgumentException.class, () -> {
        service.createUser(null, "test@example.com");
    });
}
```

**为什么运行时不需要？**
- 注解信息主要用于编译期检查和IDE提示
- 静态分析工具在构建时使用
- 运行时这些注解不参与业务逻辑

###### 4. 配置和元数据生成工具
```xml
<!-- Spring Boot 配置元数据处理器 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <version>2.7.0</version>
    <scope>provided</scope>
</dependency>
```

**配置类示例：**
```java
@ConfigurationProperties(prefix = "app.security")
@Data
public class SecurityProperties {
    private boolean enableSsl = false;
    private String keyStorePath;
    private String keyStorePassword;
    private Session session = new Session();
    
    @Data
    public static class Session {
        private Duration timeout = Duration.ofMinutes(30);
        private int maxConcurrentSessions = 1;
        private boolean preventSessionFixation = true;
    }
}
```

**测试配置绑定：**
```java
@SpringBootTest
@TestPropertySource(properties = {
    "app.security.enable-ssl=true",
    "app.security.session.timeout=PT45M",
    "app.security.session.max-concurrent-sessions=3"
})
class SecurityPropertiesTest {
    
    @Autowired
    private SecurityProperties securityProperties;
    
    @Test
    void testConfigurationBinding() {
        assertTrue(securityProperties.isEnableSsl());
        assertEquals(Duration.ofMinutes(45), securityProperties.getSession().getTimeout());
        assertEquals(3, securityProperties.getSession().getMaxConcurrentSessions());
    }
}
```

**编译后生成的元数据文件：**
```json
// META-INF/spring-configuration-metadata.json
{
  "properties": [
    {
      "name": "app.security.enable-ssl",
      "type": "java.lang.Boolean",
      "defaultValue": false,
      "description": "Whether to enable SSL"
    },
    {
      "name": "app.security.session.timeout",
      "type": "java.time.Duration",
      "defaultValue": "PT30M",
      "description": "Session timeout duration"
    }
  ]
}
```

**为什么运行时不需要？**
- 处理器只在编译时生成元数据文件
- 运行时 Spring Boot 直接读取生成的元数据
- IDE 使用元数据提供自动完成和验证

##### 关键特征总结

**编译时需要的原因：**
1. **类型检查** - 编译器需要验证方法调用、类型转换等
2. **注解处理** - 注解处理器需要读取和处理注解
3. **代码生成** - 工具需要生成额外的代码或配置
4. **静态分析** - 提供编译期的代码质量检查

**测试时需要的原因：**
1. **Mock 对象创建** - 测试框架需要创建相关类的 Mock
2. **API 调用验证** - 测试需要验证对相关 API 的调用
3. **集成测试** - 某些集成测试可能需要这些依赖

**运行时不需要的原因：**
1. **容器提供** - 运行环境已经包含这些实现
2. **代码已生成** - 编译期生成的代码已包含所需功能
3. **仅工具性质** - 纯粹的开发和构建工具，不参与运行时逻辑
4. **避免冲突** - 防止与运行环境的版本产生冲突

这种设计模式的核心价值在于**清晰的职责分离**：开发时的依赖和运行时的依赖被明确区分，既保证了开发体验，又优化了运行时性能。

#### provided 详细使用场景案例

##### 场景1：Web 容器提供的 API
```xml
<!-- Servlet API - 容器已提供 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- JSP API - 容器已提供 -->
<dependency>
    <groupId>javax.servlet.jsp</groupId>
    <artifactId>jsp-api</artifactId>
    <version>2.2</version>
    <scope>provided</scope>
</dependency>
```

**为什么使用 provided？**
- Tomcat、Jetty 等容器已经内置这些 API
- 如果打包进 WAR，会造成类路径冲突
- 编译时需要这些 API 来编写 Servlet 代码

##### 场景2：编译时工具
```xml
<!-- Lombok - 编译时代码生成 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.24</version>
    <scope>provided</scope>
</dependency>

<!-- JSR-305 注解 - 静态分析工具 -->
<dependency>
    <groupId>com.google.code.findbugs</groupId>
    <artifactId>jsr305</artifactId>
    <version>3.0.2</version>
    <scope>provided</scope>
</dependency>
```

**实际代码示例：**
```java
// 使用 Lombok 注解，编译时会生成 getter/setter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String username;
    private String email;
}

// 使用 JSR-305 注解进行空值检查
public class UserService {
    public User findUser(@Nonnull String username) {
        // 编译器和IDE会进行空值检查提示
        return userRepository.findByUsername(username);
    }
}
```

##### 场景3：企业级容器提供的 API
```xml
<!-- Java EE API - 应用服务器已提供 -->
<dependency>
    <groupId>javax</groupId>
    <artifactId>javaee-api</artifactId>
    <version>8.0.1</version>
    <scope>provided</scope>
</dependency>

<!-- JPA API - JEE容器已提供 -->
<dependency>
    <groupId>javax.persistence</groupId>
    <artifactId>javax.persistence-api</artifactId>
    <version>2.2</version>
    <scope>provided</scope>
</dependency>
```

**Enterprise Bean 示例：**
```java
// 这些注解在编译时需要，但运行时由容器提供
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class OrderService {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createOrder(Order order) {
        entityManager.persist(order);
    }
}
```

##### 场景4：Android 开发
```xml
<!-- Android SDK - 构建工具已提供 -->
<dependency>
    <groupId>com.google.android</groupId>
    <artifactId>android</artifactId>
    <version>4.1.1.4</version>
    <scope>provided</scope>
</dependency>
```

##### 场景5：微服务框架中的共享库
```xml
<!-- 在微服务架构中，某些共享库由平台统一提供 -->
<dependency>
    <groupId>com.company.platform</groupId>
    <artifactId>common-logging</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>

<dependency>
    <groupId>com.company.platform</groupId>
    <artifactId>security-framework</artifactId>
    <version>2.1.0</version>
    <scope>provided</scope>
</dependency>
```

#### provided vs compile 对比示例

**错误的做法（使用 compile）：**
```xml
<!-- ❌ 错误：会导致类路径冲突 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>compile</scope> <!-- 错误！ -->
</dependency>
```

**部署后可能出现的问题：**
```
java.lang.LinkageError: loader constraint violation
ClassCastException: cannot cast javax.servlet.http.HttpServletRequest
```

**正确的做法（使用 provided）：**
```xml
<!-- ✅ 正确：避免运行时冲突 -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>javax.servlet-api</artifactId>
    <version>4.0.1</version>
    <scope>provided</scope> <!-- 正确！ -->
</dependency>
```

#### 实际项目配置示例

**Spring Boot Web 项目的正确配置：**
```xml
<dependencies>
    <!-- Spring Boot Starter - 包含嵌入式Tomcat -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>2.7.0</version>
    </dependency>

    <!-- 如果要部署到外部Tomcat，需要排除嵌入式容器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-tomcat</artifactId>
        <version>2.7.0</version>
        <scope>provided</scope> <!-- 外部容器部署时使用 -->
    </dependency>

    <!-- Lombok 编译时工具 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.24</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

**传统 Web 项目配置：**
```xml
<dependencies>
    <!-- Spring Framework -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webmvc</artifactId>
        <version>5.3.21</version>
    </dependency>

    <!-- Servlet API - Tomcat提供 -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>

    <!-- JSP 相关 - 容器提供 -->
    <dependency>
        <groupId>javax.servlet.jsp</groupId>
        <artifactId>jsp-api</artifactId>
        <version>2.2</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>jstl</artifactId>
        <version>1.2</version>
    </dependency>
</dependencies>
```

### 3. runtime（运行时范围）

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.29</version>
    <scope>runtime</scope>
</dependency>
```

**特点：**
- 编译时不可用
- 测试和运行时可用
- 会传递给依赖项目
- 打包时包含在最终文件中

**适用场景：** 
- 数据库驱动
- 日志实现（SLF4J 的具体实现）
- 运行时才需要的实现类

### 4. test（测试范围）

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

**特点：**
- 仅测试时可用
- 编译和运行时不可用
- 不会传递给依赖项目
- 打包时不包含在最终文件中

**适用场景：** 
- JUnit、TestNG
- Mockito
- Spring Test

### 5. system（系统范围）⚠️ 不推荐

```xml
<dependency>
    <groupId>com.sun</groupId>
    <artifactId>tools</artifactId>
    <version>1.8</version>
    <scope>system</scope>
    <systemPath>${java.home}/../lib/tools.jar</systemPath>
</dependency>
```

**特点：**
- 类似 provided，但需要显式指定 jar 路径
- 不推荐使用，可移植性差

### 6. import（导入范围）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**特点：**
- 仅用于 `<dependencyManagement>` 中
- 用于导入其他 POM 的依赖管理信息
- 解决 Maven 单继承问题

## Maven DependencyManagement 详解

`dependencyManagement` 是 Maven 中用于统一管理项目依赖版本的重要机制，它提供了一种集中式的依赖版本控制方式。

### 1. dependencyManagement 基础概念

#### 核心特点
- **版本统一管理** - 在父 POM 中统一定义依赖版本
- **不直接引入依赖** - 只定义版本，不会实际添加依赖到项目中
- **子模块继承** - 子模块可以继承父 POM 的版本定义
- **版本覆盖** - 子模块可以覆盖父模块定义的版本

#### 基本语法
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>组织名</groupId>
            <artifactId>包名</artifactId>
            <version>版本号</version>
            <scope>范围</scope>
            <type>类型</type>
            <exclusions>排除项</exclusions>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 2. 实际使用场景

#### 场景1：父子项目版本统一管理
```xml
<!-- 父 POM (parent-project/pom.xml) -->
<project>
    <groupId>com.example</groupId>
    <artifactId>parent-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <properties>
        <spring.version>5.3.21</spring.version>
        <jackson.version>2.13.3</jackson.version>
        <mysql.version>8.0.29</mysql.version>
        <junit.version>5.8.2</junit.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Framework -->
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-context</artifactId>
                <version>${spring.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-web</artifactId>
                <version>${spring.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.springframework</groupId>
                <artifactId>spring-webmvc</artifactId>
                <version>${spring.version}</version>
            </dependency>
            
            <!-- Jackson -->
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-core</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            
            <!-- 数据库相关 -->
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.version}</version>
                <scope>runtime</scope>
            </dependency>
            
            <!-- 测试框架 -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

#### 子模块使用（无需指定版本）
```xml
<!-- 子模块 (web-module/pom.xml) -->
<project>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>parent-project</artifactId>
        <version>1.0.0</version>
    </parent>
    
    <artifactId>web-module</artifactId>
    
    <dependencies>
        <!-- 无需指定版本，自动继承父POM中定义的版本 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-webmvc</artifactId>
            <!-- version 自动从 dependencyManagement 继承 -->
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <!-- version 自动继承 -->
        </dependency>
        
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <!-- version 和 scope 都自动继承 -->
        </dependency>
        
        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <!-- version 和 scope 自动继承 -->
        </dependency>
    </dependencies>
</project>
```

#### 场景2：多模块项目的依赖版本统一
```xml
<!-- 多模块父项目结构 -->
<project>
    <groupId>com.example</groupId>
    <artifactId>microservice-parent</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    
    <modules>
        <module>user-service</module>
        <module>order-service</module>
        <module>common-lib</module>
    </modules>
    
    <properties>
        <spring-boot.version>2.7.0</spring-boot.version>
        <spring-cloud.version>2021.0.3</spring-cloud.version>
        <mybatis-plus.version>3.5.2</mybatis-plus.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- 自定义依赖版本管理 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            
            <!-- 内部模块依赖管理 -->
            <dependency>
                <groupId>com.example</groupId>
                <artifactId>common-lib</artifactId>
                <version>${project.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 3. 高级使用技巧

#### 技巧1：BOM (Bill of Materials) 导入
```xml
<dependencyManagement>
    <dependencies>
        <!-- 导入 Spring Boot 的完整依赖管理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 导入 Spring Cloud 的依赖管理 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>2021.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 导入 Jackson BOM -->
        <dependency>
            <groupId>com.fasterxml.jackson</groupId>
            <artifactId>jackson-bom</artifactId>
            <version>2.13.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 技巧2：依赖排除管理
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.7.0</version>
            <exclusions>
                <!-- 排除默认的 Tomcat，使用 Jetty -->
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        
        <!-- 统一使用 Jetty -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jetty</artifactId>
            <version>2.7.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 技巧3：条件化依赖管理
```xml
<profiles>
    <profile>
        <id>dev</id>
        <activation>
            <activeByDefault>true</activeByDefault>
        </activation>
        <dependencyManagement>
            <dependencies>
                <dependency>
                    <groupId>com.h2database</groupId>
                    <artifactId>h2</artifactId>
                    <version>2.1.212</version>
                    <scope>runtime</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
    </profile>
    
    <profile>
        <id>prod</id>
        <dependencyManagement>
            <dependencies>
                <dependency>
                    <groupId>mysql</groupId>
                    <artifactId>mysql-connector-java</artifactId>
                    <version>8.0.29</version>
                    <scope>runtime</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
    </profile>
</profiles>
```

### 4. 实际项目案例

#### 企业级微服务父 POM 示例
```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.company</groupId>
    <artifactId>microservice-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    
    <name>企业微服务父项目</name>
    <description>统一管理所有微服务的依赖版本</description>
    
    <properties>
        <java.version>11</java.version>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 框架版本 -->
        <spring-boot.version>2.7.0</spring-boot.version>
        <spring-cloud.version>2021.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2021.0.1.0</spring-cloud-alibaba.version>
        
        <!-- 数据库相关 -->
        <mybatis-plus.version>3.5.2</mybatis-plus.version>
        <druid.version>1.2.11</druid.version>
        <mysql.version>8.0.29</mysql.version>
        <redis.version>2.7.0</redis.version>
        
        <!-- 工具库 -->
        <hutool.version>5.8.4</hutool.version>
        <fastjson.version>1.2.83</fastjson.version>
        <swagger.version>3.0.0</swagger.version>
        
        <!-- 测试相关 -->
        <testcontainers.version>1.17.3</testcontainers.version>
    </properties>
    
    <dependencyManagement>
        <dependencies>
            <!-- Spring Boot BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring Cloud BOM -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Spring Cloud Alibaba BOM -->
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- 数据访问层 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-boot-starter</artifactId>
                <version>${mybatis-plus.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>druid-spring-boot-starter</artifactId>
                <version>${druid.version}</version>
            </dependency>
            
            <dependency>
                <groupId>mysql</groupId>
                <artifactId>mysql-connector-java</artifactId>
                <version>${mysql.version}</version>
                <scope>runtime</scope>
            </dependency>
            
            <!-- 缓存 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-redis</artifactId>
                <version>${redis.version}</version>
            </dependency>
            
            <!-- 工具库 -->
            <dependency>
                <groupId>cn.hutool</groupId>
                <artifactId>hutool-all</artifactId>
                <version>${hutool.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.alibaba</groupId>
                <artifactId>fastjson</artifactId>
                <version>${fastjson.version}</version>
            </dependency>
            
            <!-- API 文档 -->
            <dependency>
                <groupId>io.springfox</groupId>
                <artifactId>springfox-boot-starter</artifactId>
                <version>${swagger.version}</version>
            </dependency>
            
            <!-- 测试容器 -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- 所有子模块都需要的依赖 -->
    <dependencies>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        
        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

#### 子模块使用示例
```xml
<!-- 用户服务模块 -->
<project>
    <parent>
        <groupId>com.company</groupId>
        <artifactId>microservice-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    
    <artifactId>user-service</artifactId>
    <name>用户服务</name>
    
    <dependencies>
        <!-- Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- 数据访问 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>druid-spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
        </dependency>
        
        <!-- 缓存 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- 服务发现 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        
        <!-- 工具库 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
        </dependency>
        
        <!-- API 文档 -->
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>
```

### 5. 最佳实践

#### ✅ 推荐做法
```xml
<dependencyManagement>
    <dependencies>
        <!-- 1. 优先使用 BOM 导入 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>2.7.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        
        <!-- 2. 使用属性管理版本号 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>${fastjson.version}</version>
        </dependency>
        
        <!-- 3. 明确指定 scope -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>${mysql.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- 4. 管理传递依赖的排除 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### ❌ 避免的错误
```xml
<!-- 错误示例 -->
<dependencyManagement>
    <dependencies>
        <!-- ❌ 硬编码版本号 -->
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>fastjson</artifactId>
            <version>1.2.83</version> <!-- 应该使用属性 -->
        </dependency>
        
        <!-- ❌ 在 dependencyManagement 中使用 scope=system -->
        <dependency>
            <groupId>com.oracle</groupId>
            <artifactId>ojdbc</artifactId>
            <version>12.1.0</version>
            <scope>system</scope> <!-- 不推荐 -->
            <systemPath>/path/to/ojdbc.jar</systemPath>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 6. dependencyManagement vs dependencies 对比

| 特性 | dependencyManagement | dependencies |
|------|---------------------|--------------|
| **实际引入依赖** | ❌ 否 | ✅ 是 |
| **版本管理** | ✅ 是 | ❌ 否 |
| **继承性** | ✅ 子模块继承 | ✅ 子模块继承 |
| **传递性** | ❌ 不传递 | ✅ 传递依赖 |
| **强制性** | ❌ 可选使用 | ✅ 必须使用 |
| **用途** | 版本统一管理 | 实际依赖声明 |

### 7. 常见问题解决

#### 问题1：版本冲突解决
```xml
<dependencyManagement>
    <dependencies>
        <!-- 强制指定版本，解决传递依赖版本冲突 -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>2.13.3</version>
        </dependency>
        
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.13.3</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 问题2：内部模块版本管理
```xml
<dependencyManagement>
    <dependencies>
        <!-- 使用 ${project.version} 确保内部模块版本一致 -->
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>common-utils</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <dependency>
            <groupId>com.company</groupId>
            <artifactId>common-entity</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

`dependencyManagement` 是 Maven 中实现依赖版本统一管理的核心机制，通过合理使用可以大大简化多模块项目的依赖管理复杂度，避免版本冲突问题，提高项目的可维护性。

## Gradle 依赖配置

Gradle 使用配置（Configuration）来管理依赖，比 Maven 更加灵活。

### 1. implementation（实现配置）- 推荐

```gradle
dependencies {
    implementation 'org.springframework:spring-core:5.3.21'
    implementation 'com.fasterxml.jackson.core:jackson-core:2.13.3'
}
```

**特点：**
- 编译、测试、运行时可用
- 不会泄露给消费者（依赖该项目的其他项目）
- 编译时隔离，提高构建性能

**对应 Maven：** compile（但更好的封装性）

### 2. api（API 配置）

```gradle
dependencies {
    api 'org.apache.commons:commons-lang3:3.12.0'
}
```

**特点：**
- 编译、测试、运行时可用
- 会传递给消费者
- 用于公开 API

**对应 Maven：** compile

### 3. compileOnly（仅编译配置）

```gradle
dependencies {
    compileOnly 'org.projectlombok:lombok:1.18.24'
    compileOnly 'javax.servlet:javax.servlet-api:4.0.1'
}
```

**特点：**
- 仅编译时可用
- 运行时不可用
- 不会传递给消费者

**对应 Maven：** provided

#### compileOnly 详细使用场景案例

##### 场景1：代码生成工具（最典型）
```gradle
dependencies {
    // Lombok - 编译时生成代码
    compileOnly 'org.projectlombok:lombok:1.18.24'
    annotationProcessor 'org.projectlombok:lombok:1.18.24'
    
    // MapStruct - 编译时生成映射代码
    compileOnly 'org.mapstruct:mapstruct:1.5.3.Final'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.3.Final'
    
    // AutoValue - Google的值类生成器
    compileOnly 'com.google.auto.value:auto-value-annotations:1.9'
    annotationProcessor 'com.google.auto.value:auto-value:1.9'
}
```

**实际代码示例：**
```java
// Lombok - 编译后会生成完整的getter/setter/equals/hashCode
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createTime;
}

// MapStruct - 编译时生成转换代码
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
    User toEntity(UserDTO dto);
    List<UserDTO> toDTOList(List<User> users);
}

// AutoValue - 编译时生成不可变值类
@AutoValue
public abstract class Address {
    public abstract String street();
    public abstract String city();
    public abstract String zipCode();
    
    public static Address create(String street, String city, String zipCode) {
        return new AutoValue_Address(street, city, zipCode);
    }
}
```

**编译后生成的代码特点：**
- Lombok 生成的代码直接编译到字节码中
- MapStruct 生成具体的实现类
- 运行时不需要这些库的存在

##### 场景2：静态分析和代码检查注解
```gradle
dependencies {
    // JSR-305 - 空值检查注解
    compileOnly 'com.google.code.findbugs:jsr305:3.0.2'
    
    // JetBrains 注解 - IDE和静态分析工具使用
    compileOnly 'org.jetbrains:annotations:23.0.0'
    
    // SpotBugs 注解
    compileOnly 'com.github.spotbugs:spotbugs-annotations:4.7.1'
    
    // ErrorProne 注解
    compileOnly 'com.google.errorprone:error_prone_annotations:2.15.0'
}
```

**代码示例：**
```java
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class UserService {
    
    // 空值检查 - IDE会提供警告
    public User findUser(@Nonnull String username) {
        Objects.requireNonNull(username, "Username cannot be null");
        return userRepository.findByUsername(username);
    }
    
    // 可能返回null的方法
    @Nullable
    public User findUserById(@NotNull Long id) {
        return userRepository.findById(id).orElse(null);
    }
    
    // 抑制特定的静态分析警告
    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH")
    public void processUser(User user) {
        // 某些复杂逻辑，静态分析工具可能误报
        if (user != null && user.getEmail() != null) {
            sendEmail(user.getEmail());
        }
    }
}
```

##### 场景3：编译时约束和验证
```gradle
dependencies {
    // Bean Validation API - 运行时由框架提供实现
    compileOnly 'javax.validation:validation-api:2.0.1.Final'
    
    // JPA 注解 - 在一些场景下只需要编译时
    compileOnly 'javax.persistence:javax.persistence-api:2.2'
    
    // Immutables - 不可变对象生成
    compileOnly 'org.immutables:value:2.9.0'
    annotationProcessor 'org.immutables:value:2.9.0'
}
```

**代码示例：**
```java
// Bean Validation - 编译时需要注解，运行时由Spring等框架提供实现
public class CreateUserRequest {
    @NotNull
    @Size(min = 3, max = 50)
    private String username;
    
    @Email
    @NotNull
    private String email;
    
    @Min(18)
    @Max(120)
    private Integer age;
    
    // getter/setter...
}

// JPA 注解 - 在某些工具类或DTO中只需要编译时
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    // getter/setter...
}

// Immutables - 编译时生成不可变类
@Value.Immutable
public interface UserInfo {
    String username();
    String email();
    Optional<String> phoneNumber();
    
    // 编译后会生成 ImmutableUserInfo 类
}
```

##### 场景4：编译时配置和元数据
```gradle
dependencies {
    // Spring Boot Configuration Processor - 生成配置元数据
    compileOnly 'org.springframework.boot:spring-boot-configuration-processor:2.7.0'
    annotationProcessor 'org.springframework.boot:spring-boot-configuration-processor:2.7.0'
    
    // Micronaut 编译时处理器
    compileOnly 'io.micronaut:micronaut-inject-java:3.6.0'
    annotationProcessor 'io.micronaut:micronaut-inject-java:3.6.0'
}
```

**配置属性示例：**
```java
// Spring Boot - 编译时生成配置元数据，IDE可以提供自动完成
@ConfigurationProperties(prefix = "app.user")
@Data
public class UserProperties {
    private int maxLoginAttempts = 3;
    private Duration sessionTimeout = Duration.ofMinutes(30);
    private List<String> allowedDomains = new ArrayList<>();
    private Cache cache = new Cache();
    
    @Data
    public static class Cache {
        private Duration ttl = Duration.ofMinutes(5);
        private int maxSize = 1000;
    }
}
```

##### 场景5：API 文档生成工具
```gradle
dependencies {
    // OpenAPI/Swagger 注解 - 编译时生成文档
    compileOnly 'io.swagger.core.v3:swagger-annotations:2.2.2'
    
    // JavaDoc 增强注解
    compileOnly 'com.github.therapi:therapi-runtime-javadoc:0.15.0'
    annotationProcessor 'com.github.therapi:therapi-runtime-javadoc-scribe:0.15.0'
}
```

**API 文档注解示例：**
```java
@RestController
@Tag(name = "User Management", description = "用户管理相关接口")
public class UserController {
    
    @Operation(summary = "创建用户", description = "创建新的用户账号")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "409", description = "用户名已存在")
    })
    @PostMapping("/users")
    public ResponseEntity<UserDTO> createUser(
        @RequestBody @Valid CreateUserRequest request) {
        // 实现逻辑
        return ResponseEntity.ok(userService.createUser(request));
    }
}
```

##### 场景6：平台特定的编译时依赖
```gradle
dependencies {
    // Android 相关 - 只在编译时需要
    compileOnly 'com.google.android:android:4.1.1.4'
    
    // GWT (Google Web Toolkit) - 编译时转换为JavaScript
    compileOnly 'com.google.gwt:gwt-user:2.10.0'
    
    // OSGi 注解 - 编译时生成元数据
    compileOnly 'org.osgi:osgi.annotation:8.1.0'
}
```

#### Maven 中对应的 provided 场景

在 Maven 中，类似的场景使用 `provided` 范围：

```xml
<!-- Maven 对应配置 -->
<dependencies>
    <!-- 代码生成工具 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.24</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- 静态分析注解 -->
    <dependency>
        <groupId>com.google.code.findbugs</groupId>
        <artifactId>jsr305</artifactId>
        <version>3.0.2</version>
        <scope>provided</scope>
    </dependency>
    
    <!-- Spring Boot 配置处理器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <version>2.7.0</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### 编译时依赖的判断标准

**什么时候使用编译时依赖？**

1. **代码生成工具** - 编译期间生成代码，运行时不需要
2. **注解处理器** - 处理注解并生成代码或元数据
3. **静态分析工具** - 提供编译期检查，不影响运行时
4. **文档生成** - 生成API文档，运行时不需要
5. **平台工具** - 特定平台的编译工具

**关键特征：**
- 编译后的字节码中不包含对这些库的直接引用
- 运行时环境不需要这些依赖
- 主要用于编译期间的代码转换、生成或检查

#### 性能和体积优势

```bash
# 使用编译时依赖的优势对比

# ❌ 错误做法 - 使用 implementation
implementation 'org.projectlombok:lombok:1.18.24'  # 会打包到最终jar中

# ✅ 正确做法 - 使用 compileOnly
compileOnly 'org.projectlombok:lombok:1.18.24'     # 不会打包，减少文件大小

# 最终jar文件对比：
# 错误做法：app.jar (15.2 MB) - 包含lombok.jar
# 正确做法：app.jar (14.8 MB) - 不包含lombok.jar
```

这些编译时依赖的正确使用可以显著减少最终应用的体积，避免不必要的运行时依赖，提高应用启动性能。

### 4. runtimeOnly（仅运行时配置）

```gradle
dependencies {
    runtimeOnly 'mysql:mysql-connector-java:8.0.29'
    runtimeOnly 'ch.qos.logback:logback-classic:1.2.11'
}
```

**特点：**
- 仅运行时可用
- 编译时不可用
- 会传递给消费者

**对应 Maven：** runtime

### 5. testImplementation（测试实现配置）

```gradle
dependencies {
    testImplementation 'junit:junit:4.13.2'
    testImplementation 'org.mockito:mockito-core:4.6.1'
}
```

**特点：**
- 仅测试时可用
- 包含 implementation 的所有依赖
- 不会传递给消费者

**对应 Maven：** test

### 6. testCompileOnly（测试仅编译配置）

```gradle
dependencies {
    testCompileOnly 'org.projectlombok:lombok:1.18.24'
}
```

**特点：**
- 仅测试编译时可用
- 测试运行时不可用

### 7. testRuntimeOnly（测试仅运行时配置）

```gradle
dependencies {
    testRuntimeOnly 'com.h2database:h2:2.1.212'
}
```

**特点：**
- 仅测试运行时可用
- 测试编译时不可用

## 实际项目示例

### Maven 项目示例（Spring Boot Web 应用）

```xml
<dependencies>
    <!-- 核心框架 - compile scope -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>2.7.0</version>
        <!-- scope默认为compile -->
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
        <version>2.7.0</version>
    </dependency>

    <!-- 数据库驱动 - runtime scope -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <version>8.0.29</version>
        <scope>runtime</scope>
    </dependency>

    <!-- 编译时工具 - provided scope -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.24</version>
        <scope>provided</scope>
    </dependency>

    <!-- Servlet API - provided scope -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>

    <!-- 测试依赖 - test scope -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <version>2.7.0</version>
        <scope>test</scope>
    </dependency>

    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <version>2.1.212</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Gradle 项目示例（Spring Boot Web 应用）

```gradle
dependencies {
    // 核心框架 - implementation
    implementation 'org.springframework.boot:spring-boot-starter-web:2.7.0'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa:2.7.0'
    implementation 'org.springframework.boot:spring-boot-starter-validation:2.7.0'
    
    // 公共工具库 - api（如果要暴露给消费者）
    api 'org.apache.commons:commons-lang3:3.12.0'
    
    // 数据库驱动 - runtimeOnly
    runtimeOnly 'mysql:mysql-connector-java:8.0.29'
    
    // 编译时工具 - compileOnly
    compileOnly 'org.projectlombok:lombok:1.18.24'
    
    // 注解处理器
    annotationProcessor 'org.projectlombok:lombok:1.18.24'
    
    // 测试依赖 - testImplementation
    testImplementation 'org.springframework.boot:spring-boot-starter-test:2.7.0'
    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2'
    testImplementation 'org.mockito:mockito-core:4.6.1'
    
    // 测试数据库 - testRuntimeOnly
    testRuntimeOnly 'com.h2database:h2:2.1.212'
}
```

## 依赖范围对比表

| 阶段/工具 | Maven | Gradle | 编译时 | 测试时 | 运行时 | 传递性 | 打包 |
|----------|-------|--------|--------|--------|--------|--------|------|
| 默认依赖 | compile | implementation | ✓ | ✓ | ✓ | ✓/✗ | ✓ |
| API依赖 | compile | api | ✓ | ✓ | ✓ | ✓ | ✓ |
| 已提供 | provided | compileOnly | ✓ | ✓ | ✗ | ✗ | ✗ |
| 仅运行时 | runtime | runtimeOnly | ✗ | ✓ | ✓ | ✓ | ✓ |
| 仅测试 | test | testImplementation | ✗ | ✓ | ✗ | ✗ | ✗ |

## 最佳实践建议

### 1. Maven 最佳实践

```xml
<!-- ✅ 正确使用 -->
<dependencies>
    <!-- 业务依赖使用默认compile -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
    </dependency>
    
    <!-- 数据库驱动使用runtime -->
    <dependency>
        <groupId>postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- 容器提供的API使用provided -->
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>servlet-api</artifactId>
        <scope>provided</scope>
    </dependency>
    
    <!-- 测试依赖使用test -->
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Gradle 最佳实践

```gradle
dependencies {
    // ✅ 优先使用 implementation 而不是 api
    implementation 'org.springframework:spring-context:5.3.21'
    
    // ✅ 只有当依赖需要暴露给消费者时才使用 api
    api 'com.fasterxml.jackson.core:jackson-annotations:2.13.3'
    
    // ✅ 编译时工具使用 compileOnly
    compileOnly 'org.projectlombok:lombok:1.18.24'
    annotationProcessor 'org.projectlombok:lombok:1.18.24'
    
    // ✅ 运行时依赖使用 runtimeOnly
    runtimeOnly 'ch.qos.logback:logback-classic:1.2.11'
    
    // ✅ 测试依赖明确使用 test* 配置
    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2'
    testRuntimeOnly 'com.h2database:h2:2.1.212'
}
```

### 3. 常见错误避免

```xml
<!-- ❌ 错误：将运行时依赖设为compile -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <scope>compile</scope> <!-- 应该是runtime -->
</dependency>

<!-- ❌ 错误：将provided依赖设为compile -->
<dependency>
    <groupId>javax.servlet</groupId>
    <artifactId>servlet-api</artifactId>
    <scope>compile</scope> <!-- 应该是provided -->
</dependency>
```

```gradle
dependencies {
    // ❌ 错误：过度使用 api
    api 'org.apache.commons:commons-lang3:3.12.0' // 如果不需要暴露，应该用implementation
    
    // ❌ 错误：将编译时工具设为implementation
    implementation 'org.projectlombok:lombok:1.18.24' // 应该是compileOnly
}
```

## 性能影响分析

### 编译性能
- **Gradle implementation vs api**: `implementation` 提供更好的编译隔离，当依赖变化时减少重编译范围
- **Maven**: 所有 `compile` 依赖都可能影响下游项目编译

### 运行时性能
- 正确的scope设置可以减少最终打包文件大小
- `provided` 和 `compileOnly` 避免运行时冲突

### 依赖传递控制
- `implementation` (Gradle) 提供更好的依赖封装
- `api` (Gradle) 和 `compile` (Maven) 会传递依赖

选择正确的依赖范围不仅影响项目的构建性能，还关系到最终应用的运行时行为和部署包大小。理解并正确使用这些范围是Java项目开发的重要技能。
