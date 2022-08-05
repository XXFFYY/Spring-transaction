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

+ **编程式**： 自己写代码 实现功能

+ **声明式** ：通过 配置 让 框架 实现功能

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

---

### 2.3@Transactional注解标识的位置

+ @Transactional标识在方法上，咋只会影响该方法 

+ @Transactional标识的类上，咋会影响类中所有的方法

+ ---

### 2.4事务属性：只读

​	 	**对一个查询操作来说，如果我们把它设置成只读，就能够明确告诉数据库，这个操作不涉及写操作。这样数据库就能够针对查询操作来进行优化。**

1.使用方式

```java
    @Transactional(
            readOnly = true
    )
    public void buyBook(Integer userId, Integer bookId) {
        //查询图书价格
        Integer price = bookDao.getPriceByBookId(bookId);
        //更新图书库存
        bookDao.updateStock(bookId);
        //更新用户余额
        bookDao.updateBalance(userId, price);
    }
```

**注意:**

对增删改操作设置只读会抛出下面异常： 

Caused by: java.sql.SQLException: Connection is read-only. Queries leading to data modification are not allowed

---

### 2.5事务属性：超时

​		 事务在执行过程中，有可能因为遇到某些问题，导致程序卡住，从而长时间占用数据库资源。而长时间 占用资源，大概率是因为程序运行出现了问题（可能是Java程序或MySQL数据库或网络连接等等）。 

​		 此时这个很可能出问题的程序应该被回滚，撤销它已做的操作，事务结束，把资源让出来，让其他正常 程序可以执行。

​		 概括来说就是一句话：超时回滚，释放资源。

1.使用方式

```java
@Transactional(
        //readOnly = true
        timeout = 3
)
public void buyBook(Integer userId, Integer bookId) {
    try {
        TimeUnit.SECONDS.sleep(5);
    }catch (Exception e){
        e.printStackTrace();
    }
    //查询图书价格
    Integer price = bookDao.getPriceByBookId(bookId);
    //更新图书库存
    bookDao.updateStock(bookId);
    //更新用户余额
    bookDao.updateBalance(userId, price);
}
```

2.结果

执行过程中抛出异常：

org.springframework.transaction.**TransactionTimedOutException**: Transaction timed out: deadline was Thu Aug 04 20:33:01 CST 2022

---

### 2.6事务属性：回滚策略

声明式事务默认只针对运行时异常回滚，编译时异常不回滚。

可以通过@Transactional中相关属性设置回滚策略 

+ rollbackFor属性：需要设置一个Class类型的对象 

+ rollbackForClassName属性：需要设置一个字符串类型的全类名 

+ noRollbackFor属性：需要设置一个Class类型的对象 

+ rollbackFor属性：需要设置一个字符串类型的全类名

1.使用方法

```java
@Transactional(
        //readOnly = true
        //timeout = 3
        //noRollbackFor = ArithmeticException.class
        noRollbackForClassName = "java.lang.ArithmeticException"
)
public void buyBook(Integer userId, Integer bookId) {
    /*try {
        TimeUnit.SECONDS.sleep(5);
    }catch (Exception e){
        e.printStackTrace();
    }*/
    //查询图书价格
    Integer price = bookDao.getPriceByBookId(bookId);
    //更新图书库存
    bookDao.updateStock(bookId);
    //更新用户余额
    bookDao.updateBalance(userId, price);
    System.out.println(1/0);
}
```

2.结果

虽然购买图书功能中出现了数学运算异常（ArithmeticException），但是我们设置的回滚策略是，当出现ArithmeticException不发生回滚，因此购买图书的操作正常执行

---

### 2.7事务属性：事务隔离级别

数据库系统必须具有隔离并发运行各个事务的能力，使它们不会相互影响，避免各种并发问题。一个事 务与其他事务隔离的程度称为隔离级别。SQL标准中规定了多种事务隔离级别，不同隔离级别对应不同 的干扰程度，隔离级别越高，数据一致性就越好，但并发性越弱。 

隔离级别一共有四种： 

+ 读未提交：READ UNCOMMITTED 

  允许Transaction01读取Transaction02未提交的修改。 

+ 读已提交：READ COMMITTED

   要求Transaction01只能读取Transaction02已提交的修改。 

