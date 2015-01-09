package fri.util.database.jpa.tree;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * Tree DAO provides hierarchical access to records in a JPA database layer.
 * There can be more than one tree in a database table.
 * A record can not be in several trees at the same time.
 * A record can have one parent or no parent (root), but not several parents.
 * Normally there is no record that is not in a tree, except it is a root without children.
 * <p/>
 * This interface abstracts the functionality of a tree DAO so that the underlying
 * implementation can be replaced. In this library two such DAOs are implemented, 
 * one for a nested-sets-tree, one for a closure-table-tree.
 * <p/>
 * For more information see
 * <a href="http://de.slideshare.net/billkarwin/models-for-hierarchical-data">Bill Karwin slides "Models for Hierarchical Data"</a>, page 40, and
 * <a href="http://de.slideshare.net/billkarwin/sql-antipatterns-strike-back">Bill Karwin slides "SQL Antipatterns strike back"</a>, page 68.
 * 
 * @author Fritz Ritzberger, 19.10.2012
 * 
 * @param N the tree node type managed by this DAO.
 */
public interface TreeDao<N extends TreeNode> 
{
	/** The position parameter to express "append to end of parent's child list", for add, move, copy. */
	int UNDEFINED_POSITION = -1;	// must be below allowed positions which start at zero
	
	
	/** @return true if passed entity is already persistent, i.e. its getId() is not null. */
	boolean isPersistent(N entity);

	/** @return the object by identity (primary key) from database. */
	N find(Serializable id);

	/**
	 * Updates the given persistent object. This performs explicit constraint checking
	 * when <code>checkUniqueConstraintsOnUpdate</code> is true (default is false).
	 * @throws UniqueConstraintViolationException when given entity is not unique.
	 * @throws IllegalArgumentException when given node is not yet persistent.
	 */
	void update(N entity) throws UniqueConstraintViolationException;

	/** @return true if passed node is persistent and a root. */
	boolean isRoot(N entity);

	/**
	 * Creates a tree root node.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 * @throws IllegalArgumentException when root is already persistent.
	 */
	N createRoot(N root) throws UniqueConstraintViolationException;

	/** @return the count of all nodes of any depth below given node, including itself. */
	int size(N tree);

	/** @return all tree root nodes. */
	List<N> getRoots();

	/** Removes all roots, including the nodes below them. Thus clears the table. */
	void removeAll();

	/**
	 * Reads a tree or sub-tree, including all children.
	 * The result is NOT EXPECTED to be used with
	 * <code>findSubTree()</code> or <code>findDirectChildren()</code>!
	 * @param parent the parent of the tree to read, can also be root of the tree.
	 * @return all tree nodes under given parent, including parent.
	 */
	List<N> getTree(N parent);

	/**
	 * Reads a tree or sub-tree, including all children,
	 * which can be cached and passed back into
	 * <code>findSubTree()</code> or <code>findDirectChildren()</code>.
	 * Mind that any cached tree could be out-of-sync with database when another client performs changes.
	 * @param parent the parent of the tree to read, can also be root of the tree.
	 * @return all tree nodes under given parent, including parent, in depth-first order.
	 */
	List<N> getTreeCacheable(N parent);

	/**
	 * Finds a sub-tree list in a cached list of tree nodes under given parent.
	 * The subNodes list was returned from a call to <code>getTreeCacheable()</code>.
	 * Mind that any cached tree could be out-of-sync with database when another client performs changes.
	 * @param parent the parent node to search a sub-tree for, contained somewhere in the given list of nodes.
	 * @param treeCacheable a list of nodes from which to extract a sub-tree, also containing given parent.
	 * @return a list of nodes under the passed parent node.
	 */
	List<N> findSubTree(N parent, List<N> treeCacheable);

