package fri.util.database.jpa.tree.closuretable;

import java.io.Serializable;

import fri.util.database.jpa.tree.AbstractTemporalTreeTest;
import fri.util.database.jpa.tree.closuretable.pojos.ClosureTableTreePojo;
import fri.util.database.jpa.tree.closuretable.pojos.TemporalTreePathImpl;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TemporalClosureTableTreeDao;
import fri.util.database.jpa.tree.closuretable.uniqueconstraints.UniqueChildrenTemporalConstraintImpl;
import fri.util.database.jpa.tree.closuretable.uniqueconstraints.UniqueWholeTreeTemporalConstraintImpl;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * @author Fritz Ritzberger, 03.11.2012
 */
public class TemporalClosureTableTreeTest extends AbstractTemporalTreeTest<TemporalClosureTableTreeDao, ClosureTableTreeNode>
{
	public void testGetTreePathEntity() throws Exception	{
		beginDbTransaction("read TreePath entity for removed node");
		
		Serializable rootId = createTree();
		ClosureTableTreeNode root = getDao().find(rootId);
		ClosureTableTreeNode b2 = findByName(root, "B2");
		assertNotNull(getDao().getTreePathEntity(b2));
		
		getDao().remove(b2);
		assertNull(getDao().getTreePathEntity(b2));

		commitDbTransaction("read TreePath entity for removed node");
	}

	/** Overridden to allocate a new TemporalClosureTableTreeDao for this test case. */
	@Override
	protected TemporalClosureTableTreeDao newDao(DbSession session) {
		TemporalClosureTableTreeDao dao = testValidFromIsNull()
			? new TemporalClosureTableTreeDao(
					ClosureTableTreePojo.class,
					TemporalTreePathImpl.class,
					true,
					null,
					"validTo",
					session)
			: new TemporalClosureTableTreeDao(	// use another constructor for better coverage
					ClosureTableTreePojo.class,
					ClosureTableTreePojo.class.getSimpleName(),
					TemporalTreePathImpl.class,
					TemporalTreePathImpl.class.getSimpleName(),
					true,
					"validFrom",
					"validTo",
					session);
					
		dao.setRemoveReferencedNodes(true);
					
		if (isTestCopy() == false)
			dao.setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
		
		return dao;
	}
	

	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueWholeTreeConstraintImpl() {
		return new UniqueWholeTreeTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueWholeTreeConstraintImplWithoutRoots() {
		return new UniqueWholeTreeTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueChildrenTreeConstraintImpl() {
		return new UniqueChildrenTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}

	@Override
	protected UniqueTreeConstraint<ClosureTableTreeNode> newUniqueChildrenTreeConstraintImplWithoutRoots() {
		return new UniqueChildrenTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}

	
	@Override
	protected ClosureTableTreeNode newTreePojo(String name) {
		return new ClosureTableTreePojo(name);
	}

	@Override
	protected String getName(ClosureTableTreeNode node) {
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
	protected void checkTreeIntegrity(DbSession session, ClosureTableTreeNode root) {
		ClosureTableTreeTest.checkTreeIntegrity(session, root, ClosureTableTreePojo.class.getSimpleName(), TemporalTreePathImpl.class.getSimpleName());
	}

}
