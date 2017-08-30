package functiontest.springtx;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import com.github.drinkjava2.jbeanbox.BeanBox;

import functiontest.DataSourceConfig.DataSourceBox;

/**
 * A SpringTX configuration demo
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class SpringTxBox extends BeanBox {

	public TransactionInterceptor create() {
		Properties props = new Properties();
		props.put("tx_*", "PROPAGATION_REQUIRED");
		return new TransactionInterceptor((DataSourceTransactionManager) getContext().getBean(TxManagerBox.class),
				props);
	}

	public static class TxManagerBox extends BeanBox {
		public DataSourceTransactionManager create() {
			DataSourceTransactionManager dm = new DataSourceTransactionManager();
			dm.setDataSource((DataSource) getContext().getBean(DataSourceBox.class));
			return dm;
		}
	}
}