	/**
	 * Finds direct children in a cached list of tree nodes, parent is first in that cached list.
	 * The subNodes list was returned from a call to <code>getTreeCacheable()</code> or <code>findSubTree()</code>.
	 * Mind that any cached tree could be out-of-sync with database when another client performs changes.
	 * @param treeCacheable a list of nodes from which to extract the direct child list, parent at head of list.
	 * @return a list of direct children of the the parent which is first in list.
	 */
	List<N> findDirectChildren(List<N> treeCacheable);

	/** @return true when given node has no children, i.e. is not a container-node. */
	boolean isLeaf(N node);

	/** @return the number of direct children of given parent node. */
	int getChildCount(N parent);

	/**
	 * Gives the children of passed parent. This method reads the full subtree under parent.
	 * Removing from returned list will not remove that child from tree but cause an exception.
	 * @return the ordered list of <b>direct</b> children under given parent.
	 */
	List<N> getChildren(N parent);

	/** @return the root node of given node. Root has itself as root. */
	N getRoot(N node);

	/** @return the parent node of given node. Root has null as parent. */
	N getParent(N node);

	/** @return all parent nodes of given node, i.e. its path from root to (exclusive) node. Root will return an empty list. */
	List<N> getPath(N node);

	/** @return the depth of given node. Root has level 0. */
	int getLevel(N node);

	/** @return true when child is in the tree under parent, or parent is equal to child, else false. */
	boolean isEqualToOrChildOf(N child, N parent);

	/** @return true when child is in the tree under parent and not parent, else false. */
	boolean isChildOf(N child, N parent);

	/**
	 * Adds to end of children of given parent. 
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 */
	N addChild(N parent, N child) throws UniqueConstraintViolationException;

	/**
	 * Adds at specified position to children of given parent.
	 * @param position -1 for append, else target position in child list.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 */
	N addChildAt(N parent, N child, int position) throws UniqueConstraintViolationException;

	/**
	 * Adds to children before given sibling, sibling is pushed backwards in children list. 
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 */
	N addChildBefore(N sibling, N child) throws UniqueConstraintViolationException;

	/** Removes the tree under given node, including the node. Node can also be a root. */
	void remove(N node);

	/**
	 * Moves the given node to end of children list of parent.
	 * When source is identical with target, nothing happens.
	 * @throws UniqueConstraintViolationException 
	 * @throws IllegalArgumentException when target is below source.
	 */
	void move(N node, N newParent) throws UniqueConstraintViolationException;

	/**
	 * Moves the given node to given position in children list of parent.
	 * When source is identical with target, nothing happens.
	 * @throws UniqueConstraintViolationException 
	 * @throws IllegalArgumentException when target is below source.
	 */
	void moveTo(N node, N parent, int position) throws UniqueConstraintViolationException;

	/**
	 * Moves the given node to position of given sibling, pushing sibling backwards in list.
	 * When source is identical with target, nothing happens.
	 * @throws UniqueConstraintViolationException 
	 * @throws IllegalArgumentException when target is below source.
	 */
	void moveBefore(N node, N sibling) throws UniqueConstraintViolationException;

	/**
	 * Moves a sub-tree to be a root. This means it is removed from its previous root.
	 * When child is already a root, nothing happens.
	 * @throws UniqueConstraintViolationException when unique constraint(s) for roots would be violated.
	 * @throws IllegalArgumentException when passed node is not yet persistent.
	 */
	void moveToBeRoot(N child) throws UniqueConstraintViolationException;

	/**
	 * Copies the given node to end of children list of parent.
	 * When source is identical with target, the node will be be copied to the position of its originator.
	 * @param node the node to be copied.
	 * @param copiedNodeTemplate a template for the copied node containing altered properties, can be null.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 * @throws IllegalArgumentException when target is below source.
	 */
	N copy(N node, N parent, N copiedNodeTemplate) throws UniqueConstraintViolationException;

	/**
	 * Copies the given node to given position in children list of parent. 
	 * When source is identical with target, the node will be be copied to the position of its originator.
	 * @param node the node to be copied.
	 * @param parent the parent-node of the children the node should be copied into.
	 * @param position the 0-n index the node should obtain within children of parent.
	 * @param copiedNodeTemplate a template for the copied node containing altered properties, can be null.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 * @throws IllegalArgumentException when target is below source.
	 */
	N copyTo(N node, N parent, int position, N copiedNodeTemplate) throws UniqueConstraintViolationException;

