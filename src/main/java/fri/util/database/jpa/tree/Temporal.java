package fri.util.database.jpa.tree;

import java.util.Date;

/**
 * Responsibilities of a temporal entity.
 * Such entities support a valid-from and a valid-to date that reflect
 * their validity. A removed entity would have at least validTo set to a past date.
 * <p/>
 * These properties are not required, implement them by do-nothing methods
 * if your entities perform another validity check, e.g. using a "deleted" flag.
 * In that case, be sure to pass null as <code>validXXXPropertyName</code> into the
 * DAO constructor, and to override appendValidityCondition(), assignValidity(),
 * appendInvalidityCondition(), assignInvalidity() and isValid() DAO methods.
 * 
 * @author Fritz Ritzberger, 12.10.2011
 */
public interface Temporal
{
	/** Default property names for the valid-from property as given by this interface. */
	String VALID_FROM = "validFrom";
	
	/** Default property names for the valid-to property as given by this interface. */
	String VALID_TO = "validTo";

	/** @return the temporal valid-to date of this node. */
	Date getValidTo();
	
	/** Sets the temporal valid-to date of this node. */
	void setValidTo(Date validTo);
	
	/** @return the temporal valid-from date of this node. */
	Date getValidFrom();
	
	/** Sets the temporal valid-from date of this node. */
	void setValidFrom(Date validFrom);

}
