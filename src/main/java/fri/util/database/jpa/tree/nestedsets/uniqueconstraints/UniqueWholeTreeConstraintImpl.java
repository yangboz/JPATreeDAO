package fri.util.database.jpa.tree.nestedsets.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.uniqueconstraints.AbstractWholeTreeUniqueConstraintImpl;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;

/**
 * Unique nodes related to the whole tree.
 * Defines the JPQL statements for super-class.
 * 
 * @author Fritz Ritzberger, 25.10.2012
 */
public class UniqueWholeTreeConstraintImpl extends AbstractWholeTreeUniqueConstraintImpl<NestedSetsTreeNode>
{
	/** See super-class constructor. */
	public UniqueWholeTreeConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	@Override
	protected String getNodeTableAlias() {
		return "n";
	}
	
	@Override
	protected String fromClause() {
		return nodeEntityName()+" "+getNodeTableAlias()+" where ";
	}
	
	@Override
	protected void appendRootCheckingCondition(StringBuilder queryText, List<Object> parameters) {
		queryText.append(getNodeTableAlias()+".topLevel = "+getNodeTableAlias());
	}
	
	@Override
	protected void appendNodeCheckingCondition(NestedSetsTreeNode root, StringBuilder queryText, List<Object> parameters) {
		queryText.append(getNodeTableAlias()+".topLevel = "+QueryBuilderUtil.buildIndexedPlaceHolder(parameters));
		parameters.add(root);
	}

}
