package fri.util.database.jpa.tree.examples.nestedsets;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;

/** Example entity, Nst = Nested Sets Tree. */
@Entity
public class PersonNst implements NestedSetsTreeNode
{
	@Id
	@GeneratedValue
	private String id; // primary key

	@Column(nullable = false)
	private String name;

	@ManyToOne(targetEntity = PersonNst.class)
	private NestedSetsTreeNode topLevel; // root reference

	private int lft; // "left" would be a SQL keyword!
	private int rgt; // "right" would be a SQL keyword!

	public PersonNst() {
	}

	public PersonNst(String name) {
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

	@Override
	public PersonNst clone() {
		return new PersonNst(getName());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PersonNst == false)
			return false;

		if (id != null)
			return id.equals(((PersonNst) obj).getId());

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