package fri.util.database.jpa.tree.closuretable;

import java.io.Serializable;
import java.util.List;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.AbstractTreeTest;
import fri.util.database.jpa.tree.closuretable.pojos.ClosureTableTreePojo;
import fri.util.database.jpa.tree.closuretable.pojos.TreePathImpl;
import fri.util.database.jpa.tree.closuretable.uniqueconstraints.UniqueChildrenConstraintImpl;
import fri.util.database.jpa.tree.closuretable.uniqueconstraints.UniqueWholeTreeConstraintImpl;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * @author Fritz Ritzberger, 19.10.2012
 */
public class ClosureTableTreeTest extends AbstractTreeTest<ClosureTableTreeDao, ClosureTableTreeNode>
{
	private static final String[][] UNIQUE_PROPERTY_NAMES = new String [][] {{ "name" }};

	private boolean positionMatters = true;
	
	public void testGetTreePathEntity() throws Exception	{
		beginDbTransaction("read TreePath entity for node");
		
		Serializable rootId = createTree();
		ClosureTableTreeNode root = getDao().find(rootId);
		ClosureTableTreeNode b2 = findByName(root, "B2");
		TreePath b2TreePath = getDao().getTreePathEntity(b2);
		assertNotNull(b2TreePath);
		assertEquals("B2", getName(b2TreePath.getAncestor()));
		assertEquals("B2", getName(b2TreePath.getDescendant()));

		commitDbTransaction("read TreePath entity for node");
	}

	/** Overridden to allocate a new ClosureTableTreeDao for this test case. */
	@Override
	protected ClosureTableTreeDao newDao(DbSession session)	{
		ClosureTableTreeDao dao =
			new ClosureTableTreeDao(
					ClosureTableTreePojo.class,
					TreePathImpl.class,
					positionMatters,
					session);
		
		dao.setRemoveReferencedNodes(true);
		
		if (isTestCopy() == false)
			dao.setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
		
		return dao;
	}
	
	@Override
	protected ClosureTableTreeNode newTreePojo(String name) {
		return new ClosureTableTreePojo(name);
	}

	@Override
	protected String getName(ClosureTableTreeNode node)	{
		return ((ClosureTableTreePojo) node).getName();
	}
	
	@Override
	protected void setNameNotConstraintChecking(ClosureTableTreeNode node, String name) {
		((ClosureTableTreePojo) node).setName(name);
	}

	@Override
	protected void renameBeforeCopy(ClosureTableTreeNode node) {
		setNameNotConstraintChecking(node, COPIED_NAME_PREFIX+getName(node));
	}
	

	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueWholeTreeConstraintImpl() {
		return new UniqueWholeTreeConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueWholeTreeConstraintImplWithoutRoots() {
		return new UniqueWholeTreeConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}

	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueChildrenTreeConstraintImpl() {
		return new UniqueChildrenConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueChildrenTreeConstraintImplWithoutRoots() {
		return new UniqueChildrenConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	
	/** Checks that no ClosureTableTreeNode is not connected to TreePath. */
	@Override
	protected void checkTreeIntegrity(DbSession session, ClosureTableTreeNode root) {
		checkTreeIntegrity(session, root, ClosureTableTreePojo.class.getSimpleName(), TreePathImpl.class.getSimpleName());
	}
	
	
	@SuppressWarnings("unused")
	static void checkTreeIntegrity(DbSession session, ClosureTableTreeNode root, String nodeEntityName, String pathEntityName)	{
		//session.flush();
		String queryText =
			"select n from "+nodeEntityName+" n "+
			"where not exists (select 'x' from "+pathEntityName+" p where p.descendant = n)";
		@SuppressWarnings("unchecked")
		List<ClosureTableTreeNode> result = (List<ClosureTableTreeNode>) session.queryList(queryText, null);
		assertEquals("Count nodes that have no paths:", 0, result.size());
	}
	
}
