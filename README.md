(English instruction please see [README-ENGLISH.md](README-ENGLISH.md) )  
## jTransactions
开源协议: [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0) 

jTransactions是一个事明式事务工具，目的是将Java的声明式事务功能独立出来，做成一个通用、独立的库来使用，无论什么DAO工具，借助于jTransactions，都可以用相似的配置方式进行声明式事务配置。  
jTransactions中自带一个TinyTX类，它具有简单的声明式事务功能，是Spring事务之外一个小巧的选择。
声明式事务的实现需要三个条件：1.IOC/AOP工具 2.Connection连接管理类 3.切面处理类。jTransactions不包含IOC/AOP功能，但是对于Spring环境、jFinal环境，TinyTx环境都分别提供了三个不同的连接管理工具类。
jTransactions运行环境为Java6或以上。 

### 如何引入项目?  
方式一：手工下载jtransactions-2.0.4.jar并放置于项目的类目录。  
方式二：在项目的pom.xml文件中加入如下行：
```
   <dependency>  
      <groupId>com.github.drinkjava2</groupId>  
      <artifactId>jtransactions</artifactId>  
      <version>2.0.4</version> <!--或Maven最新版--> 
   </dependency>
``` 
注：  
1. 如果只使用TinyTX，以上配置就可以了。如果使用SpringTx, 还需要在pom.xml中添加对[Spring-tx](https://mvnrepository.com/artifact/org.springframework/spring-tx)的依赖。   
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
		public Class<?> value() default TheTxBox.class;
	}

	public static class TheTxBox extends BeanBox {
		{
			this.injectConstruct(TinyTx.class, DataSource.class, inject(DataSourceBox.class));
		}
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


以上即为jTransactions全部文档，更多介绍，可以参见jSqlBox中的事务配置以及它demo目录下的示例。

后记：关于这个项目，本来我没打算开发它，在开发DbUtils-Pro和jSqlBox项目时，需要找一个小巧点的独立的事务工具，原以为这方面的开源项目会有一大把，但很遗憾的是，我一个也没有找到！只好自已从头写一个TinyTx。至于为什么不直接用Spring事务是因为它太大了、太占内存、太复杂，太多标签、太多依赖，绑死在Spring环 境中无法抽取出来，使用Spring的声明式事务要加上一堆库依赖，其中居然有JdbcTemplate库，事务服务依赖于JDBC工具，难道这就是Spring号称的反转依赖不成? 至于jfinal的事务服务，看了一下源码，比较简单，但是依然写死在了Jfinal环境中，不能抽取出来给其它持久层工具使用，而且它没有和数据源绑定。jTransaction的架构是开放式的，甚至充许在运行期动态切换事务服务。