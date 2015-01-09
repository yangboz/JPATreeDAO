package fri.util.database.jpa.tree.uniqueconstraints;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.AbstractTreeDao;
import fri.util.database.jpa.tree.TreeDao;
import fri.util.database.jpa.tree.TreeNode;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Basic unique constraint check implementation.
 * Provides utility methods to check uniqueness (1) in children and (2) in whole tree.
 * <p/>
 * This also provides a check for unique roots.
 * Which of these two checks is performed (roots or tree) is managed internally
 * (depends on the arguments of checkUniqueConstraint(), see isRootsCheck()).
 * 
 * @author Fritz Ritzberger, 27.10.2012
 */
public abstract class AbstractUniqueTreeConstraintImpl <N extends TreeNode> implements UniqueTreeConstraint<N>
{
	/** One or several property name sets that represent this unique constraint. */
	protected final String [][] uniquePropertyNames;
	
	/** True when constraint should also check that no root has the same unique property set as another root. */
	protected final boolean shouldCheckRootsForUniqueness;
	
	private DbSession session;
	private TreeDao<N> dao;
	private String nodeEntityName;	// JPQL "table" name
	private String pathEntityName;
	
	/**
	 * @param uniquePropertyNames JPQL property names that should be checked for uniqueness.
	 * 		For example, to check (1) "name" combined with "code", and (2) "acronym", for uniqueness,
	 * 		pass in <code>new String [][] { { "name", "code" }, { "acronym" } }</code>.
	 * @param shouldCheckRootsForUniqueness true when constraint should also check that
	 * 		no root has the same unique property set as another root.
	 */
	protected AbstractUniqueTreeConstraintImpl(String [][] uniquePropertyNames, boolean shouldCheckRootsForUniqueness) {
		if (uniquePropertyNames == null || uniquePropertyNames.length <= 0)
			throw new IllegalArgumentException("It makes no sense to define unique constraints without field names!");
		
		for (final String [] uniqueNames : uniquePropertyNames)
			if (uniqueNames.length <= 0)
				throw new IllegalArgumentException("There is an empty unique constraint: "+Arrays.asList(uniquePropertyNames));
		
		this.uniquePropertyNames = uniquePropertyNames;
		this.shouldCheckRootsForUniqueness = shouldCheckRootsForUniqueness;
	}
	
	@Override
	public void setContext(DbSession session, TreeDao<N> dao, String nodeEntityName, String pathEntityName) {
		this.session = session;
		this.dao = dao;
		this.nodeEntityName = nodeEntityName;
		this.pathEntityName = pathEntityName;
	}
	
	/** @return the nodeEntity name passed in as context. */
	protected final String nodeEntityName() {
		return nodeEntityName;
	}

	/** @return the pathEntity name passed in as context, is null for the nested-sets-tree. */
	protected final String pathEntityName() {
		return pathEntityName;
	}

	/** @return the DAO passed in  as context. */
	protected final TreeDao<N> getDao() {
		return dao;
	}

	/** @return the database session passed in  as context. */
	protected final DbSession getSession() {
		return session;
	}

