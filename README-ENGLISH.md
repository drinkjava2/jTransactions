(English instruction please see [README-ENGLISH.md](README-ENGLISH.md) )  
## jTransactions
Licenses: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions is a seperated Java Transaction service tool。jTransactions currently include 2 implementations: TinyTX and SpringTx。  
TinyTX is a mico declarative transaction service only has 4 files.   
SpringTx is the wrap of Spring's declarative transaction service (Spring-TX).
jTransactions is an extendable framework，in future will add more transaction services. jTransactions runs on Java6 or above.
  
### How to import jTransactions?  
Download jtransactions-1.0.0.jar and put into project's lib folder, or add below lines in pom.xml: 
```
   <dependency>  
      <groupId>com.github.drinkjava2</groupId>  
      <artifactId>jtransactions</artifactId>  
      <version>1.x.x</version>  
   </dependency>
``` 
Note：  
If use SpringTx, also need add [Spring-tx](https://mvnrepository.com/artifact/org.springframework/spring-tx) dependency in pom.xml. 

### Use
jTransactions has 2 interfaces: ConnectionManager and TxInterceptor, ConnectionManager has two method: getConnection and releaseConnection，if use pure JDBC, need use these 2 method to get or release connection. TxInterceptor implementations are supplied by framework, users only need to direct use it by configure it in an IOC/AOP tool.
 
1. TinyTx Demo  
TinyTx is a tiny declarative services only has 4 files, usually used for small projects, below is the demo of TinyTx(included in jTransactions) + jBeanBox + DbUtils-pro：  
```
    public class TinyTxBox extends BeanBox {//configuration transaction service
	  {
		this.setConstructor(TinyTx.class, BeanBox.getBean(DataSourceBox.class), Connection.TRANSACTION_READ_COMMITTED);
	  }
    }
 
    public class TinyTxManagerTest {
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
		System.out.println(1 / 0); //Throw a exception, should roll back transaction
	}

	@AopAround(TinyTxBox.class)
	public void doInsert1() {
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

2. SpringTX Transaction demo 
    jTransactions only made a simple wrap of Spring-TX, demo see test\...\SpringTx folder
	
3. Runtime configure and switch transaction service
    Below is a demo at runtime configurate and switch transaction service (between TinyTx and SpringTx)：  
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
Above are all documents of jTransactions, if any question please see unit test demos or sourcecode.