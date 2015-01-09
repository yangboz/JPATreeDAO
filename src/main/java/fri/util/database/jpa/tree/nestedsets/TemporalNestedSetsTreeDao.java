package fri.util.database.jpa.tree.nestedsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.Temporal;
import fri.util.database.jpa.tree.TemporalTreeDao;

/**
 * DAO extension that allows to historicize entities instead of deleting them.
 * <p/>
 * Following methods must be overridden for another historization-mechanism than
 * the default <i>validFrom</i> and <i>validTo</i> properties:
 * <ul>
 * 	<li>isValid</li>
 * 	<li>appendValidityCondition</li>
 * 	<li>appendInvalidityCondition</li>
 * 	<li>assignValidity</li>
 * 	<li>assignInvalidity</li>
 * </ul>
 * 
 * @author Fritz Ritzberger, 12.10.2011
 */
public class TemporalNestedSetsTreeDao extends NestedSetsTreeDao implements TemporalTreeDao<NestedSetsTreeNode>
{
	private final String validFromPropertyName;
	private final String validToPropertyName;
	
	private Date now4FilterChildren;
	private boolean doNotApplyTemporalConditions = false;
	private boolean invertTemporalConditions = false;
	
	/**
	 * {@inheritDoc}
	 * @param validFromPropertyName the name of the temporal valid-from property, can be null.
	 * @param validToPropertyName the name of the temporal valid-to property, can be null when append* and assign* are overridden.
	 */
	public TemporalNestedSetsTreeDao(
			Class<? extends NestedSetsTreeNode> targetEntityClass,
			String validFromPropertyName,
			String validToPropertyName,
			DbSession session)
	{
		super(targetEntityClass, session);
		// does NOT cascade to other constructor to NOT duplicate the way how table name is derived from class
		
		this.validFromPropertyName = validFromPropertyName;
		this.validToPropertyName = validToPropertyName;
	}

	/**
	 * {@inheritDoc}
	 * @param validFromPropertyName the name of the temporal valid-from property, can be null.
	 * @param validToPropertyName the name of the temporal valid-to property, can be null when append* and assign* are overridden.
	 */
	public TemporalNestedSetsTreeDao(
			Class<? extends NestedSetsTreeNode> targetEntityClass,
			String targetEntityName,
			String validFromPropertyName,
			String validToPropertyName,
			DbSession session)
	{
		super(targetEntityClass, targetEntityName, session);

		this.validFromPropertyName = validFromPropertyName;
		this.validToPropertyName = validToPropertyName;
	}
	

	/** Overridden to always read the subtree, because historicized nodes occupy left and right indexes. */
    @Override
	public int size(NestedSetsTreeNode node)        {
		return getTree(node).size();
    }
    
	/** Overridden to read the subtree when super returns false, because historicized nodes occupy left and right indexes. */
	@Override
	public boolean isLeaf(NestedSetsTreeNode node)	{
		if (super.isLeaf(node))	// this is very fast
			return true;	// no need to check validTo limit
		
		return getTree(node).size() <= 0;
	}
	
	/**
	 * Overridden because super.getChildren() would not work with a sub-tree list
	 * that contains nodes with left/right indexes that have gaps because historicized
	 * nodes are not contained.
	 */
	@Override
	public synchronized List<NestedSetsTreeNode> getChildren(NestedSetsTreeNode parent)	{
		List<NestedSetsTreeNode> subTree = getFullTreeCacheable(parent);
		now4FilterChildren = validTo();
		try	{
			return findDirectChildren(subTree);
		}
		finally	{
			now4FilterChildren = null;
		}
	}
	
	/** Overridden to historicize roots instead of removing them physically.  */
	@Override
	public synchronized void removeAll() {
		StringBuilder updateText = new StringBuilder("update "+nodeEntityName()+" t set ");
		List<Object> parameters = new ArrayList<Object>();
		
		assignInvalidity("t", updateText, parameters);
		updateText.append(" where ");
		appendValidityCondition("t", updateText, parameters);
		session.executeUpdate(updateText.toString(), parameters.toArray());
	}
	
