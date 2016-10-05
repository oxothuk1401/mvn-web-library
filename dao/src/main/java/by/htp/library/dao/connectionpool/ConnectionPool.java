package by.htp.library.dao.connectionpool;

import by.htp.library.dao.connectionpool.db.DBParameter;
import by.htp.library.dao.connectionpool.db.DBResourceManager;
import by.htp.library.dao.connectionpool.exception.ConnectionPoolException;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;

public final class ConnectionPool {
	private BlockingQueue<Connection> connectionQueue;
	private BlockingQueue<Connection> givenAwayConQueue;
	private final static ConnectionPool INSTANCE = new ConnectionPool();

	private String driverName;
	private String url;
	private String user;
	private String password;
	private int poolsize;

	public static ConnectionPool getInstance() {
		return INSTANCE;
	}

	private ConnectionPool() {
		DBResourceManager dbResourceManager = DBResourceManager.getInstance();
		this.driverName = dbResourceManager.getValue(DBParameter.DB_DRIVER);
		this.url = dbResourceManager.getValue(DBParameter.DB_URl);
		this.user = dbResourceManager.getValue(DBParameter.DB_USER);
		this.password = dbResourceManager.getValue(DBParameter.DB_PASSWORD);

		try {
			this.poolsize = Integer.parseInt(dbResourceManager.getValue(DBParameter.DB_POOLSIZE));
		} catch (NumberFormatException e) {
			poolsize = 5;
		}
	}

	public void initPoolData() throws ConnectionPoolException {
		try {
			Class.forName(driverName);
			givenAwayConQueue = new ArrayBlockingQueue<>(poolsize);
			connectionQueue = new ArrayBlockingQueue<>(poolsize);
			for (int i = 0; i < poolsize; i++) {
				Connection connection = DriverManager.getConnection(url, user, password);
				PooledConnection pooledConnection = new PooledConnection(connection);
				connectionQueue.add(pooledConnection);
			}

		} catch (SQLException e) {
			throw new ConnectionPoolException("SQLException in itinPoolData()", e);
		} catch (ClassNotFoundException e) {
			throw new ConnectionPoolException("Can't finde driver class", e);
		}

	}

	public void dispose() {
		clearConnectionQueue();
	}

	private void clearConnectionQueue() {
		try {
			closeConnectionsQueue(givenAwayConQueue);
			closeConnectionsQueue(connectionQueue);

		} catch (SQLException exception) {
			// log
		}
	}

	public Connection takeConnection() throws ConnectionPoolException {
		Connection connection = null;
		try {
			connection = connectionQueue.take();
			givenAwayConQueue.add(connection);
		} catch (InterruptedException e) {
			throw new ConnectionPoolException("Error connectiong to the datasource", e);
		}
		return connection;
	}

	private void closeConnectionsQueue(BlockingQueue<Connection> queue) throws SQLException {
		Connection connection;
		while ((connection = queue.poll()) != null) {
			if (!connection.getAutoCommit()) {
				connection.commit();
			}
			((PooledConnection) connection).reallyClose();
		}
	}

	public void closeConnection(Connection con, Statement st, ResultSet rs) {
		try {
			con.close();
		} catch (SQLException e) {
			// logger
		}
		try {
			st.close();
		} catch (SQLException e) {
			// logger
		}
		try {
			rs.close();
		} catch (SQLException e) {
			// logger
		}
	}

	public void closeConnection(Connection con, Statement st) {
		try {
			con.close();
		} catch (SQLException e) {
			// logger
		}
		try {
			st.close();
		} catch (SQLException e) {
			// logger
		}
	}

	private class PooledConnection implements Connection {
		private Connection connection;

		public PooledConnection(Connection connection) throws SQLException {
			this.connection = connection;
			this.connection.setAutoCommit(true);
		}

		public void reallyClose() throws SQLException {
			connection.close();
		}



		@Override
		public boolean isWrapperFor(Class<?> arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.isWrapperFor(arg0);
		}

		@Override
		public <T> T unwrap(Class<T> arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.unwrap(arg0);
		}

		@Override
		public void abort(Executor arg0) throws SQLException {
			connection.abort(arg0);

		}

		@Override
		public void clearWarnings() throws SQLException {
			connection.clearWarnings();

		}

		@Override
		public void close() throws SQLException {
			if (connection.isClosed()) {
				throw new SQLException("Attempt to close closed connection.");
			}
			if (connection.isReadOnly()) {
				connection.setReadOnly(false);
			}
			if (!givenAwayConQueue.remove(this)) {
				throw new SQLException("Error deleting from givenAwayConPool");
			}
			if (!connectionQueue.offer(this)) {
				throw new SQLException("Error allocating connection in the pool");
			}

		}

		@Override
		public void commit() throws SQLException {
			connection.commit();

		}

		@Override
		public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.createArrayOf(arg0, arg1);
		}

