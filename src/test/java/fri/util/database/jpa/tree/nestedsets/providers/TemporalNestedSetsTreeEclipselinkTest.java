package fri.util.database.jpa.tree.nestedsets.providers;

import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeTest;

public class TemporalNestedSetsTreeEclipselinkTest extends TemporalNestedSetsTreeTest
{
	/** Overridden to set EclipseLink as JPA provider. */
	@Override
	protected String getPersistenceUnitName()	{
		return ECLIPSELINK_PERSISTENCE_UNIT_NAME;
	}

}
