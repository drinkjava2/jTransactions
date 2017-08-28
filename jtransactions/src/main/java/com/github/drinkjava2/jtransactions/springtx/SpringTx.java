package com.github.drinkjava2.jtransactions.springtx;

import java.util.Properties;

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

	public SpringTx(PlatformTransactionManager pm, Properties pros) {
		super(pm, pros);
	}

}