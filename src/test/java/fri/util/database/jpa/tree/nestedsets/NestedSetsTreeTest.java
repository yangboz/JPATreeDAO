package fri.util.database.jpa.tree.nestedsets;

import java.util.List;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.AbstractTreeTest;
import fri.util.database.jpa.tree.nestedsets.pojos.NestedSetsTreePojo;
import fri.util.database.jpa.tree.nestedsets.pojos.NonUniqueNestedSetsTreePojo;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueChildrenConstraintImpl;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueWholeTreeConstraintImpl;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * Unit test for NestedSetsTreeDao and NestedSetsTreeNode.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public class NestedSetsTreeTest extends AbstractTreeTest<NestedSetsTreeDao, NestedSetsTreeNode>
{
	/** Overridden to allocate a new NestedSetsTreeDao for this test case. */
	@Override
	protected NestedSetsTreeDao newDao(DbSession session)	{
		NestedSetsTreeDao dao = isTestCopy()
			? new NestedSetsTreeDao(
					NonUniqueNestedSetsTreePojo.class,
					NonUniqueNestedSetsTreePojo.class.getSimpleName(),
					session)
			: new NestedSetsTreeDao(
					NestedSetsTreePojo.class, 
					session);
		
		if (isTestCopy() == false)
			dao.setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
		
		return dao;
	}
	
	/** Factory method for new NestedSetsTreePojos. To be overridden by subclasses. */
	@Override
	protected NestedSetsTreeNode newTreePojo(String name) {
		return isTestCopy() ? new NonUniqueNestedSetsTreePojo(name) : new NestedSetsTreePojo(name);
	}

	/** For a concrete POJO we must return properties from it. Here the cast to NestedSetsTreePojo is done. */
	@Override
	protected String getName(NestedSetsTreeNode node)	{
		return isTestCopy() ? ((NonUniqueNestedSetsTreePojo) node).getName() : ((NestedSetsTreePojo) node).getName();
	}
	
	@Override
	protected void setNameNotConstraintChecking(NestedSetsTreeNode entity, String name) {
		if (isTestCopy())
			((NonUniqueNestedSetsTreePojo) entity).setName(name);
		else
			((NestedSetsTreePojo) entity).setName(name);
	}
	
	@Override
	protected void renameBeforeCopy(NestedSetsTreeNode node) {
		setNameNotConstraintChecking(node, COPIED_NAME_PREFIX+getName(node));
	}
	
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueWholeTreeConstraintImpl() {
		return new UniqueWholeTreeConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueWholeTreeConstraintImplWithoutRoots() {
		return new UniqueWholeTreeConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueChildrenTreeConstraintImpl() {
		return new UniqueChildrenConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueChildrenTreeConstraintImplWithoutRoots() {
		return new UniqueChildrenConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	
	@Override
	protected List<NestedSetsTreeNode> getFullTreeForIntegrityCheck(NestedSetsTreeNode root)	{
		return getDao().getTree(root);
	}
	
	/** Checks the left and right indexes for continuity. */
	@Override
	protected final void checkTreeIntegrity(DbSession session, NestedSetsTreeNode root)	{
		checkTreeIntegrity(root, getFullTreeForIntegrityCheck(root));
	}
	
	static void checkTreeIntegrity(NestedSetsTreeNode root, List<NestedSetsTreeNode> nodes)	{
		int orderNumber = 1;	// NestedSetsTreeDao.ROOT_LEFT, let this be private
		int index = -1;
		
		//AbstractTreeTest.outputTree(nodes);
		
		for (NestedSetsTreeNode node : nodes)	{
			index++;
			assertEquals(orderNumber, node.getLeft());
			orderNumber++;
			
			if (isLeaf(node))	{
				NestedSetsTreeNode next = index + 1 < nodes.size() ? nodes.get(index + 1) : null;
				if (isNextSibling(node, next) == false)	{	// if no next, or next is not sibling
					// search parent
					for (int i = index - 1; i >= 0; i--)	{
						NestedSetsTreeNode previous = nodes.get(i);
						if (node.getRight() + 1 == previous.getRight())	{
							orderNumber++;	// parent increments
							node = previous;
						}
					}
					
					if (next != null)
						orderNumber++;	// skip to child left
				}
				else	{	// has a follower sibling
					orderNumber++;	// skip to sibling left
				}
			}
		}
		assertEquals(orderNumber, root.getRight());
	}
	
	private static boolean isLeaf(NestedSetsTreeNode node)	{
		return node.getLeft() + 1 == node.getRight();
	}
	
	private static boolean isNextSibling(NestedSetsTreeNode node, NestedSetsTreeNode next)	{
		return next != null && node.getRight() + 1 == next.getLeft();
	}
	
}
