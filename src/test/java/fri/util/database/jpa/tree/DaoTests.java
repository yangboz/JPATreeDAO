package fri.util.database.jpa.tree;

import junit.framework.Test;
import junit.framework.TestSuite;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeTest;
import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeTest;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeTest;
import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeTest;

/**
 * Contains DAO tests without alternating JPA providers.
 * 
 * @author Fritz Ritzberger, 2013-08-31
 */
public class DaoTests
{
	public static Test suite() {
		TestSuite suite = new TestSuite(DaoTests.class.getName());

		suite.addTestSuite(NestedSetsTreeTest.class);
		suite.addTestSuite(TemporalNestedSetsTreeTest.class);
		
		suite.addTestSuite(ClosureTableTreeTest.class);
		suite.addTestSuite(TemporalClosureTableTreeTest.class);
		
		return suite;
	}
	
}