+ 可重复读：REPEATABLE READ 

  确保Transaction01可以多次从一个字段中读取到相同的值，即Transaction01执行期间禁止其它 事务对这个字段进行更新。 

+ 串行化：SERIALIZABLE 

  确保Transaction01可以多次从一个表中读取到相同的行，在Transaction01执行期间，禁止其它 事务对这个表进行添加、更新、删除操作。可以避免任何并发问题，但性能十分低下。

 各个隔离级别解决并发问题的能力见下表：

| 隔离级别         | 脏读 | 不可重复读 | 幻读 |
| ---------------- | ---- | ---------- | ---- |
| READ UNCOMMITTED | 有   | 有         | 有   |
| READ COMMITTED   | 无   | 有         | 有   |
| REPEATABLE READ  | 无   | 无         | 有   |
| SERIALIZABLE     | 无   | 无         | 无   |

各种数据库产品对事务隔离级别的支持程度：

| 隔离级别         | Oracle  | MySQL   |
| ---------------- | ------- | ------- |
| READ UNCOMMITTED | ×       | √       |
| READ COMMITTED   | √(默认) | √       |
| REPEATABLE READ  | ×       | √(默认) |
| SERIALIZABLE     | √       | √       |

1.使用方法

```java
@Transactional(isolation = Isolation.DEFAULT)//使用数据库默认的隔离级别 
@Transactional(isolation = Isolation.READ_UNCOMMITTED)//读未提交
@Transactional(isolation = Isolation.READ_COMMITTED)//读已提交
@Transactional(isolation = Isolation.REPEATABLE_READ)//可重复读 
@Transactional(isolation = Isolation.SERIALIZABLE)//串行化
```

---

### 2.8事务属性：事务传播行为

当事务方法被另一个事务方法调用时，必须指定事务应该如何传播。例如：方法可能继续在现有事务中 运行，也可能开启一个新事务，并在自己的事务中运行。

1.使用方法

创建接口CheckoutService：

```java
public interface CheckoutService {
    void checkout(Integer userId, Integer[] bookIds);
}
```

创建实现类CheckoutServiceImpl：

```java
@Service
public class CheckoutServiceImpl implements CheckoutService {

    @Autowired
    private BookService bookService;
    @Override
    @Transactional
    public void checkout(Integer userId, Integer[] bookIds) {
        for (Integer bookId : bookIds) {
            bookService.buyBook(userId, bookId);
        }
    }
}
```

在BookController中添加方法：

```java
@Autowired
private CheckoutService checkoutService;

public void checkout(Integer userId, Integer[] bookIds) {
    checkoutService.checkout(userId, bookIds);
}
```

2.结果

​	 可以通过@Transactional中的propagation属性设置事务传播行为 

​	 修改BookServiceImpl中buyBook()上，注解@Transactional的propagation属性 

​	 @Transactional(propagation = Propagation.REQUIRED)，默认情况，表示如果当前线程上有已经开 启的事务可用，那么就在这个事务中运行。经过观察，购买图书的方法buyBook()在checkout()中被调 用，checkout()上有事务注解，因此在此事务中执行。所购买的两本图书的价格为80和50，而用户的余 额为100，因此在购买第二本图书时余额不足失败，导致整个checkout()回滚，即只要有一本书买不 了，就都买不了

​	 @Transactional(propagation = Propagation.REQUIRES_NEW)，表示不管当前线程上是否有已经开启 的事务，都要开启新事务。同样的场景，每次购买图书都是在buyBook()的事务中执行，因此第一本图 书购买成功，事务结束，第二本图书购买失败，只在第二次的buyBook()中回滚，购买第一本图书不受 影响，即能买几本就买几本

---

## 3.基于XML的声明式事务

### 3.1修改Spring配置文件

```xml
<!--配置事务通知-->
<tx:advice id="tx" transaction-manager="transactionManager">
    <tx:attributes>
        <tx:method name="buyBook"/>
    </tx:attributes>
</tx:advice>

<aop:config>
    <aop:advisor advice-ref="tx" pointcut="execution(* com.Xie.service.impl.*.*(..))"/>
</aop:config>
```

**注意：基于xml实现的声明式事务，必须引入aspectJ的依赖**

```xml
<!-- https://mvnrepository.com/artifact/org.springframework/spring-aspects -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-aspects</artifactId>
    <version>5.3.22</version>
</dependency>
```

