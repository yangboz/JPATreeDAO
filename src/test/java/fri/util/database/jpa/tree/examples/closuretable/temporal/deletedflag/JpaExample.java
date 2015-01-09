package fri.util.database.jpa.tree.examples.closuretable.temporal.deletedflag;

import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.tree.Temporal;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeDao;
import fri.util.database.jpa.tree.closuretable.TreePath;
import fri.util.database.jpa.tree.examples.closuretable.PersonCtt;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;

/**
 * Shows how to use TemporalClosureTreeDao with another remove-mechanism
 * than <i>validFrom</i> and <i>validTo</i> properties.
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
		final TemporalClosureTableTreeDao dao = new TemporalClosureTableTreeDao(PersonCtt.class, PersonDeletedFlagTreePath.class, true, null, null, dbSession)
		{
			@Override
			public boolean isValid(Temporal node, Date validityDate) {
				return ((PersonDeletedFlagTreePath) node).isDeleted() == false;
			}
			
			@Override
			protected void assignInvalidity(TreePath path) {
				((PersonDeletedFlagTreePath) path).setDeleted(true);
			}
			@Override
			protected void assignValidity(TreePath path) {
				((PersonDeletedFlagTreePath) path).setDeleted(false);
			}
			
			@Override
			public void appendValidityCondition(String tableAlias, StringBuilder queryText, List<Object> parameters) {
				appendCondition(true, tableAlias, queryText, parameters);
			}
			@Override
			protected void appendInvalidityCondition(String tableAlias, StringBuilder queryText, List<Object> parameters) {
				appendCondition(false, tableAlias, queryText, parameters);
			}
			
			private void appendCondition(boolean validity, String tableAlias, StringBuilder queryText, List<Object> parameters) {
				queryText.append(buildAliasedPropertyName(tableAlias, "deleted")+" = "+buildIndexedPlaceHolder(parameters));
				parameters.add(validity ? Boolean.FALSE : Boolean.TRUE);
			}
		};
		
		final ClosureTableTreeNode walter = dao.createRoot(new PersonCtt("Walter"));
		dao.remove(walter);
		
		assert dao.getRoots().size() == 0;
		assert dao.getAllRoots().size() == 1;
	}
}