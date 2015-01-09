package fri.util.database.jpa.tree;

import junit.framework.Test;
import junit.framework.TestSuite;
import fri.util.database.jpa.tree.closuretable.providers.ClosureTableTreeEclipselinkTest;
import fri.util.database.jpa.tree.closuretable.providers.TemporalClosureTableTreeHibernateTest;
import fri.util.database.jpa.tree.hibernatesession.tests.NestedSetsTreeHibernateSessionTest;
import fri.util.database.jpa.tree.hibernatesession.tests.TemporalClosureTableTreeHibernateSessionTest;
import fri.util.database.jpa.tree.nestedsets.providers.NestedSetsTreeHibernateTest;
import fri.util.database.jpa.tree.nestedsets.providers.TemporalNestedSetsTreeEclipselinkTest;

/**
 * Contains essential unit tests of JpaTree (faster than AllTests).
 * 
 * @author Fritz Ritzberger, 2013-08-31
 */
public class ImportantTests
{
	public static Test suite() {
		TestSuite suite = new TestSuite(ImportantTests.class.getName());

		suite.addTestSuite(NestedSetsTreeHibernateTest.class);
		suite.addTestSuite(TemporalNestedSetsTreeEclipselinkTest.class);
		
		suite.addTestSuite(ClosureTableTreeEclipselinkTest.class);
		suite.addTestSuite(TemporalClosureTableTreeHibernateTest.class);
		
		suite.addTestSuite(NestedSetsTreeHibernateSessionTest.class);
		suite.addTestSuite(TemporalClosureTableTreeHibernateSessionTest.class);
		
		return suite;
	}
	
}
