package fri.util.database.jpa.tree.nestedsets.pojos;

import javax.persistence.MappedSuperclass;

import fri.util.database.jpa.commons.AbstractEntity;

/**
 * Abstraction of an NestedSetsTree POJO that holds left and right numbers.
 * <p/>
 * The topLevel property would also belong to here, but unfortunately this seems
 * impossible with JPA ManyToOne annotation as this needs a concrete class.
 * (Overriding and annotating the setter in subclass did not help.)
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
@MappedSuperclass	// tells JPA to map properties of this class to any subclass
public abstract class AbstractNestedSetsTreePojo extends AbstractEntity
{
	private int lft;	// can not be named "left" because this is a SQL keyword
	private int rgt;	// can not be named "right" because this is a SQL keyword
	
	protected AbstractNestedSetsTreePojo() {
	}
	
	public int getLeft() {
		return lft;
	}

	public void setLeft(int left) {
		this.lft = left;
	}

	public int getRight() {
		return rgt;
	}

	public void setRight(int right) {
		this.rgt = right;
	}

}
