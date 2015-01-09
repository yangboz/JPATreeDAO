package fri.util.database.jpa.tree.closuretable.pojos;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;

/**
 * The POJO that represents an ancestor to descendant relation.
 * For every ClosureTableTreeNode node type there must be one TreePath type,
 * but there could be more ("tree aspects").
 * 
 * @author Fritz Ritzberger, 14.10.2012
 */
@Entity
@IdClass(AbstractTreePathImpl.CompositeId.class)	// needed for composite primary key
@Table(
	uniqueConstraints = {	// this is not necessary, just for additional integrity on unit tests
		@UniqueConstraint(
			name = "UN_DESCENDANT_ANCESTOR_DEPTH",
			columnNames = { "DESCENDANT", "ANCESTOR", "DEPTH" }
			// the database column names are to be used here
		)
	}
)
public class TreePathImpl extends AbstractTreePathImpl
{
	@Id
	@ManyToOne(targetEntity = ClosureTableTreePojo.class)
	@JoinColumn(name = "ancestor", nullable = false)	// the name of the database foreign key column
    //@ForeignKey(name = "FK_ANCESTOR_ID")	// nice to have explicit name for foreign-key-constraint
	private ClosureTableTreeNode ancestor;
	
	@Id
	@ManyToOne(targetEntity = ClosureTableTreePojo.class)
	@JoinColumn(name = "descendant", nullable = false)	// the name of the database foreign key column
    //@ForeignKey(name = "FK_DESCENDANT_ID")	// nice to have explicit name for foreign-key-constraint
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
