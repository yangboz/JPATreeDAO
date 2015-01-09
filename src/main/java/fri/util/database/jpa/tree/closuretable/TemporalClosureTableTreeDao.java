package fri.util.database.jpa.tree.closuretable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.DbSession;
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
 * @author Fritz Ritzberger, 02.11.2012
 */
public class TemporalClosureTableTreeDao extends ClosureTableTreeDao implements TemporalTreeDao<ClosureTableTreeNode>
{
	private final String validFromPropertyName;
	private final String validToPropertyName;
	
	private Date removeDate;
	private boolean doNotApplyTemporalConditions = false;
	private boolean invertTemporalConditions = false;
	
	/**
	 * {@inheritDoc}
	 * @param validFromPropertyName the name of the temporal valid-from property in TreePath, can be null.
	 * @param validToPropertyName the name of the temporal valid-to property in TreePath, can be null when append* and assign* are overridden.
	 */
	public TemporalClosureTableTreeDao(
			Class<? extends ClosureTableTreeNode> treeNodeEntityClass,
			Class<? extends TreePath> treePathsEntityClass,
			boolean positionMatters,
			String validFromPropertyName,
			String validToPropertyName,
			DbSession session)
	{
		super(treeNodeEntityClass, treePathsEntityClass, positionMatters, session);
		// does NOT cascade to other constructor to NOT duplicate the way how table names are derived from classes
		
		this.validFromPropertyName = validFromPropertyName;
		this.validToPropertyName = validToPropertyName;
	}

