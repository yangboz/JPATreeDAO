package fri.util.database.jpa.tree.examples.closuretable;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeDao;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;

/**
 * Shows how to use ClosureTableTreeDao with JPA,
 * using two DAOs to maintain two different trees on the same node table.
 */
public class JpaExample
{
	public static void main(String[] args) throws Exception {
		// set up JPA layer
		final String PERSISTENCE_UNIT_NAME = "eclipselink-persistence-unit";	// also try "hibernate-persistence-unit"
		final EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
		final EntityManager entityManager = entityManagerFactory.createEntityManager();
		final EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();
		
		new JpaExample().run(new DbSessionJpaImpl(entityManager));
		
		transaction.commit();
		entityManager.close();
		entityManagerFactory.close();
	}

	protected void run(DbSession dbSession) throws UniqueConstraintViolationException {
		// set up 2 different DAOs for table "PersonCtt", created by JPA-layer on entity-scanning
		final ClosureTableTreeDao organizationalDao = new ClosureTableTreeDao(PersonCtt.class, PersonOrganisationalTreePath.class, true, dbSession);
		organizationalDao.setRemoveReferencedNodes(true);	// nodes referenced by this tree will be removed
		final ClosureTableTreeDao functionalDao = new ClosureTableTreeDao(PersonCtt.class, PersonFunctionalTreePath.class, false, dbSession);
		
		// create an organizational root
		final ClosureTableTreeNode walter = organizationalDao.createRoot(new PersonCtt("Walter"));
		assert organizationalDao.getRoots().size() == 1;
		
		// create a functional root
		functionalDao.createRoot(walter);
		assert functionalDao.getRoots().size() == 1;
		
		
		// build organizational tree
		
		// insert root children
		final ClosureTableTreeNode linda = organizationalDao.addChild(walter, new PersonCtt("Linda"));
		final ClosureTableTreeNode mary = organizationalDao.addChild(walter, new PersonCtt("Mary"));
		assert organizationalDao.size(walter) == 3;
		
		final ClosureTableTreeNode peter = organizationalDao.addChild(mary, new PersonCtt("Peter"));
		final ClosureTableTreeNode paul = organizationalDao.addChild(mary, new PersonCtt("Paul"));
		assert organizationalDao.size(walter) == 5;
		assert organizationalDao.size(mary) == 3;
		
		final List<ClosureTableTreeNode> organizationalChildrenOfWalter = organizationalDao.getChildren(walter);
		assert organizationalChildrenOfWalter.size() == 2 &&
			organizationalChildrenOfWalter.get(0).equals(linda) &&
			organizationalChildrenOfWalter.get(1).equals(mary);
		
		final List<ClosureTableTreeNode> organizationalChildrenOfMary = organizationalDao.getChildren(mary);
		assert organizationalChildrenOfMary.size() == 2 &&
			organizationalChildrenOfMary.get(0).equals(peter) &&
			organizationalChildrenOfMary.get(1).equals(paul);
		
		// build functional tree
		
		assert functionalDao.size(walter) == 1;
		
		functionalDao.createRoot(linda);
		assert functionalDao.getRoots().size() == 2;
		functionalDao.addChild(linda, peter);
		functionalDao.addChild(linda, paul);
		assert functionalDao.size(linda) == 3;
		
		functionalDao.addChild(walter, mary);
		assert functionalDao.size(walter) == 2;
		
		final List<ClosureTableTreeNode> functionalChildrenOfWalter = functionalDao.getChildren(walter);
		assert functionalChildrenOfWalter.size() == 1 &&
			functionalChildrenOfWalter.get(0).equals(mary);
		
		final List<ClosureTableTreeNode> functionalChildrenOfLinda = functionalDao.getChildren(linda);
		assert functionalChildrenOfLinda.size() == 2 &&
			functionalChildrenOfLinda.get(0).equals(peter) &&
			functionalChildrenOfLinda.get(1).equals(paul);
		
		
		// clean up
		functionalDao.removeAll();
		assert functionalDao.getRoots().size() == 0;
		organizationalDao.removeAll();
		assert organizationalDao.getRoots().size() == 0;
	}

}
