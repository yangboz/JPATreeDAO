package fri.util.database.jpa.tree.nestedsets.pojos;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fri.util.database.jpa.tree.nestedsets.NestedSetsTreeNode;

/**
 * Example POJO for the unit test, implementing <code>NestedSetsTree</code> via JPA.
 * <p/>
 * For sub-nodes a unique constraint can be defined on database level.
 * In this example implementation it is declared for name and topLevel (root reference),
 * so every name must be unique within its owning tree.
 * For roots this can not be defined on database level, because it would have
 * to work only on nodes for which <code>topLevel == this</code> applies.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */

@Entity
@Table(
	uniqueConstraints = {	// a database-level constraint
		@UniqueConstraint(	// to make "NAME" unique per tree
			name = "UN_NAME_TOPLEVEL",
			columnNames = { "TOPLEVEL_ID", "NAME" }
			// the database column names are to be used here
		)
	}
)
public class NestedSetsTreePojo extends AbstractNestedSetsTreePojo implements NestedSetsTreeNode
{
	@Id
    @GeneratedValue
    private String id;

    @ManyToOne(targetEntity = NestedSetsTreePojo.class)	// targetEntity tells JPA the concrete class for interface
    @JoinColumn(name="TOPLEVEL_ID")	// can not be nullable=false because MySQL then refuses to delete roots that have a self-reference
    private NestedSetsTreeNode topLevel;
    
    @Column(name="NAME", nullable=false)
	private String name;
	
	private String address;
	
	/** No-argument constructor needed by JPA. Must be present when other constructors exist. */
	public NestedSetsTreePojo() {
	}

	/** Convenience constructor for programming. */
	public NestedSetsTreePojo(String name) {
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

	/** @return a clone of this node, excluding Id property, left, right, but including topLevel. */
	@Override
	public NestedSetsTreeNode clone()	{
		NestedSetsTreePojo clone = new NestedSetsTreePojo(getName());
		clone.setTopLevel(getTopLevel());
		return clone;
	}
	
	@Override
	public String toString() {
		return "["+name+", left="+getLeft()+", right="+getRight()+", root="+(getTopLevel() != null ? ((NestedSetsTreePojo) getTopLevel()).getName() : "null")+"]";
	}

}
