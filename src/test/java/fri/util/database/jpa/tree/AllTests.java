package fri.util.database.jpa.tree;

import junit.framework.Test;
import junit.framework.TestSuite;
import fri.util.database.jpa.tree.closuretable.providers.ClosureTableTreeEclipselinkTest;
import fri.util.database.jpa.tree.closuretable.providers.ClosureTableTreeHibernateTest;
import fri.util.database.jpa.tree.closuretable.providers.TemporalClosureTableTreeEclipselinkTest;
import fri.util.database.jpa.tree.closuretable.providers.TemporalClosureTableTreeHibernateTest;
import fri.util.database.jpa.tree.nestedsets.providers.NestedSetsTreeEclipselinkTest;
import fri.util.database.jpa.tree.nestedsets.providers.NestedSetsTreeHibernateTest;
import fri.util.database.jpa.tree.nestedsets.providers.TemporalNestedSetsTreeEclipselinkTest;
import fri.util.database.jpa.tree.nestedsets.providers.TemporalNestedSetsTreeHibernateTest;
import fri.util.database.jpa.tree.hibernatesession.tests.ClosureTableTreeHibernateSessionTest;
import fri.util.database.jpa.tree.hibernatesession.tests.NestedSetsTreeHibernateSessionTest;
import fri.util.database.jpa.tree.hibernatesession.tests.TemporalClosureTableTreeHibernateSessionTest;
import fri.util.database.jpa.tree.hibernatesession.tests.TemporalNestedSetsTreeHibernateSessionTest;

/**
 * Contains all unit tests of JpaTree.
 * 
 * @author Fritz Ritzberger, 2013-08-20
 */
public class AllTests
{
	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());

		suite.addTestSuite(NestedSetsTreeHibernateTest.class);
		suite.addTestSuite(NestedSetsTreeEclipselinkTest.class);
		suite.addTestSuite(TemporalNestedSetsTreeHibernateTest.class);
		suite.addTestSuite(TemporalNestedSetsTreeEclipselinkTest.class);
		
		suite.addTestSuite(ClosureTableTreeHibernateTest.class);
		suite.addTestSuite(ClosureTableTreeEclipselinkTest.class);
		suite.addTestSuite(TemporalClosureTableTreeHibernateTest.class);
		suite.addTestSuite(TemporalClosureTableTreeEclipselinkTest.class);
		
		suite.addTestSuite(NestedSetsTreeHibernateSessionTest.class);
		suite.addTestSuite(TemporalNestedSetsTreeHibernateSessionTest.class);
		suite.addTestSuite(ClosureTableTreeHibernateSessionTest.class);
		suite.addTestSuite(TemporalClosureTableTreeHibernateSessionTest.class);
		
		return suite;
	}
	
}
