package fri.util.database.jpa.tree;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.DbSession;

/**
 * Tests additional public methods of TemporalTreeDao.
 * 
 * @author Fritz Ritzberger, 12.10.2011
 */
public abstract class AbstractTemporalTreeTest <D extends TemporalTreeDao<N>, N extends TreeNode>
	extends AbstractTreeTest<D, N>
{
	private boolean testValidFromIsNull;
	
	/** Overridden to perform a different cleanup, else data would remain in database. */
	@Override
	protected void tearDown() throws Exception {
		beginDbTransaction("cleanup temporal database");
		getTemporalDao().removeAllPhysically();
		commitDbTransaction("cleanup temporal database");
		
		super.tearDown();
	}
	
	// tests
	
	/** Ensure this test is the first one, this avoids pitfalls when database is not case-sensitive. */
	@Override
	public void testDatabaseToBeCaseSensitive() throws Exception	{
		super.testDatabaseToBeCaseSensitive();
	}
	
	/** Tests housekeeping on one removed sub-tree. */
	public void testFindRemoved() throws Exception	{
		DbSession session = beginDbTransaction("find removed node");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		
		// historicize a folder node
		N a = findByName(root, "A");
		getDao().remove(a);
		assertNull(findByName(root, "A"));
		
		// find it
		final Map<String,Object> criteria = new Hashtable<String,Object>();
		criteria.put("name", "A");
		List<N> removedAList = getDao().findRemoved(root, criteria);
		assertEquals(1, removedAList.size());
		N removedA = removedAList.get(0);
		assertEquals("A", getName(removedA));
		
		// unremove it
		getDao().unremove(removedA);
		
		N unremovedA = findByName(root, "A");
		assertNotNull(unremovedA);
		assertEquals("A", getName(unremovedA));
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("find removed node");
	}
	
	/** Tests housekeeping on one removed sub-tree. */
	public void testRemoveSubTreePhysically() throws Exception	{
		DbSession session = beginDbTransaction("remove tree node physically");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		
		// historicize a folder node
		N a = findByName(root, "A");
		getDao().remove(a);
		assertNull(findByName(root, "A"));
		assertEquals(7, getDao().size(root));
		assertEquals(9, getTemporalDao().getFullTreeCacheable(root).size());
		
		// historicize another folder node
		N c = findByName(root, "C");
		getDao().remove(c);
		assertNull(findByName(root, "C"));
		assertEquals(4, getDao().size(root));
		assertEquals(9, getTemporalDao().getFullTreeCacheable(root).size());
		
		// remove A physically
		getTemporalDao().removePhysically(a);
		
		assertEquals(4, getDao().size(root));
		assertEquals(7, getTemporalDao().getFullTreeCacheable(root).size());
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("remove tree node physically");
	}

	/** Tests housekeeping on several removed sub-trees. */
	public void testRemoveHistoricizedTreesPhysically() throws Exception	{
		DbSession session = beginDbTransaction("remove tree nodes physically");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		
		// historicize a folder node
		N b = findByName(root, "B");
		getDao().remove(b);
		assertEquals(6, getDao().size(root));
		assertEquals(9, getTemporalDao().getFullTreeCacheable(root).size());
		
		// historicize another folder node
		N c = findByName(root, "C");
		getDao().remove(c);
		assertEquals(3, getDao().size(root));
		assertEquals(9, getTemporalDao().getFullTreeCacheable(root).size());
		
		// remove physically
		getTemporalDao().removeHistoricizedTreesPhysically();
		
		assertEquals(3, getTemporalDao().getFullTreeCacheable(root).size());
		assertEquals(3, getDao().size(root));
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("remove tree nodes physically");
	}

	/** Tests housekeeping on removed roots. */
	public void testRemoveRootPhysically() throws Exception	{
		DbSession session = beginDbTransaction("remove root physically");
		Serializable rootId1 = createTree("ROOT");
		Serializable rootId2 = createTree("root");
		
		assertEquals(2, getTemporalDao().getAllRoots().size());
		assertEquals(2, getDao().getRoots().size());
		N root1 = getDao().find(rootId1);
		assertEquals(9, getDao().size(root1));
		N root2 = getDao().find(rootId2);
		assertEquals(9, getDao().size(root2));
		
		// historicize root1
		getDao().remove(root1);
		assertEquals(0, getDao().size(root1));
		assertEquals(9, getTemporalDao().getFullTreeCacheable(root1).size());
		assertEquals(2, getTemporalDao().getAllRoots().size());
		assertEquals(1, getDao().getRoots().size());
		
		// remove root1 physically
		getTemporalDao().removeHistoricizedTreesPhysically();
		assertEquals(1, getTemporalDao().getAllRoots().size());
		assertEquals(1, getDao().getRoots().size());
		
		assertEquals(9, getDao().size(root2));
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("remove root physically");
	}

	/** Tests recovery of nodes. */
	public void testUnremove() throws Exception	{
		DbSession session = beginDbTransaction("unremove tree nodes");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		// historicize folder node B
		N b = findByName(root, "B");
		getDao().remove(b);
		assertEquals(6, getDao().size(root));
		assertNull(findByName(root, "B"));
		
		// find historicized node
		b = null;
		for (N node : getTemporalDao().getFullTreeCacheable(root))	{
			if (getName(node).equals("B"))	{
				b = node;
				break;
			}
		}
		
		// recover it
		assertNotNull(b);
		getTemporalDao().unremove(b);
		
		assertEquals(9, getDao().size(root));
		assertNotNull(findByName(root, "B"));
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("unremove tree nodes");
	}

	/** Tests deletion and recovery of all trees. */
	public void testUnremoveAllRoots() throws Exception	{
		DbSession session = beginDbTransaction("unremove all roots");
		Serializable rootId1 = createTree("ROOT", false);
		Serializable rootId2 = createTree("root", true);
		
		N root1 = getDao().find(rootId1);
		assertEquals(9, getDao().size(root1));
		N root2 = getDao().find(rootId2);
		assertEquals(9, getDao().size(root2));
		
		// historicize all trees
		getDao().removeAll();
		List<N> roots = getDao().getRoots();
		assertEquals(0, roots.size());
		assertNull(findByName(root1, "C11"));
		assertNull(findByName(root2, "c11"));

		// recover
		getTemporalDao().unremove(root1);
		getTemporalDao().unremove(root2);
		
		roots = getDao().getRoots();
		assertEquals(2, roots.size());
		
		assertEquals(9, getDao().size(root1));
		assertEquals(9, getDao().size(root2));
		assertNotNull(findByName(root1, "C11"));
		assertNotNull(findByName(root2, "c11"));
		
		checkTreeIntegrity(session, root1);
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("unremove all roots");
	}

	/** Checks that it is possible to leave out validFromPropertyName. */
	public void testUniquenessWithNullValidFrom() throws Exception {
		testValidFromIsNull = true;
		try	{
			super.testUniqueness();
		}
		finally	{
			testValidFromIsNull = false;
		}
	}
	
	/**
	 * Demonstrates how to retrieve the tree structure from a list of all nodes under a root,
	 * optionally also containing removed nodes.
	 */
	public void testFindChildrenInFullTree() throws Exception	{
		beginDbTransaction("find children in full tree");
		
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		
		// historicize a folder node
		N b = findByName(root, "B");
		getDao().remove(b);
		assertNull(findByName(root, "B"));
		
		// read the full tree, also containing removed nodes
		List<N> fullTree = getTemporalDao().getFullTreeCacheable(root);
		assertEquals(9, fullTree.size());	// ensure even removed nodes are in list
		
		// extract children of root, not containing removed nodes
		List<N> children = getTemporalDao().findValidDirectChildren(fullTree);
		assertEquals(2, children.size());	// removed node must not be contained
		assertEquals("A", getName(children.get(0)));
		assertEquals("C", getName(children.get(1)));
		
		// extract also removed children of root
		children = getDao().findDirectChildren(fullTree);
		assertEquals(3, children.size());	// removed node must be contained
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		commitDbTransaction("find children in full tree");
	}

	
	
	/** @return true when a null validFromPropertyName should be tested. */
	protected final boolean testValidFromIsNull()	{
		return testValidFromIsNull;
	}

	/** @return the casted temporal DAO for convenience. */
	protected final TemporalTreeDao<N> getTemporalDao() {
		return getDao();
	}

}
