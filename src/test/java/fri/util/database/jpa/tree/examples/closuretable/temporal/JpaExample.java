package fri.util.database.jpa.tree.examples.closuretable.temporal;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.tree.Temporal;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeDao;
import fri.util.database.jpa.tree.examples.closuretable.PersonCtt;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;

/**
 * Shows how to use TemporalClosureTreeDao.
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
		// set up a temporal DAO for table "PersonCtt", implementing another remove-mechanism
		final TemporalClosureTableTreeDao dao = new TemporalClosureTableTreeDao(
				PersonCtt.class,
				PersonTemporalTreePath.class,
				true,
				Temporal.VALID_FROM,
				Temporal.VALID_TO,
				dbSession);
		
		final ClosureTableTreeNode walter = dao.createRoot(new PersonCtt("Walter"));
		dao.remove(walter);
		
		assert dao.getRoots().size() == 0;
		assert dao.getAllRoots().size() == 1;
		
		dao.unremove(walter);
		assert dao.getRoots().size() == 1;
	}
}