## jTransactions
Licenses: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions is a seperated Java Transaction  tool。jTransactions currently include 4 implementations:   
TinyTX, used for any JDBC tool, only 1 connection is allowed in trasaction.  
GroupTx, used for any JDBC tool, allow more connection in trasaction but it does not guarantee transactions' integrity.
SpringTx, used in Spring environment
jFinalTx, used in JFinal environment
  
### How to import jTransactions?  
Add below lines in pom.xml: 
```
   <dependency>  
      <groupId>com.github.drinkjava2</groupId>  
      <artifactId>jtransactions</artifactId>  
      <version>4.0</version> <!-- or latest version-->  
   </dependency>
``` 

### Use
Here is an example to use jTransactions, full sourecode located in project's unit test folder.  
```
public class TinyTxTester {
	TinyJdbc tiny = new TinyJdbc((DataSource) getBean(DataSourceBox.class), TinyTxConnectionManager.instance());

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD })
	@AOP
	public static @interface TX {
		public Class<?> value() default TinyTxAOP.class;
	}

	@TX
	public void tx_Insert1() {
		tiny.executeSql("insert into users (id) values('123')");
	}

	@TX
	public void tx_Insert2() {
		tiny.executeSql("insert into users (id) values('456')");
		Assert.assertEquals(2L, (long) tiny.queryForObject("select count(*) from users"));
		System.out.println("Now have 2 records in users table, but will roll back to 1");
		System.out.println(1 / 0);
	}

	@Test
	public void doTest() {
		System.out.println("============Testing: TinyTxTester============");
		TinyTxTester tester = JBEANBOX.getBean(TinyTxTester.class);

		try {
			tiny.executeSql("drop table users");
		} catch (Exception e) {
		}
		tiny.executeSql("create table users (id varchar(40))engine=InnoDB");

		Assert.assertEquals(0L, (long) tiny.queryForObject("select count(*) from users"));

		try {
			tester.tx_Insert1();// this one inserted 1 record
			tester.tx_Insert2();// this one did not insert, roll back
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("div/0 exception found, tx_Insert2 should roll back");
		}
		Assert.assertEquals(1L, (long) tiny.queryForObject("select count(*) from users"));
		JBEANBOX.ctx().reset();// Release DataSource Pool
	}
}
```