	/** {@inheritDoc} */
	@Override
	public List<NestedSetsTreeNode> findRemoved(NestedSetsTreeNode parent, Map<String, Object> criteria) {
		try	{
			invertTemporalConditions = true;
			return super.find(parent, criteria);
		}
		finally	{
			invertTemporalConditions = false;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized List<NestedSetsTreeNode> getAllRoots()	{
		try	{
			doNotApplyTemporalConditions = true;
			return super.getRoots();
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized List<NestedSetsTreeNode> getFullTreeCacheable(NestedSetsTreeNode node) {
		try	{
			doNotApplyTemporalConditions = true;
			return super.getTreeCacheable(node);
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}
	
	/** {@inheritDoc} */
	@Override
	public void unremove(NestedSetsTreeNode node)	{
		removeOrUnremove(node, false);
	}
	
	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public synchronized void removeHistoricizedTreesPhysically()	{
		for (NestedSetsTreeNode root : getAllRoots())	{
			final StringBuilder queryText = new StringBuilder(
					"select t from "+nodeEntityName()+" t "+
					"where t.topLevel = ?1 and ");
			final List<Object> parameters = new ArrayList<Object>();
			parameters.add(root);
			appendInvalidityCondition("t", queryText, parameters);
			queryText.append(" order by t.lft");
			
			List<NestedSetsTreeNode> removed;
			do	{
				removed = (List<NestedSetsTreeNode>) session.queryList(queryText.toString(), parameters.toArray());
				
				if (removed.size() > 0)	{
					NestedSetsTreeNode highest = removed.get(0);
					removePhysically(highest);
				}
			}
			while (removed.size() > 0);
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void removeAllPhysically() {
		for (NestedSetsTreeNode root : getAllRoots())
			removePhysically(root);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void removePhysically(NestedSetsTreeNode node) {
		if (node == null || isPersistent(node) == false)
			throw new IllegalArgumentException("Node is null or not persistent: "+node);
		
		doNotApplyTemporalConditions = true;
		try	{
			remove(node);
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	public List<NestedSetsTreeNode> findValidDirectChildren(List<NestedSetsTreeNode> subNodes) {
		now4FilterChildren = validTo();
		try	{
			return super.findDirectChildren(subNodes);
		}
		finally	{
			now4FilterChildren = null;
		}
	}



	/** Overridden to search only nodes that were not historicized. */
	@Override
	protected final void beforeFindQuery(String tableAlias, StringBuilder queryText, List<Object> parameters, boolean whereWasAppended) {
		beforeFindQuery(tableAlias, queryText, parameters, whereWasAppended, doNotApplyTemporalConditions, invertTemporalConditions);
	}

	
	/** Overridden to include removed nodes into copy-list, else inconsistent left/right numbers would occur. */
	@Override
	protected List<NestedSetsTreeNode> getSubTreeDepthFirstForCopy(NestedSetsTreeNode nodeToCopy) {
		return getFullTreeCacheable(nodeToCopy);
	}
	
	/** Overridden to filter out historicized children on findDirectChildren(). */
	@Override
	protected final boolean isValidFilterChild(NestedSetsTreeNode node) {
		if (now4FilterChildren == null)
			return super.isValidFilterChild(node);
		
		return isValid((Temporal) node, now4FilterChildren);
	}

	/** Overridden to return children including invalid ones. */
	@Override
	protected final List<NestedSetsTreeNode> getChildListForInsertion(NestedSetsTreeNode parent)	{
		// can't call super as this would call overridden getSubTreeDepthFirst()
		List<NestedSetsTreeNode> subTree = getFullTreeCacheable(parent);
		return findDirectChildren(subTree);
	}
	
	/** Overridden to historicize children. */
	@Override
	protected final void remove(NestedSetsTreeNode node, int removedNodesCount) {
		if (doNotApplyTemporalConditions)	{
			super.remove(node, removedNodesCount);
		}
		else	{
			removeOrUnremove(node, true);
		}
	}

	/** Overridden to bridge index gaps that occur when nodes have been historicized. */
	@Override
	protected boolean isNextChild(int nextChildLeft, NestedSetsTreeNode node, int currentChildRight, List<NestedSetsTreeNode> subNodes, int currentIndex) {
		if (super.isNextChild(nextChildLeft, node, currentChildRight, subNodes, currentIndex))
			return true;
		
		if (node.getRight() < currentChildRight)
			return false;	// is a child of currentChild
		
		// bridge temporal index gaps
		// check if there is a left value between node.getLeft() and nextChildLeft
		final int size = subNodes.size();
		if (currentIndex + 1 < size)	{	// is not last in list
			for (NestedSetsTreeNode n : subNodes.subList(currentIndex + 1, size))	{
				if (n.getLeft() <= nextChildLeft)
					return false;	// is not next child, there is another node
				
				if (n.getLeft() > nextChildLeft)	// list is ordered by left, there will be no more smaller left
					return true;	// there was a (temporal empty) index gap
			}
		}
		return true;
	}


	/**
	 * Called when removing nodes.
	 * Appends the (temporal) invalidity assignment to passed JPQL statement,
	 * something like "t.validTo = ?", where validTo is taken from .
	 * Override this to use other invalidity assignments than valid-to property.
	 * @param tableAlias the alias of the table containing the <i>validTo</i> property, without trailing dot.
	 * @param updateText the pending JPQL query text removing invalid nodes.
	 * @param parameters the positional arguments for the pending query.
	 */
	protected void assignInvalidity(String tableAlias, StringBuilder updateText, List<Object> parameters) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override assignInvalidity when validToPropertyName is null!");
		
		final String validToPropertyName = buildAliasedPropertyName(tableAlias, getValidToPropertyName());
		updateText.append(validToPropertyName+" = "+buildIndexedPlaceHolder(parameters));
		parameters.add(validToOnRemove());
	}

	/**
	 * Called when unremoving nodes.
	 * Appends the (temporal) validity assignment to passed JPQL statement,
	 * which is "t.validTo = null". Override for using another unremove-date.
	 * Override this to use other validity assignments.
	 * @param tableAlias the alias of the table containing the <i>validTo</i> property, without trailing dot.
	 * @param updateText the pending JPQL query text unremoving nodes.
	 * @param parameters the positional arguments for the pending query.
	 */
	protected void assignValidity(String tableAlias, StringBuilder updateText, List<Object> parameters) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override assignValidity when validToPropertyName is null!");
		
		final String validToPropertyName = buildAliasedPropertyName(tableAlias, getValidToPropertyName());
		updateText.append(validToPropertyName+" = null");
	}

	
	private void removeOrUnremove(NestedSetsTreeNode node, boolean isRemove) {
		final NestedSetsTreeNode topLevel = node.getTopLevel();
		
		// as this update ignores the JPA layer, we must refresh affected nodes after update
		final String selectWhere = "t.topLevel = ?1 and t.lft >= ?2 and t.rgt <= ?3";
		final Object [] selectParams = new Object [] { topLevel, node.getLeft(), node.getRight() };
		// read affected nodes BEFORE update
		final List<?> nodesToRefresh = session.queryList(
				"select t from "+nodeEntityName()+" t where "+selectWhere,
				selectParams);

		// now update all nodes
		final StringBuilder updateText = new StringBuilder("update "+nodeEntityName()+" t set ");
		final List<Object> updateParams = new ArrayList<Object>();
		
		if (isRemove)
			assignInvalidity("t", updateText, updateParams);
		else	// is recover
			assignValidity("t", updateText, updateParams);
		
		final int paramIndex = updateParams.size();
		final String updateWhere = selectWhere
				.replace("?3", "?"+(paramIndex + 3))
				.replace("?2", "?"+(paramIndex + 2))
				.replace("?1", "?"+(paramIndex + 1));
		updateText.append(" where "+updateWhere);
		updateParams.addAll(Arrays.asList(selectParams));
		
		session.executeUpdate(updateText.toString(), updateParams.toArray());

		// refresh their properties in cache
		refresh(nodesToRefresh);
	}

	@Override
	protected final String getValidFromPropertyName()	{
		return validFromPropertyName;
	}
	
	@Override
	protected final String getValidToPropertyName()	{
		return validToPropertyName;
	}

}
