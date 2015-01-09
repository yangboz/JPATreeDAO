package fri.util.database.jpa.tree;

import java.util.List;
import java.util.Map;

/**
 * Responsibilities of a temporal DAO extension. Provides means to read
 * and recover historicized nodes, or remove them physically for cleanups.
 * <p/>
 * The mechanism how nodes are historicized is hidden in the implementations
 * of this interface (and thus can be overridden). The implementations
 * provide the constructor parameters <code>validFromPropertyName</code>
 * and <code>validToPropertyName</code> as default historization mechanism,
 * assumed to be attributes that describe the validity period of a node (or path).
 * 
 * @author Fritz Ritzberger, 05.11.2012
 *
 * @param <N> the tree node type managed by this DAO.
 */
public interface TemporalTreeDao <N extends TreeNode> extends TreeDao<N>
{
	List<N>  findRemoved(N parent, Map<String,Object> criteria);
	
	/** @return all roots, including removed (historicized) ones. */
	List<N> getAllRoots();
	
	/** @return the full tree under given parent, including removed (historicized) nodes. */
	List<N> getFullTreeCacheable(N parent);

	/**
	 * Use this to retrieve children lists that do <b>not</b> contain removed nodes
	 * from trees returned by <code>getFullTreeCacheable()</code>.
	 * Use <code>findDirectChildren()</code> on a fullTreeCacheable to retrieve children lists that
	 * also contain removed nodes. The difference of both would be the removed children.
	 * @param treeCacheable the sub-tree to retrieve children from, containing parent at first position.
	 * @return the children list of the first node in given subNodes list, NOT containing removed nodes.
	 */
	List<N> findValidDirectChildren(List<N> treeCacheable);
	
	/** Recovers the given removed (historicized) node, including all sub-nodes. */
	void unremove(N node);
	
	/** Physically deletes the tree under given node, including the node itself. Node can also be a root. */
	void removePhysically(N node);

	/** Physically deletes all historicized tree nodes in all roots, including their sub-nodes. */
	void removeHistoricizedTreesPhysically();
	
	/** Physically deletes everything, all roots and the trees below them. */
	void removeAllPhysically();

}
