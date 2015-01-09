package fri.util.database.jpa.tree.examples.closuretable;

import java.io.Serializable;

/**
 * Primary key class for TreePath, reusable for all nodes types that have a String id.
 * 
 * @author Fritz Ritzberger, 2012-10-14
 */
public class CompositePersonTreePathId implements Serializable
{
	private String ancestor;	// must have same name as TreePathImpl.ancestor, and data-type like TreePathImpl.ancestor.id
	private String descendant;	// must have same name as TreePathImpl.descendant, and data-type like TreePathImpl.descendant.id
	
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
		
		if (o instanceof CompositePersonTreePathId == false)
			return false;
		
		final CompositePersonTreePathId other = (CompositePersonTreePathId) o;
		return other.getDescendant().equals(getDescendant()) && other.getAncestor().equals(getAncestor());
	}
	
	@Override
	public int hashCode() {
		return getDescendant().hashCode() * 31 + getAncestor().hashCode();
	}
}