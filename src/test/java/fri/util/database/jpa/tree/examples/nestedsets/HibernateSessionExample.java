package fri.util.database.jpa.tree.examples.nestedsets;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import fri.util.database.jpa.tree.hibernatesession.DbSessionHibernateImpl;

/**
 * Shows how to use NestedSetsTreeDao with Hibernate Session.
 * <p/>
 * HQL is not JPQL-compatible:
 * <ul>
 * 	<li>no "?1" parameter place-holders are allowed,</li>
 * 	<li>queries without aliases are allowed,</li>
 * 	<li>entity Date fields are not forced to have the <i>Temporal</i> annotation.</li>
 * </ul>
 * This example uses <code>DbSessionHibernateImpl</code> as tree persistence,
 * which replaces "?1" place-holders by "?" in every given query.
 */
public class HibernateSessionExample extends JpaExample
{
	public static void main(String[] args) throws Exception {
		// set up a Hibernate Session
		final Configuration configuration = new Configuration();
		configuration.addAnnotatedClass(PersonNst.class);
		configuration.configure();
		final ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(configuration.getProperties()).buildServiceRegistry();
		final SessionFactory sessionFactory = configuration.buildSessionFactory(serviceRegistry);
		final Session session = sessionFactory.openSession();
		session.beginTransaction();
		
		new HibernateSessionExample().run(new DbSessionHibernateImpl(session));
		
		session.getTransaction().commit();
		session.close();
		sessionFactory.close();
	}
}