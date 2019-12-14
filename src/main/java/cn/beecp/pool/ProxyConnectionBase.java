/*
 * Copyright Chris2018998
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.beecp.pool;

import static cn.beecp.pool.PoolExceptionList.ConnectionClosedException;
import static cn.beecp.util.BeecpUtil.equalsText;

import java.sql.Connection;
import java.sql.SQLException;

import cn.beecp.BeeDataSourceConfig;
/**
 * physical connection wrapper
 * 
 * @author Chris.Liao
 * @version 1.0
 */
public abstract class ProxyConnectionBase implements Connection{
	private boolean isClosed;
	protected Connection delegate;
	protected boolean stmCacheValid;
	protected StatementCache stmCache;
	protected PooledConnection pooledConn;
	private BeeDataSourceConfig poolConfig;
	
	public ProxyConnectionBase(PooledConnection pooledConn) {
		this.pooledConn=pooledConn;
		delegate=pooledConn.rawConnection;
		poolConfig=pooledConn.poolConfig;
		stmCacheValid=pooledConn.stmCacheValid;
		stmCache=pooledConn.stmCache;
	}
	protected void checkClose() throws SQLException {
		if(isClosed)throw ConnectionClosedException;
	}
	public void setAutoCommit(boolean autoCommit) throws SQLException {
		checkClose();
		delegate.setAutoCommit(autoCommit);
		pooledConn.updateAccessTime();
		pooledConn.setCurAutoCommit(autoCommit);
		pooledConn.setChangedInd(PooledConnection.Pos_AutoCommitInd,autoCommit!=poolConfig.isDefaultAutoCommit()); 
	}
	public void setTransactionIsolation(int level) throws SQLException {
		checkClose();
		delegate.setTransactionIsolation(level);
		pooledConn.updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_TransactionIsolationInd,level!=poolConfig.getDefaultTransactionIsolation());
	}
	public void setReadOnly(boolean readOnly) throws SQLException {
		checkClose();
		delegate.setReadOnly(readOnly);
		pooledConn.updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_ReadOnlyInd,readOnly!=poolConfig.isDefaultReadOnly());
	}
	public void setCatalog(String catalog) throws SQLException {
		checkClose();
		delegate.setCatalog(catalog);
		pooledConn.updateAccessTime();
		pooledConn.setChangedInd(PooledConnection.Pos_CatalogInd,!equalsText(catalog, poolConfig.getDefaultCatalog()));
	}
	public final boolean isWrapperFor(Class<?> iface) throws SQLException {
		checkClose();
		return iface.isInstance(delegate);
	}
	@SuppressWarnings("unchecked")
	public final <T> T unwrap(Class<T> iface) throws SQLException{
	  checkClose();
	  if (iface.isInstance(delegate)) {
         return (T)this;
      }else {
    	  throw new SQLException("Wrapped object is not an instance of " + iface);
      } 
	}
	void setConnectionDataToNull() {
		isClosed=true;
		delegate=null;
		pooledConn=null;
		stmCache=null;
		poolConfig=null;
	}
	public void close() throws SQLException {
		try{
			isClosed = true;
			pooledConn.returnToPoolBySelf();
		}finally{
			setConnectionDataToNull();
		}
	}
}