package fri.util.database.jpa.tree.examples.closuretable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import fri.util.database.jpa.tree.closuretable.ClosureTableTreeNode;

/** Example entity, Ctt = Closure Table Tree. */
@Entity
public class PersonCtt implements ClosureTableTreeNode
{
	@Id
	@GeneratedValue
	private String id;

	@Column(nullable = false)
	private String name;

	public PersonCtt() {
	}

	public PersonCtt(String name) {
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
	public ClosureTableTreeNode clone() {
		return new PersonCtt(getName());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName()+"@"+System.identityHashCode(this)+": id="+getId()+", name="+getName();
	}
}