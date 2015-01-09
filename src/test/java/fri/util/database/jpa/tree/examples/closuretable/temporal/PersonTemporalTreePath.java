package fri.util.database.jpa.tree.examples.closuretable.temporal;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TemporalTreePath;
import fri.util.database.jpa.tree.examples.closuretable.CompositePersonTreePathId;
import fri.util.database.jpa.tree.examples.closuretable.PersonAbstractTreePath;
import fri.util.database.jpa.tree.examples.closuretable.PersonCtt;

/** Example for temporal TreePath implementation. */
@Entity
@IdClass(CompositePersonTreePathId.class)	// has a composite primary key
public class PersonTemporalTreePath extends PersonAbstractTreePath implements TemporalTreePath
{
	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "ancestor", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode ancestor;

	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "descendant", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode descendant;

    @Temporal(TemporalType.TIMESTAMP)
	private Date validFrom;
    
    @Temporal(TemporalType.TIMESTAMP)
	private Date validTo;
	
	@Override
	public ClosureTableTreeNode getAncestor() {
		return ancestor;
	}
	@Override
	public void setAncestor(ClosureTableTreeNode ancestor) {
		this.ancestor = ancestor;
	}

	@Override
	public ClosureTableTreeNode getDescendant() {
		return descendant;
	}
	@Override
	public void setDescendant(ClosureTableTreeNode descendant) {
		this.descendant = descendant;
	}

	// interface Temporal
	
	@Override
	public Date getValidTo() {
		return validTo;
	}
	@Override
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}
	@Override
	public Date getValidFrom() {
		return validFrom;
	}
	@Override
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}
	
}