package fri.util.database.jpa.tree.uniqueconstraints;

import java.util.List;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.TreeDao;
import fri.util.database.jpa.tree.TreeNode;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Implementers check unique constraints in a tree-table.
 * This interface expects the constructor of the implementation to receive
 * names of properties that have to be unique.
 * <p/>
 * Mind that this constraint implementation must be applied BEFORE setXxx() is called
 * on a domain object (POJO), as the JPA container might flush the setXxx() call to
 * database before the checker query is launched (then, of course, the wrong value
 * will be found by the query). Thus the application MUST call
 * <code>treeDao.checkUniqueConstraint()</code> BEFORE updating an unique property,
 * and it must do this using a clone of the persistent entity.
 * See <code>AbstractTreeTest.setName()</code> unit test method for an example how to do this.
 * 
 * @author Fritz Ritzberger, 20.10.2011
 */
public interface UniqueTreeConstraint<N extends TreeNode>
{
	/**
	 * The implementation of this is expected to check the uniqueness of passed node(s).
	 * Called by the DAO when addChild(), move() or copy() is performed.
	 * Mind that you must call <code>treeDao.checkUniqueConstraint()</code>
	 * explicitly when updating a node property, see explanation in header comment!
	 * @param candidates one (default) or more (copy/move only) entities that hold values
	 * 		to check for uniqueness at location, when list, the copied node will be first,
	 * 		then its sub-nodes.
	 * @param location the information where the pending modification is going to happen.
	 * @return false when unique constraint would be violated, else true.
	 */
	boolean checkUniqueConstraint(List<N> candidates, TreeActionLocation<N> location);
	
	/**
	 * Called by the DAO to publish context information to this constraint-checker.
	 * @param dao the calling DAO, provides tree access for reading children, parents, etc.
	 * @param nodeEntityName name of the JPQL node database table.
	 * @param pathEntityName optionally the name of the JPQL tree-path database table, can be null, this is for ClosureTable only.
	 * @param session the database session to use for uniqueness checks.
	 */
	void setContext(DbSession session, TreeDao<N> dao, String nodeEntityName, String pathEntityName);

}
