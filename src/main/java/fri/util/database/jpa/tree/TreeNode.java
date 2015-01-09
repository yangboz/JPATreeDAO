package fri.util.database.jpa.tree;

import java.io.Serializable;

/**
 * Responsibilities of entities that are managed by a TreeDao.
 * Any applier of JpaTree will have to implement this interface
 * in his JPA domain objects.
 * 
 * @author Fritz Ritzberger, 19.10.2012
 */
public interface TreeNode extends Cloneable
{
	/**
	 * @return the primary key of this tree node.
	 */
	Serializable getId();
	
	/**
	 * For copy and unique constraint-checks cloning is required.
	 * Mind that a clone MUST NOT have a primary key (id of clone must be null)!
	 */
	TreeNode clone();

}
