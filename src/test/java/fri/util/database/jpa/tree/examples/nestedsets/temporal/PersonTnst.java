package fri.util.database.jpa.tree.examples.nestedsets.temporal;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeNode;

/**
 * Example temporal entity, Tnst = Temporal Nested Sets Tree,
 * redirecting validTo to endValid, and validFrom to nothing,
 * as validTo is enough to support historization.
 */
@Entity
public class PersonTnst implements TemporalNestedSetsTreeNode
{
	@Id
	@GeneratedValue
	private String id; // primary key

	@Column(nullable = false)
	private String name;

	@ManyToOne(targetEntity = PersonTnst.class)
	private NestedSetsTreeNode topLevel; // root reference

	private int lft; // "left" would be a SQL keyword!
	private int rgt; // "right" would be a SQL keyword!

    @Temporal(TemporalType.TIMESTAMP)
	private Date endValid;
    
	public PersonTnst() {
	}

	public PersonTnst(String name) {
		this.name = name;
	}

	@Override
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public Date getEndValid() {
		return endValid;
	}
	public void setEndValid(Date endValid) {
		this.endValid = endValid;
	}
	
	// NestedSetsTreeNode
	
	@Override
	public NestedSetsTreeNode getTopLevel() {
		return topLevel;
	}
	@Override
	public void setTopLevel(NestedSetsTreeNode topLevel) {
		this.topLevel = topLevel;
	}

	@Override
	public int getLeft() {
		return lft;
	}
	@Override
	public void setLeft(int left) {
		this.lft = left;
	}

	@Override
	public int getRight() {
		return rgt;
	}
	@Override
	public void setRight(int right) {
		this.rgt = right;
	}

	// Cloneable
	
	@Override
	public PersonTnst clone() {
		return new PersonTnst(getName());
	}

	// Temporal
	
	@Transient
	@Override
	public Date getValidTo() {
		return endValid;
	}
	@Transient
	@Override
	public void setValidTo(Date validTo) {
		this.endValid = validTo;
	}
	
	@Transient
	@Override
	public Date getValidFrom() {
		return null;
	}
	@Transient
	@Override
	public void setValidFrom(Date validFrom) {
	}
	
	// others
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PersonTnst == false)
			return false;

		if (id != null)
			return id.equals(((PersonTnst) obj).getId());

		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		if (id != null)
			return id.hashCode();

		throw new IllegalStateException("Do not use hashCode() with tranisent POJOs, they could get lost in Maps!");
	}

	/** Overridden to avoid super.toString() calling hashCode(), as this might throw exception when no id exists yet. */
	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+System.identityHashCode(this)+": id="+getId()+", name="+getName();
	}

}