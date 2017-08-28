(English instruction please see [README-ENGLISH.md](README-ENGLISH.md) )  
## jTransactions
开源协议: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions是一个事明式事务工具，目的是将Java的事务功能独立出来，做成一个单独的库来使用。jTransactions目前仅包含两个实现: TinyTX和SpringTx。  
TinyTX是一个微型的只有4个文件组成的、具有简单功能的声明式事务服务，是Spring事务之外一个小巧的选择。  
SpringTx是对Spring声明式事务的包装，SpringTx适用于开发复杂的大型项目(例如使用到了嵌套事务)，使用SpringTx需要在项目中添加对Spring库的依赖。原则上，除了TinyTx外，jTransactions不重新开发自已的事务实现，其它事务服务如Spring事务、JTA事务、EJB容器事务等将只做简单的包装容纳进这个项目，jTransactions与具体实现的关系正如common-log与Log4J的关系，一个是接口，一个是具体的实现。  
jTransactions是一个可扩充的架构，将来可能会不断有新的事务实现添加进来。jTransactions运行环境为Java6或以上。 

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
1. 如果只使用TinyTX，以上配置就可以了。如果使用SpringTx, 还需要在pom.xml中添加对[Spring-tx](https://mvnrepository.com/artifact/org.springframework/spring-tx)的依赖。   
2. jTransactions还必须与一个支持AOP Alliance接口规范的AOP工具如Spring或jBeanBox配合使用，因为声明式事务的基本原理是对方法的拦截，必须有一个AOP工具来实现这个功能。
3. jTransactions还必须一个JDBC工具配合使用，如纯JDBC、JdbcTemplate、DbUtils-pro或jSqlBox等。

### 使用   
jTransactions分为连接管理器ConnectionManager和TxInterceptor两个接口，ConnectionManager接口的实现类的两个方法，getConnection和releaseConnection，如使用纯JDBC,必须用这两个方法来获取和释放连接。如使用一些工具类如JdbcTemplate、DbUtils-Pro和jSqlBox等，其内部获取和释放连接已经调用这两个方法，所以无需关心连接的获取和释放。TxInterceptor实现类是由框架来提供，用户不必关心实现细节，只需要利用AOP工具配置使用即可。以下分别介绍两个实现TinyTx和SpringTx的使用： 

一. TinyTx声明式事务演示 TinyTx是一个核心只有4个类组成的微型的声明式事务实现，它旨在小项目中替代复杂的Spring声明式事务，在精简功能的同时达到降低学习成本、提高项目的掌控感(即可维护性)的目的。其功能比较简单，只要发现有未捕获的任何异常即回滚事务，可用于小型项目的开发。以下是演示TinyTx(内含于jTransactions中) + jBeanBox + DbUtils-pro配合使用的一个范例：  
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
	public void doInsert1() { //这里设置了一个事务切面
		insert1();// 这个方法插入一条记录
		insert2();// 这个方法出错，造成整个事务回滚
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

二. SpringTX声明式事务演示 
    jTransactions对Spring声明式事务做了简单包装，配置上基本原搬Spring配置方式，详见test目录下SpringTx目录的测试示例。 
	
三. 运行期动态配置事务服务
    以下是在运行期动态配置并切换TinyTx事务和SpringTx事务的一个演示，利用到了jBeanBox的动态配置特性。  
``
public class CommonTxTester {

	public static class TxBox extends BeanBox {
		{
			this.setConstructor(TinyTx.class, BeanBox.getBean(DataSourceBox.class),
					Connection.TRANSACTION_READ_COMMITTED);
		}
	}

	public static class TxTester {
		TinyJdbc tiny;

		public TxTester(DataSource ds, ConnectionManager cm, CommonTx tx) {
			tiny = new TinyJdbc(ds, cm);
		}

		@AopAround(TxBox.class)
		public void tx_Insert1() {
			tiny.executeSql("insert into users (id) values('123')");
		}

		@AopAround(TxBox.class)
		public void tx_Insert2() {
			tiny.executeSql("insert into users (id) values('456')");
			Assert.assertEquals(2L, tiny.queryForObject("select count(*) from users"));
			System.out.println("Now have 2 records in users table, but will roll back to 1");
			System.out.println(1 / 0);
		}

		public void doTest() {
			try {
				tiny.executeSql("drop table users");
			} catch (Exception e) {
			}
			tiny.executeSql("create table users (id varchar(40))engine=InnoDB");
			Assert.assertEquals(0L, tiny.queryForObject("select count(*) from users"));
			try {
				tx_Insert1();// this one inserted 1 record
				tx_Insert2();// this one did not insert, roll back
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("div/0 exception found, tx_Insert2 should roll back");
			}
			Assert.assertEquals(1L, tiny.queryForObject("select count(*) from users"));
		}
	}

	@Test
	public void userTinyTx() {
		BeanBox testerBox = new BeanBox(); //jBeanBox is a IOC/AOP tool
		testerBox.setConstructor(TxTester.class, BeanBox.getBean(DataSourceBox.class),
				TinyTxConnectionManager.instance(), BeanBox.getBean(TxBox.class));
		testerBox.setConstructorTypes(DataSource.class, ConnectionManager.class, CommonTx.class);

		TxTester tester = testerBox.getBean();
		System.out.println(tester);
		tester.doTest();
		BeanBox.defaultContext.close();// Release DataSource Pool
	}

	@Test
	public void userSpringTx() {
		DataSource ds = BeanBox.getBean(DataSourceBox.class);
		DataSourceTransactionManager pm = new DataSourceTransactionManager();
		pm.setDataSource(ds);
		Properties props = new Properties();
		props.put("tx_*", "PROPAGATION_REQUIRED");

		BeanBox springBox = new BeanBox();
		springBox.setConstructor(SpringTx.class, pm, props);
		SpringTx springTx = springBox.getBean();

		BeanBox testerBox = new BeanBox();
		testerBox.setConstructorTypes(DataSource.class, ConnectionManager.class, CommonTx.class);
		testerBox.setConstructor(TxTester.class, ds, SpringConnectionManager.instance(), springTx);
		// below replace singleton to SpringTX
		BeanBox.defaultContext.getSignletonCache().put(TxBox.class.getName(), springTx);

		TxTester tester = testerBox.getBean();
		System.out.println(tester);
		tester.doTest();
		BeanBox.defaultContext.close();// Release DataSource Pool
	}
```

以上即为jTransactions全部文档，如有疑问，请下载并运行单元测试示例或查看源码。

后记：关于这个项目，本来我根本没打算开发它，事务是一个烂了街的话题，在开发DbUtils-Pro和jSqlBox项目时，需要找一个小巧点的独立的事务工具，原以为这方面的开源项目会有一大把，但很遗憾的是，我一个也没有找到！只好自已从头写一个TinyTx，水平很烂，但是勉强能够工作了。 至于为什么不直接用Spring的事务是因为它太大了、太占内存、太复杂，太多标签、太多依赖，绑死在Spring环 境中无法抽取出来，使用Spring的声明式事务居然要加上JdbcTemplate的库依赖，事务服务依赖于JDBC工具，简直是太搞了，难道这就是Spring号称的反转依赖不成? 至于jfinal的事务服务, 看了一下源码，比较简单，但是依然写死在了Jfinal环境中，不能抽取出来，而且没有和数据源绑定。这种架构将来在多数据源开发时会有问题。  jTransaction的架构是开放式的，甚至充许在运行期动态切换事务服务，今后如果有人发现了什么小巧、成熟的事务工具也请告诉我，我可将它加到这个项目中来。目前下一步的开发打算是研究一下，能不能将一个现成的JTA事务实现集成进来，好支持分步式事务。