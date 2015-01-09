package fri.util.database.jpa.tree.nestedsets;

import java.util.List;

import fri.util.database.jpa.tree.AbstractTemporalTreeTest;
import fri.util.database.jpa.tree.nestedsets.pojos.TemporalNestedSetsTreePojo;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeDao;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueChildrenTemporalConstraintImpl;
import fri.util.database.jpa.tree.nestedsets.uniqueconstraints.UniqueWholeTreeTemporalConstraintImpl;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * @author Fritz Ritzberger, 12.10.2011
 */
public class TemporalNestedSetsTreeTest extends AbstractTemporalTreeTest<TemporalNestedSetsTreeDao, NestedSetsTreeNode>
{
	/** Overridden to allocate a new TemporalNestedSetsTreeDao for this test case. */
	@Override
	protected TemporalNestedSetsTreeDao newDao(DbSession session) {
		TemporalNestedSetsTreeDao dao = testValidFromIsNull()
			? new TemporalNestedSetsTreeDao(
					TemporalNestedSetsTreePojo.class,
					TemporalNestedSetsTreePojo.class.getSimpleName(),
					null,
					"validTo",
					session)
			: new TemporalNestedSetsTreeDao(
					TemporalNestedSetsTreePojo.class,
					"validFrom",
					"validTo",
					session);
					
		if (isTestCopy() == false)
			dao.setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
		
		return dao;
	}

	@Override
	protected NestedSetsTreeNode newTreePojo(String name) {
		return new TemporalNestedSetsTreePojo(name);
	}

	@Override
	protected String getName(NestedSetsTreeNode node)	{
		return ((TemporalNestedSetsTreePojo) node).getName();
	}
	
	@Override
	protected void setNameNotConstraintChecking(NestedSetsTreeNode entity, String name) {
		((TemporalNestedSetsTreePojo) entity).setName(name);
	}
	
	@Override
	protected void renameBeforeCopy(NestedSetsTreeNode node) {
		setNameNotConstraintChecking(node, COPIED_NAME_PREFIX+getName(node));
	}
	
	
	@Override
	protected void setValid(NestedSetsTreeNode entity) {
		((TemporalNestedSetsTreePojo) entity).setValidTo(null);
	}


	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueWholeTreeConstraintImpl() {
		return new UniqueWholeTreeTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueWholeTreeConstraintImplWithoutRoots() {
		return new UniqueWholeTreeTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueChildrenTreeConstraintImpl() {
		return new UniqueChildrenTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, true);
	}
	
	@Override
	protected UniqueTreeConstraint<NestedSetsTreeNode> newUniqueChildrenTreeConstraintImplWithoutRoots() {
		return new UniqueChildrenTemporalConstraintImpl(UNIQUE_PROPERTY_NAMES, false);
	}
	
	
	@Override
	protected List<NestedSetsTreeNode> getFullTreeForIntegrityCheck(NestedSetsTreeNode root)	{
		return getDao().getFullTreeCacheable(root);
	}
	
	@Override
	protected void checkTreeIntegrity(DbSession session, NestedSetsTreeNode root) {
		NestedSetsTreeTest.checkTreeIntegrity(root, getFullTreeForIntegrityCheck(root));
	}
	
}
