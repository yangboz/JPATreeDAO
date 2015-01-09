package fri.util.database.jpa.tree;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.AbstractJpaTest;
import fri.util.database.jpa.commons.DbSessionJpaImpl;
import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.TreeDao;
import fri.util.database.jpa.tree.TreeNode;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;

/**
 * Tests all public methods of TreeDao with following sample tree:
 * <pre>
    ROOT
      - A
        - A1
      - B
        - B1
        - B2
      - C
        - C1
          - C11
 * </pre>
 * 
 * TODO: add a concurrent test, e.g. reading nodes while adding, moving and copying.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public abstract class AbstractTreeTest <D extends TreeDao<N>, N extends TreeNode>
	extends AbstractJpaTest
{
	/** Unique constraint property names to test. */
	protected static final String[][] UNIQUE_PROPERTY_NAMES = new String [][] {
		{ "name" },
		{ "address", "name" }
	};

	/** Constant prefix for testing node renames. */
	protected static final String COPIED_NAME_PREFIX = "Copy of ";
	
	
	/** Utility method. */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public final static void outputTree(TreeNode root, TreeDao dao) {
		outputTree(dao.getTree(root));
	}
	
	public final static void outputTree(List<? extends TreeNode> nodes) {
		for (TreeNode node : nodes)	{
			System.out.println(node+" id="+node.getId());
		}
	}
	

	private D dao;
	private boolean testCopy = false;	// flag to create a non-unique POJO
	
	
	// begin of tests
	
	/** Especially for MySql, check that string-compares are case-sensitive! */
	public void testDatabaseToBeCaseSensitive() throws Exception	{
		beginDbTransaction("check case sensitivity");
		Serializable rootId = createTree("ROOT");	// upper case names
		
		N root = getDao().find(rootId);
		
		// there is a node "A" under "ROOT", try to create a node "a",
		// this would fail because of unique constraint when database is not case-sensitive
		getDao().addChild(root, newTreePojo("a"));
		List<N> children = getDao().getChildren(root);
		assertEquals(4, children.size());
		
		// when database is not case-sensitive we would get IllegalStateException
		// when searching for "A" because two nodes would be found
		N a = findByName(root, "A");
		assertNotNull(a);
		
		commitDbTransaction("check case sensitivity");
	}
	
	/** Tests creation of a tree, including addChild(). */
	public void testCreateTree() throws Exception	{
		DbSession session = beginDbTransaction("create tree nodes");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		assertNotNull(root);
		assertFalse(getDao().isChildOf(root, root));
		assertTrue(getDao().isEqualToOrChildOf(root, root));
		assertNodesExist(root);
		assertTrue(getDao().isRoot(root));
		assertEquals(9, getDao().size(root));
		
		List<N> children;
		assertEquals(3, getDao().getChildCount(root));
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		N a = findByName(root, "A");
		assertFalse(getDao().isRoot(a));
		assertTrue(getDao().isChildOf(a, root));
		assertTrue(getDao().isEqualToOrChildOf(a, root));
		assertEquals(root, getDao().getRoot(a));
		children = getDao().getChildren(a);
		assertEquals(1, children.size());
		assertEquals("A1", getName(children.get(0)));
		assertEquals(2, getDao().size(a));
		
		N b = findByName(root, "B");
		assertFalse(getDao().isLeaf(b));
		assertEquals(2, getDao().getChildCount(b));
		children = getDao().getChildren(b);
		assertEquals(2, children.size());
		assertEquals("B1", getName(children.get(0)));
		assertTrue(getDao().isLeaf(children.get(0)));
		assertEquals("B2", getName(children.get(1)));
		assertEquals(3, getDao().size(b));
		
		N c = findByName(root, "C");
		assertFalse(getDao().isLeaf(c));
		children = getDao().getChildren(c);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		assertEquals(3, getDao().size(c));
		
		N c1 = children.get(0);
		assertTrue(getDao().isChildOf(c1, root));
		assertTrue(getDao().isChildOf(c1, c));
		children = getDao().getChildren(c1);
		assertFalse(getDao().isRoot(c1));
		assertEquals(root, getDao().getRoot(c1));
		assertEquals(1, children.size());
		
		N c11 = children.get(0);
		assertEquals("C11", getName(c11));
		assertTrue(getDao().isChildOf(c11, root));
		assertTrue(getDao().isChildOf(c11, c));
		assertTrue(getDao().isChildOf(c11, c1));
		assertEquals(1, getDao().size(c11));
		assertEquals(0, getDao().getChildCount(c11));
		assertTrue(getDao().isLeaf(c11));
		children = getDao().getChildren(c11);
		assertEquals(0, children.size());
		
		// assert getTree() method
		
		List<N> treeList = getDao().getTree(root);
		assertEquals(9, treeList.size());
		
		List<String> names = new ArrayList<String>();
		for (N node : treeList)
			names.add(getName(node));
		
		assertTrue(names.contains("ROOT"));
		assertTrue(names.contains("A"));
		assertTrue(names.contains("A1"));
		assertTrue(names.contains("B"));
		assertTrue(names.contains("B1"));
		assertTrue(names.contains("B2"));
		assertTrue(names.contains("C"));
		assertTrue(names.contains("C1"));
		assertTrue(names.contains("C11"));
		
		assertTreeCacheable(getDao().getTreeCacheable(root));	// for special DAO types
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("create tree nodes");
	}
	
	/** Check depth-first order. To be overridden for other capabilities. */
	protected void assertTreeCacheable(List<N> treeList)	{
		assertEquals("ROOT", getName(treeList.get(0)));
		assertEquals("A", getName(treeList.get(1)));
		assertEquals("A1", getName(treeList.get(2)));
		assertEquals("B", getName(treeList.get(3)));
		assertEquals("B1", getName(treeList.get(4)));
		assertEquals("B2", getName(treeList.get(5)));
		assertEquals("C", getName(treeList.get(6)));
		assertEquals("C1", getName(treeList.get(7)));
		assertEquals("C11", getName(treeList.get(8)));
		
		N x = newTreePojo("X");
		Collection<N> nodes = new ArrayList<N>();
		try	{ treeList.add(x); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.add(0, x); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.remove(0); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.remove(x); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.removeAll(nodes); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.addAll(nodes); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.addAll(0, nodes); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.clear(); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.retainAll(nodes); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
		try	{ treeList.set(0, x); fail("Tree list must be unmodifiable"); } catch (RuntimeException e) { /* is expected here */ }
	}
	
	public void testIsRoot() throws Exception	{
		beginDbTransaction("check is root");
		
		N root = getDao().createRoot(newTreePojo("ROOT"));
		assertTrue(getDao().isRoot(root));
		
		N a = getDao().addChild(root, newTreePojo("A"));
		assertTrue(getDao().isRoot(root));
		assertFalse(getDao().isRoot(a));
		
		commitDbTransaction("check is root");
	}
	
	public void testCreateAndRemoveRoot() throws Exception	{
		beginDbTransaction("check create/remove root");
		
		N root = getDao().createRoot(newTreePojo("ROOT"));
		assertEquals(1, getDao().getRoots().size());
		
		getDao().remove(root);
		assertEquals(0, getDao().getRoots().size());
		
		commitDbTransaction("check create/remove root");
	}
	
	public void testUpdateNode() throws Exception	{
		DbSession session = beginDbTransaction("check update nodes");
		
		N root = getDao().createRoot(newTreePojo("ROOT"));
		N a = getDao().addChild(root, newTreePojo("A"));
		
		setName(root, root, "root");
		getDao().update(root);
		root = findByName(root, "root");
		assertNotNull(root);
		assertEquals("root", getName(root));
		
		setName(a, root, "a");
		getDao().update(a);
		a = findByName(root, "a");
		assertNotNull(a);
		assertEquals("a", getName(a));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("check update nodes");
	}

	public void testUpdateToSameNameWithUniqueConstraint() throws Exception	{
		DbSession session = beginDbTransaction("check update to same name");
		getDao().setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());

		N root = getDao().createRoot(newTreePojo("ROOT"));
		N a = getDao().addChild(root, newTreePojo("A"));
		
		final String newAName = "A";
		setName(a, root, newAName);
		dao.update(a);
		a = findByName(root, "A");
		assertNotNull(a);
		
		final String newRootName = "ROOT";
		setName(root, root, newRootName);
		dao.update(root);
		root = findByName(root, "ROOT");
		assertNotNull(root);
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("check update to same name");
	}
	
	public void testUpdateRootWithWholeTreeUniqueConstraint() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique table constraint on name
		
		DbSession session = beginDbTransaction("check update root");
		getDao().setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());

		N root = getDao().createRoot(newTreePojo("ROOT"));
		N a = getDao().addChild(root, newTreePojo("A"));
		
		// test if can rename root to a unique name
		setName(root, root, "root");
		getDao().update(root);
		root = findByName(root, "root");
		assertNotNull(root);
		
		// test if can rename root to a non-unique name
		try	{
			setName(root, root, "A");
			getDao().update(root);
			fail("Can not update root to same name as one of its children");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}

		// test if can rename A to a non-unique name
		try	{
			setName(a, root, "root");
			getDao().update(a);
			fail("Can not update node to same name as its root");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}

		checkTreeIntegrity(session, root);
		
		commitDbTransaction("check update root");
	}

	public void testNonUniqueRootsButWholeTreeUniqueNodes() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique table constraint on name
		
		DbSession session = beginDbTransaction("check non-unique roots");
		getDao().setUniqueTreeConstraint(newUniqueWholeTreeConstraintImplWithoutRoots());

		N root = getDao().createRoot(newTreePojo("ROOT"));
		N a = getDao().addChild(root, newTreePojo("A"));
		
		// test if can create a second root with a same name
		N root2 = getDao().createRoot(newTreePojo("ROOT"));
		getDao().addChild(root2, newTreePojo("A"));
		
		// test if can create a third root with same name
		getDao().createRoot(newTreePojo("ROOT"));
		
		// test if can add a node with a non-unique name to first tree
		try	{
			getDao().addChild(a, newTreePojo("A"));
			fail("Can not add a node with non-unique name");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}

		// test if can add a node with a non-unique name to second tree
		try	{
			getDao().addChild(root2, newTreePojo("A"));
			fail("Can not add a node with non-unique name");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}

		// test if can rename root to same name as its child
		try	{
			setName(root, root, "A");
			fail("Can not rename root to a non-unique name");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}

		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("check non-unique roots");
	}

	public void testNonUniqueRootsButUniqueChildren() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique table constraint on name
		
		DbSession session = beginDbTransaction("check non-unique roots with unique children");
		getDao().setUniqueTreeConstraint(newUniqueChildrenTreeConstraintImplWithoutRoots());
		
		// test if it is possible to create two trees with same root names and same content names
		Serializable rootId1 = createTree("root");
		Serializable rootId2 = createTree("root");
		
		N root1 = getDao().find(rootId1);
		N root2 = getDao().find(rootId2);
		
		// test if can add a node with a non-unique name to a root
		try	{
			getDao().addChild(root2, newTreePojo("B"));
			fail("Can not add a node with non-unique name");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}
		
		// test if can rename root
		setName(root1, root1, "root1");
		setName(root1, root1, "root");

		// test if can rename node to same name as one of its siblings
		N b = findByName(root1, "B");
		try	{
			setName(b, root1, "A");
			fail("Can not rename node to a non-unique name");
		}
		catch (UniqueConstraintViolationException e)	{
			// is expected here
		}
		
		// test if can set a child name to root name
		setName(b, root1, "root");
		
		checkTreeIntegrity(session, root1);
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("check non-unique roots with unique children");
	}
	
	/** Tests reading all roots. */
	public void testGetRoots() throws Exception	{
		beginDbTransaction("check get roots");
		Serializable rootId1 = createTree("root1");
		Serializable rootId2 = createTree("root2");
		
		N root1 = getDao().find(rootId1);
		N root2 = getDao().find(rootId2);
		
		assertTrue(getDao().isRoot(root1));
		assertTrue(getDao().isRoot(root2));
		
		assertEquals(root1, getDao().getRoot(root1));
		assertEquals(root2, getDao().getRoot(root2));
		
		assertNotNull(findByName(root1, "root1"));
		assertNotNull(findByName(root2, "root2"));
		
		assertNodesExist(root1);
		assertNodesExist(root2);
		assertNodesDontExist(getDao().find(rootId1), new String [] { "ROOT" });
		assertNodesDontExist(getDao().find(rootId2), new String [] { "ROOT" });
		
		List<N> roots = getDao().getRoots();
		assertEquals(2, roots.size());
		assertFalse(getName(roots.get(0)).equals(getName(roots.get(1))));
		assertTrue(getName(roots.get(0)).equals("root1") || getName(roots.get(0)).equals("root2"));
		assertTrue(getName(roots.get(1)).equals("root1") || getName(roots.get(1)).equals("root2"));
		
		commitDbTransaction("check get roots");
	}
	
	/** Tests level of all tree nodes. */
	public void testLevel() throws Exception	{
		beginDbTransaction("get level");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		assertEquals(0, getDao().getLevel(root));
		
		N node;
		node = findByName(root, "A");
		assertEquals(1, getDao().getLevel(node));
		node = findByName(root, "B");
		assertEquals(1, getDao().getLevel(node));
		node = findByName(root, "C");
		assertEquals(1, getDao().getLevel(node));
		node = findByName(root, "A1");
		assertEquals(2, getDao().getLevel(node));
		node = findByName(root, "B1");
		assertEquals(2, getDao().getLevel(node));
		node = findByName(root, "B2");
		assertEquals(2, getDao().getLevel(node));
		node = findByName(root, "C1");
		assertEquals(2, getDao().getLevel(node));
		node = findByName(root, "C11");
		assertEquals(3, getDao().getLevel(node));
		
		commitDbTransaction("get level");
	}
	
	/** Tests reading parent of all tree nodes. */
	public void testParent() throws Exception	{
		beginDbTransaction("get parent");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		assertNull(getDao().getParent(root));
		
		N node;
		node = findByName(root, "A");
		assertEquals("ROOT", getName(getDao().getParent(node)));
		node = findByName(root, "B");
		assertEquals("ROOT", getName(getDao().getParent(node)));
		node = findByName(root, "C");
		assertEquals("ROOT", getName(getDao().getParent(node)));
		node = findByName(root, "A1");
		assertEquals("A", getName(getDao().getParent(node)));
		node = findByName(root, "B1");
		assertEquals("B", getName(getDao().getParent(node)));
		node = findByName(root, "B2");
		assertEquals("B", getName(getDao().getParent(node)));
		node = findByName(root, "C1");
		assertEquals("C", getName(getDao().getParent(node)));
		node = findByName(root, "C11");
		assertEquals("C1", getName(getDao().getParent(node)));
		
		commitDbTransaction("get parent");
	}
	
	/** Tests reading parents of tree nodes. */
	public void testPath() throws Exception	{
		beginDbTransaction("get parents");
		createTree();
		
		Map<String,Object> criteria = new Hashtable<String,Object>();
		criteria.put("name", "ROOT");
		List<N> results = getDao().find(null, criteria);
		assertEquals(1, results.size());
		N root = results.get(0);
		assertNull(getDao().getParent(root));
		
		List<N> path;
		path = getDao().getPath(root);
		assertEquals(0, path.size());
		
		N c11 = findByName(root, "C11");
		assertEquals("C11", getName(c11));
		path = getDao().getPath(c11);
		
		assertEquals(3, path.size());
		assertEquals("ROOT", getName(path.get(0)));
		assertEquals("C", getName(path.get(1)));
		assertEquals("C1", getName(path.get(2)));
		
		commitDbTransaction("get parents");
	}
	
	/** Tests reading parent of all tree nodes. */
	public void testIsBelow() throws Exception	{
		beginDbTransaction("is below");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		assertTrue(getDao().isEqualToOrChildOf(root, root));
		
		N node;
		node = findByName(root, "A");
		assertTrue(getDao().isEqualToOrChildOf(node, node));
		assertTrue(getDao().isChildOf(node, root));
		assertFalse(getDao().isChildOf(root, node));
		
		node = findByName(root, "B");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "C");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "A1");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "B1");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "B2");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "C1");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		node = findByName(root, "C11");
		assertTrue(getDao().isEqualToOrChildOf(node, root));
		assertFalse(getDao().isEqualToOrChildOf(root, node));
		
		N c1 = findByName(root, "C1");
		node = findByName(root, "C11");
		assertTrue(getDao().isChildOf(node, c1));
		assertFalse(getDao().isChildOf(c1, node));
		
		commitDbTransaction("is below");
	}
	
	/** Tests adding tree nodes. */
	public void testAddToTree() throws Exception	{
		DbSession session = beginDbTransaction("adding tree nodes");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		List<N> children;
		
		N b2 = findByName(root, "B2");
		getDao().addChildAt(b2, newTreePojo("B23"), 0);
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23" });
		children = getDao().getChildren(b2);
		assertEquals(1, children.size());
		assertEquals("B23", getName(children.get(0)));
		
		getDao().addChildAt(b2, newTreePojo("B21"), 0);
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23", "B21" });
		children = getDao().getChildren(b2);
		assertEquals(2, children.size());
		assertEquals("B21", getName(children.get(0)));
		assertEquals("B23", getName(children.get(1)));
		
		getDao().addChildAt(b2, newTreePojo("B22"), 1);
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23", "B21", "B22" });
		children = getDao().getChildren(b2);
		assertEquals(3, children.size());
		assertEquals("B21", getName(children.get(0)));
		assertEquals("B22", getName(children.get(1)));
		assertEquals("B23", getName(children.get(2)));
		
		N c = findByName(root, "C");
		getDao().addChildAt(c, newTreePojo("C0"), 0);
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23", "B21", "B22", "C0" });
		children = getDao().getChildren(c);
		assertEquals(2, children.size());
		assertEquals("C0", getName(children.get(0)));
		assertEquals("C1", getName(children.get(1)));
		
		getDao().addChildAt(c, newTreePojo("C2"), 2);
		assertNodesExist(root, new String [] { "B23", "B21", "B22", "C0", "C2" });
		children = getDao().getChildren(c);
		assertEquals(3, children.size());
		assertEquals("C0", getName(children.get(0)));
		assertEquals("C1", getName(children.get(1)));
		assertEquals("C2", getName(children.get(2)));
		
		N a = findByName(root, "A");
		N a1Sibling = findByName(root, "A1");
		getDao().addChildBefore(a1Sibling, newTreePojo("A0"));
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23", "B21", "B22", "C0", "C2", "A0" });
		children = getDao().getChildren(a);
		assertEquals(2, children.size());
		assertEquals("A0", getName(children.get(0)));
		assertEquals("A1", getName(children.get(1)));
		
		N a2 = getDao().addChildAt(a, newTreePojo("A2"), -1);
		assertNodesExist(root);
		assertNodesExist(root, new String [] { "B23", "B21", "B22", "C0", "C2", "A0", "A2" });
		children = getDao().getChildren(a);
		assertEquals(3, children.size());
		assertEquals("A0", getName(children.get(0)));
		assertEquals("A1", getName(children.get(1)));
		assertEquals("A2", getName(children.get(2)));
		
		try	{
			getDao().addChild(root, a2);
			fail("Adding existing child must fail!");
		}
		catch (IllegalArgumentException e)	{
			// is expected here
		}
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("adding tree nodes");
	}

	/** Tests removing tree nodes. */
	public void testRemoveFromTree() throws Exception	{
		DbSession session = beginDbTransaction("remove tree nodes");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		List<N> children;
		
		// remove a leaf node
		N b2 = findByName(root, "B2");
		N b2Parent = getDao().getParent(b2);
		getDao().remove(b2);
		assertNodesDontExist(root, new String [] { "B2" });
		assertNodesExist(root, new String [] { "A", "A1", "B", "B1", "C", "C1", "C11" });
		N b = findByName(root, "B");
		assertEquals(b2Parent, b);
		children = getDao().getChildren(b);
		assertEquals(1, children.size());
		assertEquals("B1", getName(children.get(0)));
		
		// remove a container node at end
		N c = findByName(root, "C");
		getDao().remove(c);
		assertNodesDontExist(root, new String [] { "C", "C1", "C11" });
		assertNodesExist(root, new String [] { "A", "A1", "B", "B1" });
		children = getDao().getChildren(root);
		assertEquals(2, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		
		// remove a container node at start
		N a = findByName(root, "A");
		getDao().remove(a);
		assertNodesDontExist(root, new String [] { "C", "C1", "C11", "A", "A1" });
		assertNodesExist(root, new String [] { "B", "B1" });
		children = getDao().getChildren(root);
		assertEquals(1, children.size());
		assertEquals("B", getName(children.get(0)));
		assertFalse(getDao().isLeaf(children.get(0)));
		
		// add a node again, at start of tree
		a = getDao().addChildAt(root, newTreePojo("A"), 0);
		assertNodesExist(root, new String [] { "A", "B", "B1" });
		children = getDao().getChildren(root);
		assertEquals(2, children.size());
		assertTrue(getDao().isLeaf(a));
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		
		// remove from middle of tree
		getDao().remove(b);
		assertNodesDontExist(root, new String [] { "C", "C1", "C11", "B", "B1", "B2", });
		assertNodesExist(root, new String [] { "A", });
		children = getDao().getChildren(root);
		assertEquals(1, children.size());
		assertEquals("A", getName(children.get(0)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("remove tree nodes");
	}

	/** Tests removing tree roots. */
	public void testRemoveTree() throws Exception	{
		DbSession session = beginDbTransaction("remove tree roots");
		Serializable rootId = createTree();
		Serializable rootId3 = createTree("root3");
		
		assertEquals(2, getDao().getRoots().size());
		
		N root = getDao().find(rootId);
		assertNotNull(root);
		checkTreeIntegrity(session, root);
		
		getDao().remove(root);
		assertNodesDontExist(root);
		assertEquals(1, getDao().getRoots().size());
		
		N root3 = getDao().find(rootId3);
		assertNodesExist(root3);
		checkTreeIntegrity(session, root3);
		
		getDao().remove(root3);
		assertNodesDontExist(root);
		assertNodesDontExist(root3);
		assertEquals(0, getDao().getRoots().size());
		
		commitDbTransaction("remove tree roots");
	}

	/** Tests removing all tree roots. */
	public void testRemoveAllTrees() throws Exception	{
		beginDbTransaction("remove all tree roots");
		Serializable rootId1 = createTree("ROOT1");
		Serializable rootId2 = createTree("ROOT2");
		
		N root1 = getDao().find(rootId1);
		N root2 = getDao().find(rootId2);
		assertNodesExist(root1);
		assertNodesExist(root2);
		assertEquals(2, getDao().getRoots().size());
		
		getDao().removeAll();
		assertNodesDontExist(root1);
		assertNodesDontExist(root2);
		assertEquals(0, getDao().getRoots().size());
		
		commitDbTransaction("remove all tree roots");
	}

	/** Tests moving subtrees in a tree. */
	public void testMoveTree() throws Exception	{
		DbSession session = beginDbTransaction("move tree");
		Serializable rootId = createTree();
		
		N root = getDao().find(rootId);
		checkTreeIntegrity(session, root);
		List<N> children;
		
		// move B before A
		N b = findByName(root, "B");
		getDao().moveTo(b, root, 0);
		assertEquals(1, getDao().getRoots().size());
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("B", getName(children.get(0)));
		assertEquals("A", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		// move B before C
		N c = findByName(root, "C");
		getDao().moveBefore(b, c);
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		// move A after B
		N a = findByName(root, "A");
		getDao().moveTo(a, root, 1);
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("B", getName(children.get(0)));
		assertEquals("A", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		// move C before B
		getDao().moveBefore(c, b);
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("C", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("A", getName(children.get(2)));
		
		// move C after A
		getDao().moveTo(c, root, -1);
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("B", getName(children.get(0)));
		assertEquals("A", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		// move A before B
		getDao().moveBefore(a, b);
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		// move A below C1
		N c1 = findByName(root, "C1");
		getDao().move(a, c1);
		children = getDao().getChildren(c1);
		assertEquals(2, children.size());
		assertEquals("C11", getName(children.get(0)));
		assertEquals("A", getName(children.get(1)));
		children = getDao().getChildren(c);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		children = getDao().getChildren(root);
		assertEquals(2, children.size());
		assertEquals("B", getName(children.get(0)));
		assertEquals("C", getName(children.get(1)));
		
		// swap B1 and B2
		b = findByName(root, "B");
		N b1 = findByName(root, "B1");
		getDao().moveTo(b1, b, 1);
		children = getDao().getChildren(b);
		assertEquals(2, children.size());
		assertEquals("B2", getName(children.get(0)));
		assertEquals("B1", getName(children.get(1)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("move tree");
	}
	
	/** Test unique constraint for root and sub-node. */
	public void testUniqueness() throws Exception	{
		DbSession session = beginDbTransaction("check uniqueness");
		N root = getDao().createRoot(newTreePojo("ROOT"));
		
		// try to insert a root with same name
		try	{
			getDao().createRoot(newTreePojo("ROOT"));
			fail("Multiple roots with same name (by creation) should not be allowed!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		
		assertEquals(1, getDao().getRoots().size());
		
		// insert another root
		N otherRoot = getDao().createRoot(newTreePojo("OTHER_ROOT"));
		
		// try to rename it to ROOT with the DAO calling the constraint
		getDao().setCheckUniqueConstraintOnUpdate(true);
		try	{
			setNameNotConstraintChecking(otherRoot, "ROOT");
			getDao().update(otherRoot);
			session.flush();	// make database constraint work in case Java constraint failed
			fail("Multiple roots with same name (by update) should not be allowed!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from update()
			// is expected here
		}
		finally	{
			getDao().setCheckUniqueConstraintOnUpdate(false);
			// we must reset the wrong property manually!
			setNameNotConstraintChecking(otherRoot, "OTHER_ROOT");
		}
		
		assertEquals("OTHER_ROOT", getName(otherRoot));
		
		// try to rename it to ROOT with explicit constraint checking
		try	{
			setName(otherRoot, null, "ROOT");
			getDao().update(otherRoot);
			session.flush();	// make database constraint work in case Java constraint failed
			fail("Multiple roots with same name (by update) should not be allowed!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from setName()
			// is expected here
		}
		
		// check that the update was not already done by the JPA provider
		List<N> roots = getDao().getRoots();
		assertEquals(2, roots.size());
		if (getName(roots.get(0)).equals("ROOT"))
			assertEquals("OTHER_ROOT", getName(roots.get(1)));
		else if (getName(roots.get(0)).equals("OTHER_ROOT"))
			assertEquals("ROOT", getName(roots.get(1)));
		else
			fail("Unknown roots: "+roots);

		// add a child to root
		getDao().addChild(root, newTreePojo("A"));
		
		// following is a test that requires a rollback, so commit everything
		commitDbTransaction("check uniqueness");
		session = beginDbTransaction("check uniqueness");
		root = getDao().find(root.getId());
		assertEquals(2, getDao().getRoots().size());
		
		// try to insert a child with same name into same root
		try	{
			getDao().addChild(root, newTreePojo("A"));
			session.flush();	// make database constraint work in case Java constraint failed
			fail("Multiple nodes with same name within the same root should not be allowed!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here with TemporalNestedSetsTreeTest
		}
		catch (Exception e)	{	// database constraint exception
			rollbackDbTransaction("check uniqueness");
			throw e;
		}
		
		assertEquals(2, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, otherRoot);
		
		List<N> children = getDao().getChildren(root);
		assertEquals(1, children.size());
		assertEquals("A", getName(children.get(0)));
		
		// try to rename ROOT to the name of one of its child nodes 
		try	{
			setName(root, root, "A");
			getDao().update(root);
			session.flush();	// make database constraint work in case Java constraint failed
			fail("Root name must be unique within its tree!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here with TemporalNestedSetsTreeTest
		}
		catch (Exception e)	{	// database constraint exception
			rollbackDbTransaction("check uniqueness");
			throw e;
		}
		
		assertNotNull(root);
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, otherRoot);
		
		// try to insert a child with same name as root
		N a = findByName(root, "A");
		try	{
			getDao().addChildAt(a, newTreePojo("ROOT"), 1);
			session.flush();	// make database constraint work in case Java constraint failed
			fail("Can not add a child with non-unique name!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here with TemporalNestedSetsTreeTest
		}
		catch (Exception e)	{	// database constraint exception
			rollbackDbTransaction("check uniqueness");
			throw e;
		}
		
		commitDbTransaction("check uniqueness");
	}
	
	/** Test unique constraint together with deletion, special for temporal extension. */
	public void testUniquenessWithDeletion() throws Exception	{
		DbSession session = beginDbTransaction("check uniqueness with delete");
		
		N root = getDao().createRoot(newTreePojo("ROOT"));
		
		getDao().remove(root);	// remove it again
		assertEquals(0, getDao().getRoots().size());
		
		// insert a root with same name, must be possible
		root = getDao().createRoot(newTreePojo("ROOT"));
		N a = getDao().addChild(root, newTreePojo("A"));
		assertEquals(2, getDao().size(root));
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		getDao().remove(a);	// remove it again
		assertEquals(1, getDao().size(root));
		
		// insert a child with same name into root
		getDao().addChild(root, newTreePojo("A"));
		assertEquals(2, getDao().size(root));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("check uniqueness with delete");
	}

	/** Test move a sub-tree below itself. */
	public void testMoveTreeBelowItselfFails() throws Exception	{
		beginDbTransaction("move subtree below itself");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		N b = findByName(root, "B");
		N b1 = findByName(root, "B1");	// B1 is below B
		try	{
			getDao().move(b, b1);
			fail("DAO check must prevent this!");
		}
		catch (IllegalArgumentException e)	{
			// else: is expected here
		}
		finally	{
			rollbackDbTransaction("move subtree below itself");
		}
	}
	
	/** Test moving sub-trees to other roots. */
	public void testMoveToOtherTree() throws Exception	{
		DbSession session = beginDbTransaction("move to other tree");
		Serializable rootId1 = createTree("ROOT1", false);	// upper case names
		Serializable rootId2 = createTree("root2", true);	// lower case names
		
		assertEquals(2, getDao().getRoots().size());
		
		N root1 = getDao().find(rootId1);
		assertEquals(9, getDao().size(root1));
		N root2 = getDao().find(rootId2);
		assertEquals(9, getDao().size(root2));
		
		// move "B" from root1 before "a" in root2
		N b = findByName(root1, "B");
		getDao().moveTo(b, root2, 0);	// moves the B subtree to position 0 under root2
		assertEquals(2, getDao().getRoots().size());
		assertEquals(6, getDao().size(root1));
		assertEquals(12, getDao().size(root2));
		checkTreeIntegrity(session, root1);
		checkTreeIntegrity(session, root2);
		
		List<N> children;
		children = getDao().getChildren(root1);
		assertEquals(2, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("C", getName(children.get(1)));
		
		children = getDao().getChildren(root2);
		assertEquals(4, children.size());
		assertEquals("B", getName(children.get(0)));
		assertEquals("a", getName(children.get(1)));
		assertEquals("b", getName(children.get(2)));
		assertEquals("c", getName(children.get(3)));
		
		// move "c1" from root2 before "A" in root1
		N c1 = findByName(root2, "c1");
		N a1 = findByName(root1, "A1");
		getDao().moveBefore(c1, a1);	// moves the c1 subtree before A1
		assertEquals(8, getDao().size(root1));
		assertEquals(10, getDao().size(root2));
		
		N a = findByName(root1, "A");
		assertNotNull(a);
		children = getDao().getChildren(a);
		assertEquals(2, children.size());
		assertEquals("c1", getName(children.get(0)));
		assertEquals("A1", getName(children.get(1)));
		c1 = findByName(root1, "c1");
		assertNotNull(c1);
		children = getDao().getChildren(c1);
		assertEquals(1, children.size());
		assertEquals("c11", getName(children.get(0)));
		
		commitDbTransaction("move to other tree");
	}

	/** Test moving sub-trees to other roots. */
	public void testMoveRootToOtherTree() throws Exception	{
		DbSession session = beginDbTransaction("move root to other tree");
		Serializable rootId1 = createTree("ROOT1", false);	// upper case names
		Serializable rootId2 = createTree("root2", true);	// lower case names
		
		N root1 = getDao().find(rootId1);
		N root2 = getDao().find(rootId2);
		assertEquals(2, getDao().getRoots().size());
		
		// move root2 before "C11" in root1
		N c1 = findByName(root1, "C1");
		getDao().move(root2, c1);	// moves root2 to last position in C1 under root1
		
		assertEquals(1, getDao().getRoots().size());	// root2 must have been gone
		assertEquals(18, getDao().size(root1));
		checkTreeIntegrity(session, root1);
		
		List<N> children;
		children = getDao().getChildren(c1);
		assertEquals(2, children.size());
		assertEquals("C11", getName(children.get(0)));
		assertEquals("root2", getName(children.get(1)));
		
		root2 = findByName(root1, "root2");
		assertEquals(9, getDao().size(root2));
		children = getDao().getChildren(root2);
		assertEquals(3, children.size());
		assertEquals("a", getName(children.get(0)));
		assertEquals("b", getName(children.get(1)));
		assertEquals("c", getName(children.get(2)));
				
		commitDbTransaction("move root to other tree");
	}
	
	/** Test moving tree to be roots. */
	public void testMoveTreeToBeRoot() throws Exception	{
		DbSession session = beginDbTransaction("move subtree to be root");
		Serializable rootId = createTree("ROOT1");
		
		assertEquals(1, getDao().getRoots().size());
		
		// move "C" to be a root
		N root = getDao().find(rootId);
		N c = findByName(root, "C");
		assertNotNull(c);
		getDao().moveToBeRoot(c);
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, c);
		
		// assert that new root exists
		assertEquals(2, getDao().getRoots().size());
		
		// and all children have gone to new root
		assertEquals(3, getDao().size(c));
		List<N> children;
		children = getDao().getChildren(c);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		children = getDao().getChildren(children.get(0));
		assertEquals(1, children.size());
		assertEquals("C11", getName(children.get(0)));
		
		// assert that the subtree has disappeared from its originator
		children = getDao().getChildren(root);
		assertEquals(2, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		children = getDao().getChildren(children.get(0));
		assertEquals(1, children.size());
		
		// move "A1" to be a root
		N a1 = findByName(root, "A1");
		assertNotNull(a1);
		getDao().moveToBeRoot(a1);
		
		checkTreeIntegrity(session, a1);
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, c);
		
		// assert that new roots exists
		assertEquals(3, getDao().getRoots().size());
		children = getDao().getChildren(a1);
		assertEquals(0, children.size());
		// and has disappeared from its parent
		N a = findByName(root, "A");
		children = getDao().getChildren(a);
		assertEquals(0, children.size());
		
		// test that nothing happens when moving a root to be root
		getDao().moveToBeRoot(a1);
		assertEquals(3, getDao().getRoots().size());
		
		checkTreeIntegrity(session, a1);
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, c);
		
		commitDbTransaction("move subtree to be root");
	}
	
	/** Test copy a sub-tree. Unique constraint on database level will prevent this. */
	public void testCopyTreeFailsWithUniqueConstraint() throws Exception	{
		DbSession session = beginDbTransaction("copy subtree");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		N b = findByName(root, "B");
		N c = findByName(root, "C");
		try	{
			getDao().copy(b, c, null);	// copy B to children list of C
			session.flush();	// make database constraint work
			fail("Unique constraint must prevent this!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		catch (Exception e)	{
			if (e.toString().toLowerCase().contains("violation") == false)
				throw e;
			// else: is expected here
		}
		finally	{
			rollbackDbTransaction("copy subtree");
		}
	}
	
	/** Test copy a sub-tree without unique constraint on database level. */
	public void testCopyTree() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		// mind that the findByName method might not work anymore!
		
		DbSession session = beginDbTransaction("copy subtree");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N b = findByName(root, "B");
		N c = findByName(root, "C");
		
		N bClone = getDao().copy(b, c, null);	// copy B to children list of C
		checkTreeIntegrity(session, root);
		
		assertEquals(12, getDao().size(root));	// 3 nodes have been copied: B, B1, B2
		assertEquals(2, getDao().getLevel(bClone));
		
		List<N> children;
		children = getDao().getChildren(c);
		assertEquals(2, children.size());
		assertEquals("C1", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		
		// check children of C1
		children = getDao().getChildren(children.get(0));
		assertEquals(1, children.size());
		assertEquals("C11", getName(children.get(0)));
		
		// check children of B
		children = getDao().getChildren(bClone);
		assertEquals(2, children.size());
		assertEquals("B1", getName(children.get(0)));
		assertEquals("B2", getName(children.get(1)));
		assertEquals(3, getDao().getLevel(children.get(0)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("copy subtree");
	}
	
	/** Test copy a sub-tree without unique constraint on database level. */
	public void testCopyTreeWithPrecedingDelete() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		// mind that the findByName method might not work anymore!
		
		DbSession session = beginDbTransaction("copy subtree with delete");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N b1 = findByName(root, "B1");
		getDao().remove(b1);	// remove B1 from B
		assertEquals(8, getDao().size(root));	// 1 node has been removed: B1
		
		N b = findByName(root, "B");
		N c = findByName(root, "C");
		
		N bClone = getDao().copy(b, c, null);	// copy B to children list of C
		
		checkTreeIntegrity(session, root);
		assertEquals(10, getDao().size(root));	// 2 nodes have been copied: B, B2
		
		// check children of B below C
		List<N> children = getDao().getChildren(bClone);
		assertEquals(1, children.size());
		assertEquals("B2", getName(children.get(0)));
		
		commitDbTransaction("copy subtree with delete");
	}
	
	/** Test copy a sub-tree to another name. */
	public void testCopyTreeToAlteredName() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		DbSession session = beginDbTransaction("copy subtree to altered name");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N a = findByName(root, "A");
		N c = findByName(root, "C");
		@SuppressWarnings("unchecked")
		N aTemplate = (N) a.clone();
		setName(aTemplate, root, COPIED_NAME_PREFIX+"A");
		
		N aClone = getDao().copy(a, c, aTemplate);	// copy A to children list of C
		
		assertEquals(COPIED_NAME_PREFIX+"A", getName(aClone));
		assertEquals(11, getDao().size(root));	// 2 nodes have been copied: A, A1
		List<N> children = getDao().getChildren(c);
		assertEquals(2, children.size());
		assertEquals("C1", getName(children.get(0)));
		assertEquals(COPIED_NAME_PREFIX+"A", getName(children.get(1)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("copy subtree to altered name");
	}
	
	/** Test copy a sub-tree to another name with unique constraint turned on. */
	public void testCopyTreeToAlteredNameWithUniqueConstraint() throws Exception	{
		DbSession session = beginDbTransaction("copy subtree to altered name with unique constraint");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N a1 = findByName(root, "A1");
		// this has no children, so it must be copyable to another name even with unique constraint
		N c = findByName(root, "C");
		@SuppressWarnings("unchecked")
		N a1Template = (N) a1.clone();
		setName(a1Template, root, COPIED_NAME_PREFIX+"A1");
		
		N a1Clone = getDao().copyBefore(a1, c, a1Template);	// copy A1 to position of C
		
		assertEquals(COPIED_NAME_PREFIX+"A1", getName(a1Clone));
		assertEquals(10, getDao().size(root));	// 1 node has been copied: A1
		List<N> children = getDao().getChildren(root);
		assertEquals(4, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals(COPIED_NAME_PREFIX+"A1", getName(children.get(2)));
		assertEquals("C", getName(children.get(3)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("copy subtree to altered name with unique constraint");
	}
	
	/** Test copy a sub-tree below itself. */
	public void testCopyTreeBelowItselfFails() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		beginDbTransaction("copy subtree below itself");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		N b = findByName(root, "B");
		N b1 = findByName(root, "B1");	// B1 is below B
		try	{
			getDao().copy(b, b1, null);
			fail("DAO check must prevent this!");
		}
		catch (IllegalArgumentException e)	{
			// else: is expected here
		}
		finally	{
			rollbackDbTransaction("copy subtree below itself");
		}
	}
	
	/** Test copy a sub-tree to itself, which is cloning a tree. */
	public void testCopyToSelf() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		DbSession session = beginDbTransaction("copy subtree to itself");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		N c = findByName(root, "C");
		N cClone = getDao().copyBefore(c, c, null);	// copy C
		
		assertEquals(12, getDao().size(root));	// 3 nodes have been copied: C, C1, C11
		assertEquals(1, getDao().getLevel(cClone));
		
		List<N> children;
		children = getDao().getChildren(root);
		assertEquals(4, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		assertEquals("C", getName(children.get(3)));
		
		children = getDao().getChildren(c);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		children = getDao().getChildren(children.get(0));
		assertEquals(1, children.size());
		assertEquals("C11", getName(children.get(0)));
		
		assertEquals(1, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("copy subtree to itself");
	}

	/** Test copy a sub-tree to another root. */
	public void testCopyToOtherRoot() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		DbSession session = beginDbTransaction("copy to another root");
		Serializable rootId1 = createTree("ROOT", false);
		Serializable rootId2 = createTree("root", true);
		
		N root1 = getDao().find(rootId1);
		N root2 = getDao().find(rootId2);
		N c = findByName(root1, "C");
		N a = findByName(root2, "a");
		
		N cClone = getDao().copyTo(c, a, 0, null);	// copy "C" to "a" at position 0
		
		assertEquals(12, getDao().size(root2));	// 3 nodes have been copied: C, C1, C11
		assertEquals(2, getDao().getLevel(cClone));
		
		List<N> children;
		children = getDao().getChildren(a);
		assertEquals(2, children.size());
		assertEquals("C", getName(children.get(0)));
		assertEquals("a1", getName(children.get(1)));
		children = getDao().getChildren(cClone);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		children = getDao().getChildren(children.get(0));
		assertEquals("C11", getName(children.get(0)));
		
		assertEquals(2, getDao().getRoots().size());
		checkTreeIntegrity(session, root1);
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("copy to another root");
	}

	/** Test copying trees to be roots. */
	public void testCopyTreeToBeRoot() throws Exception	{
		DbSession session = beginDbTransaction("copy subtree to be root");
		Serializable rootId = createTree("ROOT1");
		
		assertEquals(1, getDao().getRoots().size());
		
		// move "C" to be a root
		N root = getDao().find(rootId);
		N c = findByName(root, "C");
		assertNotNull(c);
		N newRoot = getDao().copyToBeRoot(c, null);
		
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, newRoot);
		
		// assert that new root exists
		assertEquals(2, getDao().getRoots().size());
		
		// and all children have been copied to new root
		assertEquals(3, getDao().size(newRoot));
		List<N> children;
		children = getDao().getChildren(newRoot);
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		children = getDao().getChildren(children.get(0));
		assertEquals(1, children.size());
		assertEquals("C11", getName(children.get(0)));
		
		// assert that the subtree still exists in its originator
		children = getDao().getChildren(root);
		assertEquals(3, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		children = getDao().getChildren(children.get(2));
		assertEquals(1, children.size());
		assertEquals("C1", getName(children.get(0)));
		
		commitDbTransaction("copy subtree to be root");
	}
	
	/** Test copying root to be another root. */
	public void testCopyRootToBeRoot() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		DbSession session = beginDbTransaction("copy root to be root");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		
		// copy a root to be another root
		N newRoot = getDao().copyToBeRoot(root, null);
		assertEquals(2, getDao().getRoots().size());
		assertTrue(getDao().isRoot(newRoot));
		assertFalse(root.equals(newRoot));
		List<N> children;
		children = getDao().getChildren(newRoot);
		assertEquals(3, children.size());
		assertEquals("A", getName(children.get(0)));
		assertEquals("B", getName(children.get(1)));
		assertEquals("C", getName(children.get(2)));
		
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, newRoot);
		
		commitDbTransaction("copy root to be root");
	}
	
	/** Test renaming nodes while copying. */
	public void testCopiedNodeRenamer() throws Exception	{
		DbSession session = beginDbTransaction("copied node renamer");
		
		// define a node renamer
		getDao().setCopiedNodeRenamer(new TreeDao.CopiedNodeRenamer<N>() {
			@Override
			public void renameCopiedNode(N node) {
				AbstractTreeTest.this.renameBeforeCopy(node);
			}
		});
		
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		
		// copy a tree using the node renamer
		N b = findByName(root, "B");
		assertNotNull(b);
		
		getDao().copyTo(b, root, 1, null);
		
		List<N> children;
		children = getDao().getChildren(root);
		assertEquals(4, children.size());
		assertEquals("A", getName(children.get(0)));
		N bCopy = children.get(1);
		assertEquals(COPIED_NAME_PREFIX+"B", getName(bCopy));	// "Copy of" must be identical with implementations in derived tests
		assertEquals("B", getName(children.get(2)));
		assertEquals("C", getName(children.get(3)));
		
		children = getDao().getChildren(bCopy);
		assertEquals(2, children.size());
		assertEquals(COPIED_NAME_PREFIX+"B1", getName(children.get(0)));
		assertEquals(COPIED_NAME_PREFIX+"B2", getName(children.get(1)));
		
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("copied node renamer");
	}
	
	/**
	 * Demonstrates how to retrieve the tree structure from a list of all nodes under a root.
	 */
	public void testFindChildrenInCachedTree() throws Exception	{
		beginDbTransaction("find children");
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N c = findByName(root, "C");
		
		// make some modifications to provoke left-index gaps
		N c1 = getDao().getChildren(c).get(0);
		getDao().remove(c1);	// C11 is removed now too
		assertEquals(7, getDao().size(root));
		
		// add them again
		c1 = getDao().addChild(c, cloneValid(c1));	// clone: do not use any deleted entity after deletion!
		getDao().addChild(c1, newTreePojo("C11"));
		assertEquals(9, getDao().size(root));
		
		N c2 = newTreePojo("C2");
		c2 = getDao().addChild(c, c2);
		getDao().remove(c2);
		getDao().addChild(c, cloneValid(c2));
		assertEquals(10, getDao().size(root));	// C2 is new
		
		N cx = newTreePojo("CX");
		cx = getDao().addChild(c, cx);
		assertEquals(11, getDao().size(root));	// CX is new
		
		N c3 = newTreePojo("C3");
		c3 = getDao().addChild(c, c3);
		getDao().remove(c3);
		
		//setValid(c3);
		//getDao().addChild(c, c3);
		getDao().addChild(c, cloneValid(c3));
		assertEquals(12, getDao().size(root));	// C3 is new

		getDao().remove(cx);
		assertEquals(11, getDao().size(root));
		
		N cy = newTreePojo("CY");
		cy = getDao().addChild(c, cy);
		getDao().remove(cy);
		
		// now read subtree and filter for children
		List<N> tree = getDao().getTreeCacheable(root);
		assertEquals(11, tree.size());
		
		List<N> subTree = getDao().findSubTree(c, tree);
		assertEquals(5, subTree.size());
		assertEquals("C", getName(subTree.get(0)));
		assertEquals("C1", getName(subTree.get(1)));
		assertEquals("C11", getName(subTree.get(2)));
		assertEquals("C2", getName(subTree.get(3)));
		assertEquals("C3", getName(subTree.get(4)));
		
		List<N> children = getDao().findDirectChildren(subTree);
		assertEquals(3, children.size());
		assertEquals("C1", getName(children.get(0)));
		assertEquals("C2", getName(children.get(1)));
		assertEquals("C3", getName(children.get(2)));
		
		commitDbTransaction("find children");
	}
	
	/** Test unique constraint for children. */
	public void testUniqueWholeTreeConstraintOnMove() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique table constraint on name
		
		DbSession session = beginDbTransaction("unique whole tree constraint on move");
		getDao().setUniqueTreeConstraint(newUniqueWholeTreeConstraintImpl());
		Serializable rootId = createTree("ROOT", false);	// first tree
		Serializable rootId2 = createTree("root", true);	// second tree
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		N root2 = getDao().find(rootId2);
		assertEquals(9, getDao().size(root));
		
		N B = findByName(root, "B");
		// add a new child to tree which would be non-unique in second tree
		getDao().addChild(B, newTreePojo("b"));
		assertEquals(10, getDao().size(root));
		
		// move B with new child to second tree where it is non-unique
		N c1 = findByName(root2, "c1");
		try	{
			getDao().moveTo(B, c1, 1);
			fail("Unique whole tree constraint doesn't work on move to other tree!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		
		assertEquals(10, getDao().size(root));
		assertEquals(9, getDao().size(root2));
		checkTreeIntegrity(session, root);
		checkTreeIntegrity(session, root2);
		
		commitDbTransaction("unique whole tree constraint on move");
	}
	
	/** Test unique constraint for children. */
	public void testUniqueChildrenConstraint() throws Exception	{
		testCopy = true;	// need another POJO class that has no unique constraint on name
		
		DbSession session = beginDbTransaction("unique children constraint");
		getDao().setUniqueTreeConstraint(newUniqueChildrenTreeConstraintImpl());
		Serializable rootId = createTree("ROOT");
		
		N root = getDao().find(rootId);
		assertEquals(9, getDao().size(root));
		
		N b = findByName(root, "B");
		N c = findByName(root, "C");
		
		// rename some node to be non-unique
		try	{
			setName(b, root, "C");
			fail("Unique children constraint doesn't work on rename!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		
		// move some node before a sibling within tree
		getDao().moveBefore(c, b);	// move C before B
		checkTreeIntegrity(session, root);
		
		// copy some unique node
		getDao().copy(b, c, null);	// copy B to children list of C
		assertEquals(12, getDao().size(root));	// 3 nodes have been copied: B, B1, B2
		checkTreeIntegrity(session, root);
		
		// copy B again to C, where now is a non-unique place
		try	{
			getDao().copy(b, c, null);	// once more
			fail("Unique children constraint doesn't work on copy!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		checkTreeIntegrity(session, root);
		
		// add a unique node
		N c2 = getDao().addChild(c, newTreePojo("C2"));
		assertEquals(13, getDao().size(root));	// 1 node has been added
		checkTreeIntegrity(session, root);
		
		// move a new node to new parent within tree
		getDao().moveTo(c2, b, 1);	// move C2 to B
		checkTreeIntegrity(session, root);
		
		// add a non-unique node
		try	{
			getDao().addChild(c, newTreePojo("B"));
			fail("Unique children constraint doesn't work on addChild!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		
		// add a unique root
		N root2 = getDao().createRoot(newTreePojo("root"));
		assertEquals(2, getDao().getRoots().size());
		checkTreeIntegrity(session, root2);

		// add a non-unique root
		try	{
			getDao().createRoot(newTreePojo("ROOT"));
			fail("Unique children constraint doesn't work on createRoot!");
		}
		catch (UniqueConstraintViolationException e)	{	// is thrown from Java uniqueness check
			// is expected here
		}
		
		assertEquals(2, getDao().getRoots().size());
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("unique children constraint");
	}

	/** Create a big tree. */
	public void testBigTree() throws UniqueConstraintViolationException	{
		DbSession session = beginDbTransaction("build big tree");
		
		long time = System.currentTimeMillis();
		final N root = createBigTree("Big Tree Root");
		session.flush();
		System.out.println("Built big tree in seconds: "+Math.round(((double) System.currentTimeMillis() - (double) time) / 1000));
		// ClosureTableDao needed 25 seconds for 341 nodes
		// NestedSetsTreeDao needed 12 seconds for 341 nodes
		System.out.println("Count of nodes in big tree is: "+getDao().size(root));

		checkTreeIntegrity(session, root);
		
		copyInBigTree(root);
		checkTreeIntegrity(session, root);
		
		moveInBigTree(root);
		checkTreeIntegrity(session, root);
		
		removeInBigTree(root);
		checkTreeIntegrity(session, root);
		
		commitDbTransaction("build big tree");
	}
	
	// end of tests
	
	
	/** @return an UniqueTreeConstraint implementation that checks for uniqueness per tree. */
	protected abstract UniqueTreeConstraint<N> newUniqueWholeTreeConstraintImpl();

	/** @return an UniqueTreeConstraint implementation that checks for uniqueness per tree. */
	protected abstract UniqueTreeConstraint<N> newUniqueWholeTreeConstraintImplWithoutRoots();

	/** @return an UniqueTreeConstraint implementation that checks for uniqueness on child level only. */
	protected abstract UniqueTreeConstraint<N> newUniqueChildrenTreeConstraintImpl();
	
	/** @return an UniqueTreeConstraint implementation that checks for uniqueness on child level only, ignoring uniqueness among roots. */
	protected abstract UniqueTreeConstraint<N> newUniqueChildrenTreeConstraintImplWithoutRoots();
	
	
	/** Overridden to clean-up test table(s). */
	@Override
	protected void tearDown() throws Exception {
		beginDbTransaction("cleanup database");
		getDao().removeAll();
		commitDbTransaction("cleanup database");
		
		super.tearDown();
	}
	
	// test helpers
	
	/** @return a new DAO for passed session. To be overridden by subclasses. */
	protected abstract D newDao(DbSession session);
	
	/** @return the current session DAO. */
	protected final D getDao() {
		return dao;
	}

	/** Factory method for new NestedSetsTreePojos. To be overridden by subclasses. */
	protected abstract N newTreePojo(String name);

	/** For a concrete POJO we must return properties from it. Here the cast to NestedSetsTreePojo is done. */
	protected abstract String getName(N node);
	
	/** Name is unique, so a check is done here explicitly before calling <code>entity.setName()</code>. */
	protected final void setName(N entity, N root, String name) throws UniqueConstraintViolationException {
		N clone = newTreePojo(name);
		// no other properties than name will be checked, so we set no other properties to the clone
		getDao().checkUniqueConstraint(clone, root, entity);
		// throws UniqueConstraintViolationException
		
		setNameNotConstraintChecking(entity, name);	// no exception has been thrown
	}
	
	
	protected abstract void setNameNotConstraintChecking(N entity, String name);

	protected abstract void checkTreeIntegrity(DbSession session, N root);
	
	protected List<N> getFullTreeForIntegrityCheck(N root)	{
		return getDao().getTree(root);
	}
	
	
	protected final boolean isTestCopy()	{
		return testCopy;
	}

	
	protected abstract void renameBeforeCopy(N node);

	
	protected final DbSession beginDbTransaction(String message) {
		final DbSession session = newDbSession(message);
		this.dao = newDao(session);
		return session;
	}
	
	protected DbSession newDbSession(String message) {
		return new DbSessionJpaImpl(beginTransaction(message));
	}
	
	protected void commitDbTransaction(String message) {
		commitTransaction(message);
	}

	protected void rollbackDbTransaction(String message) {
		rollbackTransaction(message);
	}

	
	protected final N findByName(N root, String name)	{
		Map<String,Object> namedValues = new HashMap<String,Object>();
		namedValues.put("name", name);
		List<N> result = getDao().find(root, namedValues);
		
		if (result.size() > 1)
			throw new IllegalStateException("More than one results: "+result);
		else if (result.size() == 1)
			return result.get(0);
		
		return null;
	}
	
	protected final Serializable createTree() throws UniqueConstraintViolationException	{
		return createTree("ROOT");
	}
	
	protected final Serializable createTree(String rootName) throws UniqueConstraintViolationException	{
		return createTree(rootName, false);
	}
	
	protected final Serializable createTree(String rootName, boolean useLowerCaseNames) throws UniqueConstraintViolationException	{
		//DbSessionImpl session = beginDbTransaction("insert tree nodes");
		
		N root = getDao().createRoot(newTreePojo(rootName));
		
		N a = getDao().addChild(root, newTreePojo(useLowerCaseNames ? "a" : "A"));
		N b = getDao().addChild(root, newTreePojo(useLowerCaseNames ? "b" : "B"));
		N c = getDao().addChild(root, newTreePojo(useLowerCaseNames ? "c" : "C"));
		getDao().addChild(b, newTreePojo(useLowerCaseNames ? "b1" : "B1"));
		getDao().addChild(b, newTreePojo(useLowerCaseNames ? "b2" : "B2"));
		getDao().addChild(a, newTreePojo(useLowerCaseNames ? "a1" : "A1"));
		N c1 = getDao().addChild(c, newTreePojo(useLowerCaseNames ? "c1" : "C1"));
		getDao().addChild(c1, newTreePojo(useLowerCaseNames ? "c11" : "C11"));
		outputTree(root, getDao());
		
		//commitDbTransaction(session, "insert tree nodes");
		return root.getId();
	}

	/** To be overridden by temporal test. */
	@SuppressWarnings("unused")
	protected void setValid(N entity) {
	}

	
	private N cloneValid(N node)	{
		@SuppressWarnings("unchecked")
		N clone = (N) node.clone();
		setValid(clone);
		return clone;
	}
	
	private void assertNodesExist(N root, String [] names) throws IllegalStateException {
		for (String name : names)
			assertNotNull(findByName(root, name));
	}
	
	private void assertNodesDontExist(N root, String [] names) {
		for (String name : names)	{
			try	{
				if (findByName(root, name) != null)
					fail("Node still exists: "+name);
			}
			catch (IllegalStateException e)	{
				// thrown when object does not exist, which is intended here
			}
		}
	}
	
	private void assertNodesExist(N root) throws IllegalStateException {
		String [] names = new String [] {
				getName(root), "A", "B", "C", "A1", "B1", "B2", "C1", "C11",
		};
		assertNodesExist(root, names);
	}

	private void assertNodesDontExist(N root) {
		String [] names = new String [] {
				getName(root), "A", "B", "C", "A1", "B1", "B2", "C1", "C11",
		};
		assertNodesDontExist(root, names);
	}

	
	// big tree test
	
	/**
	 * Creates a tree of depth 4, with each folder having 4 children,
	 * named by dotted positional numbers ("chapter numbering").
	 * Thus there will be following nodes (depth first traversal):
	 * 1, 1.1, 1.1.1, 1.1.1.1, 1.1.1.2, ..., 1.1.2.1, ..., 1.2, 1.2.1, ...
	 * 2, ..., 3, ..., 4, ..., 4.4.4.4
	 */
	private N createBigTree(String rootName) throws UniqueConstraintViolationException	{
		final N root = getDao().createRoot(newTreePojo(rootName));
		addChildren(root, "", 4, 4);
		// 4, 4 -> 341 nodes 
		// 3, 10 -> 1111 nodes, flat tree
		// 8, 3 -> 9841 nodes, deep tree
		
		assertEquals(341, getDao().size(root));
		assertEquals(4, getDao().getChildren(root).size());
		final N leaf = findByName(root, "1.1.1.1");
		assertEquals(4, getDao().getLevel(leaf));
		
		return root;
	}

	private void addChildren(N parent, String prefix, int depth, int childCount) throws UniqueConstraintViolationException {
		final List<N> children = new ArrayList<N>();
		for (int i = 1; i <= childCount; i++)
			children.add(newTreePojo((prefix.length() == 0 ? "" : prefix+".")+i));
		
		for (N child : children)	{
			child = getDao().addChild(parent, child);
			if (depth > 1)
				addChildren(child, getName(child), depth - 1, childCount);
		}
	}

	private void copyInBigTree(N root) throws UniqueConstraintViolationException	{
		// copy a leaf
		
		final N leafNode = findByName(root, "4.4.1.4");
		assertTrue(getDao().isLeaf(leafNode));
		final int originalSize = getDao().size(root);
		
		// use a copy-template to avoid unique constraint violation
		final N copiedLeaf = getDao().copyBefore(leafNode, leafNode, newTreePojo("4.4.1.3-4"));
		assertEquals(originalSize + 1, getDao().size(root));
		assertEquals(copiedLeaf, getDao().getChildren(getDao().getParent(leafNode)).get(3));
		
		// copy a folder
		
		final N folderNode = findByName(root, "2.2");
		assertFalse(getDao().isLeaf(folderNode));
		final int folderSize = getDao().size(folderNode);
		// need a copy-renamer to avoid unique constraint violations
		getDao().setCopiedNodeRenamer(new TreeDao.CopiedNodeRenamer<N>()	{
			@Override
			public void renameCopiedNode(N node) {
				setNameNotConstraintChecking(node, "_"+getName(node));
			}
		});
		
		final N copiedFolder = getDao().copy(folderNode, leafNode, null);
		assertEquals(originalSize + 1 + folderSize, getDao().size(root));
		assertEquals(copiedFolder, getDao().getChildren(leafNode).get(0));
	}
		
	private void moveInBigTree(N root) throws UniqueConstraintViolationException	{
		// move a leaf
		
		final N leafNode = findByName(root, "1.1.1.1");
		assertTrue(getDao().isLeaf(leafNode));
		final int originalSize = getDao().size(root);
		final N targetParent = findByName(root, "1.1.2");
		
		getDao().move(leafNode, targetParent);	// append to end
		assertEquals(originalSize, getDao().size(root));
		assertEquals(leafNode, getDao().getChildren(targetParent).get(4));
		
		// move a folder
		final N folderNode = findByName(root, "4.4");
		assertFalse(getDao().isLeaf(folderNode));
		
		getDao().moveTo(folderNode, targetParent, 2);
		assertEquals(originalSize, getDao().size(root));
		assertEquals(folderNode, getDao().getChildren(targetParent).get(2));
	}
	
	private void removeInBigTree(N root)	{
		// remove a leaf
		
		final N leafNode = findByName(root, "3.3.3.3");
		assertTrue(getDao().isLeaf(leafNode));
		final int originalSize = getDao().size(root);
		final N leafParent = getDao().getParent(leafNode);
		
		getDao().remove(leafNode);
		assertEquals(originalSize - 1, getDao().size(root));
		assertFalse(getName(getDao().getChildren(leafParent).get(2)).equals("3.3.3.3"));
		
		// remove a folder
		
		final N folderNode = findByName(root, "1");
		assertFalse(getDao().isLeaf(folderNode));
		final int folderSize = getDao().size(folderNode);
		final N folderParent = getDao().getParent(folderNode);
		
		getDao().remove(folderNode);
		assertEquals(originalSize - 1 - folderSize, getDao().size(root));
		assertFalse(getName(getDao().getChildren(folderParent).get(0)).equals("1"));
	}

}
