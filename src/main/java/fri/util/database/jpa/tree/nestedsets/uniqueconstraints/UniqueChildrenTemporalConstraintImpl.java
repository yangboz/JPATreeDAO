package fri.util.database.jpa.tree.nestedsets.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * @see fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueChildrenConstraintImpl
 * 
 * @author Fritz Ritzberger, 31.10.2012
 */
public class UniqueChildrenTemporalConstraintImpl extends UniqueWholeTreeTemporalConstraintImpl
{
	public UniqueChildrenTemporalConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	/** {@inheritDoc} */
	@Override
	public final boolean checkUniqueConstraint(List<NestedSetsTreeNode> nodes, TreeActionLocation<NestedSetsTreeNode> location)	{
		// this assumes that children are unique and only the topmost node (first in list) has to be checked when asserting unique children
		final NestedSetsTreeNode node = nodes.get(0);
		if (isRootsCheck(location.root, node))	{
			return super.checkUniqueConstraint(nodes, location);
		}
		return checkUniqueChildrenConstraint(node, location);
	}
	
}
