package fri.util.database.jpa.tree;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import fri.util.database.jpa.commons.DbSession;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueConstraintViolationException;
import fri.util.database.jpa.tree.uniqueconstraints.UniqueTreeConstraint;
import fri.util.database.jpa.tree.util.QueryBuilderUtil;
import fri.util.database.jpa.tree.util.TreeActionLocation;

/**
 * Common functionalities for a TreeDao implementation.
 * <p/>
 * Mind that concurrency is not handled in any way.
 * The caller is expected to provide a transaction around every DAO write-method.
 * DAO write-methods execute more than one JPQL statement when called!
 * 
 * @author Fritz Ritzberger, 27.10.2012
 * 
 * @param <N> the tree node type handled by this DAO.
 */
public abstract class AbstractTreeDao <N extends TreeNode> implements TreeDao<N>
{
	protected final DbSession session;
	
	private final String nodeEntityName;
	
	private UniqueTreeConstraint<N> uniqueTreeConstraint;
	private boolean checkUniqueConstraintOnUpdate = false;
	private CopiedNodeRenamer<N> copiedNodeRenamer;


	protected AbstractTreeDao(DbSession session, String nodeEntityName)	{
		assert session != null;
		
		this.session = session;
		this.nodeEntityName = nodeEntityName;
	}

	/** {@inheritDoc} */
	@Override
	public boolean isPersistent(N entity) {
		return entity.getId() != null;
	}

	
	/** Saves the given node to session. To be overridden for additional actions on save. */
	protected Object save(N node) {
		return session.save(node);
	}
	
	/**
	 * Decides if two nodes are equal. Used by all implementations.
	 * Can handle nulls, returns true if n1 == n2. To be overridden.
	 * Is public to be reused also from <code>UniqueTreeConstraint</code> implementations.
	 */
	public boolean equal(N n1, N n2)	{
		return (n1 == n2) ? true : (n1 == null || n2 == null) ? false : n1.equals(n2);
	}
	

	/** {@inheritDoc} */
	@Override
	public void setUniqueTreeConstraint(UniqueTreeConstraint<N> uniqueTreeConstraint)	{
		this.uniqueTreeConstraint = uniqueTreeConstraint;
	}

	/** {@inheritDoc} */
	@Override
	public void setCheckUniqueConstraintOnUpdate(boolean checkUniqueConstraintOnUpdate)	{
		this.checkUniqueConstraintOnUpdate = checkUniqueConstraintOnUpdate;
	}

	/**
	 * @return true when unique constraint should be checked on UPDATE,
	 * 		default false, as callers should check explicitly this by
	 * 		calling checkUniqueConstraint().
	 */
	protected final boolean shouldCheckUniqueConstraintOnUpdate()	{
		return checkUniqueConstraintOnUpdate;
	}

	/** @return the optional unique constraint implementation. */
	protected final UniqueTreeConstraint<N> getUniqueTreeConstraint() {
		return uniqueTreeConstraint;
	}
	
	/** {@inheritDoc} */
	@Override
	public void checkUniqueConstraint(N cloneOfExistingNodeWithNewValues, N root, N existingNode) throws UniqueConstraintViolationException {
		TreeActionLocation<N> location = new TreeActionLocation<N>(root, null, existingNode, TreeActionLocation.ActionType.UPDATE);
		List<N> clones = new ArrayList<N>();
		clones.add(cloneOfExistingNodeWithNewValues);
		checkUniqueness(clones, location);
	}

