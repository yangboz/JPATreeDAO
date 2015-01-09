package fri.util.database.jpa.tree.nestedsets.pojos;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;
import fri.util.database.jpa.tree.nestedsets.TemporalNestedSetsTreeNode;

/**
 * Example POJO for the unit test, implementing <code>TemporalNestedSetsTree</code> via JPA.
 * 
 * @author Fritz Ritzberger, 12.10.2011
 */
@Entity
public class TemporalNestedSetsTreePojo extends AbstractNestedSetsTreePojo implements TemporalNestedSetsTreeNode
{
	@Id
    @GeneratedValue
    private String id;

    @Temporal(TemporalType.TIMESTAMP)
    //@Column(nullable=false)
	private Date validFrom;
    
    @Temporal(TemporalType.TIMESTAMP)
    //@Column(nullable=false)
	private Date validTo;
	
    @ManyToOne(targetEntity=TemporalNestedSetsTreePojo.class)
    @JoinColumn(name="TOPLEVEL_ID")	// can not be nullable=false because physical remove would fail then
    private NestedSetsTreeNode topLevel;
    
    @Column(nullable=false)
	private String name;
	
	private String address;
	
	/** No-argument constructor needed by JPA. */
	public TemporalNestedSetsTreePojo() {
	}

	/** Convenience constructor for programming. */
	public TemporalNestedSetsTreePojo(String name) {
		assert name != null : "Need a non-null name for unit testing!";
		this.name = name;
		this.address = "address of "+name;
	}

	
	@Override
	public String getId() {
		return id;
	}

	@Override
	public Date getValidFrom() {
		return validFrom;
	}
	
	@Override
	public Date getValidTo() {
		return validTo;
	}
	
	@Override
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}
	
	@Override
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
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
		TemporalNestedSetsTreePojo clone = new TemporalNestedSetsTreePojo(getName());
		clone.setTopLevel(getTopLevel());
		clone.setValidFrom(getValidFrom());
		clone.setValidTo(getValidTo());
		return clone;
	}
	
	@Override
	public String toString() {
		return "["+name+", left="+getLeft()+", right="+getRight()+", validFrom="+getValidFrom()+", validTo="+getValidTo()+", root="+(getTopLevel() != null ? ((TemporalNestedSetsTreePojo) getTopLevel()).getName() : "null")+"]";
	}
	
}
