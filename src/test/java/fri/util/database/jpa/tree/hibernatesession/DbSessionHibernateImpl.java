package fri.util.database.jpa.tree.hibernatesession;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;

import fri.util.database.jpa.commons.DbSession;

/**
 * <i>Hibernate Session</i> (not JPA-compatible!) implementation of <code>DbSession</code>.
 * 
 * @author Fritz Ritzberger, 2013-08-24
 */
public class DbSessionHibernateImpl implements DbSession
{
	private final Session session;
	
	public DbSessionHibernateImpl(Session session) {
		this.session = session;
	}
	
	@Override
	public Object get(Class<?> entityClass, Serializable id) {
		return session.get(entityClass, id);
	}
	
	@Override
	public Object save(Object node)	{
		session.saveOrUpdate(node);
		return node;
	}
	
	@Override
	public void flush() {
		session.flush();
	}
	
	@Override
	public void refresh(Object node) {
		session.refresh(node);
	}
	
	@Override
	public void delete(Object node) {
		session.delete(node);
	}
	
	@Override
	public List<?> queryList(String queryText, Object[] parameters) {
		Query query = query(queryText, parameters);
		return query.list();
	}

	@Override
	public int queryCount(String queryText, Object[] parameters) {
		@SuppressWarnings("rawtypes")
		List result = queryList(queryText, parameters);
		return ((Number) result.get(0)).intValue();
	}
	
	@Override
	public void executeUpdate(String sqlCommand, Object[] parameters) {
		Query query = query(sqlCommand, parameters);
		query.executeUpdate();
	}
	
	
	/** Do not use. Convenience method for unit tests. */
	public Session getHibernateSession() {
		return session;
	}

	
	private Query query(String queryText, Object[] parameters) {
		Query query = session.createQuery(replaceNumberedParameterPlaceholders(queryText));
		if (parameters != null)	{
			int i = 0;
			for (Object parameter : parameters)	{
				if (parameter == null)
					throw new IllegalArgumentException("Binding parameter at position "+i+" can not be null: "+queryText);
				
				if (parameter instanceof Integer)
					query.setInteger(i, (Integer) parameter);
				else if (parameter instanceof Number)
					throw new RuntimeException("Number type not supported here: "+parameter.getClass().getName());
				else if (parameter instanceof Date)
					query.setTimestamp(i, new Timestamp(((Date) parameter).getTime()));
				else if (parameter instanceof String)
					query.setString(i, (String) parameter);
				else
					query.setEntity(i, parameter);
				i++;
			}
		}
		return query;
	}

	/** Hibernate Session can not deal with JPQL numbered positional parameters. */
	private String replaceNumberedParameterPlaceholders(String queryText) {
		for (int i = 1; ; i++)	{
			final String toReplace = "?"+i;
			final int position = queryText.indexOf(toReplace);
			
			if (position >= 0)
				queryText = queryText.substring(0, position)+"?"+queryText.substring(position + toReplace.length());
			else
				return queryText;
		}
	}

}
