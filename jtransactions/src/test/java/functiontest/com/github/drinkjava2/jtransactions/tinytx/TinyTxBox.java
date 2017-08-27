package functiontest.com.github.drinkjava2.jtransactions.tinytx;

import java.sql.Connection;

import com.github.drinkjava2.jbeanbox.BeanBox;
import com.github.drinkjava2.jtransactions.tinytx.TinyTx;

import functiontest.DataSourceConfig.DataSourceBox;

/**
 * A TinyTx configuration
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class TinyTxBox extends BeanBox {
	{
		this.setConstructor(TinyTx.class, BeanBox.getBean(DataSourceBox.class), Connection.TRANSACTION_READ_COMMITTED);
	}
}