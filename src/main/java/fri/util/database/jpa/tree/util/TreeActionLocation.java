package fri.util.database.jpa.tree.util;

import fri.util.database.jpa.tree.TreeNode;

/**
 * Parameter object that describes the location where a tree action happens,
 * used by TreeDao and UniqueConstraint implementations.
 * The inserted/updated/copied/moved node is not part of this,
 * it is passed separately to the unique constraint implementation.
 * 
 * @author Fritz Ritzberger, 26.10.2012
 */
public class TreeActionLocation<N extends TreeNode>
{
	/** Describes what type the related node is. */
	public enum RelatedNodeType
	{
		PARENT,
		SIBLING,
	}
	
	/** Describes what tree action is happening. */
	public enum ActionType
	{
		INSERT,
		UPDATE,
		COPY,
		MOVE,
	}
	
	/** The root of the tree where this modification is about to take place. When null, this is a root insertion. */
	public final N root;

	/** The relation type of the related node. */
	public final RelatedNodeType relatedNodeType;
	
	/** The related node where the action is about to take place, either parent or sibling. */
	public final N relatedNode;
	
	/** The type of the action about to take place. */
	public final ActionType actionType;
	
	public TreeActionLocation(N root, RelatedNodeType relatedNodeType, N relatedNode, ActionType actionType) {
		this.root = root;
		this.relatedNode = relatedNode;
		this.relatedNodeType = relatedNodeType;
		this.actionType = actionType;
	}

}
