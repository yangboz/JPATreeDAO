package fri.util.database.jpa.tree.closuretable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.tree.Temporal;

/**
 * A temporal cacheable depth-first tree list.
 * 
 * @author Fritz Ritzberger, 21.10.2012
 */
class TemporalCacheableTreeList extends CacheableTreeList
{
	private final TemporalClosureTableTreeDao dao;
	private final Date validityDate;
	private Map<Serializable,Boolean> removedFlags;
	
	/** Stores the given validityDate and DAO to member fields. */
	TemporalCacheableTreeList(TemporalClosureTableTreeDao dao, Date validityDate) {
		this.dao = dao;
		this.validityDate = validityDate;
	}
	
	/** Internal sub-tree constructor. */
	protected TemporalCacheableTreeList(TemporalClosureTableTreeDao dao, Date validityDate, ClosureTableTreeNode root, Map<Serializable,List<ClosureTableTreeNode>> hierarchy, Map<Serializable,Boolean> removedFlags) {
		super(root, hierarchy);
		
		this.dao = dao;
		this.validityDate = validityDate;
		this.removedFlags = removedFlags;
	}
	

	/** Initializes this list after package-visible constructor. */
	@Override
	CacheableTreeList init(ClosureTableTreeNode root, List<TreePath> paths) {
		this.removedFlags = new HashMap<Serializable,Boolean>(paths.size());
		
		return super.init(root, paths);
	}
	
	
	public List<ClosureTableTreeNode> getValidChildren(ClosureTableTreeNode parent) {
		List<ClosureTableTreeNode> allChildren = getChildren(parent);
		List<ClosureTableTreeNode> validChildren = new ArrayList<ClosureTableTreeNode>();
		
		for (ClosureTableTreeNode n : allChildren)
			if (removedFlags.get(n.getId()).booleanValue())
				validChildren.add(n);
		
		return validChildren;
	}


	/** Factory method for internal constructor. Overridden for temporal variant. */
	@Override
	protected CacheableTreeList newCacheableTreeList(ClosureTableTreeNode parent) {
		return new TemporalCacheableTreeList(dao, validityDate, parent, new HashMap<Serializable,List<ClosureTableTreeNode>>(), new HashMap<Serializable,Boolean>());
	}
	
	/** Puts a node into hierarchy. Overridden for temporal variant. */
	@Override
	protected void putToHierarchy(CacheableTreeList treeList, TreePath path, ClosureTableTreeNode node, List<ClosureTableTreeNode> children) {
		super.putToHierarchy(treeList, path, node, children);
		
		if (path != null)	// called by init()
			removedFlags.put(node.getId(), Boolean.valueOf(dao.isValid((Temporal) path, validityDate)));
		else	// constructed internally
			removedFlags.put(node.getId(), ((TemporalCacheableTreeList) treeList).removedFlags.get(node.getId()));
	}
	
}
