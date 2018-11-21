package functiontest.tinytx;

import static com.github.drinkjava2.jbeanbox.JBEANBOX.getBean;
import static com.github.drinkjava2.jbeanbox.JBEANBOX.inject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.github.drinkjava2.jbeanbox.BeanBox;
import com.github.drinkjava2.jbeanbox.JBEANBOX;
import com.github.drinkjava2.jbeanbox.annotation.AOP;
import com.github.drinkjava2.jtransactions.tinytx.TinyTx;
import com.github.drinkjava2.jtransactions.tinytx.TinyTxConnectionManager;

import functiontest.DataSourceConfig.DataSourceBox;
import functiontest.TinyJdbc;

/**
 * This is to test TinyTx Declarative Transaction
 *
 * @author Yong Zhu
 *
 * @version 1.0.0
 * @since 1.0.0
 */
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