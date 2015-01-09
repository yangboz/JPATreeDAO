package fri.util.database.jpa.tree.hibernatesession.tests;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeTest;

public class NestedSetsTreeHibernateSessionTest extends NestedSetsTreeTest
{
	private HibernateSessionTestDelegate delegate = new HibernateSessionTestDelegate();

	@Override
	protected void setUp() throws Exception {
		delegate.setUp();
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		delegate.tearDown();
	}
	
	@Override
	protected DbSession newDbSession(String message) {
		logStart(message);
		return delegate.newDbSession();
	}
	
	@Override
	protected void commitDbTransaction(String message) {
		logBeforeEnd(message, "commit");
		delegate.commitDbTransaction();
		logAfterEnd(message, "commit");
	}

	@Override
	protected void rollbackDbTransaction(String message) {
		logBeforeEnd(message, "rollback");
		delegate.rollbackDbTransaction();
		logAfterEnd(message, "rollback");
	}

}
