package fri.util.database.jpa.tree.closuretable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.AbstractTreeDao;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Data-access-object for a hierarchical representation of records (nodes),
 * using two database tables, having no parent reference in children.
 * This closure-table tree will maintain child positions, meaning children
 * lists have a defined order which can only be changed by a move().
 * For better performance this can be turned off by a constructor parameter.
 * <p/>
 * This DAO manages the tree structure of any entity type that implements ClosureTableTreeNode.
 * You don't need to save or delete the ClosureTableTreeNode separately, this is done here.
 * <p/>
 * The "closure table" method acts on two database tables, thus
 * it needs two entity types for its operations:
 * <ul>
 * 	<li>treeNode is the node table (data),</li>
 * 	<li>treePath is the associated paths table (tree structure).</li>
 * </ul>
 * For every ClosureTableTreeNode implementation you need at least one associated
 * TreePath implementation. As the latter references the first, they always go together.
 * A TreePath implementation represents a tree aspect of some data. The data (nodes)
 * do not have any information about their tree structure in them, but must expose
 * an "id" property (like the TreeNode interface demands).
 * You can have several tree "aspects" of the data nodes, this would require several
 * TreePath implementations for one ClosureTableTreeNode implementation. For each TreePath
 * implementation you must use a separate DAO instance.
 * <p/>
 * Following is the path/node structure: every node is connected to every node
 * above it (by a path), and to itself, but not to its siblings. A root has connections
 * (paths) to all nodes in tree. Sibling order is represented by a 0-n order-index
 * in all paths with depth 1 (all other depths have no order information).
 * A node's self-reference path has depth 0, any other path has depth > 0.
 * <p/>
 * See links in TreeDao for more information.
 * <p/>
 * Note: as the temporal derivation obtains a state, all write-methods here are synchronized.
 * 
 * @see fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode
 * @see fri.util.database.jpa.tree.TreeDao
 * 
 * @author Fritz Ritzberger, 14.10.2012. Thanks to Bill Karwin for his instructions on the internet.
 */
public class ClosureTableTreeDao extends AbstractTreeDao<ClosureTableTreeNode>
{
	private final Class<? extends ClosureTableTreeNode> treeNodeEntityClass;
	private final Class<? extends TreePath> treePathEntityClass;
	private final String treePathEntity;
	private final boolean orderIndexMatters;

	private boolean removeReferencedNodes = false;	// this is for driving several DAOs on same node table

	/**
	 * @param treeNodeEntityClass the persistence class representing the tree, implementing ClosureTableTreeNode.
	 * 		Its simpleName will be used as table name for queries.
	 * @param treePathsEntityClass the persistence class representing ancestor-child relations, implementing TreePaths.
	 * 		Its simpleName will be used as table name for queries.
	 * @param orderIndexMatters true when position of nodes should be managed, false if position does not matter (makes operations a little faster).
	 */
	public ClosureTableTreeDao(
			Class<? extends ClosureTableTreeNode> treeNodeEntityClass,
			Class<? extends TreePath> treePathsEntityClass,
			boolean orderIndexMatters,
			DbSession session)
	{
		this(
			treeNodeEntityClass,
			treeNodeEntityClass.getSimpleName(),
			treePathsEntityClass,
			treePathsEntityClass.getSimpleName(),
			orderIndexMatters,
			session);
	}
	
	/**
	 * @param treeNodeEntityClass the persistence class representing the tree, implementing ClosureTableTreeNode.
	 * @param treeNodeEntity the JPQL entity name of the database table to be used for node queries.
	 * @param treePathEntityClass the persistence class representing ancestor-child relations, implementing TreePaths.
	 * @param treePathEntity the JPQL entity name of the database table to be used for path queries.
	 * @param orderIndexMatters true when position of nodes should be managed, false if position does not matter (makes operations a little faster).
	 */
	public ClosureTableTreeDao(
			Class<? extends ClosureTableTreeNode> treeNodeEntityClass,
			String treeNodeEntity,
			Class<? extends TreePath> treePathEntityClass,
			String treePathEntity,
			boolean orderIndexMatters,
			DbSession session)
	{
		super(session, treeNodeEntity);
		
		assert treeNodeEntityClass != null && treeNodeEntity != null && treePathEntityClass != null && treePathEntity != null;
		
		this.treeNodeEntityClass = treeNodeEntityClass;
		this.treePathEntityClass = treePathEntityClass;
		this.treePathEntity = treePathEntity;
		this.orderIndexMatters = orderIndexMatters;
	}

	
	/** @return true if nodes will removed when their paths get removed. */
	public boolean isRemoveReferencedNodes() {
		return removeReferencedNodes;
	}

