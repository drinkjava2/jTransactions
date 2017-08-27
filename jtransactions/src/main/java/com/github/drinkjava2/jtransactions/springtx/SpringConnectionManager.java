/*
 * Copyright (C) 2016 Yong Zhu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.drinkjava2.jtransactions.springtx;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;

import javax.sql.DataSource;

import com.github.drinkjava2.jtransactions.ConnectionManager;
import com.github.drinkjava2.jtransactions.tinytx.TinyTxRuntimeException;

/**
 * SpringConnectionManager is the implementation of ConnectionManager, get
 * connection and release connection by using Spring's DataSourceUtils methods
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class SpringConnectionManager implements ConnectionManager {
	protected final Method getConnectionMethod;
	protected final Method releaseConnectionMethod;

	public SpringConnectionManager() {
		Class<?> dataSourceUtilClass = null;
		try {
			dataSourceUtilClass = Class.forName("org.springframework.jdbc.datasource.DataSourceUtils");
			getConnectionMethod = dataSourceUtilClass.getMethod("getConnection", DataSource.class);
			releaseConnectionMethod = dataSourceUtilClass.getMethod("releaseConnection", Connection.class,
					DataSource.class);
		} catch (NoSuchMethodException e) {
			throw new TinyTxRuntimeException("No methods exception for class: \"" + dataSourceUtilClass + "\" ", e);
		} catch (SecurityException e) {
			throw new TinyTxRuntimeException("Security exception for class: \"" + dataSourceUtilClass + "\" ", e);
		} catch (ClassNotFoundException e) {
			throw new TinyTxRuntimeException(
					"Class not found Exception for class: \"org.springframework.jdbc.datasource.DataSourceUtils\" ", e);
		}
	}

	private static class SpringDsManagerSinglton {
		private static final SpringConnectionManager INSTANCE = new SpringConnectionManager();
	}

	/**
	 * @return A singleton instance of TinySpringDataSourceManager
	 */
	public static final SpringConnectionManager instance() {
		return SpringDsManagerSinglton.INSTANCE;
	}

	/*
	 * Equal to Spring's DataSourceUtils.getConnection()
	 */
	@Override
	public Connection getConnection(DataSource dataSource) {
		try {
			return (Connection) getConnectionMethod.invoke(null, dataSource);
		} catch (InvocationTargetException e) {
			throw new TinyTxRuntimeException("Fail to get connection", e);
		} catch (IllegalArgumentException e) {
			throw new TinyTxRuntimeException("Fail to get connection ", e);
		} catch (IllegalAccessException e) {
			throw new TinyTxRuntimeException("Fail to get connection  ", e);
		}
	}

	/*
	 * Equal to Spring's DataSourceUtils.releaseConnection()
	 */
	@Override
	public void releaseConnection(Connection conn, DataSource dataSource) {
		try {
			releaseConnectionMethod.invoke(null, conn, dataSource);
		} catch (InvocationTargetException e) {
			throw new TinyTxRuntimeException("Fail to release connection", e);
		} catch (IllegalArgumentException e) {
			throw new TinyTxRuntimeException("Fail to release connection ", e);
		} catch (IllegalAccessException e) {
			throw new TinyTxRuntimeException("Fail to release connection  ", e);
		}
	}

}