	/**
	 * Utility method that calls the  implementation when one was defined.
	 * @param nodes one or several nodes to insert or update. Updates should be tested with a clone, else JPA object would get dirty.
	 * @param root the root of the tree where to check, can be null for a uniqueness check among roots.
	 * @param originalNode the node to be updated, must be null for a new node to be inserted.
	 * @param modificationLocation insert/update location information, implementation-specific.
	 * @throws UniqueConstraintViolationException when constraint would be violated.
	 */
	protected void checkUniqueness(List<N> nodes, TreeActionLocation<N> location) throws UniqueConstraintViolationException	{
		if (getUniqueTreeConstraint() == null)
			return;	// nothing to check
		
		assert nodes != null && nodes.size() >= 1 && location != null : "Need node and location to check unique constraint!";
		
		getUniqueTreeConstraint().setContext(session, this, nodeEntityName(), pathEntityName());
		
		if (getUniqueTreeConstraint().checkUniqueConstraint(nodes, location) == false)	{
			// there is no chance to recover the old state of the updated entity from database,
			// as the check-query triggered a flush() and updates are already in transaction
			
			String message = "One of following entities is not unique: "+nodes;	// create error message BEFORE refresh
			@SuppressWarnings("unchecked")
			N clone = (N) nodes.get(0).clone();	// assume the first node is invalid
			throw new UniqueConstraintViolationException(message, clone);
		}
	}
	
	
	/** {@inheritDoc} */
	@Override
	public void setCopiedNodeRenamer(CopiedNodeRenamer<N> copiedNodeRenamer)	{
		this.copiedNodeRenamer = copiedNodeRenamer;
	}
	
	/** Applies the currently set CopiedNodeRenamer if one was set. */
	protected final void applyCopiedNodeRenamer(N node)	{
		if (copiedNodeRenamer != null)
			copiedNodeRenamer.renameCopiedNode(node);
	}
	
	
	/** @return the name of the JPQL node entity. */
	protected final String nodeEntityName() {
		return nodeEntityName;
	}
	
	/** @return the name of the JPQL path entity, needed only in closure-table. Returns null, to be overridden. */
	protected String pathEntityName()	{
		return null;
	}
	
	

	/** Throws IllegalArgumentException when node is not persistent, because then it is not an UPDATE. */
	protected void assertUpdate(N node)	{
		if (isPersistent(node) == false)
			throw new IllegalArgumentException("Entity is not persistent! Use addChild() for "+node);
	}

	/** Throws IllegalArgumentException when copy or move could not performed, checks if tree would be copied/moved into itself. */
	protected void copyOrMovePreconditions(N relativeNode, N nodeToMove) {
		if (relativeNode != null && isPersistent(relativeNode) == false || isPersistent(nodeToMove) == false)
			throw new IllegalArgumentException("Node not in tree, or has no root!");
		
		if (relativeNode != null && isChildOf(relativeNode, nodeToMove))
			throw new IllegalArgumentException("Target node is in subtree of source node: target="+relativeNode+", source="+nodeToMove);
	}
	
	/**
	 * Updates nodes from database, overwriting pending local changes.
	 * Needed only when several TreeDao calls happen in same session.
	 * Override this to do nothing when your session is closed or cleared
	 * after any TreeDao call, this will save performance.
	 * <p/>
	 * This is to be called for all nodes affected by an SQL or JPQL update statement.
	 * This method performs a flush() before refresh() to avoid inconsistencies.
	 */
	protected void refresh(List<?> nodesToRefresh)	{
		session.flush(); // be sure to have written anything to database, refresh() does not trigger such
		// fixes HibernateSystemException "this instance does not yet exist as a row in the database"

		for (Object nodeToRefresh : nodesToRefresh)
			session.refresh(nodeToRefresh);
	}

	
	// temporal extensions
	
	/**
	 * Temporal extension. Shared code. Called when querying valid nodes.
	 */
	protected final void beforeFindQuery(String tableAlias, StringBuilder queryText, List<Object> parameters, boolean whereWasAppended, boolean doNotApplyTemporalConditions, boolean invertTemporalConditions) {
		if (invertTemporalConditions == true)	{
			queryText.append(whereWasAppended ? " and " : " where ");
			appendInvalidityCondition(tableAlias, queryText, parameters);
		}
		else if (doNotApplyTemporalConditions == false)	{
			queryText.append(whereWasAppended ? " and " : " where ");
			appendValidityCondition(tableAlias, queryText, parameters);
		}
	}
	
