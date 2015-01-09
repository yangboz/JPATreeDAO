package fri.util.database.jpa.commons;

import java.io.Serializable;
import java.util.List;

/**
 * Responsibilities of an JPA database session in interaction with NestedSetsTreeDAO.
 * Should be implementable using a Hibernate Session or an JPA EntityManager.
 * 
 * @author Fritz Ritzberger, 08.10.2011
 */
public interface DbSession
{
	/**
	 * Fast cached read of an object by identity (primary key).
	 * @param entityClass the persistence class of the entity (POJO).
	 * @param id the primary key of the entity.
	 * @return the entity object with given primary key.
	 */
	Object get(Class<?> entityClass, Serializable id);
	
	/**
	 * Executes a query and returns its result list.
	 * @param queryText the JPQL text for the query.
	 * @param parameters the positional parameters for "?" place-holders in query text.
	 * @return the result list of the query.
	 */
	List<?> queryList(String queryText, Object [] parameters);
	
	/**
	 * Executes given query and returns the resulting count of found records.
	 * The query must be a <code>select count(x) ...</code> query.
	 * @param queryText the JPQL text for the query.
	 * @param parameters the positional parameters for "?" place-holders in query text.
	 * @return the number of records found by the query.
	 */
	int queryCount(String queryText, Object [] parameters);
	
	/**
	 * Save the passed object to persistence.
	 * @param node the object to save.
	 */
	Object save(Object node);
	
	/**
	 * Re-reads passed object from persistence.
	 * This is needed as some changes are not done via JPA objects but directly via JPQL.
	 * @param node the object to refresh.
	 */
	void refresh(Object node);
	
	/**
	 * Deletes passed object from persistence.
	 * @param node the object to delete.
	 */
	void delete(Object node);
	
	/**
	 * Executes an update or delete statement.
	 * @param statement the text of the JPQL statement.
	 * @param parameters the positional parameters for place-holders in command text.
	 */
	void executeUpdate(String statement, Object [] parameters);
	
	/**
	 * Flushes all changes to database.
	 * This is needed as some changes are not done via JPA objects but directly via JPQL.
	 */
	void flush();
	
}
