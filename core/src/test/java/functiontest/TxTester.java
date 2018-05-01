package functiontest;

import java.sql.Connection;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;

import com.github.drinkjava2.jbeanbox.AopAround;
import com.github.drinkjava2.jbeanbox.BeanBox;
import com.github.drinkjava2.jtransactions.ConnectionManager;
import com.github.drinkjava2.jtransactions.springtx.SpringTx;
import com.github.drinkjava2.jtransactions.springtx.SpringTxConnectionManager;
import com.github.drinkjava2.jtransactions.tinytx.TinyTx;
import com.github.drinkjava2.jtransactions.tinytx.TinyTxConnectionManager;

import functiontest.DataSourceConfig.DataSourceBox;
import functiontest.TinyJdbc;

/**
 * This demo shows how to change Transaction at runtime
 *
 * @author Yong Zhu
 *
 * @version 1.0.0
 * @since 1.0.0
 */

public class TxTester {
	private static Class<?> tx = TinyTx.class;
	private static ConnectionManager cm = TinyTxConnectionManager.instance();

	public static class TxBox extends BeanBox {
		{
			this.setConstructor(tx, BeanBox.getBean(DataSourceBox.class), Connection.TRANSACTION_READ_COMMITTED);
		}
	}

	TinyJdbc db = new TinyJdbc((DataSource) BeanBox.getBean(DataSourceBox.class), cm);

	@AopAround(TxBox.class)
	public void tx_Insert1() {
		db.executeSql("insert into users (id) values('123')");
	}

	@AopAround(TxBox.class)
	public void tx_Insert2() {
		db.executeSql("insert into users (id) values('456')");
		Assert.assertEquals(2L, db.queryForObject("select count(*) from users"));
		System.out.println("Now have 2 records in users table, but will roll back to 1");
		System.out.println(1 / 0);
	}

	public void doTest() {
		try {
			db.executeSql("drop table users");
		} catch (Exception e) {
		}
		db.executeSql("create table users (id varchar(40))engine=InnoDB");
		Assert.assertEquals(0L, db.queryForObject("select count(*) from users"));
		try {
			tx_Insert1();// this one inserted 1 record
			tx_Insert2();// this one did not insert, roll back
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("div/0 exception found, tx_Insert2 should roll back");
		}
		Assert.assertEquals(1L, db.queryForObject("select count(*) from users"));
	}

	@Test
	public void userTinyTx() {
		TxTester tester = BeanBox.getBean(TxTester.class);
		tester.doTest();
		BeanBox.defaultContext.close();// Release DataSource Pool
	}

	@Test
	public void testSprintTx() {
		tx = SpringTx.class;
		cm = SpringTxConnectionManager.instance();
		TxTester tester = BeanBox.getBean(TxTester.class);
		tester.doTest();
		BeanBox.defaultContext.close();// Release DataSource Pool
	}
}