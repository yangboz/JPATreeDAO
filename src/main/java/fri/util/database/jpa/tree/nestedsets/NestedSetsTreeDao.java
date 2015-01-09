package fri.util.database.jpa.tree.nestedsets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.AbstractTreeDao;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Data-access-object for a hierarchical representation of records (nodes),
 * using one database table, having no parent reference in children.
 * A nested-sets tree will always maintain child positions, meaning children
 * lists have a defined order which can only be changed by a move().
 * <p/>
 * A nested-sets tree maintains its structure by "left" and "right" indexes on every node.
 * The left index represents the depth-first traversal order.
 * The right index is (left + 1) for a leaf child, for a folder it is
 * (right + 1) of the child it follows according to depth-first traversal.
 * For a root, left index is 1, right index is (number of nodes * 2).
 * See links in TreeDao for more information.
 * <p/>
 * Note: as the temporal derivation obtains a state, all write-methods here are synchronized.
 * 
 * @see fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode
 * @see fri.util.database.jpa.tree.TreeDao
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public class NestedSetsTreeDao extends AbstractTreeDao<NestedSetsTreeNode>
{
	/** The "left" order number of any root. */
	private static final int ROOT_LEFT = 1;
	
	/** JPA class of the database table that represents the NestedSetsTree. */
	private final Class<? extends NestedSetsTreeNode> nestedSetsTreeEntityClass;
	
	
	/**
	 * @param entityClass the persistence class representing the tree, implementing NestedSetsTreeNode.
	 * @param dbSession the database layer abstraction to be used for persistence actions.
	 * Its simpleName will be used as table name for queries.
	 */
	public NestedSetsTreeDao(
			Class<? extends NestedSetsTreeNode> entityClass,
			DbSession session)
	{
		this(entityClass, entityClass.getSimpleName(), session);
	}
	
	/**
	 * @param entityClass the persistence class representing the tree table, implementing NestedSetsTreeNode.
	 * @param entityName the JPQL entity name of the database table to be used for queries, normally entityClass.getSimpleName().
	 * @param dbSession the database layer abstraction to be used for persistence actions.
	 */
	public NestedSetsTreeDao(
			Class<? extends NestedSetsTreeNode> entityClass,
			String entityName,
			DbSession session)
	{
		super(session, entityName);
		
		assert entityClass != null && entityName != null;
		
		this.nestedSetsTreeEntityClass = entityClass;
	}
	
	
	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode find(Serializable id) {
		return (NestedSetsTreeNode) session.get(nestedSetsTreeEntityClass, id);
	}
	
	/** {@inheritDoc} */
	@Override
	public void update(NestedSetsTreeNode entity) throws UniqueConstraintViolationException	{
		assertUpdate(entity);
		
		if (shouldCheckUniqueConstraintOnUpdate())	{
			Location location = new Location(entity.getTopLevel(), null, entity, TreeActionLocation.ActionType.UPDATE, -1);
			checkUniqueness(Arrays.asList(new NestedSetsTreeNode [] { entity }), location);
			// caller must reset the non-unique property when this fails!
		}
		
		save(entity);
	}

	/** {@inheritDoc} */
	@Override
	public final boolean isRoot(NestedSetsTreeNode entity) {
		return isPersistent(entity) && equal(entity.getTopLevel(), entity);
	}

	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode createRoot(NestedSetsTreeNode root) throws UniqueConstraintViolationException {
		if (isPersistent(root))
			throw new IllegalArgumentException("Node is already persistent and part of a tree, use moveToBeRoot() or copyToBeRoot() for "+root);

		root.setLeft(ROOT_LEFT);
		root.setRight(ROOT_LEFT + 1);
		root.setTopLevel(root);
		
		Location location = new Location(null, TreeActionLocation.RelatedNodeType.PARENT, null, TreeActionLocation.ActionType.INSERT, ROOT_LEFT);
		checkUniqueness(Arrays.asList(new NestedSetsTreeNode [] { root }), location);
		
		return (NestedSetsTreeNode) save(root);
	}
	
	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<NestedSetsTreeNode> getRoots()	{
		StringBuilder queryText = new StringBuilder(
				"select t from "+nodeEntityName()+" t "+
				"where t.topLevel = t");
		List<Object> parameters = new ArrayList<Object>();
		beforeFindQuery("t", queryText, parameters, true);
		return (List<NestedSetsTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void removeAll() {
		session.executeUpdate("update "+nodeEntityName()+" t set t.topLevel = null", null);
		// else roots would not be removable (under MySQL) because they have a self-reference
		session.executeUpdate("delete from "+nodeEntityName(), null);
	}
	
	/** {@inheritDoc} */
    @Override
	public int size(NestedSetsTreeNode entity)        {
        return numberOfNodesInSubTree(entity);
    }
    
	/** {@inheritDoc} */
	@Override
	public List<NestedSetsTreeNode> getTree(NestedSetsTreeNode parent) {
		if (isRoot(parent))
			return getRootTreeDepthFirst(parent);
		return getSubTreeDepthFirst(parent);
	}

	/** {@inheritDoc} */
	@Override
	public List<NestedSetsTreeNode> getTreeCacheable(NestedSetsTreeNode parent) {
		return Collections.unmodifiableList(getTree(parent));
	}

	/** {@inheritDoc} */
	@Override
	public boolean isLeaf(NestedSetsTreeNode node)	{
		return node.getLeft() + 1 == node.getRight();
	}
	
	/** {@inheritDoc} */
	@Override
	public int getChildCount(NestedSetsTreeNode parent) {
		return getChildren(parent).size();
	}
	
	/** {@inheritDoc} */
	@Override
	public List<NestedSetsTreeNode> getChildren(NestedSetsTreeNode parent) {
		List<NestedSetsTreeNode> subTree = getSubTreeDepthFirst(parent);	// refreshes parent
		return findDirectChildren(subTree);
	}

	/**
	 * Finds a children list from a predefined list of nodes under a parent which is first in list.
	 * This might help analyzing and caching tree lists.
	 * @return a list of direct children of the the parent node that is first in list.
	 */
	@Override
	public List<NestedSetsTreeNode> findDirectChildren(List<NestedSetsTreeNode> subNodes) {
		List<NestedSetsTreeNode> children = new ArrayList<NestedSetsTreeNode>();
		
		// check if there are sub-nodes
		final int size = subNodes.size();
		if (size <= 1)	// only parent is present
			return Collections.unmodifiableList(children);
		
		// remove parent from child candidates
		NestedSetsTreeNode parent = subNodes.get(0);
		subNodes = subNodes.subList(1, size);
		
		int nextChildLeft = parent.getLeft() + 1;	// this is 'left' of first child
		int currentChildRight = subNodes.get(0).getRight();
		int i = 0;
		for (NestedSetsTreeNode node : subNodes)	{
			if (isNextChild(nextChildLeft, node, currentChildRight, subNodes, i))	{
				if (isValidFilterChild(node))	{
					children.add(node);
					currentChildRight = node.getRight();
				}
				nextChildLeft = node.getRight() + 1;	// calculate 'left' of next child
			}
			i++;
		}
		return Collections.unmodifiableList(children);
	}

	/** @return true if node-left is nextChildLeft. Override this when left-indexes have gaps. */
	@SuppressWarnings("unused")
	protected boolean isNextChild(int nextChildLeft, NestedSetsTreeNode node, int currentChildRight, List<NestedSetsTreeNode> subNodes, int currentIndex) {
		return node.getLeft() == nextChildLeft;
	}

	/**
	 * Finds a sub-tree list from a predefined list of nodes under a parent.
	 * This might help analyzing and caching tree lists.
	 * @param parent the parent node to search a sub-tree for, contained somewhere in the given list of nodes.
	 * @param tree a list of nodes from which to extract a sub-tree, containing the given parent.
	 * @return a list of nodes under the passed parent node.
	 */
	@Override
	public final List<NestedSetsTreeNode> findSubTree(NestedSetsTreeNode parent, List<NestedSetsTreeNode> tree) {
		List<NestedSetsTreeNode> subTree = new ArrayList<NestedSetsTreeNode>();
		for (NestedSetsTreeNode node : tree)	{
			if (node.getLeft() >= parent.getLeft() && node.getRight() <= parent.getRight())	{
				if (isValidFilterChild(node))
					subTree.add(node);
			}
		}
		return Collections.unmodifiableList(subTree);
	}

	
	/**
	 * Called by filterChildren() from getChildren().
	 * Always returns true because this is not a temporal implementation.
	 * To be overridden.
	 */
	@SuppressWarnings("unused")
	protected boolean isValidFilterChild(NestedSetsTreeNode entity) {
		return true;
	}

	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode getRoot(NestedSetsTreeNode node) {
		return isPersistent(node) ? node.getTopLevel() : null;
	}
	
	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode getParent(NestedSetsTreeNode node) {
		List<NestedSetsTreeNode> path = getPath(node);
		int size = path.size();
		return size <= 0 ? null : path.get(size - 1);	// last in path will be the direct parent of child
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<NestedSetsTreeNode> getPath(NestedSetsTreeNode node) {
		if (node.getTopLevel() == null || isRoot(node))
			return new ArrayList<NestedSetsTreeNode>();	// not yet in tree, or is root
		
		return (List<NestedSetsTreeNode>) session.queryList(
				pathQuery("select parent", "order by parent.lft"),
				new Object [] { node.getTopLevel(), node });
	}

	/** {@inheritDoc} */
	@Override
	public int getLevel(NestedSetsTreeNode node) {
		return session.queryCount(
				pathQuery("select count(parent)", ""),
				new Object [] { node.getTopLevel(), node });
	}
	
	/** {@inheritDoc} */
	@Override
	public boolean isEqualToOrChildOf(NestedSetsTreeNode child, NestedSetsTreeNode parent)	{
		if (isChildOf(child, parent))
			return true;
        return equal(parent, child);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isChildOf(NestedSetsTreeNode child, NestedSetsTreeNode parent)	{
		if (child.getTopLevel() == null || parent.getTopLevel() == null || equal(parent.getTopLevel(), child.getTopLevel()) == false)
			return false;	// not yet in tree, or in different trees
        return parent.getLeft() < child.getLeft() && parent.getRight() > child.getRight();
	}


	
	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode addChild(NestedSetsTreeNode parent, NestedSetsTreeNode child) throws UniqueConstraintViolationException {
		return addChildAt(parent, child, UNDEFINED_POSITION);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized NestedSetsTreeNode addChildAt(NestedSetsTreeNode parent, NestedSetsTreeNode child, int position) throws UniqueConstraintViolationException {
		Location location = location(parent, position, null, false);
		return addChild(location, child);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized NestedSetsTreeNode addChildBefore(NestedSetsTreeNode sibling, NestedSetsTreeNode child) throws UniqueConstraintViolationException {
		Location location = new Location(sibling.getTopLevel(), TreeActionLocation.RelatedNodeType.SIBLING, sibling, TreeActionLocation.ActionType.INSERT, sibling.getLeft());
		return addChild(location, child);
	}
	
	
	/** {@inheritDoc} */
	@Override
	public synchronized void remove(NestedSetsTreeNode node) {
		if (node == null || isPersistent(node) == false)
			throw new IllegalArgumentException("Node is null or not persistent: "+node);

		remove(node, numberOfNodesInSubTree(node));
	}

	
	/** {@inheritDoc} */
	@Override
	public void move(NestedSetsTreeNode node, NestedSetsTreeNode newParent) throws UniqueConstraintViolationException {
		moveTo(node, newParent, UNDEFINED_POSITION);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void moveTo(NestedSetsTreeNode node, NestedSetsTreeNode parent, int position) throws UniqueConstraintViolationException {
		Location location = location(parent, position, node, false);
		move(location, node);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void moveBefore(NestedSetsTreeNode node, NestedSetsTreeNode sibling) throws UniqueConstraintViolationException {
		Location location = new Location(sibling.getTopLevel(), TreeActionLocation.RelatedNodeType.SIBLING, sibling, TreeActionLocation.ActionType.MOVE, sibling.getLeft());
		move(location, node);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void moveToBeRoot(NestedSetsTreeNode child) throws UniqueConstraintViolationException {
		if (isRoot(child))
			return;
			
		copyOrMoveToBeRoot(child, false, null);
	}



	/** {@inheritDoc} */
	@Override
	public NestedSetsTreeNode copy(NestedSetsTreeNode node, NestedSetsTreeNode parent, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copyTo(node, parent, UNDEFINED_POSITION, copiedNodeTemplate);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized NestedSetsTreeNode copyTo(NestedSetsTreeNode node, NestedSetsTreeNode parent, int position, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		Location location = location(parent, position, node, true);
		return copy(location, node, copiedNodeTemplate);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized NestedSetsTreeNode copyBefore(NestedSetsTreeNode node, NestedSetsTreeNode sibling, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		Location location = new Location(sibling.getTopLevel(), TreeActionLocation.RelatedNodeType.SIBLING, sibling, TreeActionLocation.ActionType.COPY, sibling.getLeft());
		return copy(location, node, copiedNodeTemplate);
	}

	/** {@inheritDoc} */
	@Override
	public synchronized NestedSetsTreeNode copyToBeRoot(NestedSetsTreeNode child, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		return copyOrMoveToBeRoot(child, true, copiedNodeTemplate);
	}


	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public List<NestedSetsTreeNode> find(final NestedSetsTreeNode parent, Map<String,Object> criteria)	{
		StringBuilder queryText = new StringBuilder("select t from "+nodeEntityName()+" t ");
		List<Object> parameters = new ArrayList<Object>();
		boolean whereAppended = false;
		
		if (parent != null)	{
			queryText.append("where t.topLevel = ?1 ");
			parameters.add(parent);
			whereAppended = true;
		}
		
		whereAppended = QueryBuilderUtil.appendCriteria(true, queryText, "t", parameters, criteria, whereAppended);
		
		beforeFindQuery("t", queryText, parameters, whereAppended);
		
		return (List<NestedSetsTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}

	
	/**
	 * Does nothing.
	 * Override to append temporal conditions. Called from all querying methods.
	 * This method is expected to first append a WHERE when whereWasAppended is false,
	 * or an AND when whereWasAppended is true.
	 */
	@SuppressWarnings("unused")
	protected void beforeFindQuery(String tableAlias, StringBuilder queryText, List<Object> parameters, boolean whereWasAppended) {
	}


	
	/** Removes given children. To be overridden by subclasses. */
	protected void remove(NestedSetsTreeNode node, final int removedNodesCount) {
		final NestedSetsTreeNode topLevel = node.getTopLevel();
		final int left = node.getLeft();
		final int right = node.getRight();
		if (isRoot(node))	{	// must set topLevel to null on all tree members, else referential integrity violation on some databases (MySQL)
			session.executeUpdate(
					"update "+nodeEntityName()+" t "+
					"set t.topLevel = null "+
					"where t.topLevel = ?1",
					new Object [] { topLevel });
		}
		
		session.executeUpdate(
				"delete from "+nodeEntityName()+" t "+
				"where (t.topLevel is null or t.topLevel = ?1) and t.lft >= ?2 and t.rgt <= ?3",
				new Object [] { topLevel, left, right });
		
		closeGap(left, right, topLevel, removedNodesCount);
	}

	/** @return children list of given parent, called from insertionParameters(), to be overridden by subclasses. */
	protected List<NestedSetsTreeNode> getChildListForInsertion(NestedSetsTreeNode parent)	{
		return getChildren(parent);
	}
	

	
	private NestedSetsTreeNode copyOrMoveToBeRoot(NestedSetsTreeNode child, boolean isCopy, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		if (isPersistent(child) == false)
			throw new IllegalArgumentException("Node is not member of a tree: "+child);
		
		Location location = new Location(
				null,
				TreeActionLocation.RelatedNodeType.PARENT,
				null,
				isCopy ? TreeActionLocation.ActionType.COPY : TreeActionLocation.ActionType.MOVE,
				ROOT_LEFT);
		if (isCopy)
			return copy(location, child, copiedNodeTemplate);
		else
			move(location, child);

		return child;
	}

	@SuppressWarnings("unchecked")
	private List<NestedSetsTreeNode> getRootTreeDepthFirst(NestedSetsTreeNode root) {
		StringBuilder queryText = new StringBuilder(
				"select t from "+nodeEntityName()+" t where t.topLevel = ?1 ");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(root.getTopLevel());
		beforeFindQuery("t", queryText, parameters, true);
		queryText.append(" order by t.lft");
		return (List<NestedSetsTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}

	@SuppressWarnings("unchecked")
	private List<NestedSetsTreeNode> getSubTreeDepthFirst(NestedSetsTreeNode parent) {
		StringBuilder queryText = new StringBuilder(
				"select t from "+nodeEntityName()+" t where t.topLevel = ?1 and t.lft >= ?2 and t.rgt <= ?3 ");
		List<Object> parameters = new ArrayList<Object>();
		parameters.add(parent.getTopLevel());
		parameters.add(Integer.valueOf(parent.getLeft()));
		parameters.add(Integer.valueOf(parent.getRight()));
		beforeFindQuery("t", queryText, parameters, true);
		queryText.append(" order by t.lft");
		return (List<NestedSetsTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
	}
	
	private Location location(NestedSetsTreeNode parent, int position, NestedSetsTreeNode movingOrCopiedNode, boolean isCopy)	{
		final TreeActionLocation.ActionType actionType = isCopy
				? TreeActionLocation.ActionType.COPY
				: movingOrCopiedNode != null
					? TreeActionLocation.ActionType.MOVE
					: TreeActionLocation.ActionType.INSERT;
	
		if (isLeaf(parent))	// empty parent
			return new Location(parent.getTopLevel(), TreeActionLocation.RelatedNodeType.PARENT, parent, actionType, parent.getLeft() + 1);	// there could be historicized siblings, insert at head
		
		if (position <= UNDEFINED_POSITION)	// position is UNDEFINED_POSITION, do append
			return new Location(parent.getTopLevel(), TreeActionLocation.RelatedNodeType.PARENT, parent, actionType, parent.getRight());
		
		List<NestedSetsTreeNode> children = getChildListForInsertion(parent);
		NestedSetsTreeNode sibling = (position < children.size()) ? children.get(position) : null;
		if (sibling == null)	// append to end when position is beyond length
			return new Location(parent.getTopLevel(), TreeActionLocation.RelatedNodeType.PARENT, parent, actionType, parent.getRight());

		if (movingOrCopiedNode != null)	{
			// in case of move find out if moving node already is in children list, and is before target position
			int movingChildPosition = children.indexOf(movingOrCopiedNode);
			if (movingChildPosition >= 0 && movingChildPosition < position)	{
				// is already in target children list, and is before target position
				if (position + 1 < children.size())	// when possible, skip to next sibling, as list will skip after removal of child
					sibling = children.get(position + 1);
				else	// append to end
					return new Location(parent.getTopLevel(), TreeActionLocation.RelatedNodeType.PARENT, parent, actionType, parent.getRight());
			}
		}
		
		return new Location(sibling.getTopLevel(), TreeActionLocation.RelatedNodeType.SIBLING, sibling, actionType, sibling.getLeft());
	}
	
	/**
	 * @param relatedNode can be parent or sibling, will be used to retrieve topLevel. Will be refreshed after this call.
	 * @param targetLeft the left order number the new node will obtain, right must be left + 1.
	 * @param child the node to insert.
	 * @throws UniqueConstraintViolationException when uniqueness would be violated.
	 */
	private NestedSetsTreeNode addChild(Location location, NestedSetsTreeNode child) throws UniqueConstraintViolationException	{
		if (isPersistent(child))
			throw new IllegalArgumentException("Node is already persistent, can not be added as child: "+child);
		
		final NestedSetsTreeNode topLevel = location.root;
		child.setTopLevel(topLevel);
		child.setLeft(location.targetLeft);
		child.setRight(location.targetLeft + 1);
		
		checkUniqueness(Arrays.asList(new NestedSetsTreeNode [] { child }), location);	// check this BEFORE creating gap
		
		createGap(location.targetLeft, topLevel, 1);	// ... for adding one leaf node
		
		return (NestedSetsTreeNode) save(child);
	}

	@Override
	protected void copyOrMovePreconditions(NestedSetsTreeNode relativeNode, NestedSetsTreeNode nodeToMove) {
		super.copyOrMovePreconditions(relativeNode, nodeToMove);
		
		if (relativeNode != null && relativeNode.getTopLevel() == null || nodeToMove.getTopLevel() == null)
			throw new IllegalArgumentException("Node not in tree, or has no root!");
	}

	
	private void move(Location location, NestedSetsTreeNode nodeToMove) throws UniqueConstraintViolationException {
		copyOrMovePreconditions(location.relatedNode, nodeToMove);
		
		if (location.relatedNodeType == TreeActionLocation.RelatedNodeType.SIBLING && equal(nodeToMove, location.relatedNode))
			return;	// is already there, nothing to do

		final NestedSetsTreeNode targetTopLevel = (location.root != null) ? location.root : nodeToMove;
		final NestedSetsTreeNode sourceTopLevel = nodeToMove.getTopLevel();
		final boolean isMoveInSameTree = (location.root != null && equal(sourceTopLevel, targetTopLevel));
		
		checkUniqueness(Arrays.asList(new NestedSetsTreeNode [] { nodeToMove }), location);	// check this BEFORE creating gap
		
		int sourceLeft = nodeToMove.getLeft();
		int sourceRight = nodeToMove.getRight();
		final int movedNodesCount = numberOfNodesInSubTree(nodeToMove);
		
		// create a gap at move target
		if (location.targetLeft > ROOT_LEFT)
			createGap(location.targetLeft, targetTopLevel, movedNodesCount);
		// else: will be a root
		
		final int movedNodesCountRange = movedNodesCount * 2;
		int distance = location.targetLeft - sourceLeft;
		if (isMoveInSameTree && distance < 0)	{	// source has been moved downwards when creating gap, correct distance
			distance = distance - movedNodesCountRange;
			sourceLeft += movedNodesCountRange;
			sourceRight += movedNodesCountRange;
		}

		// as this update ignores the JPA layer, we must refresh affected nodes after update
		final String selectWhere = "t.topLevel = ?1 and t.lft >= ?2 and t.rgt <= ?3";
		final Object [] selectParams = new Object [] { sourceTopLevel, sourceLeft, sourceRight };
		
		final String updateWhere = selectWhere.replace("?1", "?4").replace("?2", "?5").replace("?3", "?6");
		final Object [] updateParamsFirstPart = new Object [] { distance, distance, targetTopLevel };
		final Object [] updateParams = new Object[updateParamsFirstPart.length + selectParams.length];
		System.arraycopy(updateParamsFirstPart, 0, updateParams, 0, updateParamsFirstPart.length);
		System.arraycopy(selectParams, 0, updateParams, updateParamsFirstPart.length, selectParams.length);
		
		// read affected nodes BEFORE update
		final List<?> nodesToRefresh = session.queryList(
				"select t from "+nodeEntityName()+" t where "+selectWhere, selectParams);
		
		// move the tree to gap
		session.executeUpdate(
				"update "+nodeEntityName()+" t "+
					"set t.lft = t.lft + ?1, t.rgt = t.rgt + ?2, t.topLevel = ?3 "+
					"where "+updateWhere,
				updateParams);
		
		refresh(nodesToRefresh);
		
		// close the gap where tree has been
		int gapLeft = sourceLeft + movedNodesCountRange;
		closeGap(gapLeft, gapLeft, sourceTopLevel, movedNodesCount);
	}


	private NestedSetsTreeNode copy(Location location, NestedSetsTreeNode nodeToCopy, NestedSetsTreeNode copiedNodeTemplate) throws UniqueConstraintViolationException {
		copyOrMovePreconditions(location.relatedNode, nodeToCopy);
		
		final List<NestedSetsTreeNode> treeToCopy = getSubTreeDepthFirstForCopy(nodeToCopy);
		final NestedSetsTreeNode targetTopLevel = (location.root != null) ? location.root : nodeToCopy;
		final int distance = location.targetLeft - nodeToCopy.getLeft();
		
		// clone tree BEFORE left/right gets updated
		NestedSetsTreeNode copiedNode = null;
		final List<NestedSetsTreeNode> clonedTree = new ArrayList<NestedSetsTreeNode>();
		for (NestedSetsTreeNode node : treeToCopy)	{
			NestedSetsTreeNode clone = (copiedNode == null && copiedNodeTemplate != null) ? copiedNodeTemplate : node.clone();
			assert clone != null : "Need clone() to copy a node!";
			
			if (copiedNode == null)	// copiedNode == null: first is topmost of the copied hierarchy
				copiedNode = clone;
			
			applyCopiedNodeRenamer(clone);
			
			clone.setLeft(node.getLeft() + distance);
			clone.setRight(node.getRight() + distance);
			clone.setTopLevel(location.root != null ? targetTopLevel : copiedNode);
			clonedTree.add(clone);
		}

		checkUniqueness(clonedTree, location);	// check BEFORE creating a gap
		
		// create a gap
		final int copiedNodesCount = numberOfNodesInSubTree(nodeToCopy);
		if (location.targetLeft > ROOT_LEFT)
			createGap(location.targetLeft, targetTopLevel, copiedNodesCount);
		// else: will be a root
		
		// copy the tree to gap
		final NestedSetsTreeNode unmergedCopiedNode = copiedNode;
		for (NestedSetsTreeNode clone : clonedTree)	{
			final NestedSetsTreeNode mergedClone = (NestedSetsTreeNode) save(clone);
			
			if (clone == copiedNode)	// merged entity is other instance
				copiedNode = mergedClone;	// so keep return value a managed instance
			
			if (clone.getTopLevel() == unmergedCopiedNode)	// avoid unmanaged entity exception, no need to save() once more
				mergedClone.setTopLevel(copiedNode);	// copiedNode must be first in list for this to work!
		}
		
		return copiedNode;
	}
	
	/** @return the tree under given node when copying, to be overridden. */
	protected List<NestedSetsTreeNode> getSubTreeDepthFirstForCopy(NestedSetsTreeNode nodeToCopy) {
		return getSubTreeDepthFirst(nodeToCopy);
	}

	private void createGap(int gapLeft, NestedSetsTreeNode topLevel, int nodesCount) {
		gap("+", gapLeft, gapLeft, topLevel, nodesCount);
	}

	private void closeGap(int gapLeft, int gapRight, NestedSetsTreeNode topLevel, int nodesCount) {
		gap("-", gapLeft, gapRight, topLevel, nodesCount);
	}
	
	private void gap(String operator, int gapLeft, int gapRight, NestedSetsTreeNode topLevel, int nodesCount) {
		final int nodesCountRange = nodesCount * 2;
		
		final Object [] paramsLeft  = new Object [] { nodesCountRange, topLevel, gapLeft };
		final String whereLeft  = "t.topLevel = ?2 and t.lft >= ?3";
		final Object [] paramsRight = new Object [] { nodesCountRange, topLevel, gapRight };
		final String whereRight = "t.topLevel = ?2 and t.rgt >= ?3";

		// as this update ignores the JPA layer, we must refresh affected nodes after update
		final String refreshWhere = 
			whereLeft.replace("?2", "?1").replace("?3", "?2") +
			" or "+
			whereRight.replace("?3", "?4").replace("?2", "?3");
		
		final Object [] refreshParams = new Object [] { topLevel, gapLeft, topLevel, gapRight };
		
		// read affected nodes BEFORE update
		final List<?> nodesToRefresh = session.queryList(
				"select t from "+nodeEntityName()+" t where "+refreshWhere, refreshParams);
		
		session.executeUpdate(
				"update "+nodeEntityName()+" t "+
				"set t.lft = t.lft "+operator+" ?1 "+
				"where "+whereLeft,
				paramsLeft);
		
		session.executeUpdate(
				"update "+nodeEntityName()+" t "+
				"set t.rgt = t.rgt "+operator+" ?1 "+
				"where "+whereRight,
				paramsRight);
		
		refresh(nodesToRefresh);
	}

	private String pathQuery(String selectWhat, String orderBy) {
		return 
			selectWhat+	// is "select parent" or "select count(parent)"
			" from "+nodeEntityName()+" parent, "+nodeEntityName()+" child "+
			" where parent.topLevel = ?1 and child.topLevel = parent.topLevel and "+
			"       child = ?2 and child.lft > parent.lft and child.rgt < parent.rgt "+
			orderBy;	// is optional, needed only for ordered path list
	}

    private int numberOfNodesInSubTree(NestedSetsTreeNode entity)     {
        return (entity.getRight() - entity.getLeft()) / 2 + 1;
    }



	/** Holds all information about a pending insert or update. */
	private static class Location extends TreeActionLocation<NestedSetsTreeNode>
	{
		/** The aimed nested-sets-tree "left" order number of the modification node. */
		public final int targetLeft;
		
		private Location(
				NestedSetsTreeNode root,
				TreeActionLocation.RelatedNodeType relatedNodeType,
				NestedSetsTreeNode relatedNode,
				TreeActionLocation.ActionType actionType,
				int targetLeft)
		{
			super(root, relatedNodeType, relatedNode, actionType);
			this.targetLeft = targetLeft;
		}
	}

}