	/**
	 * Copies the given node to position of given sibling, pushing sibling backwards in list.
	 * When source is identical with target, the node will be be copied to the position of its originator.
	 * @param node the node to be copied.
	 * @param sibling the target node the copied node should push backwards in children list.
	 * @param copiedNodeTemplate a template for the copied node containing altered properties, can be null.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 * @throws IllegalArgumentException when target is below source.
	 */
	N copyBefore(N node, N sibling, N copiedNodeTemplate) throws UniqueConstraintViolationException;

	/**
	 * Copies a tree to be a root. This means it is removed from its previous root. 
	 * When child is already a root, the node will be be copied to be another root.
	 * @param child the node to copy to be a root.
	 * @param copiedNodeTemplate a template for the copied node containing altered properties, can be null.
	 * @throws UniqueConstraintViolationException when unique constraint(s) for roots would be violated.
	 * @throws IllegalArgumentException when passed node is not yet persistent.
	 */
	N copyToBeRoot(N child, N copiedNodeTemplate) throws UniqueConstraintViolationException;

	
	/**
	 * Implementers have the opportunity to edit copied nodes before they are inserted.
	 * This might prevent unique constraint(s) violations.
	 */
	public interface CopiedNodeRenamer <N extends TreeNode>
	{
		/**
		 * Edits the properties of given tree node to <b>not</b> violate unique constraint(s).
		 * The implementer is expected to cast the given node for editing.
		 * @param node the copied node that should be modified before insertion.
		 */
		void renameCopiedNode(N node);
	}
	
	/**
	 * Sets a CopiedNodeRenamer for copy actions in trees guarded by unique constraint(s).
	 * Mind that, once set, this will edit every following copy-action!
	 * @param copiedNodeRenamer the editor to be applied for following copy action, can be null.
	 */
	void setCopiedNodeRenamer(CopiedNodeRenamer<N> copiedNodeRenamer);

	
	/**
	 * Convenience finder method. All criteria will be AND'ed.
	 * @param parent the parent under which to search, can be null.
	 * @param criteria a name/value mapping for the nodes to be found under given tree.
	 * @return tree nodes with given criteria.
	 */
	List<N> find(N parent, Map<String,Object> criteria);

	/**
	 * Optionally lets set unique constraint(s).
	 * @param uniqueTreeConstraint the constraint checker implementation, or null to switch off checking.
	 */
	void setUniqueTreeConstraint(UniqueTreeConstraint<N> uniqueTreeConstraint);

	/**
	 * Setting this to true will call unique constraint(s) even on <code>update()</code>.
	 * Default is false, because it is assumed that caller itself does a check
	 * by calling <code>dao.checkUniqueConstraint()</code> before updating.
	 * @param checkUniqueConstraintOnUpdate the behavior to be set.
	 */
	void setCheckUniqueConstraintOnUpdate(boolean checkUniqueConstraintOnUpdate);

	/**
	 * Checks unique constraint(s) for passed entity before an update of unique properties.
	 * Assuming that there is a unique property <code>name</code> in entity, this method MUST be
	 * called BEFORE <code>entity.setName(name)</code> is called, else entity will get dirty and
	 * will be flushed by the JPA layer before the constraint checking query can be launched.
	 * For that purpose you must create a clone (template) of the node pending to be updated,
	 * and set the new value of the property into the clone.
	 * @param cloneOfExistingNodeWithNewValues non-persistent clone of the entity to be checked,
	 * 		containing at least the properties the constraint will check.
	 * @param root the root where insert or update will happen, null when node is (or will be) be a root.
	 * @param originalNode the original node to be inserted or updated, null when node is not yet persistent.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 */
	void checkUniqueConstraint(N cloneOfExistingNodeWithNewValues, N root, N originalNode) throws UniqueConstraintViolationException;

}
