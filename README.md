# Spring-transaction
Spring事务管理学习

## 1.声明式事务概念

### 1.1 编程式事务

​	事务功能的相关操作全部通过自己编写代码来实现：

编程式的实现方式存在缺陷： 

+ 细节没有被屏蔽：具体操作过程中，所有细节都需要程序员自己来完成，比较繁琐。 

+ 代码复用性不高：如果没有有效抽取出来，每次实现功能都需要自己编写代码，代码就没有得到复用。

------------

### 1.2声明式事务

既然事务控制的代码有规律可循，代码的结构基本是确定的，所以框架就可以将固定模式的代码抽取出来，进行相关的封装。

封装起来后，我们只需要在配置文件中进行简单的配置即可完成操作。

+ 好处 1 ：提高开发效率

+ 好处 2 ：消除了冗余的代码

+ 好处 3 ：框架会综合考虑相关领域中在实际开发环境下有可能遇到的各种问题，进行了健壮性、性能等各个方面的优化

所以，我们可以总结下面两个概念：

+ **编程式 ：** 自己写代码 实现功能

+ **声明式 ：**通过 配置 让 框架 实现功能

-----------

## 2. 基于注解的声明式事务

### 2.1 准备工作

1.加入依赖

```xml
<dependencies>
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
    <scope>test</scope>
  </dependency>

  <!-- Spring -->
  <!-- https://mvnrepository.com/artifact/org.springframework/spring-context -->
  <dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.22</version>
  </dependency>

  <!-- Spring-orm -->
  <!-- https://mvnrepository.com/artifact/org.springframework/spring-orm -->
  <dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-orm</artifactId>
    <version>5.3.22</version>
  </dependency>

  <!-- Spring-test -->
  <!-- https://mvnrepository.com/artifact/org.springframework/spring-test -->
  <dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-test</artifactId>
    <version>5.3.22</version>
    <scope>test</scope>
  </dependency>

  <!-- MySQL驱动包 -->
  <!-- https://mvnrepository.com/artifact/mysql/mysql-connector-java -->
  <dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.30</version>
  </dependency>

  <!-- 数据源 -->
  <!-- https://mvnrepository.com/artifact/com.alibaba/druid -->
  <dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid</artifactId>
    <version>1.2.11</version>
  </dependency>

</dependencies>
```

2.创建jdbc.properties

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://localhost:3306/ssm?serverTimezone=UTC
jdbc.username=root
jdbc.password=xfy010320
```

3.配置spring的配置文件

```xml
<!-- 扫描组件 -->
<context:component-scan base-package="com.Xie"/>

<!-- 引入jdbc.properties配置文件 -->
<context:property-placeholder location="classpath:jdbc.properties" />

<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="driverClassName" value="${jdbc.driver}"/>
    <property name="url" value="${jdbc.url}"/>
    <property name="username" value="${jdbc.username}"/>
    <property name="password" value="${jdbc.password}"/>
</bean>

<bean class="org.springframework.jdbc.core.JdbcTemplate">
    <property name="dataSource" ref="dataSource"/>
</bean>
```

4.创建组件

创建controller

```java
public class BookController {

    @Autowired
    private BookService bookService;

    public void buyBook(Integer userId , Integer bookId){
        bookService.buyBook(userId, bookId);
    }

    }
```

创建接口BookService

```java
public interface BookService {

    /**
     * 买书
     * @param userId
     * @param bookId
     */
    void buyBook(Integer userId, Integer bookId);
}
```

创建实现类BookServiceImpl

```java
public class BookServiceImpl implements BookService {

    @Autowired
    private BookDao bookDao;

    @Override
    public void buyBook(Integer userId, Integer bookId) {
        //查询图书价格
        Integer price = bookDao.getPriceByBookId(bookId);
        //更新图书库存
        bookDao.updateStock(bookId);
        //更新用户余额
        bookDao.updateBalance(userId, price);
    }
}
```

创建接口BookDao

```java
public interface BookDao {
    /**
     * 根据图书id查询图书价格
     * @param bookId
     * @return
     */
    Integer getPriceByBookId(Integer bookId);

    /**
     * 更新图书库存
     * @param bookId
     */
    void updateStock(Integer bookId);

    /**
     * 更新用户余额
     * @param userId
     * @param price
     */
    void updateBalance(Integer userId, Integer price);
}
```

创建实现类BookDaoImpl

```java
@Repository
public class BookDaoImpl implements BookDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Override
    public Integer getPriceByBookId(Integer bookId) {

        String sql = "select price from t_book where book_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, bookId);
    }

    @Override
    public void updateStock(Integer bookId) {
        String sql = "update t_book set stock = stock - 1 where book_id = ?";
        jdbcTemplate.update(sql, bookId);
    }

    @Override
    public void updateBalance(Integer userId, Integer price) {
        String sql = "update t_user set balance = balance - ? where user_id = ?";
        jdbcTemplate.update(sql, price, userId);
    }
}
```

**如果没有事务，每条SQL语句都会单独提交。例如买书时余额不够的情况下，更新库存已经成功提交，但是余额不足，无法更新余额，导致逻辑错误**

---

### 2.2加入事务

1.添加事务配置

```xml
<!--配置事务管理器-->
<bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
    <property name="dataSource" ref="dataSource"/>
</bean>

<!--
    开启事务的注解驱动
    将使用@Transactional注解的方法或类中的所有方法被spring事务管理
    transaction-manager属性设置事务管理器的id
    若事务管理器的id默认为transactionManager，则可以不设置该属性
-->
<tx:annotation-driven transaction-manager="transactionManager"/>
```

2.添加事务注解

因为service层表示业务逻辑层，一个方法表示一个完成的功能，因此处理事务一般在service层处理 在BookServiceImpl的buybook()添加注解@Transactional

3.结果

余额不足时库存和余额都无变化。
