package fri.util.database.jpa.tree.examples.closuretable.temporal.deletedflag;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;
import fri.util.database.jpa.tree.closuretable.TemporalTreePath;
import fri.util.database.jpa.tree.examples.closuretable.CompositePersonTreePathId;
import fri.util.database.jpa.tree.examples.closuretable.PersonAbstractTreePath;
import fri.util.database.jpa.tree.examples.closuretable.PersonCtt;

/** Example for temporal TreePath implementation. */
@Entity
@IdClass(CompositePersonTreePathId.class)	// has a composite primary key
public class PersonDeletedFlagTreePath extends PersonAbstractTreePath implements TemporalTreePath
{
	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "ancestor", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode ancestor;

	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "descendant", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode descendant;

	private boolean deleted;

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

	// dummy implementation of interface Temporal
	
	@Transient
	@Override
	public Date getValidTo() {
		return null;
	}
	@Transient
	@Override
	public void setValidTo(Date validTo) {
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
	
	// the really used remove-implementation
	
	public boolean isDeleted() {
		return deleted;
	}
	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}
}