	/** Retrieves a named value from given Object by reflection. The uniqueName is case-sensitive. */
	protected final Object getNodeValueForProperty(String uniqueName, Object node) {
		assert uniqueName != null && uniqueName.length() > 0;
		
		Class<?> clazz = node.getClass();
		do	{
			try {
				Field property = clazz.getDeclaredField(uniqueName);
				property.setAccessible(true);
				return property.get(node);
			}
			catch (NoSuchFieldException e) {
				// skip to superclass when field not here
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			clazz = clazz.getSuperclass();
		}
		while (clazz != null);
		throw new RuntimeException("Field not found when checking unique constraint: "+uniqueName+", node: "+node);
	}

	
	
	// unique children implementation
	
	/** Checks if passed node would be unique in the children of given location, but can not check roots. */
	protected boolean checkUniqueChildrenConstraint(N node, TreeActionLocation<N> location)	{
		final Map<String,Object> propertyValuesCache = new HashMap<String,Object>();
		
		final List<N> children;
		
		if (location.relatedNodeType == TreeActionLocation.RelatedNodeType.PARENT)	{
			children = getDao().getChildren(location.relatedNode);
		}
		else	{	// relatedNode is sibling, or this is an update and the existing node was stored in relatedNode
			N parent = getDao().getParent(location.relatedNode);
			if (parent == null)	{	// this happens on root rename
				List<N> nodes = new ArrayList<N>();
				nodes.add(node);
				return checkUniqueWholeTreeConstraint(nodes, location);
			}
			children = getDao().getChildren(parent);
		}
		
		for (N child : children)	{
			for (String [] uniqueNameSet : uniquePropertyNames)	{
				if (location.actionType != TreeActionLocation.ActionType.MOVE || equal(child, node) == false)	{
					boolean different = false;
					
					for (String uniqueName : uniqueNameSet)	{
						Object newValue = getCachedNodeValueForProperty(uniqueName, node, propertyValuesCache);
						Object existingValue = getNodeValueForProperty(uniqueName, child);
						if (isDifferent(newValue, existingValue))	{
							different = true;
							break;
						}
					}
					
					if (different == false)	// not one property of this child differed from candidate
						return false;	// all unique properties are equal to those of given node
				}
			}
		}
		
		return true;
	}
	
	/** Delegates to dao.equal(). */
	protected final boolean equal(N n1, N n2)	{
		return ((AbstractTreeDao<N>) getDao()).equal(n1, n2);
	}
	
	/**
	 * Equality of node property values. To be overridden.
	 * @return true if the given property values are equal, or one or both are null, else false.
	 */
	protected boolean isDifferent(Object newValue, Object existingValue) {
		if (newValue == null || existingValue == null)
			return true;	// regard both null or one null to be different
		
		return existingValue.equals(newValue) == false;
	}

	
	private Object getCachedNodeValueForProperty(String uniqueName, N node, Map<String,Object> cache) {
		Object value = cache.get(uniqueName);
		if (value != null)
			return value;
		
		value = getNodeValueForProperty(uniqueName, node);
		cache.put(uniqueName, value);
		return value;
	}

	
	
	// unique whole tree implementation
	
	/** Checks if passed node would be unique within its tree. */
	protected final boolean checkUniqueWholeTreeConstraint(List<N> nodes, TreeActionLocation<N> location)	{
		for (N node : getNodesToCheck(nodes, location))	{
			final StringBuilder queryText = new StringBuilder("select count("+getNodeTableAlias()+") from ");
			queryText.append(fromClause());
			
			final List<Object> parameters = new ArrayList<Object>();
			appendUpdateCondition(node, queryText, parameters, getNodeTableAlias(), location);
			appendUniquenessConditions(node, queryText, parameters, getNodeTableAlias());
			
			final boolean isRootsOnlyCheck = isRootsCheck(location.root, node);
			
			if (isRootsOnlyCheck == false || shouldCheckRootsForUniqueness)	{
				queryText.append(" and ");
				
				if (isRootsOnlyCheck)	{	// check unique roots
					appendRootCheckingCondition(queryText, parameters);
				}
				else	{	// check unique nodes
					if (location.root == null)
						throw new IllegalArgumentException("Root is null, can not perform node uniqueness check without the root of a tree!");
					
					appendNodeCheckingCondition(location.root, queryText, parameters);
				}
				
				beforeCheckUniqueness(queryText, parameters);
				
				if (getSession().queryCount(queryText.toString(), parameters.toArray()) > 0)
					return false;
				
				if (isRootsOnlyCheck)
					return true;	// no need to check all copied nodes
			}
		}
		return true;
	}

	/** @return the node table alias (not the path table alias!) to be used for appending unique constraint conditions. */
	protected abstract String getNodeTableAlias();

	/** @return the FROM clause contents, not starting with "from", but closing with "where" or "and". */
	protected abstract String fromClause();

	/** @return the condition that includes only roots in the check, not starting with AND, not ending with AND. */
	protected abstract void appendRootCheckingCondition(StringBuilder queryText, List<Object> parameters);

	/** @return the condition that checks for unique nodes only within the given root, not starting with AND, not ending with AND. */
	protected abstract void appendNodeCheckingCondition(N root, StringBuilder queryText, List<Object> parameters);

	/**
	 * @param root the root of the node about to be inserted or updated, must be null for root checks.
	 * @param node the node about to be updated, null when this is an insert.
	 * @return true when the given entities indicate a uniqueness check among roots.
	 */
	protected boolean isRootsCheck(N root, N node)	{
		if (root != null && node == null)
			throw new IllegalArgumentException("Can not insert/update a null node into a not-null root!");
		
		return (root == null || equal(root, node));
	}
	
	/**
	 * When node is persistent and action is neither MOVE nor INSERT,
	 * appends a condition saying "node != originalNode", thus avoiding the node to detect itself.
	 * Ends with "AND".
	 */
	protected void appendUpdateCondition(N node, StringBuilder queryText, List<Object> parameters, String tableAlias, TreeActionLocation<N> location)	{
		boolean nodeIsPersistent = getDao().isPersistent(node);
		boolean updatingExistingNode =
				(location.actionType == TreeActionLocation.ActionType.UPDATE &&
				location.relatedNode != null &&
				getDao().isPersistent(location.relatedNode));
		
		if ((nodeIsPersistent || updatingExistingNode) &&
				location.actionType != TreeActionLocation.ActionType.COPY &&
				location.actionType != TreeActionLocation.ActionType.INSERT)
		{
			queryText.append(" "+tableAlias+" <> "+QueryBuilderUtil.buildIndexedPlaceHolder(parameters)+" and ");
			parameters.add(nodeIsPersistent ? node : location.relatedNode);
		}
	}

	/** Appends unique property conditions to queryText and parameters, not starting with "AND". */
	protected final void appendUniquenessConditions(N node, StringBuilder queryText, List<Object> parameters, String tableAlias) {
		queryText.append(" (");	// needed to encapsulate "OR"
		int i = 0;
		for (final String [] uniqueNames : uniquePropertyNames)	{
			// put criteria into Map
			final Map<String,Object> criteria = new HashMap<String,Object>();
			for (String uniqueName : uniqueNames)	{
				assert uniqueName != null && uniqueName.length() > 0 : "There is an empty unique constraint property: "+uniqueNames;
				criteria.put(uniqueName, getNodeValueForProperty(uniqueName, node));
			}
			
			// append all criteria by AND'ing them
			QueryBuilderUtil.appendCriteria(true, queryText, tableAlias, parameters, criteria, true, false);
			
			i++;
			if (i < uniquePropertyNames.length)	// another constraint is present
				queryText.append(" or ");
		}
		queryText.append(") ");
	}

	/**
	 * Does nothing.
	 * Called with ready-made uniqueness query before it is executed.
	 * To be overridden for appending temporal conditions.
	 * When overriding, use getPathTableAlias() or getNodeTableAlias() to point to the correct table.
	 */
	@SuppressWarnings("unused")
	protected void beforeCheckUniqueness(StringBuilder queryText, List<Object> parameters) {
	}


	private List<N> getNodesToCheck(List<N> nodes, TreeActionLocation<N> location) {
		if (location.actionType == TreeActionLocation.ActionType.MOVE)	{
			final N movingNode = nodes.get(0);
			
			if (isRootsCheck(location.root, movingNode) == false)	{	// not moving to be a new root
				final N sourceRoot = getDao().getRoot(movingNode);
				
				if (equal(sourceRoot, location.root))
					nodes = new ArrayList<N>();	// nothing to do when moving within same tree
				else
					nodes = getDao().getTree(movingNode);	// must check ALL nodes when moving into another tree
			}
		}
		return nodes;
	}

}
