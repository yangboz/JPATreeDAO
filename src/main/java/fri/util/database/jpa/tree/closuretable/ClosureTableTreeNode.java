package fri.util.database.jpa.tree.closuretable;

import fri.util.database.jpa.tree.TreeNode;

/**
 * Every domain object (POJO) that wants tree structure
 * by a ClosureTableTreeDao needs to implement this interface.
 * 
 * @author Fritz Ritzberger, 14.10.2012
 */
public interface ClosureTableTreeNode extends TreeNode
{
	/** For copy and unique constraint checking this is required. */
	@Override
	ClosureTableTreeNode clone();

}
