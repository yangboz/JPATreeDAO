package fri.util.database.jpa.tree.closuretable.uniqueconstraints;

import java.util.List;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeDao;

/**
 * This appends validFrom and validTo dates to uniqueness-query,
 * to NOT find historicized objects.
 * 
 * @author Fritz Ritzberger, 02.11.2012
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
		((ClosureTableTreeDao) getDao()).appendValidityCondition(getPathTableAlias(), queryText, parameters);
	}

}
