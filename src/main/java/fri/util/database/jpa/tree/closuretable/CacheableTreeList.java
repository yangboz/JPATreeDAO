package fri.util.database.jpa.tree.closuretable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A cacheable depth-first tree list that provides fast access to
 * children lists and sub-trees without database queries,
 * returned from the DAO method getTreeCacheable().
 * 
 * @author Fritz Ritzberger, 21.10.2012
 */
class CacheableTreeList extends ArrayList<ClosureTableTreeNode>
{
	/** The topmost node of this sub-tree. */
	private ClosureTableTreeNode root;
	/** The children cache, key = primary key of node, value = child list. */
	private Map<Serializable,List<ClosureTableTreeNode>> hierarchy;
	/** Runtime safety: after ready became true, add(node) will throw an exception. */
	private boolean ready;
	
	/** Do-nothing constructor. */
	CacheableTreeList() {
	}
	
	/** Internal sub-tree constructor. */
	protected CacheableTreeList(ClosureTableTreeNode root, Map<Serializable,List<ClosureTableTreeNode>> hierarchy) {
		assert root != null && hierarchy != null;
		
		this.root = root;
		this.hierarchy = hierarchy;
	}
	
	
	/** Initializes this list after package-visible do-nothing constructor. */
	CacheableTreeList init(ClosureTableTreeNode root, List<TreePath> paths)	{
		assert paths.size() <= 0 || paths.get(0).getAncestor().equals(paths.get(0).getAncestor()) : "Incorrect TreePath list, does not contain root at position 0: "+paths.get(0);
		
		this.root = root;
		this.hierarchy = new HashMap<Serializable,List<ClosureTableTreeNode>>(paths.size());
			
		loopChildren(paths, paths);
		
		assert size() == paths.size() : "Something went wrong on building tree-list with "+paths.size()+" paths, having only "+size()+" nodes!";

		this.ready = true;	// make this list unmodifiable
		
		return this;
	}
	
	
	/** @return the root of this tree. */
	public ClosureTableTreeNode getRoot()	{
		return root;
	}
	
	/** @return the children of given parent. */
	public List<ClosureTableTreeNode> getChildren(ClosureTableTreeNode parent)	{
		if (parent.getId() == null)
			throw new IllegalArgumentException("Parent to retrieve children for is not persistent: "+parent);
		
		return hierarchy.get(parent.getId());
	}
	
	/** @return the sub-tree of given parent. */
	public List<ClosureTableTreeNode> getSubTree(ClosureTableTreeNode parent) {
		if (parent.getId() == null)
			throw new IllegalArgumentException("Parent to retrieve sub-tree for is not persistent: "+parent);
		
		CacheableTreeList subTree = newCacheableTreeList(parent);
		addSubTreeRecursive(parent, getChildren(parent), subTree);
		subTree.ready = true;
		
		return subTree;
	}

	
	/** Factory method for internal constructor. To be overridden by temporal variant. */
	protected CacheableTreeList newCacheableTreeList(ClosureTableTreeNode parent) {
		return new CacheableTreeList(parent, new HashMap<Serializable,List<ClosureTableTreeNode>>());
	}
	
	/** Puts a node into hierarchy. To be overridden by temporal variant. */
	@SuppressWarnings("unused")
	protected void putToHierarchy(CacheableTreeList treeList, TreePath path, ClosureTableTreeNode node, List<ClosureTableTreeNode> children)	{
		treeList.hierarchy.put(node.getId(), children);
	}
	
	
	private void loopChildren(List<TreePath> paths, List<TreePath> allPaths) {
		for (TreePath path : paths)
			checkDependency(path, allPaths);
	}
	
	private void checkDependency(TreePath path, List<TreePath> allPaths) {
		List<ClosureTableTreeNode> children = hierarchy.get(path.getDescendant().getId());
		if (children == null)	{	// not yet reached this node
			children = new ArrayList<ClosureTableTreeNode>();
			ClosureTableTreeNode node = path.getDescendant();
			putToHierarchy(this, path, node, children);
			
			add(node);	// pre-order: depth first
			
			List<TreePath> childPaths = getChildPaths(node, allPaths);
			for (TreePath childPath : childPaths)
				children.add(childPath.getDescendant());
			
			loopChildren(childPaths, allPaths);
		}
	}
	
	private List<TreePath> getChildPaths(ClosureTableTreeNode parent, List<TreePath> paths)	{
		List<TreePath> childPaths = new ArrayList<TreePath>();
		for (TreePath path : paths)
			if (path.getAncestor().equals(parent) && path.getDepth() == 1)	// depth: root would be added as child else
				childPaths.add(path);
		return childPaths;
	}

	private void addSubTreeRecursive(ClosureTableTreeNode parent, List<ClosureTableTreeNode> children, CacheableTreeList subTree) {
		subTree.add(parent);
		putToHierarchy(subTree, null, parent, children);
		
		for (ClosureTableTreeNode node : children)
			addSubTreeRecursive(node, getChildren(node), subTree);
	}


	
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean add(ClosureTableTreeNode n) {
		if (ready)
			throw new RuntimeException("Can not modify this list!");
		return super.add(n);
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public void add(int index, ClosureTableTreeNode n) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public ClosureTableTreeNode remove(int index) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean remove(Object o) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean addAll(Collection<? extends ClosureTableTreeNode> c) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean addAll(int index, Collection<? extends ClosureTableTreeNode> c) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public void clear() {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException("Can not modify this list!");
	}
	/** Overridden to make this an unmodifiable list. */
	@Override
	public ClosureTableTreeNode set(int index, ClosureTableTreeNode element) {
		throw new RuntimeException("Can not modify this list!");
	}

}
