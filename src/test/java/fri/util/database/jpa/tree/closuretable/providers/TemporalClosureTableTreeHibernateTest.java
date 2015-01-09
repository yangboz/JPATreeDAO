package fri.util.database.jpa.tree.closuretable.providers;

import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeTest;

public class TemporalClosureTableTreeHibernateTest extends TemporalClosureTableTreeTest
{
	/** Overridden to set Hibernate as JPA provider. */
	@Override
	protected String getPersistenceUnitName()	{
		return HIBERNATE_PERSISTENCE_UNIT_NAME;
	}

}
