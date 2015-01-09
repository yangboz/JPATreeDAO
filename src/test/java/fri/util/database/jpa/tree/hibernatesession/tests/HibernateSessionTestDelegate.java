package fri.util.database.jpa.tree.hibernatesession.tests;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.closuretable.pojos.ClosureTableTreePojo;
import fri.util.database.jpa.tree.closuretable.pojos.TemporalTreePathImpl;
import fri.util.database.jpa.tree.closuretable.pojos.TreePathImpl;
import fri.util.database.jpa.tree.hibernatesession.DbSessionHibernateImpl;
import fri.util.database.jpa.tree.nestedsets.pojos.NestedSetsTreePojo;
import fri.util.database.jpa.tree.nestedsets.pojos.NonUniqueNestedSetsTreePojo;
import fri.util.database.jpa.tree.nestedsets.pojos.TemporalNestedSetsTreePojo;

/**
 * Implements Hibernate Session management for all Hibernate Session tests.
 * 
 * @author Fritz Ritzberger, Aug 24, 2013
 */
class HibernateSessionTestDelegate
{
	// any new test POJO class must be added here when used in AbstractTreeTest!
	private Class<?> [] persistenceClasses = new Class<?> []	{
		NestedSetsTreePojo.class,
		NonUniqueNestedSetsTreePojo.class,
		TemporalNestedSetsTreePojo.class,
		ClosureTableTreePojo.class,
		TreePathImpl.class,
		TemporalTreePathImpl.class,
	};

	private SessionFactory sessionFactory;
	private Session session;
	
	void setUp() throws Exception {
		final Configuration configuration = new Configuration();
		for (Class<?> persistenceClass : persistenceClasses)
			configuration.addAnnotatedClass(persistenceClass);

		configuration.configure();
		
		final ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
		sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		session =  sessionFactory.openSession();
	}
	
	void tearDown() throws Exception {
		session.close();
		sessionFactory.close();
	}
	
	DbSession newDbSession() {
		session.beginTransaction();
		return new DbSessionHibernateImpl(session);
	}
	
	void commitDbTransaction() {
		session.getTransaction().commit();
	}

	void rollbackDbTransaction() {
		session.getTransaction().rollback();
	}

}
