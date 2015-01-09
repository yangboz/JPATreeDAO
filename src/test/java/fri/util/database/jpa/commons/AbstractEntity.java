package fri.util.database.jpa.commons;

import java.io.Serializable;

/**
 * This is an example base class for domain objects, used by unit tests.
 * It uses the "Id" property for equals() and hashCode() implementations.
 * This is not the best way to implement such a POJO, but sufficient for unit tests.
 * Mind that a POJO could get lost when being inserted into a Map without having an ID
 * (transient state), and being retrieved after it got its ID by some JPA primary key factory.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public abstract class AbstractEntity
{
	public abstract Serializable getId();
	
	/**
	 * @return true if the UID's (database primary keys) of this and
	 * the other object are defined (persistent state) and equal, else
	 * delegates to <code>super.equals()</code> which normally uses
	 * the object's memory address.
	 */
	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
			
		if (other == null || other instanceof AbstractEntity == false)
			return false;

		final Serializable id = getId();
		if (id != null)
			return id.equals(((AbstractEntity) other).getId());

		return super.equals(other);
	}

	/**
	 * @return the ID's hash-code (database primary key), when ID
	 * is not null (persistent state), else delegates to <code>super.hashCode()</code>
	 * which normally returns the object's memory address.
	 */
	@Override
	public int hashCode() {
		final Serializable id = getId();
		if (id != null)
			return id.hashCode();
		
		throw new IllegalStateException("Do not use hashCode() with tranisent POJOs, they could get lost in Maps!");
	}
}