	/**
	 * Temporal extension. Called when querying valid nodes.
	 * Appends the (temporal) validity check condition to passed JPQL statement.
	 * The appended text must NOT start or end with "AND".
	 * Override this to use other validity checks than valid-from and valid-to properties.
	 * This is public to be reused by <code>UniqueTemporalTreeConstraint</code> implementations.
	 * @param tableAlias the alias of the table containing the <i>validTo</i> property, without trailing dot, could be null.
	 * @param queryText the pending JPQL query text looking for valid nodes.
	 * @param parameters the positional arguments for the pending query.
	 */
	public void appendValidityCondition(String tableAlias, StringBuilder queryText, List<Object> parameters) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override appendValidityCondition when validToPropertyName is null!");
		
		QueryBuilderUtil.appendValidityConditions(tableAlias, getValidFromPropertyName(), validFrom(), getValidToPropertyName(), validTo(), queryText, parameters);
	}
	
	/**
	 * Temporal extension. Called when querying removed nodes.
	 * Appends the (temporal) invalidity check condition to passed JPQL statement.
	 * The appended text must NOT start with "and".
	 * Override this to use other validity checks than valid-to property.
	 * @param tableAlias the alias of the table containing the <i>validTo</i> property, without trailing dot, could be null.
	 * @param queryText the pending JPQL query text looking for invalid nodes.
	 * @param parameters the positional arguments for the pending query.
	 */
	protected void appendInvalidityCondition(String tableAlias, StringBuilder queryText, List<Object> parameters) {
		if (getValidToPropertyName() == null)
			throw new IllegalStateException("Please override appendInvalidityCondition when validToPropertyName is null!");
		
		final String validToPropertyName = buildAliasedPropertyName(tableAlias, getValidToPropertyName());
		queryText.append(validToPropertyName+" is not null and "+validToPropertyName+" <= "+buildIndexedPlaceHolder(parameters));
		parameters.add(validTo());
	}

	/** @return "tableAlias.propertyName" when alias is not null, else "propertyName". */
	protected final String buildAliasedPropertyName(String tableAlias, String propertyName) {
		return QueryBuilderUtil.buildAliasedPropertyName(tableAlias, propertyName);
	}
	
	/** @return "?5" when parameters.size() == 4. */
	protected final String buildIndexedPlaceHolder(List<Object> parameters) {
		return QueryBuilderUtil.buildIndexedPlaceHolder(parameters);
	}
	
	/**
	 * Temporal extension. This is called when querying. Override for specific temporal queries.
	 * @return by default <code>new Date()</code> which represents "now".
	 */
	protected Date validFrom()	{
		return new Date();
	}
	
	/**
	 * Temporal extension. This is called when querying. Override for specific temporal queries.
	 * @return by default <code>new Date()</code> which represents "now".
	 */
	protected Date validTo()	{
		return new Date();
	}

	/**
	 * Temporal extension. This is called when historicizing a node.
	 * @return by default <code>new Date()</code> which represents "now".
	 */
	protected Date validToOnRemove()	{
		return new Date();
	}
	
	/**
	 * Temporal extension. This is called when deciding whether a node is valid at given date.
	 * @return true when <code>node.validFrom</code> is null or before or equal to given date,
	 * 		and <code>node.validTo</code> is null or after given date.
	 */
	public boolean isValid(Temporal node, Date validityDate)	{
		return
			(node.getValidFrom() == null || node.getValidFrom().before(validityDate) || node.getValidFrom().equals(validityDate)) &&
			(node.getValidTo() == null || node.getValidTo().after(validityDate));
	}

	/** Temporal extension. Override this in temporal DAOs. Throws RuntimeException. */
	protected String getValidFromPropertyName()	{
		throw new RuntimeException("Must override this for temporal DAO!");
	}
	
	/** Temporal extension. Override this in temporal DAOs. Throws RuntimeException. */
	protected String getValidToPropertyName()	{
		throw new RuntimeException("Must override this for temporal DAO!");
	}
	
}
