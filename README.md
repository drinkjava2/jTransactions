(English instruction please see [README-ENGLISH.md](README-ENGLISH.md) )  
## jTransactions
开源协议: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions是一个事明式事务工具，目的是将Java的事务（及声明式事务)功能独立出来，做成一个通用、独立的库来使用，无论什么DAO工具，借助于jTransactions，都可以用相似的配置方式进行声明式事务配置，jTransactions运行环境为Java6或以上。    

事务的基础是对DAO工具获取和释放Connection进行拦截，添加事务处理。目前jTransactions包含以下几个连接管理类：  
ManualTxConnectionManager 这是最简单的手工进行开启、关闭事务管理  
TinyTxConnectionManager 这是最常用的事务管理器，对当前线程的DAO进行事务管理，一个线程中只允许一个Dao工具获取连接
GroupTxConnectionManager 用于对一组DAO进行事务管理，一个线程中允许多个Dao工具获取连接，但提交不保证数据的一致性，用于不重要的场合  
JFinalTxConnectionManager 当DAO工具用于JFinal环境中时，可以使用这个连接管理器适配JFinal的事务
SpringTxConnectionManager 当DAO工具用于Spring环境中时，可以使用这个连接管理器适配Spring的事务

jTransactions中自带一个TinyTxAOP切面处理类，它具有简单的声明式事务功能，是Spring事务之外一个小巧的选择。  
声明式事务的实现需要三个条件：  
1.IOC/AOP工具, jTransactions自身不包含IOC/AOP功能   
2.Connection连接管理类     
3.切面处理类   
 
### 如何引入项目?  
方式一：手工在Maven中央库下载jtransactions-3.0.0.jar并放置于项目的类目录。  
方式二：在项目的pom.xml文件中加入如下行：
```
   <dependency>  
      <groupId>com.github.drinkjava2</groupId>  
      <artifactId>jtransactions</artifactId>  
      <version>3.0.0</version> <!--或Maven最新版--> 
   </dependency>
``` 
注：  
1. 如果只使用TinyTxAOP，以上配置就可以了。如果使用SpringTx或JFinal, 还需要在pom.xml中添加各自环境的依赖库。  
2. jTransactions还必须与一个支持AOP Alliance接口规范的AOP工具如Spring或jBeanBox配合使用，因为声明式事务的基本原理是对方法的拦截，必须有一个AOP工具来实现这个功能。     
3. jTransactions还必须一个JDBC工具配合使用，如纯JDBC、JdbcTemplate、DbUtils-pro或jSqlBox等，并且必须保证它们在获取、关闭Connection时从jTransactions提供的连接管理器中获取。   

### 使用演示
以下示例演示了使用TinyTx进行声明式事务的配置和使用，演示程序可以在jTransaction的单元测试目录下找到：  
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
		Assert.assertEquals(2L, tiny.queryForObject("select count(*) from users"));
		System.out.println("Now have 2 records in users table, but will roll back to 1");
		System.out.println(1 / 0);
	}

	@Test
	public void doTest() {
		System.out.println("============Testing: TinyTxTester============");
		TinyTxTester tester = BeanBox.getBean(TinyTxTester.class);

		try {
			tiny.executeSql("drop table users");
		} catch (Exception e) {
		}
		tiny.executeSql("create table users (id varchar(40))engine=InnoDB");

		Assert.assertEquals(0L, tiny.queryForObject("select count(*) from users"));

		try {
			tester.tx_Insert1();// this one inserted 1 record
			tester.tx_Insert2();// this one did not insert, roll back
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("div/0 exception found, tx_Insert2 should roll back");
		}
		Assert.assertEquals(1L, tiny.queryForObject("select count(*) from users"));
		JBEANBOX.bctx().close();// Release DataSource Pool
	}

}
```
上例通过一个IOC/AOP工具，自定义一个声明式事务注解@TX，然后将JDBC工具的连接管理器设置成TinyTxConnectionManager.instance()，就可以进行声明式事务编程了，被@TX注解的方法如果有运行时异常发生，将会回滚事务。

以上即为jTransactions全部文档，更多介绍，可以参见jSqlBox中的事务配置以及demo目录下的示例。   

因为分布式事务的复杂性，jTransactions中没有支持分布式事务的实现，但是在jSqlBox中实现了支持分库分表的分布式事务，以及相关的工具类。详情请见jSqlBox项目。