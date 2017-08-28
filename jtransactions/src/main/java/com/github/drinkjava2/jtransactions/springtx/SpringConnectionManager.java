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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceUtils;

import com.github.drinkjava2.jtransactions.ConnectionManager;

/**
 * SpringConnectionManager is the implementation of ConnectionManager, get
 * connection and release connection by using Spring's DataSourceUtils methods
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class SpringConnectionManager implements ConnectionManager {
	private static SpringConnectionManager instance = new SpringConnectionManager();

	public static SpringConnectionManager instance() {
		return instance;
	}

	@Override
	public Connection getConnection(DataSource ds) throws SQLException {
		return DataSourceUtils.getConnection(ds);
	}

	@Override
	public void releaseConnection(Connection conn, DataSource ds) throws SQLException {
		DataSourceUtils.releaseConnection(conn, ds);
	}

}
