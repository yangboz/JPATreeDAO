package fri.util.database.jpa.tree.nestedsets.pojos;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;

/**
 * Example POJO for the unit test, implementing <code>NestedSetsTree</code> via JPA.
 * This is the same as NestedSetsTreePojo but without unique constraint on database level.
 * 
 * @author Fritz Ritzberger, 12.11.2011
 */
@Entity
public class NonUniqueNestedSetsTreePojo extends AbstractNestedSetsTreePojo implements NestedSetsTreeNode
{
	@Id
    @GeneratedValue
    private String id;

    @ManyToOne(targetEntity=NonUniqueNestedSetsTreePojo.class)	// targetEntity tells JPA the concrete class for interface
    @JoinColumn(name="TOPLEVEL_ID")	// can not be nullable=false because MySQL refuses to delete roots that have a self-reference
    private NestedSetsTreeNode topLevel;
    
    @Column(nullable=false)
	private String name;
	
	private String address;
	
	/** No-argument constructor needed by JPA. Must be present when other constructors exist. */
	public NonUniqueNestedSetsTreePojo() {
	}

	/** Convenience constructor for programming. */
	public NonUniqueNestedSetsTreePojo(String name) {
		assert name != null : "Need a non-null name for unit testing!";
		this.name = name;
		this.address = "address of "+name;
	}

	@Override
	public String getId() {
		return id;
	}

	/** @return the top-level (root) node of this tree node. This is NOT its parent! */
	@Override
	public NestedSetsTreeNode getTopLevel() {
		return topLevel;
	}

	/** Do not call. Public due to implementation constraints. */
	@Override
	public void setTopLevel(NestedSetsTreeNode topLevel) {
		this.topLevel = topLevel;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	/** @return a clone of this node, excluding Id property. */
	@Override
	public NestedSetsTreeNode clone()	{
		NonUniqueNestedSetsTreePojo clone = new NonUniqueNestedSetsTreePojo(getName());
		clone.setTopLevel(getTopLevel());
		return clone;
	}
	
	@Override
	public String toString() {
		return "["+name+", left="+getLeft()+", right="+getRight()+", root="+(getTopLevel() != null ? ((NonUniqueNestedSetsTreePojo) getTopLevel()).getName() : "null")+"]";
	}

}
