package fri.util.database.jpa.tree.closuretable.providers;

import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeTest;

public class TemporalClosureTableTreeEclipselinkTest extends TemporalClosureTableTreeTest
{
	/** Overridden to set EclipseLink as JPA provider. */
	@Override
	protected String getPersistenceUnitName()	{
		return ECLIPSELINK_PERSISTENCE_UNIT_NAME;
	}

}
