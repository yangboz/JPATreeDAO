package fri.util.database.jpa.tree.uniqueconstraints;

import fri.util.database.jpa.tree.TreeNode;

/**
 * This is thrown when the DAO detects a unique constraint violation,
 * i.e. a Java-programmed check indicates an error.
 */
public class UniqueConstraintViolationException extends Exception
{
	private final TreeNode originator;
	
	public UniqueConstraintViolationException(String message, TreeNode originator) {
		super(message);
		this.originator = originator;
	}

	/** @return a clone of the node that caused this exception, containing the invalid value. */
	public TreeNode getOriginator() {
		return originator;
	}
}
