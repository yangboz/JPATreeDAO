package fri.util.database.jpa.tree.examples.nestedsets.temporal;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeDao;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueWholeTreeConstraintImpl;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;

/**
 * Shows how to use TemporalNestedSetsTreeDao with JPA.
 */
public class JpaExample
{
	public static void main(String[] args) throws Exception {
		// set up JPA layer
		final String PERSISTENCE_UNIT_NAME = "hibernate-persistence-unit";	// also try "eclipselink-persistence-unit"
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
		// set up DAO for table "PersonTnst", created by JPA-layer on entity-scanning
		final TemporalNestedSetsTreeDao dao = new TemporalNestedSetsTreeDao(
				PersonTnst.class,
				null,	// entity does not support validFrom property
				"endValid",	// entity's alternative property name for validTo, needed in JPQL queries
				dbSession);
		
		// add a constraint that checks that a person's name is unique across the whole tree
		dao.setUniqueTreeConstraint(new UniqueWholeTreeConstraintImpl(
						new String [][] { { "name" } },
						false));
		
		// create a tree
		final NestedSetsTreeNode walter = dao.createRoot(new PersonTnst("Walter"));
		final NestedSetsTreeNode peter = dao.addChild(walter, new PersonTnst("Peter"));
		final NestedSetsTreeNode paul = dao.addChild(walter, new PersonTnst("Paul"));
		final NestedSetsTreeNode mary = dao.addChild(walter, new PersonTnst("Mary"));
		
		// retrieve children lists
		final List<NestedSetsTreeNode> childrenOfWalter = dao.getChildren(walter);
		assert childrenOfWalter.size() == 3 &&
			childrenOfWalter.get(0).equals(peter) &&
			childrenOfWalter.get(1).equals(paul) &&
			childrenOfWalter.get(2).equals(mary);
		
		// historicize a node
		dao.remove(peter);
		
		final List<NestedSetsTreeNode> newChildrenOfWalter = dao.getChildren(walter);
		assert newChildrenOfWalter.size() == 2 &&
			newChildrenOfWalter.get(0).equals(paul) &&
			newChildrenOfWalter.get(1).equals(mary);
		
		// recover node
		final Map<String,Object> criteria = new Hashtable<String,Object>();
		criteria.put("name", "Peter");
		final List<NestedSetsTreeNode> result = dao.findRemoved(walter, criteria);
		assert result.size() == 1;
		final PersonTnst removedMary = (PersonTnst) result.get(0);
		dao.unremove(removedMary);
		
		final List<NestedSetsTreeNode> restoredChildrenOfWalter = dao.getChildren(walter);
		assert restoredChildrenOfWalter.size() == 3 &&
			restoredChildrenOfWalter.get(0).equals(peter) &&
			restoredChildrenOfWalter.get(1).equals(paul) &&
			restoredChildrenOfWalter.get(2).equals(mary);
		
		// clean up
		dao.removeAll();
	}
}