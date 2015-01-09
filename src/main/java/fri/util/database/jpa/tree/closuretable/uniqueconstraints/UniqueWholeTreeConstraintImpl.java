package fri.util.database.jpa.tree.closuretable.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.uniqueconstraints.AbstractWholeTreeUniqueConstraintImpl;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;

/**
 * Unique nodes related to the whole tree.
 * Defines the JPQL statements for super-class.
 * 
 * @author Fritz Ritzberger, 25.10.2012
 */
public class UniqueWholeTreeConstraintImpl extends AbstractWholeTreeUniqueConstraintImpl<ClosureTableTreeNode>
{
	/** See super-class constructor. */
	public UniqueWholeTreeConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		super(uniquePropertyNames, shouldCheckRootsForUniqueness);
	}
	
	@Override
	protected String getNodeTableAlias() {
		return "n";
	}
	
	protected String getPathTableAlias() {
		return "p";
	}
	
	@Override
	protected String fromClause() {
		return
			nodeEntityName()+" "+getNodeTableAlias()+", "+pathEntityName()+" "+getPathTableAlias()+
			" where "+getPathTableAlias()+".descendant = "+getNodeTableAlias()+" and ";
	}
	
	@Override
	protected void appendRootCheckingCondition(StringBuilder queryText, List<Object> parameters) {
		queryText.append("not exists "+	// any parent
				"  (select 'x' from "+pathEntityName()+" p2"+
				"   where p2.descendant = "+getPathTableAlias()+".descendant and p2.depth > 0)");
	}
	
	@Override
	protected void appendNodeCheckingCondition(ClosureTableTreeNode root, StringBuilder queryText, List<Object> parameters) {
		queryText.append(getPathTableAlias()+".ancestor = "+QueryBuilderUtil.buildIndexedPlaceHolder(parameters));
		parameters.add(root);
	}

}
