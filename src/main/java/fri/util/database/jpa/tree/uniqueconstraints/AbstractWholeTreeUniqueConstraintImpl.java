package fri.util.database.jpa.tree.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.tree.TreeNode;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Checks if the values of a a property set is unique within a specific tree.
 * That means that property values set can appear just once in one tree,
 * but can appear in other trees in the same database table.
 * 
 * @author Fritz Ritzberger, 20.10.2011
 */
public abstract class AbstractWholeTreeUniqueConstraintImpl <N extends TreeNode>
	extends AbstractUniqueTreeConstraintImpl<N>
{
	/** See super-class constructor. */
	protected AbstractWholeTreeUniqueConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	/** {@inheritDoc}. */
	@Override
	public boolean checkUniqueConstraint(List<N> nodes, TreeActionLocation<N> location)	{
		return checkUniqueWholeTreeConstraint(nodes, location);
	}
	
}
