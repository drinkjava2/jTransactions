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

2. SpringTX demo 
    jTransactions only make a simple wrap of Spring-TX, below is the usuage: 
```
    will add
```

Above are all documents of jTransactions, if any question please see unit test demos or sourcecode.