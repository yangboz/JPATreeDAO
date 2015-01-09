package fri.util.database.jpa.tree.closuretable.pojos;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import fri.util.database.jpa.tree.closuretable.TreePath;

/**
 * Abstraction of the POJO that represents an ancestor to descendant relation.
 * Foreign keys and primary key must be implemented in the concrete derivation.
 * Mind that names and data types of CompositeKey are bound to the names and
 * data types of their concrete derivations!
 * 
 * @author Fritz Ritzberger, 14.10.2012
 */
@MappedSuperclass
public abstract class AbstractTreePathImpl implements TreePath
{
	/**
	 * Primary key mapping class, holding the node id of both ancestor and descendant,
	 * which in combination will be unique and thus suitable as primary key for any TreePathImpl.
	 */
	public static class CompositeId implements Serializable
	{
		private String ancestor;	// must have same name and data-type as TreePathImpl.ancestor
		private String descendant;	// must have same name and data-type as TreePathImpl.descendant
		
		public String getAncestor() {
			return ancestor;
		}
		public void setAncestor(String ancestorId) {
			this.ancestor = ancestorId;
		}
		public String getDescendant() {
			return descendant;
		}
		public void setDescendant(String descendantId) {
			this.descendant = descendantId;
		}
		
		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof CompositeId == false)
				return false;
			CompositeId other = (CompositeId) o;
			return other.getDescendant().equals(getDescendant()) && other.getAncestor().equals(getAncestor());
		}
		@Override
		public int hashCode() {
			return getDescendant().hashCode() * 31 + getAncestor().hashCode();
		}
	}
	
    @Column(nullable = false)
	private int depth;
    
    @Column(nullable = true)
	private int orderIndex;
	
	@Override
	public int getDepth() {
		return depth;
	}
	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public int getOrderIndex() {
		return orderIndex;
	}
	@Override
	public void setOrderIndex(int position) {
		this.orderIndex = position;
	}

	@Override
	public String toString() {
		return "[ancestor="+getAncestor()+", descendant="+getDescendant()+", depth="+getDepth()+", orderIndex="+getOrderIndex()+"]";
	}

}
