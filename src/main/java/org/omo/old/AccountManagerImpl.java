package org.omo.old;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class AccountManagerImpl extends PositionManagerImpl<Account, Trade> implements AccountManager {

	protected final Log log = LogFactory.getLog(getClass());

	public AccountManagerImpl(Account identity) {
		super(identity);
	}
	
}
