package fri.util.database.jpa.tree.util;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Utilities for editing JPQL queries.
 * 
 * @author Fritz Ritzberger, 21.10.2012
 */
public final class QueryBuilderUtil
{
	/**
	 * Appends all criteria to query, ANDing them, starting with WHERE or AND (depending on whereWasAppended).
	 * @param nullIsACriterion true when null values in Map should be rendered as "and xxx is null".
	 * @param queryText the query text to populate with criteria.
	 * @param parameterValues the positional query parameters to fill.
	 * @param criteria the query criteria, key = property name, value = value to be queried.
	 * @param whereWasAppended true when the incoming query has a "WHERE" already appended.
	 * @return true when a WHERE was appended to query.
	 */
	public static boolean appendCriteria(
			boolean nullIsACriterion,
			StringBuilder queryText,
			String tableAlias,
			List<Object> parameterValues,
			Map<String,Object> criteria,
			boolean whereWasAppended)
	{
		return appendCriteria(nullIsACriterion, queryText, tableAlias, parameterValues, criteria, whereWasAppended, true);
	}
	
	/**
	 * Appends all criteria to query, ANDing them.
	 * @param nullIsACriterion true when null values in Map should be rendered as "and xxx is null".
	 * @param queryText the query text to populate with criteria.
	 * @param parameterValues the positional query parameters to fill.
	 * @param criteria the query criteria, key = property name, value = value to be queried.
	 * @param whereWasAppended true when the incoming query has a "WHERE" already appended.
	 * @param tableAlias the table-alias in query to prefix the property name: e.g. "p" to achieve "p.xxx".
	 * @param startWithWhereOrAnd when true, an AND or WHERE is appended at start, depending on whereWasAppended.
	 * @return true when a WHERE was appended to query.
	 */
	public static boolean appendCriteria(
			boolean nullIsACriterion,
			StringBuilder queryText,
			String tableAlias,
			List<Object> parameterValues,
			Map<String,Object> criteria,
			boolean whereWasAppended,
			boolean startWithWhereOrAnd)
	{
		if (criteria != null)	{
			for (Map.Entry<String,Object> criterion : criteria.entrySet())	{
				if (criterion.getValue() != null || nullIsACriterion)	{
					if (startWithWhereOrAnd)
						queryText.append(whereWasAppended ? " and " : " where ");
					
					whereWasAppended = true;
					startWithWhereOrAnd = true;
					
					final String aliasedPropertyName = buildAliasedPropertyName(tableAlias, criterion.getKey());
					
					if (criterion.getValue() != null)	{
						queryText.append(aliasedPropertyName+" = "+buildIndexedPlaceHolder(parameterValues)+" ");
						parameterValues.add(criterion.getValue());
					}
					else {	// nullIsACriterion == true
						queryText.append(aliasedPropertyName+" is null ");
					}
				}
			}
		}
		
		return whereWasAppended;
	}
	
	
	/**
	 * Appends "(validFrom is null or validFrom <= ?) and (validTo is null or validTo > ?)".
	 * @param tableAlias the alias to use for query text, can be null.
	 * @param validFromPropertyName the name of the property that represents the temporal valid-from date, can be null.
	 * @param validFrom the value for valid-from property, can be null, then "now" is used.
	 * @param validToPropertyName the name of the property that represents the temporal valid-to date, can NOT be null.
	 * @param validTo the value for valid-to property, can be null, then "now" is used.
	 * @param queryText the query to append property conditions to.
	 * @param parameters the query parameters to append property values to.
	 */
	public static void appendValidityConditions(
			String tableAlias,
			String validFromPropertyName,
			Date validFrom,
			String validToPropertyName,
			Date validTo,
			StringBuilder queryText,
			List<Object> parameters)
	{
		final Date now = new Date();
		if (validFromPropertyName != null)	{
			validFromPropertyName = buildAliasedPropertyName(tableAlias, validFromPropertyName);
			queryText.append(" ("+validFromPropertyName+" is null or "+validFromPropertyName+" <= "+buildIndexedPlaceHolder(parameters)+") and ");
			parameters.add(validFrom != null ? validFrom : now);
		}
		validToPropertyName = buildAliasedPropertyName(tableAlias, validToPropertyName);
		queryText.append(" ("+validToPropertyName+" is null or "+validToPropertyName+" > "+buildIndexedPlaceHolder(parameters)+") ");
		parameters.add(validTo != null ? validTo : now);
	}

	
	/** @return "tableAlias.propertyName" when alias is not null, else "propertyName". */
	public static String buildAliasedPropertyName(String tableAlias, String propertyName) {
		return (tableAlias != null ? tableAlias+"." : "")+propertyName;
	}
	
	/** @return "?5" when parameters.size() == 4. */
	public static String buildIndexedPlaceHolder(List<Object> parameters) {
		return "?"+(parameters.size() + 1);
	}

	
	private QueryBuilderUtil() {}	// do not instantiate
}
