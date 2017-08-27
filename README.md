(English instruction please see [README-ENGLISH.md](README-ENGLISH.md) )  
## jTransactions
开源协议: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions是一个事明式事务工具，目的是将Java的事务功能独立出来，做成一个单独的库来使用。jTransactions目前仅包含两个实现: TinyTX和SpringTx。  
TinyTX是一个微型的只有4个文件组成的、具有简单功能的声明式事务服务，是Spring事务之外一个小巧的选择。  
SpringTx是对Spring声明式事务的包装，SpringTx适用于开发复杂的大型项目(例如使用到了嵌套事务)，使用SpringTx需要在项目中添加对Spring库的依赖。原则上，除了TinyTx外，jTransactions不重新开发自已的事务实现，其它事务服务如Spring事务、JTA事务、EJB容器事务等将只做简单的包装容纳进这个项目，jTransactions与具体实现的关系正如common-log与Log4J的关系，一个是接口，一个是具体的实现。  
jTransactions是一个可扩充架构，将来可能会不断有新的声明式事务实现添加进来。jTransactions运行环境为Java6或以上。 
  
### 如何引入项目?  
方式一：手工下载jtransactions-1.0.0.jar并放置于项目的类目录。
方式二：在项目的pom.xml文件中加入如下行：
```
   <dependency>  
      <groupId>com.github.drinkjava2</groupId>  
      <artifactId>jtransactions</artifactId>  
      <version>1.x.x</version>  
   </dependency>
``` 
注：  
1. 如果只使用TinyTX，以上配置就可以了。如果使用了SpringTx, 还需要在pom.xml中添加对[Spring-tx](https://mvnrepository.com/artifact/org.springframework/spring-tx)的依赖。   
2. jTransactions还必须与一个支持AOP Alliance接口规范的AOP工具如Spring或jBeanBox配合使用，因为声明式事务的基本原理是对方法的拦截，必须有一个AOP工具来实现这个功能。
3. jTransactions还必须一个JDBC工具配合使用，如纯JDBC、JdbcTemplate、DbUtils-pro或jSqlBox等。

### 使用   
jTransactions分为连接管理器ConnectionManager和TxInterceptor两个接口，ConnectionManager接口的实现类的两个方法，getConnection和releaseConnection，如使用纯JDBC,必须用这两个方法来获取和释放连接。如使用一些工具类如JdbcTemplate、DbUtils-Pro和jSqlBox等，其内部获取和释放连接已经调用这两个方法，所以无需关心连接的获取和释放。TxInterceptor实现类是由框架来提供，用户不必关心实现细节，只需要利用AOP工具配置使用即可。以下分别介绍两个实现TinyTx和SpringTx的使用： 

一. TinyTx声明式事务演示 TinyTx是一个核心只有4个类组成的微型的声明式事务实现，它旨在小项目中替代复杂的Spring声明式事务，在精简功能的同时达到降低学习成本、提高项目的掌控感(即可维护性)的目的。其功能比较简单，只要发现有未捕获的任何异常即回滚事务，通常用于小型项目的开发。以下是演示TinyTx(内含于jTransactions中) + jBeanBox + DbUtils-pro配合使用的一个范例：  
```
    public class TinyTxBox extends BeanBox {//TinyTx的配置
	  {
		this.setConstructor(TinyTx.class, BeanBox.getBean(DataSourceBox.class), Connection.TRANSACTION_READ_COMMITTED);
	  }
    }
 
    public class TinyTxManagerTest {//测试类
	DbPro dbPro = new DbPro((DataSource) BeanBox.getBean(DataSourceBox.class));

	@Before
	public void createDB() {
		DbPro db = new DbPro((DataSource) BeanBox.getBean(DataSourceBox.class));
		try {
			db.iExecute("drop table users");
		} catch (Exception e) {
		}
		db.iExecute("create table users(id varchar(36), name varchar(40) )engine=InnoDB");
	}

	@After
	public void cleanup() {
		BeanBox.defaultContext.close();// close datasource pool
	}
 
	public void insert1() {
		dbPro.iExecute("insert into users (id) values(?)" + param0(UUID.randomUUID().toString()));
		Assert.assertEquals(1L, dbPro.iQueryForObject("select count(*) from users"));
		System.out.println("Now have 1 records in users table");
	}
 
	public void insert2() {
		dbPro.iExecute("insert into users (id) values(?)" + param0(UUID.randomUUID().toString()));
		Assert.assertEquals(2L, dbPro.iQueryForObject("select count(*) from users"));
		System.out.println("Now have 2 records in users table, but will roll back to 0");
		System.out.println(1 / 0); //抛一个异常，事务将回滚
	}

	@AopAround(TinyTxBox.class)
	public void doInsert1() { //这个方法被设置了一个事务切面
		insert1();// this one inserted 1 record
		insert2();// this one did not insert, roll back
	}

	@Test
	public void doTest1() throws Exception {
		System.out.println("============Testing: TinyTxManagerTest doTest1============");
		TinyTxManagerTest tester = BeanBox.getBean(TinyTxManagerTest.class);
		Assert.assertEquals(0L, dbPro.iQueryForObject("select count(*) from users"));
		try {
			tester.doInsert1();
		} catch (Exception e) {
			System.out.println("div/0 exception found, doInsert1 should roll back");
		}
		Assert.assertEquals(0L, dbPro.iQueryForObject("select count(*) from users"));
		BeanBox.defaultContext.close();// Release DataSource Pool
	}
}
```

三. SpringTX声明式事务演示 
    jTransactions只是对Spring-TX做了简单包装，配置上与Spring-TX极为类似， 
```
    待加入
```

以上即为jTransactions全部文档，如有疑问，请下载并运行单元测试示例或查看源码。