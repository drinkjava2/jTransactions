package functiontest.com.github.drinkjava2.jtransactions.commontx;

import java.sql.Connection;
import java.util.Properties;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.github.drinkjava2.jbeanbox.AopAround;
import com.github.drinkjava2.jbeanbox.BeanBox;
import com.github.drinkjava2.jtransactions.CommonTx;
import com.github.drinkjava2.jtransactions.ConnectionManager;
import com.github.drinkjava2.jtransactions.springtx.SpringConnectionManager;
import com.github.drinkjava2.jtransactions.springtx.SpringTx;
import com.github.drinkjava2.jtransactions.tinytx.TinyTx;
import com.github.drinkjava2.jtransactions.tinytx.TinyTxConnectionManager;

import functiontest.com.github.drinkjava2.jtransactions.DataSourceConfig.DataSourceBox;
import functiontest.com.github.drinkjava2.jtransactions.TinyJdbc;

/**
 * This demo shows how to change Transaction at runtime
 *
 * @author Yong Zhu
 *
 * @version 1.0.0
 * @since 1.0.0
 */

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
		BeanBox testerBox = new BeanBox();
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

}