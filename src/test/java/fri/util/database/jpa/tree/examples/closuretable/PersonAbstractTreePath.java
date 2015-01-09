package fri.util.database.jpa.tree.examples.closuretable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import fri.util.database.jpa.tree.closuretable.TreePath;

@MappedSuperclass
public abstract class PersonAbstractTreePath implements TreePath
{
	@Column(nullable = false)
	private int depth;

	private int orderIndex;

	@Override
	public int getDepth() {
		return depth;
	}
	@Override
	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public int getOrderIndex() {
		return orderIndex;
	}
	@Override
	public void setOrderIndex(int position) {
		this.orderIndex = position;
	}
}