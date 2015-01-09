package fri.util.database.jpa.commons;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import junit.framework.TestCase;

/**
 * Base class of JPA unit tests, binds POJO classes via Java statements to JPA provider.
 * Sub-classes must provide an array of classes to bind.
 * Sessions with transactions can be requested from protected service methods.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public abstract class AbstractJpaTest extends TestCase
{
	protected static final String HIBERNATE_PERSISTENCE_UNIT_NAME = "hibernate-persistence-unit";
	protected static final String ECLIPSELINK_PERSISTENCE_UNIT_NAME = "eclipselink-persistence-unit";
	
	private EntityManagerFactory factory;
	private EntityManager entityManager;
	
	@Override
	protected void setUp() throws Exception {
		factory = Persistence.createEntityManagerFactory(getPersistenceUnitName());
		entityManager = factory.createEntityManager();
	}
	
	@Override
	protected void tearDown() throws Exception {
		if (entityManager != null)	{	// could be overridden by Hibernate Session
			entityManager.close();
			factory.close();
		}
	}
	
	/** To be overridden by unit tests that refer to different JPA providers. */
	protected String getPersistenceUnitName()	{
		return HIBERNATE_PERSISTENCE_UNIT_NAME;
	}
	
	
	// convenience methods
	
	protected final EntityManager beginTransaction(String message) {
		logStart(message);
		final EntityTransaction transaction = entityManager.getTransaction();
		transaction.begin();	// would throw exception if another transaction is running
		return entityManager;
	}

	protected final void commitTransaction(String message) {
		commitOrRollbackTransaction(message, true);
	}

	protected final void rollbackTransaction(String message) {
		commitOrRollbackTransaction(message, false);
	}

	protected void log(String message)	{
		System.out.println(message);
	}
	
	protected final void logStart(String message)	{
		log("----> starting to "+message);
	}
	protected final void logBeforeEnd(String message, String action)	{
		log("----> before "+message+" transaction "+action);
	}
	protected final void logAfterEnd(String message, String action)	{
		log("----> after "+message+" transaction "+action);
	}
	
	private void commitOrRollbackTransaction(String message, boolean commit) {
		final String action = (commit ? "commit" : "rollback");
		logBeforeEnd(message, action);
		
		if (commit)
			entityManager.getTransaction().commit();
		else
			entityManager.getTransaction().rollback();
		
		logAfterEnd(message, action);
	}

}
