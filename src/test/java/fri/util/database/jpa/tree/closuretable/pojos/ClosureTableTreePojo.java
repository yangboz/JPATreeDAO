package fri.util.database.jpa.tree.closuretable.pojos;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import fri.util.database.jpa.commons.AbstractEntity;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;

/**
 * Example POJO for the unit test, implementing <code>ClosureTableTreeNode</code> via JPA.
 * This is a very normal JPA domain object and contains no tree properties.
 * Instances of such a entity type should be persisted using ClosureTableTreeDao.
 * 
 * @author Fritz Ritzberger, 14.10.2012
 */
@Entity
public class ClosureTableTreePojo extends AbstractEntity implements ClosureTableTreeNode
{
	@Id
	@GeneratedValue
    private String id;

    @Column(nullable=false)
	private String name;
	
	private String address;
	
    
	/** No-argument constructor needed by JPA layer. Must be present when other constructors exist. */
	public ClosureTableTreePojo() {
	}

	/** Convenience constructor for programming. */
	public ClosureTableTreePojo(String name) {
		assert name != null : "Need a non-null name for unit testing!";
		this.name = name;
		this.address = "address of "+name;
	}

	
	@Override
	public Serializable getId() {
		return id;
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
	public ClosureTableTreeNode clone()	{
		return new ClosureTableTreePojo(getName());
	}
	
	@Override
	public String toString() {
		return getName();
	}

}
