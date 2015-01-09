package fri.util.database.jpa.tree.closuretable.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Accepts a single or a combination of several unique properties,
 * and it checks if they are unique relative to its parent node.
 * That means a property value can appear just once among siblings,
 * but can appear in other child lists.
 * Also performs the root uniqueness check if location requires that.
 * 
 * @author Fritz Ritzberger, 31.10.2012
 */
public class UniqueChildrenConstraintImpl extends UniqueWholeTreeConstraintImpl
{
	public UniqueChildrenConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	/** {@inheritDoc} */
	@Override
	public final boolean checkUniqueConstraint(List<ClosureTableTreeNode> nodes, TreeActionLocation<ClosureTableTreeNode> location)	{
		// this assumes that children are unique and only the topmost node (first in list) has to be checked when asserting unique children
		final ClosureTableTreeNode node = nodes.get(0);
		if (isRootsCheck(location.root, node))	{
			return super.checkUniqueConstraint(nodes, location);
		}
		return checkUniqueChildrenConstraint(node, location);
	}
	
}
