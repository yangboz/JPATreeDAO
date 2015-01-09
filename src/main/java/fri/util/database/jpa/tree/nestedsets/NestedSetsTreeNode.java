package fri.util.database.jpa.tree.nestedsets;

import fri.util.database.jpa.tree.TreeNode;

/**
 * A tree node.
 * It represents a hierarchy of records in a database table without parent
 * references in children or vice versa. The hierarchy is managed via
 * <code>lft</code> and <code>rgt</code> depth-first order numbers directly
 * in the database record. To be able to store more than one tree in a table,
 * every node additionally has a reference to the top-level root node (not to its parent!).
 * <p/>
 * This interface must be implemented by any domain object that represents
 * hierarchical data and should be managed via <code>NestedSetsTreeDao</code>.
 * Do not use these interface methods outside (except clone()), they are for the DAO only.
 * <p/>
 * See http://www.klempert.de/nested_sets/ or Wikipedia "nested sets tree".
 * <P/>
 * <code>Cloneable</code> interface is required only for copy and unique
 * constraint checking. You can return null from clone() if neither is needed.
 * 
 * @see fri.util.database.jpa.tree.nestedsets.NestedSetsTreeDao
 * @see test.fri.util.database.jpa.tree.nestedsets.pojos.AbstractNestedSetsTreePojo
 * @see test.fri.util.database.jpa.tree.nestedsets.pojos.NestedSetsTreePojo
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public interface NestedSetsTreeNode extends TreeNode
{
	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * The private Java property name for this MUST BE "lft" in any implementation,
	 * as that name is used in DAO queries.
	 * @return the left order number.
	 */
	int getLeft();
	
	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * Sets the left order number.
	 */
	void setLeft(int left);

	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * The private Java property name for this MUST BE "rgt" in any implementation,
	 * as that name is used in DAO queries.
	 * @return the right order number.
	 */
	int getRight();

	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * Sets the right order number.
	 */
	void setRight(int right);

	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * The private Java property name for this MUST BE "topLevel" in any implementation,
	 * as that name is used in DAO queries.
	 * @return the root node of this tree node (root and topLevel are synonyms).
	 */
	NestedSetsTreeNode getTopLevel();

	/**
	 * DO NOT use this, is for the DAO exclusively.
	 * Sets the top-level root of this tree node.
	 */
	void setTopLevel(NestedSetsTreeNode topLevel);
	
	/**
	 * For copy and unique constraint checking this is required.
	 * @return a clone of this node, not including the "id" property (which is set by the JPA layer).
	 */
	@Override
	NestedSetsTreeNode clone();

}
