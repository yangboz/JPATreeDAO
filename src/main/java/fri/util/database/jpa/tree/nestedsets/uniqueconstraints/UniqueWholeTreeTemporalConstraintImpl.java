package fri.util.database.jpa.tree.nestedsets.uniqueconstraints;

import java.util.List;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeDao;

/**
 * This appends validFrom and validTo dates to uniqueness-query,
 * to NOT find historicized objects.
 * 
 * @author Fritz Ritzberger, 20.10.2011
 */
public class UniqueWholeTreeTemporalConstraintImpl extends UniqueWholeTreeConstraintImpl
{
	/** See super-class constructor. */
	public UniqueWholeTreeTemporalConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	/** Overridden to append temporal conditions. */
	@Override
	protected void beforeCheckUniqueness(StringBuilder queryText, List<Object> parameters) {
		queryText.append(" and ");
		((NestedSetsTreeDao) getDao()).appendValidityCondition(getNodeTableAlias(), queryText, parameters);
	}
	
}
