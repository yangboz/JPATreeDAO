package fri.util.database.jpa.tree.examples.closuretable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;

/** Tree aspect: organizational hierarchy among persons, e.g. who gives orders to whom. */
@Entity
@IdClass(CompositePersonTreePathId.class)	// has a composite primary key
public class PersonOrganisationalTreePath  extends PersonAbstractTreePath
{
	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "ancestor", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode ancestor;

	@Id
	@ManyToOne(targetEntity = PersonCtt.class)
	@JoinColumn(name = "descendant", nullable = false)	// the name of the database foreign key column
	private ClosureTableTreeNode descendant;

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
}