	/**
	 * Set this to true when driving no more than one tree over a node table.
	 * The consequence is that nodes will be removed when their paths get removed.
	 * Default is false, to support more than one tree on the same node table.
	 */
	public void setRemoveReferencedNodes(boolean removeReferencedNodes) {
		this.removeReferencedNodes = removeReferencedNodes;
	}

	
	/**
	 * This is for the case when the provided TreePath implementation contains
	 * additional properties concerning the node. Caller is expected to cast the
	 * return to an adequate type.
	 * @param node the node for which TreePath information is requested.
	 * @return the TreePath instance that contains the node's self-reference.
	 */
	public TreePath getTreePathEntity(ClosureTableTreeNode node)	{
		StringBuilder queryText = new StringBuilder(
				"select p from "+pathEntityName()+" p where p.ancestor = ?1 and p.descendant = ?2");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		parameters.add(node);
		
		beforeFindQuery("p", queryText, parameters, true);
		
		@SuppressWarnings("unchecked")
		List<TreePath> result = (List<TreePath>) session.queryList(queryText.toString(), parameters.toArray());
		if (result.size() <= 0)
			return null;	// this is legal, not any node must be in paths

		if (result.size() > 1)
			throw new IllegalStateException("Found more than one path for node "+node+", paths are: "+result);
		
		return result.get(0);
	}
	
	
	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode find(Serializable id) {
		return (ClosureTableTreeNode) session.get(treeNodeEntityClass, id);
	}
	
	/** {@inheritDoc} */
	@Override
	public void update(ClosureTableTreeNode node) throws UniqueConstraintViolationException {
		assertUpdate(node);
		
		if (shouldCheckUniqueConstraintOnUpdate())	{
			checkUniqueConstraint(node, getRootForCheckUniqueness(node), node);
			// caller must reset the non-unique property when this fails!
		}
		
		save(node);
	}