		@Override
		public Blob createBlob() throws SQLException {
			// TODO Auto-generated method stub
			return connection.createBlob();
		}

		@Override
		public Clob createClob() throws SQLException {
			// TODO Auto-generated method stub
			return connection.createClob();
		}

		@Override
		public NClob createNClob() throws SQLException {
			// TODO Auto-generated method stub
			return connection.createNClob();
		}

		@Override
		public SQLXML createSQLXML() throws SQLException {
			// TODO Auto-generated method stub
			return connection.createSQLXML();
		}

		@Override
		public Statement createStatement() throws SQLException {
			// TODO Auto-generated method stub
			return connection.createStatement();
		}

		@Override
		public Statement createStatement(int arg0, int arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.createStatement(arg0, arg1);
		}

		@Override
		public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
			// TODO Auto-generated method stub
			return connection.createStatement(arg0, arg1, arg2);
		}

		@Override
		public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.createStruct(arg0, arg1);
		}

		@Override
		public boolean getAutoCommit() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getAutoCommit();
		}

		@Override
		public String getCatalog() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getCatalog();
		}

		@Override
		public Properties getClientInfo() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getClientInfo();
		}

		@Override
		public String getClientInfo(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.getClientInfo(arg0);
		}

		@Override
		public int getHoldability() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getHoldability();
		}

		@Override
		public DatabaseMetaData getMetaData() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getMetaData();
		}

		@Override
		public int getNetworkTimeout() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getNetworkTimeout();
		}

		@Override
		public String getSchema() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getSchema();
		}

		@Override
		public int getTransactionIsolation() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getTransactionIsolation();
		}

		@Override
		public Map<String, Class<?>> getTypeMap() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getTypeMap();
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			// TODO Auto-generated method stub
			return connection.getWarnings();
		}

		@Override
		public boolean isClosed() throws SQLException {
			// TODO Auto-generated method stub
			return connection.isClosed();
		}

		@Override
		public boolean isReadOnly() throws SQLException {
			// TODO Auto-generated method stub
			return connection.isReadOnly();
		}

		@Override
		public boolean isValid(int arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.isValid(arg0);
		}

		@Override
		public String nativeSQL(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.nativeSQL(arg0);
		}

		@Override
		public CallableStatement prepareCall(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareCall(arg0);
		}

		@Override
		public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareCall(arg0, arg1, arg2);
		}

		@Override
		public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareCall(arg0, arg1, arg2, arg3);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0, arg1);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0, arg1);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0, arg1);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0, arg1, arg2);
		}

		@Override
		public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
			// TODO Auto-generated method stub
			return connection.prepareStatement(arg0, arg1, arg2, arg3);
		}

		@Override
		public void releaseSavepoint(Savepoint arg0) throws SQLException {
			// TODO Auto-generated method stub
			connection.releaseSavepoint(arg0);
		}

		@Override
		public void rollback() throws SQLException {
			// TODO Auto-generated method stub
			connection.rollback();
		}

		@Override
		public void rollback(Savepoint arg0) throws SQLException {
			// TODO Auto-generated method stub
			connection.rollback(arg0);
		}

		@Override
		public void setAutoCommit(boolean arg0) throws SQLException {
			// TODO Auto-generated method stub
			connection.setAutoCommit(arg0);
		}

		@Override
		public void setCatalog(String arg0) throws SQLException {
			// TODO Auto-generated method stub
			connection.setCatalog(arg0);
		}

		@Override
		public void setClientInfo(Properties arg0) throws SQLClientInfoException {
			// TODO Auto-generated method stub
			connection.setClientInfo(arg0);
		}

		@Override
		public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
			// TODO Auto-generated method stub
			connection.setClientInfo(arg0, arg1);
		}

		@Override
		public void setHoldability(int arg0) throws SQLException {
			// TODO Auto-generated method stub
			connection.setHoldability(arg0);
		}

		@Override
		public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
			// TODO Auto-generated method stub
			connection.setNetworkTimeout(arg0, arg1);
		}

		@Override
		public void setReadOnly(boolean readOnly) throws SQLException {
			// TODO Auto-generated method stub
			connection.setReadOnly(readOnly);
		}

		@Override
		public Savepoint setSavepoint() throws SQLException {
			// TODO Auto-generated method stub
			return connection.setSavepoint();
		}

		@Override
		public Savepoint setSavepoint(String name) throws SQLException {
			// TODO Auto-generated method stub
			return connection.setSavepoint(name);
		}

		@Override
		public void setSchema(String schema) throws SQLException {
			// TODO Auto-generated method stub
			connection.setSchema(schema);
		}

		@Override
		public void setTransactionIsolation(int level) throws SQLException {
			// TODO Auto-generated method stub
			connection.setTransactionIsolation(level);
		}

		@Override
		public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
			// TODO Auto-generated method stub
			connection.setTypeMap(map);
		}
	}
}
