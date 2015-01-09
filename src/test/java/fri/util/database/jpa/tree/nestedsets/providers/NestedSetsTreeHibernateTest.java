package fri.util.database.jpa.tree.nestedsets.providers;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeTest;

public class NestedSetsTreeHibernateTest extends NestedSetsTreeTest
{
	/** Overridden to set Hibernate as JPA provider. */
	@Override
	protected String getPersistenceUnitName()	{
		return HIBERNATE_PERSISTENCE_UNIT_NAME;
	}

}
