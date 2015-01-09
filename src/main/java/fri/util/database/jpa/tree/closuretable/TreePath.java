package fri.util.database.jpa.tree.closuretable;


/**
 * The entity type (database table) where tree paths are stored.
 * For every descendant, all ancestor nodes are stored, including
 * the descendant itself, which means that for every node one
 * TreePaths object exists where descendant == ancestor.
 * 
 * @author Fritz Ritzberger, 14.10.2012
 */
public interface TreePath
{
	/**
	 * One of the ancestor tree nodes of the descendant, or the descendant itself.
	 * The private Java property name for this MUST BE "ancestor" in any implementation,
	 * as that name is used in DAO queries.
	 */
	ClosureTableTreeNode getAncestor();

	void setAncestor(ClosureTableTreeNode ancestor);

	/**
	 * The (descendant) tree node (of the ancestor).
	 * The private Java property name for this MUST BE "descendant" in any implementation,
	 * as that name is used in DAO queries.
	 */
	ClosureTableTreeNode getDescendant();

	void setDescendant(ClosureTableTreeNode descendant);

	/**
	 * The 0-n level this descendant tree node occurs, 0 is self-reference.
	 * The private Java property name for this MUST BE "depth" in any implementation,
	 * as that name is used in DAO queries.
	 */
	int getDepth();

	void setDepth(int depth);
	
	/**
	 * The 0-n child position this descendant occurs at.
	 * The private Java property name for this MUST BE "orderIndex" in any implementation,
	 * as that name is used in DAO queries.
	 */
	int getOrderIndex();

	void setOrderIndex(int orderIndex);
	
}
