package fri.util.database.jpa.tree.examples.nestedsets;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueWholeTreeConstraintImpl;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeDao;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;

/**
 * Shows how to use NestedSetsTreeDao with JPA.
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
		// set up DAO for table "PersonNst", created by JPA-layer on entity-scanning
		final NestedSetsTreeDao dao = new NestedSetsTreeDao(PersonNst.class, dbSession);
		
		// add a constraint that checks that a person's name is unique across the whole tree
		dao.setUniqueTreeConstraint(new UniqueWholeTreeConstraintImpl(
						new String [][] { { "name" } },
						false));
		
		// create a tree
		final NestedSetsTreeNode walter = dao.createRoot(new PersonNst("Walter"));
		assert dao.getRoots().size() == 1;
		
		// insert root children
		final NestedSetsTreeNode linda = dao.addChild(walter, new PersonNst("Linda"));
		final NestedSetsTreeNode mary = dao.addChild(walter, new PersonNst("Mary"));
		assert dao.size(walter) == 3;
		
		// add children to a child
		final NestedSetsTreeNode peter = dao.addChild(mary, new PersonNst("Peter"));
		final NestedSetsTreeNode paul = dao.addChild(mary, new PersonNst("Paul"));
		assert dao.size(walter) == 5;
		assert dao.size(mary) == 3;
		
		// retrieve children lists
		final List<NestedSetsTreeNode> childrenOfWalter = dao.getChildren(walter);
		assert childrenOfWalter.size() == 2 && childrenOfWalter.get(0).equals(linda) && childrenOfWalter.get(1).equals(mary);
		
		final List<NestedSetsTreeNode> childrenOfMary = dao.getChildren(mary);
		assert childrenOfMary.size() == 2 && childrenOfMary.get(0).equals(peter) && childrenOfMary.get(1).equals(paul);
		
		final int levelOfWalter = dao.getLevel(walter);
		assert levelOfWalter == 0;
		final int levelOfPeter = dao.getLevel(peter);
		assert levelOfPeter == 2;
		
		final List<NestedSetsTreeNode> pathOfPeter = dao.getPath(peter);
		assert pathOfPeter.size() == 2 && pathOfPeter.get(0).equals(walter) && pathOfPeter.get(1).equals(mary);
		
		// rename a child
		final String petersNewName = "Pietro";
		final PersonNst peterClone = (PersonNst) peter.clone();	// has null primary key!
		peterClone.setName(petersNewName);
		dao.checkUniqueConstraint(	// would throw exception when not unique
				peterClone,	// the rename candidate
				walter,	// the root node
				peter);	// the existing node
		((PersonNst) peter).setName(petersNewName);	// no exception was thrown
		dao.update(peter);
		
		// search for the renamed child
		final Map<String,Object> criteria = new Hashtable<String,Object>();
		criteria.put("name", petersNewName);
		final List<NestedSetsTreeNode> result = dao.find(walter, criteria);
		assert result.size() == 1;
		final PersonNst renamedPeter = (PersonNst) result.get(0);
		assert renamedPeter.getName().equals(petersNewName);
		
		// remove a root child, children will be gone too
		dao.remove(mary);
		assert dao.size(walter) == 2;	// "Walter" and "Linda" are left
		dao.remove(linda);
		assert dao.size(walter) == 1;
		
		// clean up
		dao.removeAll();
	}
}