	/** {@inheritDoc} */
	@Override
	public final boolean isRoot(ClosureTableTreeNode node) {
		if (isPersistent(node) == false)
			return false;
		
		StringBuilder queryText = new StringBuilder(
			"select count(p) from "+pathEntityName()+" p where p.descendant = ?1 and p.depth > 0");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		beforeFindQuery("p", queryText, parameters, true);
		
		return 0 == session.queryCount(queryText.toString(), parameters.toArray());	// is not a descendant to any node except itself
	}

	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode createRoot(ClosureTableTreeNode root) throws UniqueConstraintViolationException {
		return addChild(null, root);
	}
	
	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<ClosureTableTreeNode> getRoots() {
		StringBuilder queryText = new StringBuilder(
			"select p.ancestor from "+pathEntityName()+" p where p.depth = 0");	// select self-references
		List<Object> parameters = new ArrayList<Object>();
		beforeFindQuery("p", queryText, parameters, true);
		
		queryText.append(" and not exists "+	// ... any parent of it
			"(select 'x' from "+pathEntityName()+" p2 "+
			" where p2.descendant = p.descendant and p2.depth > 0)");
		
		return (List<ClosureTableTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}
	
	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public synchronized void removeAll() {
		for (TreePath path : (List<TreePath>) session.queryList("select p from "+pathEntityName()+" p", null))
			removePath(path);
		
		for (ClosureTableTreeNode node : (List<ClosureTableTreeNode>) session.queryList("select n from "+nodeEntityName()+" n", null))
			removeNode(node);
	}
	

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	public List<ClosureTableTreeNode> getTree(ClosureTableTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
			"select p.descendant from "+pathEntityName()+" p where p.ancestor = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p", queryText, parameters, true);
		return (List<ClosureTableTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}
	
	/** {@inheritDoc} */
	@Override
	public List<ClosureTableTreeNode> getTreeCacheable(ClosureTableTreeNode parent)	{
		// select only child references and parent's self-reference
		StringBuilder queryText = new StringBuilder(
			"select p from "+pathEntityName()+" p where (p.depth = 1 or (p.depth = 0 and p.ancestor = ?1))");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		
		StringBuilder subQueryText = new StringBuilder(
			"select p1.descendant from "+pathEntityName()+" p1 where p1.ancestor = ?2");	// all sub-tree paths
		parameters.add(parent);
		beforeFindQuery("p1", subQueryText, parameters, true);
		
		queryText.append(" and p.descendant in ("+subQueryText+")");
		
		@SuppressWarnings("unchecked")
		List<TreePath> breadthFirstTree = (List<TreePath>) session.queryList(
				queryText.append(" order by p.depth, p.ancestor, p.orderIndex").toString(), parameters.toArray());
		
		return newCacheableTreeList(parent, breadthFirstTree);
	}
	
	/** Factory method for new CacheableTreeList. To be overridden by temporal variant. */
	protected CacheableTreeList newCacheableTreeList(ClosureTableTreeNode parent, List<TreePath> breadthFirstTree)	{
		return new CacheableTreeList().init(parent, breadthFirstTree);
	}
	
	/** {@inheritDoc} */
	@Override
	public List<ClosureTableTreeNode> findSubTree(ClosureTableTreeNode parent, List<ClosureTableTreeNode> subNodes) {
		CacheableTreeList treeList = (CacheableTreeList) subNodes;
		return treeList.getSubTree(parent);
	}

	/** {@inheritDoc} */
	@Override
	public List<ClosureTableTreeNode> findDirectChildren(List<ClosureTableTreeNode> subNodes) {
		CacheableTreeList treeList = (CacheableTreeList) subNodes;
		return treeList.size() > 0 ? treeList.getChildren(treeList.getRoot()) : new ArrayList<ClosureTableTreeNode>();
	}

	/** {@inheritDoc} */
	@Override
	public int getChildCount(ClosureTableTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
			"select count(p) from "+pathEntityName()+" p where p.ancestor = ?1 and p.depth = 1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p", queryText, parameters, true);
		return session.queryCount(queryText.toString(), parameters.toArray());
	}
		
	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<ClosureTableTreeNode> getChildren(ClosureTableTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
			"select p.descendant from "+pathEntityName()+" p where p.ancestor = ?1 and p.depth = 1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p", queryText, parameters, true);
		return (List<ClosureTableTreeNode>) session.queryList(
				queryText.append(" order by p.orderIndex").toString(), parameters.toArray());
	}

	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode getRoot(ClosureTableTreeNode node) {
		if (node == null)
			throw new IllegalArgumentException("Can not read root for a null node!");
		
		List<ClosureTableTreeNode> path = getPath(node);
		return path.size() > 0 ? path.get(0) : node;
	}
	
	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode getParent(ClosureTableTreeNode child) {
		StringBuilder queryText = new StringBuilder(
			"select p.ancestor from "+pathEntityName()+" p where p.descendant = ?1 and p.depth = 1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(child);
		beforeFindQuery("p", queryText, parameters, true);
		@SuppressWarnings("unchecked")
		List<ClosureTableTreeNode> parents = (List<ClosureTableTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
		
		if (parents.size() == 1)
			return parents.get(0);
		
		if (parents.size() == 0)
			return null;
		
		throw new IllegalArgumentException("More than one parent found: "+parents);
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<ClosureTableTreeNode> getPath(ClosureTableTreeNode node) {
		StringBuilder queryText = new StringBuilder(
			"select p.ancestor from "+pathEntityName()+" p where p.descendant = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		beforeFindQuery("p", queryText, parameters, true);
		List<ClosureTableTreeNode> path = (List<ClosureTableTreeNode>) session.queryList(
				queryText.append(" order by p.depth desc").toString(), parameters.toArray());
				// desc: the deeper the path the higher the parent above
		
		path.remove(path.size() - 1);	// remove given child from path
		return path;
	}

	/** {@inheritDoc} */
	@Override
	public int getLevel(ClosureTableTreeNode node) {
		StringBuilder queryText = new StringBuilder(
			"select count(p) from "+pathEntityName()+" p where p.descendant = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		beforeFindQuery("p", queryText, parameters, true);
		return session.queryCount(queryText.toString(), parameters.toArray()) - 1;
	}

	/** {@inheritDoc} */
	@Override
	public int size(ClosureTableTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
			"select count(p) from "+pathEntityName()+" p where p.ancestor = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p", queryText, parameters, true);
		return session.queryCount(queryText.toString(), parameters.toArray());
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isLeaf(ClosureTableTreeNode node) {
		return getChildCount(node) <= 0;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isEqualToOrChildOf(ClosureTableTreeNode child, ClosureTableTreeNode parent) {
		if (equal(parent, child))
			return true;
		return isChildOf(child, parent);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isChildOf(ClosureTableTreeNode child, ClosureTableTreeNode parent) {
		if (equal(parent, child))
			return false;
		
		StringBuilder queryText = new StringBuilder(
			"select count(p) from "+pathEntityName()+" p where p.ancestor = ?1 and p.descendant = ?2");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		parameters.add(child);
		beforeFindQuery("p", queryText, parameters, true);
		int count = session.queryCount(queryText.toString(), parameters.toArray());
		
		if (count > 1)
			throw new IllegalStateException("Ambiguous ancestor/descendant, found "+count+" paths for parent "+parent+" and child "+child);
		
		return count == 1;
	}

	
	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode addChild(ClosureTableTreeNode parent, ClosureTableTreeNode child) throws UniqueConstraintViolationException {
		return addChildAt(parent, child, UNDEFINED_POSITION);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized ClosureTableTreeNode addChildAt(ClosureTableTreeNode parent, ClosureTableTreeNode child, int position) throws UniqueConstraintViolationException {
		return addChild(parent, null, child, position);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized ClosureTableTreeNode addChildBefore(ClosureTableTreeNode sibling, ClosureTableTreeNode child) throws UniqueConstraintViolationException {
		return addChild(null, sibling, child, UNDEFINED_POSITION);
	}
	
	
	/** {@inheritDoc} */
	@Override
	public synchronized void remove(ClosureTableTreeNode node)	{
		if (node == null || isPersistent(node) == false)
			throw new IllegalArgumentException("Node to remove is null or not persistent: "+node);
		
		removeTree(node);
	}


	/** {@inheritDoc} */
	@Override
	public void move(ClosureTableTreeNode node, ClosureTableTreeNode parent) throws UniqueConstraintViolationException {
		moveTo(node, parent, UNDEFINED_POSITION);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void moveTo(ClosureTableTreeNode node, ClosureTableTreeNode parent, int position) throws UniqueConstraintViolationException {
		move(node, parent, position, null);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void moveBefore(ClosureTableTreeNode node, ClosureTableTreeNode sibling) throws UniqueConstraintViolationException {
		move(node, null, UNDEFINED_POSITION, sibling);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void moveToBeRoot(ClosureTableTreeNode child) throws UniqueConstraintViolationException {
		move(child, null, UNDEFINED_POSITION, null);
	}

	
	
	/** {@inheritDoc} */
	@Override
	public ClosureTableTreeNode copy(ClosureTableTreeNode node, ClosureTableTreeNode parent, ClosureTableTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copyTo(node, parent, UNDEFINED_POSITION, copiedNodeTemplate);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized ClosureTableTreeNode copyTo(ClosureTableTreeNode node, ClosureTableTreeNode parent, int position, ClosureTableTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copy(node, parent, position, null, copiedNodeTemplate);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized ClosureTableTreeNode copyBefore(ClosureTableTreeNode node, ClosureTableTreeNode sibling, ClosureTableTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copy(node, null, UNDEFINED_POSITION, sibling, copiedNodeTemplate);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized ClosureTableTreeNode copyToBeRoot(ClosureTableTreeNode child, ClosureTableTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copy(child, null, UNDEFINED_POSITION, null, copiedNodeTemplate);
	}

	

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<ClosureTableTreeNode> find(ClosureTableTreeNode parent, Map<String,Object> criteria) {
		final StringBuilder queryText = new StringBuilder(
				"select n"+
				" from "+nodeEntityName()+" n, "+pathEntityName()+" p "+
				" where p.descendant = n");
		final List<Object> parameters = new ArrayList<Object>();
		
		if (parent != null)	{
			queryText.append(" and p.ancestor = ?1");
			parameters.add(parent);
		}
		queryText.append(" ");
		
		QueryBuilderUtil.appendCriteria(true, queryText, "n", parameters, criteria, true);
		
		beforeFindQuery("p", queryText, parameters, true);
		
		return (List<ClosureTableTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}
	
	
	/**
	 * Does nothing.
	 * Override to append temporal conditions. Called from all querying methods.
	 * This method is expected to first append a WHERE when whereWasAppended is false,
	 * or an AND when whereWasAppended is true.
	 */
	@SuppressWarnings("unused")
	protected void beforeFindQuery(String tableAlias, StringBuilder queryText, List<Object> parameters, boolean whereWasAppended)	{
	}

	
	
	/** Overridden to return the name of TreePaths entity. */
	@Override
	protected String pathEntityName()	{
		return treePathEntity;
	}

	/** Creates a new TreePath instance from treePathEntityClass. To be overridden for additional actions on save. */
	protected TreePath newTreePathInstance() {
		try {
			return treePathEntityClass.newInstance();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/** Saves the given path to session. To be overridden for additional actions on save. */
	protected Object save(TreePath path) {
		return session.save(path);
	}

	
	
	/**
	 * Called from remove(). To be overridden.
	 * @return true for closing a gap and reordering siblings after physical remove.
	 */
	protected boolean shouldCloseGapOnRemove() {
		return true;
	}

	/** Called from remove() after locking tree. To be overridden. */
	protected void removeTree(ClosureTableTreeNode parent) {
		final boolean closeGap = shouldCloseGapOnRemove();
		
		// read siblings for re-ordering them after removal
		final List<TreePath> pathSiblings = closeGap ? getAllTreePathSiblings(parent) : null;
		
		// remove the paths
		final Set<ClosureTableTreeNode> nodesToRemove = new HashSet<ClosureTableTreeNode>();	// collect nodes to remove
		int orderIndex = -1;	// find out the position of the removed node

		for (TreePath path : getPathsToRemove(parent))	{
			nodesToRemove.add(path.getDescendant());
			
			if (closeGap && path.getDepth() == 1 && equal(path.getDescendant(), parent))	{
				assert orderIndex == -1 : "Found second path with depth = 1 where node is descendant: "+path;
				orderIndex = path.getOrderIndex();
			}
			
			removePath(path);
		}
		
		if (closeGap)
			closeGap(pathSiblings, orderIndex);
		
		// now remove the nodes
		for (ClosureTableTreeNode nodeToRemove : nodesToRemove)	{
			removeNode(nodeToRemove);
		}
	}

	/**
	 * Called from remove() for all sub-nodes of removed tree.
	 * Deletes physically. To be overridden for historicizing paths.
	 */
	protected void removePath(TreePath path) {
		session.delete(path);
	}

	/**
	 * Called from remove() for all sub-nodes of removed tree.
	 * Deletes physically. To be overridden.
	 * Here you could additionally historicize the node,
	 * which normally is not needed because paths are historicized.
	 * When using more than one DAOs on one node table, you MUST override this
	 * to NOT delete physically, because the node could be in another DAO's tree!
	 */
	protected void removeNode(ClosureTableTreeNode nodeToRemove) {
		if (isRemoveReferencedNodes())
			session.delete(nodeToRemove);
	}

	@SuppressWarnings("unchecked")
	protected final List<? extends TreePath> getPathsToRemove(ClosureTableTreeNode node) {
		StringBuilder queryText = new StringBuilder(
			"select p from "+pathEntityName()+" p where p.descendant in ("+
				"select p1.descendant from "+pathEntityName()+" p1 where p1.ancestor = ?1");	// closing ")" see below
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		beforeFindQuery("p1", queryText, parameters, true);
		queryText.append(")");
		return (List<? extends TreePath>) session.queryList(queryText.toString(), parameters.toArray());
	}

	private ClosureTableTreeNode addChild(
			ClosureTableTreeNode parent,
			ClosureTableTreeNode sibling,
			ClosureTableTreeNode child,
			int orderIndex)
		throws UniqueConstraintViolationException
	{
		if (child == null)
			throw new IllegalArgumentException("Node to add is null!");
		
		if (isPersistent(child) && exists(child))
			throw new IllegalArgumentException("Node is already part of tree: "+child);
		
		assertInsertParameters(parent, sibling, orderIndex);
		
		checkUniqueness(Arrays.asList(new ClosureTableTreeNode [] { child }), treeActionLocation(parent, sibling, TreeActionLocation.ActionType.INSERT));
		
		final boolean relatedNodeIsParent = (parent != null);
		final List<TreePath> pathsToClone = new ArrayList<TreePath>();
		orderIndex = getPositionAndPathsToConnectSubTree(relatedNodeIsParent, orderIndex, parent, sibling, pathsToClone);
		
		if (isPersistent(child) == false)
			child = (ClosureTableTreeNode) save(child);
			// must save the node first to provide referential integrity for tree paths
		
		// now create path objects
		insertSelfReference(child);
		clonePaths(child, 0, orderIndex, pathsToClone, relatedNodeIsParent);
		
		return child;
	}

	private boolean exists(ClosureTableTreeNode node)	{
		StringBuilder queryText = new StringBuilder(
				"select count(p) from "+pathEntityName()+" p where p.descendant = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(node);
		beforeFindQuery("p", queryText, parameters, true);
			
		return 0 < session.queryCount(queryText.toString(), parameters.toArray());	// no such node exists in paths
	}
	
	/** Insert self reference with depth 0. */
	private void insertSelfReference(ClosureTableTreeNode child) {
		TreePath newPath = newTreePathInstance();
		newPath.setAncestor(child);
		newPath.setDescendant(child);
		newPath.setDepth(0);
		newPath.setOrderIndex(UNDEFINED_POSITION);
		save(newPath);
	}

	
	private void move(ClosureTableTreeNode nodeToMove, ClosureTableTreeNode newParent, int position, ClosureTableTreeNode sibling) throws UniqueConstraintViolationException {
		assertCopyOrMoveParameters(nodeToMove, newParent, position, sibling);
		
		checkUniqueness(Arrays.asList(new ClosureTableTreeNode [] { nodeToMove }), treeActionLocation(newParent, sibling, TreeActionLocation.ActionType.MOVE));
		
		disconnectSubTree(nodeToMove);
		
		if (newParent != null || sibling != null)	{	// is not a root
			final List<TreePath> childPaths = getPathsIntoSubtree(nodeToMove);
			connectSubTree(newParent, position, sibling, childPaths);
		}
	}
	
	private void disconnectSubTree(ClosureTableTreeNode node)	{
		// read paths to remove: those that end in moved tree, but do not start in it
		final String removeQueryText =
				"select p from "+pathEntityName()+" p "+
				"  where p.descendant in"+
				"    (select p1.descendant from "+pathEntityName()+" p1 where p1.ancestor = ?1)"+	// sub-tree below node
				"  and p.ancestor not in "+
				"    (select p2.descendant from "+pathEntityName()+" p2 where p2.ancestor = ?2)";
		@SuppressWarnings("unchecked")
		final List<TreePath> pathsToRemove = (List<TreePath>) session.queryList(
				removeQueryText, new Object [] { node, node });
		
		// read path siblings for re-ordering them after removal
		final List<TreePath> pathSiblings = getAllTreePathSiblings(node);
		// remove paths
		int oldPosition = -1;	// find out the position of the removed node
		
		for (TreePath path : pathsToRemove)	{	// will be an empty list when node is root
			if (path.getDepth() == 1)
				oldPosition = path.getOrderIndex();
			
			session.delete(path);	// do not call removePath() here, because this would historicize nodes!
		}
		
		closeGap(pathSiblings, oldPosition);
	}

	/** @return the paths that point from parent to any sub-node, but not the paths that go from sub-node to sub-node. */
	@SuppressWarnings("unchecked")
	private List<TreePath> getPathsIntoSubtree(ClosureTableTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
				"select p from "+pathEntityName()+" p where p.ancestor = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p", queryText, parameters, true);
		return (List<TreePath>) session.queryList(queryText.toString(), parameters.toArray());
	}
		
	
	private ClosureTableTreeNode copy(final ClosureTableTreeNode node, ClosureTableTreeNode newParent, int position, ClosureTableTreeNode sibling, ClosureTableTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		assertCopyOrMoveParameters(node, newParent, position, sibling);
		
		final List<TreePath> pathsToCopy = getSubTreePathsToCopy(node);	// read paths to copy
		final List<TreePath> childPaths = new ArrayList<TreePath>();	// will be filled after copySubTree
		final ClosureTableTreeNode copiedNode = copySubTree(
				pathsToCopy,
				node,
				copiedNodeTemplate,
				childPaths,
				treeActionLocation(newParent, sibling, TreeActionLocation.ActionType.COPY));
		
		if (newParent != null || sibling != null)	{	// is not a root
			connectSubTree(newParent, position, sibling, childPaths);
		}
		return copiedNode;
	}
	
	/** Read paths to copy: those that start AND end in moved tree, not those that only end in it. */
	@SuppressWarnings("unchecked")
	private List<TreePath> getSubTreePathsToCopy(ClosureTableTreeNode parent) {
		final StringBuilder subTreeBelowNode = new StringBuilder(	// sub-tree below node
				"select p2.descendant from "+pathEntityName()+" p2 where p2.ancestor = ?1");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent);
		beforeFindQuery("p2", subTreeBelowNode, parameters, true);	// do not copy historicized nodes
		
		final int paramCount = parameters.size();
		parameters.addAll(new ArrayList<Object>(parameters));	// as we apply copySubQueryText two times, double parameters
		
		final StringBuilder copyQueryText = new StringBuilder(
				"select p from "+pathEntityName()+" p "+
				"  where p.descendant in ("+subTreeBelowNode+")"+
				"  and p.ancestor in ("+shiftParams(subTreeBelowNode, paramCount)+")");
		return (List<TreePath>) session.queryList(copyQueryText.toString(), parameters.toArray());
	}
	
	private String shiftParams(StringBuilder queryBuffer, int shiftCount)	{
		String query = queryBuffer.toString();
		for (int i = shiftCount; i > 0; i--)
			query = query.replace("?"+i, "?"+(i + shiftCount));
		return query;
	}

	private ClosureTableTreeNode copySubTree(List<TreePath> pathsToCopy, ClosureTableTreeNode node, ClosureTableTreeNode copiedNodeTemplate, List<TreePath> childPaths, TreeActionLocation<ClosureTableTreeNode> treeActionLocation)
		throws UniqueConstraintViolationException
	{
		// clone nodes (retrieved from paths) and check their uniqueness
		final Map<Serializable,ClosureTableTreeNode> cloneMap = new Hashtable<Serializable,ClosureTableTreeNode>();
		ClosureTableTreeNode copiedNode = cloneNodesCheckUniqueness(pathsToCopy, node, copiedNodeTemplate, treeActionLocation, cloneMap);
		
		// save node clones to provide referential integrity for subsequent path clones save
		final Map<Serializable,ClosureTableTreeNode> mergedCloneMap = new Hashtable<Serializable,ClosureTableTreeNode>();
		for (Map.Entry<Serializable,ClosureTableTreeNode> e : cloneMap.entrySet())	{
			final Serializable originalId = e.getKey();
			final ClosureTableTreeNode clonedNode = e.getValue();
			
			final ClosureTableTreeNode mergedClonedNode = (ClosureTableTreeNode) save(clonedNode);	// save to database
			mergedCloneMap.put(originalId, mergedClonedNode);	// is another instance!
			
			if (clonedNode == copiedNode)	// ensure that returned node is merged one
				copiedNode = mergedClonedNode;
		}
		
		// clone and save paths to build an unconnected new sub-tree
		for (TreePath pathToCopy : pathsToCopy)	{
			final TreePath clonedPath = newTreePathInstance();
			clonedPath.setAncestor(mergedCloneMap.get(pathToCopy.getAncestor().getId()));
			clonedPath.setDescendant(mergedCloneMap.get(pathToCopy.getDescendant().getId()));
			clonedPath.setDepth(pathToCopy.getDepth());
			clonedPath.setOrderIndex(pathToCopy.getOrderIndex());
			
			final TreePath mergedClonedPath = (TreePath) save(clonedPath);
			
			// collect children to connect, which are paths that go from copied node to any sub-node, not from sub-node to sub-node
			if (equal(pathToCopy.getAncestor(), node))
				childPaths.add(mergedClonedPath);
		}
		
		return copiedNode;
	}

	private ClosureTableTreeNode cloneNodesCheckUniqueness(List<TreePath> pathsToCopy, ClosureTableTreeNode node, ClosureTableTreeNode copiedNodeTemplate, TreeActionLocation<ClosureTableTreeNode> treeActionLocation, Map<Serializable,ClosureTableTreeNode> cloneMap)
		throws UniqueConstraintViolationException
	{
		ClosureTableTreeNode copiedNode = null;
		final List<ClosureTableTreeNode> uniqueCheckList = new ArrayList<ClosureTableTreeNode>();
		
		for (TreePath pathToCopy : pathsToCopy)	{
			final ClosureTableTreeNode nodeToCopy = pathToCopy.getDescendant();
			
			if (cloneMap.containsKey(nodeToCopy.getId()) == false)	{
				final ClosureTableTreeNode clone;
				if (copiedNode == null && equal(nodeToCopy, node))	{	// topmost of copied nodes
					clone = (copiedNodeTemplate != null) ? copiedNodeTemplate : nodeToCopy.clone();
					copiedNode = clone;
					uniqueCheckList.add(0, clone);	// topmost MUST be first in List for checkUniqueness() to succeed!
				}
				else	{
					clone = nodeToCopy.clone();
					uniqueCheckList.add(clone);
				}
				assert clone != null : "Need clone() to copy a node!";
				cloneMap.put(nodeToCopy.getId(), clone);
				applyCopiedNodeRenamer(clone);
			}
		}
		assert copiedNode != null : "The node to copy was not in paths: "+pathsToCopy;
		
		checkUniqueness(uniqueCheckList, treeActionLocation);
		return copiedNode;
	}
	

	private void connectSubTree(ClosureTableTreeNode newParent, int position, ClosureTableTreeNode sibling, final List<TreePath> childPaths) {
		final boolean relatedNodeIsParent = (newParent != null);
		final List<TreePath> pathsToClone = new ArrayList<TreePath>();
		position = getPositionAndPathsToConnectSubTree(relatedNodeIsParent, position, newParent, sibling, pathsToClone);
		
		for (TreePath childPath : childPaths)
			clonePaths(childPath.getDescendant(), childPath.getDepth(), position, pathsToClone, relatedNodeIsParent);
	}

	@SuppressWarnings("unchecked")
	private int getPositionAndPathsToConnectSubTree(boolean relatedNodeIsParent, int position, ClosureTableTreeNode parent, ClosureTableTreeNode sibling, List<TreePath> pathsToClone) {
		assert pathsToClone != null && pathsToClone.size() <= 0;	// must be empty list
		assert relatedNodeIsParent == false || parent != null;
		
		// read paths to clone, also historicized ones
		final String queryText = "select p from "+pathEntityName()+" p where p.descendant = ?1";
		if (relatedNodeIsParent)	{
			pathsToClone.addAll((List<TreePath>) session.queryList(
				queryText, new Object [] { parent }));
		}
		else if (sibling != null)	{
			pathsToClone.addAll((List<TreePath>) session.queryList(
				queryText+" and p.depth > 0 order by p.depth", new Object [] { sibling }));
				// depth > 0: exclude sibling's self-reference
			
			if (pathsToClone.size() <= 0)
				throw new IllegalArgumentException("Sibling seems not to be a child but a root: "+sibling);
			
			// order by depth, so sibling is first in list
			position = pathsToClone.get(0).getOrderIndex();
			assert position >= 0 : "Position of first path is not valid: "+pathsToClone;
		}
		// else: root creation, nothing to do
		
		return position;
	}

	/** Clone all given parent paths, but pointing to new node as descendant. */
	private void clonePaths(ClosureTableTreeNode child, int addToDepth, int position, List<TreePath> pathsToClone, boolean isParentPaths) {
		for (TreePath pathToClone : pathsToClone)	{
			TreePath newPath = newTreePathInstance();
			newPath.setAncestor(pathToClone.getAncestor());
			newPath.setDescendant(child);
			newPath.setDepth(pathToClone.getDepth() + addToDepth + (isParentPaths ? 1 : 0));
			
			int newPosition = (newPath.getDepth() == 1)	// direct child, must shift other positions
					? createGap(isParentPaths ? pathToClone.getDescendant() : pathToClone.getAncestor(), position)
					: UNDEFINED_POSITION;
			newPath.setOrderIndex(newPosition);
			
			save(newPath);
		}
	}

	/** Persistently increments all positions greater or equal to the given one. */
	private int createGap(ClosureTableTreeNode parent, int position) {
		if (orderIndexMatters == false)
			return 0;
		
		final List<TreePath> children = getAllDirectTreePathChildren(parent);
		
		if (position == UNDEFINED_POSITION)	// append to end
			return children.size();	// nothing to do
		
		// skip positions of follower child nodes
		for (int i = children.size() - 1; i >= position; i--)	{
			final TreePath treePath = children.get(i);
			treePath.setOrderIndex(i + 1);
			save(treePath);
		}
		return position;
	}

	/** Re-order siblings of a removed sub-tree. The sibling list does not contain removed node. */
	private void closeGap(final List<TreePath> siblings, int removedPosition) {
		if (orderIndexMatters == false)
			return;
		
		if (removedPosition >= 0)	{	// for roots this would be -1
			for (int i = removedPosition; i < siblings.size(); i++)	{
				TreePath pathSibling = siblings.get(i);
				pathSibling.setOrderIndex(i);
				save(pathSibling);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private List<TreePath> getAllDirectTreePathChildren(ClosureTableTreeNode parent) {
		String queryText =
				"select p from "+pathEntityName()+" p where p.ancestor = ?1 and p.depth = 1 order by p.orderIndex";
		return (List<TreePath>) session.queryList(queryText, new Object [] { parent });
	}
	
	/** @return the siblings, exclusive the node itself. */
	@SuppressWarnings("unchecked")
	private List<TreePath> getAllTreePathSiblings(ClosureTableTreeNode node) {
		String queryText = 
				"select p from "+pathEntityName()+" p where p.depth = 1 and p.descendant != ?1 and p.ancestor in "+
				"    (select p2.ancestor from "+pathEntityName()+" p2 where p2.descendant = ?2 and p2.depth = 1)"+
				"  order by p.orderIndex";
		return (List<TreePath>) session.queryList(queryText, new Object [] { node, node });
	}
		
	private TreeActionLocation<ClosureTableTreeNode> treeActionLocation(ClosureTableTreeNode parent, ClosureTableTreeNode sibling, TreeActionLocation.ActionType actionType) {
		final TreeActionLocation.RelatedNodeType relatedNodeType = (sibling != null) ? TreeActionLocation.RelatedNodeType.SIBLING : parent != null ? TreeActionLocation.RelatedNodeType.PARENT : null;
		final ClosureTableTreeNode relatedNode = (sibling != null) ? sibling : parent != null ? parent : null;
		return new TreeActionLocation<ClosureTableTreeNode>(getRootForCheckUniqueness(relatedNode), relatedNodeType, relatedNode, actionType);
	}

	/** @return root of given node when UniqueTreeConstraint exist, else null. */
	private ClosureTableTreeNode getRootForCheckUniqueness(ClosureTableTreeNode node) {
		return (getUniqueTreeConstraint() != null && node != null && isPersistent(node))
			? getRoot(node)
			: null;
	}
	
	

	private void assertInsertParameters(ClosureTableTreeNode parent, ClosureTableTreeNode sibling, int position) {
		assert (parent == null || sibling == null);
		assert (sibling == null || position == UNDEFINED_POSITION);
	}

	private void assertCopyOrMoveParameters(ClosureTableTreeNode node, ClosureTableTreeNode newParent, int position, ClosureTableTreeNode sibling) {
		if (node == null || isPersistent(node) == false)
			throw new IllegalArgumentException("Node to move is null or not persistent: "+node);
		
		assertInsertParameters(newParent, sibling, position);
		
		copyOrMovePreconditions(newParent != null ? newParent : sibling, node);
	}

}