	/**
	 * {@inheritDoc}
	 * @param validFromPropertyName the name of the temporal valid-from property in TreePath, can be null.
	 * @param validToPropertyName the name of the temporal valid-to property in TreePath, can be null when append* and assign* are overridden.
	 */
	public TemporalClosureTableTreeDao(
			Class<? extends ClosureTableTreeNode> treeNodeEntityClass,
			String treeNodeEntity,
			Class<? extends TreePath> treePathEntityClass,
			String treePathEntity,
			boolean positionMatters,
			String validFromPropertyName,
			String validToPropertyName,
			DbSession session)
	{
		super(treeNodeEntityClass, treeNodeEntity, treePathEntityClass, treePathEntity, positionMatters, session);
		
		this.validFromPropertyName = validFromPropertyName;
		this.validToPropertyName = validToPropertyName;
	}

	
	/** {@inheritDoc} */
	@Override
	public synchronized List<ClosureTableTreeNode> getAllRoots()	{
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
	public List<ClosureTableTreeNode> findRemoved(ClosureTableTreeNode parent, Map<String, Object> criteria) {
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
	public List<ClosureTableTreeNode> getFullTreeCacheable(ClosureTableTreeNode node) {
		try	{
			doNotApplyTemporalConditions = true;
			return getTreeCacheable(node);
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	public List<ClosureTableTreeNode> findValidDirectChildren(List<ClosureTableTreeNode> subNodes) {
		TemporalCacheableTreeList treeList = (TemporalCacheableTreeList) subNodes;
		return treeList.size() > 0 ? treeList.getValidChildren(treeList.getRoot()) : new ArrayList<ClosureTableTreeNode>();
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void removeAll() {
		removeDate = validToOnRemove();
		super.removeAll();
	}
	
	/** {@inheritDoc} */
	@Override
	public void unremove(ClosureTableTreeNode node) {
		try	{
			doNotApplyTemporalConditions = true;
			for (TreePath path : getPathsToRemove(node))	{
				assignValidity(path);
				save(path);
			}
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	public synchronized void removeAllPhysically() {
		for (ClosureTableTreeNode root : getAllRoots())
			removePhysically(root);
	}
	
	/** {@inheritDoc} */
	@Override
	public synchronized void removePhysically(ClosureTableTreeNode node) {
		if (node == null || isPersistent(node) == false)
			throw new IllegalArgumentException("Node is null or not persistent: "+node);
		
		try	{
			doNotApplyTemporalConditions = true;
			remove(node);
		}
		finally	{
			doNotApplyTemporalConditions = false;
		}
	}

	/** {@inheritDoc} */
	@Override
	@SuppressWarnings("unchecked")
	public synchronized void removeHistoricizedTreesPhysically() {
		for (ClosureTableTreeNode root : getAllRoots())	{
			// select all removed paths under that root, highest level first
			final StringBuilder queryText = new StringBuilder(
				"select p from "+pathEntityName()+" p where p.ancestor = ?1 and ");
			final List<Object> parameters = new ArrayList<Object>();
			parameters.add(root);
			appendInvalidityCondition("p", queryText, parameters);
			queryText.append(" order by p.depth");
			
			List<TemporalTreePath> toRemove;
			do	{
				toRemove = (List<TemporalTreePath>) session.queryList(queryText.toString(), parameters.toArray());
				
				int depth = -1;	// just do the highest level and then re-read, maybe all is done with that level
				boolean levelDone = false;
				for (int i = 0; levelDone == false && i < toRemove.size(); i++)	{
					final TemporalTreePath path = toRemove.get(i);
					
					if (depth == -1)	// not yet determined
						depth = path.getDepth();
					
					if (depth != path.getDepth())	// do not go to deeper levels
						levelDone = true;
					else
						removePhysically(path.getDescendant());
				}
			}
			while (toRemove.size() > 0);
		}
	}


	/** Factory method for new CacheableTreeList. Overridden for temporal variant. */
	@Override
	protected CacheableTreeList newCacheableTreeList(ClosureTableTreeNode parent, List<TreePath> breadthFirstTree)	{
		return new TemporalCacheableTreeList(this, validTo()).init(parent, breadthFirstTree);
	}
	
	/**
	 * Called when removing paths. Assigns invalidity to passed domain object.
	 * Override this to use other invalidity assignments than valid-to property.
	 * @param path the tree-path to historicize.
	 */
	protected void assignInvalidity(TreePath path) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override assignInvalidity when validToPropertyName is null!");

		if (removeDate == null)
			throw new IllegalStateException("The remove-date is null on historizicing paths!");
		
		((TemporalTreePath) path).setValidTo(removeDate);
	}
	
	/**
	 * Called when unremoving paths. Assigns validity to passed domain object.
	 * Override this to use other validity assignments than valid-to property.
	 * @param path the tree-path to unremove.
	 */
	protected void assignValidity(TreePath path) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override assignValidity when validToPropertyName is null!");

		((TemporalTreePath) path).setValidTo(null);
	}
	

	/** Overridden to append temporal conditions. */
	@Override
	protected final void beforeFindQuery(String tableAlias, StringBuilder queryText, List<Object> parameters, boolean whereWasAppended) {
		beforeFindQuery(tableAlias, queryText, parameters, whereWasAppended, doNotApplyTemporalConditions, invertTemporalConditions);
	}
	
	
	/**
	 * Overridden to return false to prevent closing a gap on remove and
	 * reordering siblings after historization.
	 */
	@Override
	protected boolean shouldCloseGapOnRemove() {
		return false;
	}

	/** Overridden to set the historicizing date by calling validToOnRemove(). */
	@Override
	protected void removeTree(ClosureTableTreeNode parent) {
		removeDate = validToOnRemove();
		super.removeTree(parent);
	}
	
	/** Overridden to do nothing as related paths are historicized. */
	@Override
	protected final void removeNode(ClosureTableTreeNode nodeToRemove) {
		if (doNotApplyTemporalConditions)	{
			super.removeNode(nodeToRemove);
		}
		// else: do nothing, path gets historicized
	}
	
	/** Overridden to historicize path, using the historicizing date. */
	@Override
	protected final void removePath(TreePath path) {
		if (doNotApplyTemporalConditions)	{
			super.removePath(path);
		}
		else	{
			assignInvalidity(path);
			save(path);
		}
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
