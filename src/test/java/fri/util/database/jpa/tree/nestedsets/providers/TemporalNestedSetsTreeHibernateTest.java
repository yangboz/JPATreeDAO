package fri.util.database.jpa.tree.nestedsets.providers;

import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeTest;

public class TemporalNestedSetsTreeHibernateTest extends TemporalNestedSetsTreeTest
{
	/** Overridden to set Hibernate as JPA provider. */
	@Override
	protected String getPersistenceUnitName()	{
		return HIBERNATE_PERSISTENCE_UNIT_NAME;
	}

}
