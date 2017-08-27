package functiontest.com.github.drinkjava2.jtransactions.tinytx;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.github.drinkjava2.jtransactions.ConnectionManager;

/**
 * A Tiny JDBC tool only for unit test
 * 
 * @author Yong Zhu
 * @since 1.0.0
 */
public class TinyJdbc {
	ConnectionManager cm;
	DataSource ds;

	public TinyJdbc(DataSource ds, ConnectionManager cm) {
		this.ds = ds;
		this.cm = cm;
	}

	@SuppressWarnings("unchecked")
	public <T> T queryForObject(String sql) {
		Connection con = getConnection();
		PreparedStatement pst = null;
		try {
			pst = con.prepareStatement(sql);
			ResultSet rs = pst.executeQuery();
			rs.next();
			return (T) rs.getObject(1);
		} catch (SQLException e) {
			throw new RuntimeException("Fail to execute queryForInteger:", e);
		} finally {
			RuntimeException e = closePST(pst);
			releaseConnection(con);
			if (e != null)
				throw e;
		}
	}

	public void executeSql(String sql) {
		Connection con = getConnection();
		PreparedStatement pst = null;
		try {
			pst = con.prepareStatement(sql);
			pst.execute();
		} catch (SQLException e) {
			throw new RuntimeException("Exception found:", e);
		} finally {
			RuntimeException e = closePST(pst);
			releaseConnection(con);
			if (e != null)
				throw e;
		}
	}

	private static RuntimeException closePST(PreparedStatement pst) {
		try {
			if (pst != null)
				pst.close();
			return null;
		} catch (SQLException e) {
			return new RuntimeException("Fail to close PreparedStatement", e);
		}
	}

	private Connection getConnection() {
		try {
			return cm.getConnection(ds);
		} catch (SQLException e) {
			throw new RuntimeException("Fail to get Connection", e);
		}
	}

	private void releaseConnection(Connection conn) {
		try {
			cm.releaseConnection(conn, ds);
		} catch (SQLException e) {
			throw new RuntimeException("Fail to release Connection", e);
		}
	}

}