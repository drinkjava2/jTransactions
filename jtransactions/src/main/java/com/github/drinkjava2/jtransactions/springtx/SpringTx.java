package com.github.drinkjava2.jtransactions.springtx;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.github.drinkjava2.jtransactions.CommonTx;

/**
 * The wrap of Spring's TransactionInterceptor
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class SpringTx extends TransactionInterceptor implements CommonTx {
	private static final long serialVersionUID = 1L;

	public static final int ISOLATION_DEFAULT = -1;
	public static final int ISOLATION_READ_UNCOMMITTED = 1;
	public static final int ISOLATION_READ_COMMITTED = 2;
	public static final int ISOLATION_REPEATABLE_READ = 4;
	public static final int ISOLATION_SERIALIZABLE = 8;

	private static Properties simplePros(Integer transactionIsolation) {
		String isolation = "";
		switch (transactionIsolation) {
		case -1:
			isolation = "ISOLATION_DEFAULT";
			break;
		case 1:
			isolation = "ISOLATION_READ_UNCOMMITTED";
			break;
		case 2:
			isolation = "ISOLATION_READ_COMMITTED";
			break;
		case 4:
			isolation = "ISOLATION_REPEATABLE_READ";
			break;
		case 8:
			isolation = "ISOLATION_SERIALIZABLE";
			break;
		default:
			throw new RuntimeException("Isolation value can only be -1, 1, 2, 4, 8, but set to" + transactionIsolation);
		}
		Properties props = new Properties();
		props.put("*", "PROPAGATION_REQUIRED, " + isolation);
		return props;
	}

	public SpringTx(DataSource ds, Integer transactionIsolation) {
		super(new DataSourceTransactionManager(ds), simplePros(transactionIsolation));
	}

	public SpringTx(PlatformTransactionManager pm, Properties pros) {
		super(pm, pros);
	}

}