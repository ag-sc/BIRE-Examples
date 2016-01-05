package variables;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import corpus.Document;
import corpus.Token;
import factors.FactorGraph;
import utility.VariableID;

public class State extends AbstractState implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(State.class.getName());

	private static final String GENERATED_ENTITY_ID_PREFIX = "G";
	private static final DecimalFormat scoreFormat = new DecimalFormat("0.00000");

	/**
	 * Since Entities only hold weak pointer via references to one another,
	 * using a Map is sensible to enable an efficient access to the entities.
	 */
	private Map<VariableID, EntityAnnotation> entities = new HashMap<>();
	// TODO Maybe not really needed anymore
	private Map<Integer, Set<VariableID>> tokenToEntities = new HashMap<>();

	private AtomicInteger entityIDIndex = new AtomicInteger();
	/**
	 * The state needs to keep track of the changes that were made to its
	 * entities in order to allow for efficient computation of factors and their
	 * features. Note: The changes are not stored in the Entity object since it
	 * is more efficient to just clear this map instead of iterating over all
	 * entities and reset a field in order to mark all entities as unchanged.
	 */
	private Document<State> document;

	private State() {
		super();
	}

	/**
	 * This Copy Constructor creates an exact copy of itself including all
	 * internal annotations.
	 * 
	 * @param state
	 */
	public State(State state) {
		this();
		this.entityIDIndex = new AtomicInteger(state.entityIDIndex.get());
		this.document = state.document;
		this.factorGraph = new FactorGraph(state.factorGraph);
		for (EntityAnnotation e : state.entities.values()) {
			this.entities.put(e.getID(), new EntityAnnotation(this, e));
		}
		for (Entry<Integer, Set<VariableID>> e : state.tokenToEntities.entrySet()) {
			this.tokenToEntities.put(e.getKey(), new HashSet<VariableID>(e.getValue()));
		}
		this.modelScore = state.modelScore;
		this.objectiveScore = state.objectiveScore;
	}

	public State(Document<State> document) {
		this();
		this.document = document;
	}

	public Document<State> getDocument() {
		return document;
	}

	public void addEntity(EntityAnnotation entity) {
		log.debug("State %s: ADD new annotation: %s", this.getID(), entity);
		entities.put(entity.getID(), entity);
		addToTokenToEntityMapping(entity);
		// changedEntities.put(entity.getID(), StateChange.ADD_ANNOTATION);
	}

	public void removeEntity(EntityAnnotation entity) {
		log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
		entities.remove(entity.getID());
		// entities.put(entity.getID(), entity);
		removeFromTokenToEntityMapping(entity);
		removeReferencingArguments(entity);
		// changedEntities.put(entity.getID(), StateChange.REMOVE_ANNOTATION);
	}

	public void removeEntity(VariableID entityID) {
		EntityAnnotation entity = getEntity(entityID);
		if (entity != null) {
			log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
			entities.remove(entityID);
			removeFromTokenToEntityMapping(entity);
			removeReferencingArguments(entity);
			// changedEntities.put(entityID, StateChange.REMOVE_ANNOTATION);
		} else {
			log.warn("Cannot remove entity %s. Entity not found!", entityID);
		}
	}

	/**
	 * Returns ALL entity IDs in this state. This includes entities marked as
	 * fixed. Explorers should consider using the getNonFixedEntityIDs() method.
	 * 
	 * @return
	 */

	public Set<VariableID> getEntityIDs() {
		return entities.keySet();
	}

	/**
	 * Returns ALL entities in this state. This includes entities marked as
	 * fixed. Explorers should consider using the getNonFixedEntities() method.
	 * 
	 * @return
	 */
	public Collection<EntityAnnotation> getEntities() {
		return entities.values();
	}

	public EntityAnnotation getEntity(VariableID id) {
		return entities.get(id);
	}

	public boolean tokenHasAnnotation(Token token) {
		Set<VariableID> entitiesForToken = tokenToEntities.get(token.getIndex());
		return entitiesForToken != null && !entitiesForToken.isEmpty();
	}

	public boolean tokenHasAnnotation(int tokenIndex) {
		if (tokenIndex >= document.getTokens().size()) {
			log.error("Token index %s exceeds bounds of document of length %s", tokenIndex,
					document.getTokens().size());
		}
		Set<VariableID> entitiesForToken = tokenToEntities.get(tokenIndex);
		return entitiesForToken != null && !entitiesForToken.isEmpty();
	}

	public Set<VariableID> getAnnotationsForToken(Token token) {
		return getAnnotationsForToken(token.getIndex());
	}

	public Set<VariableID> getAnnotationsForToken(int tokenIndex) {
		Set<VariableID> entitiesForToken = tokenToEntities.get(tokenIndex);
		if (entitiesForToken == null) {
			entitiesForToken = new HashSet<VariableID>();
			tokenToEntities.put(tokenIndex, entitiesForToken);
		}
		return entitiesForToken;
	}

	protected void removeFromTokenToEntityMapping(EntityAnnotation entityAnnotation) {
		for (int i = entityAnnotation.getBeginTokenIndex(); i < entityAnnotation.getEndTokenIndex(); i++) {
			Set<VariableID> entitiesForToken = tokenToEntities.get(i);
			if (entitiesForToken != null) {
				entitiesForToken.remove(entityAnnotation.getID());
			}
		}
	}

	protected void addToTokenToEntityMapping(EntityAnnotation entityAnnotation) {
		for (int i = entityAnnotation.getBeginTokenIndex(); i < entityAnnotation.getEndTokenIndex(); i++) {
			Set<VariableID> entitiesForToken = tokenToEntities.get(i);
			if (entitiesForToken == null) {
				entitiesForToken = new HashSet<VariableID>();
				tokenToEntities.put(i, entitiesForToken);
			}
			entitiesForToken.add(entityAnnotation.getID());
		}
	}

	/**
	 * This function iterates over all entities and all of their arguments to
	 * remove all reference to the given entity.
	 * 
	 * @param removedEntity
	 */
	private void removeReferencingArguments(EntityAnnotation removedEntity) {
		for (EntityAnnotation e : entities.values()) {
			Multimap<ArgumentRole, VariableID> arguments = e.getReadOnlyArguments();
			for (Entry<ArgumentRole, VariableID> entry : arguments.entries()) {
				if (entry.getValue().equals(removedEntity.getID())) {
					e.removeArgument(entry.getKey(), entry.getValue());
				}
			}
		}
	}

	protected VariableID generateEntityID() {
		int currentID = entityIDIndex.getAndIncrement();
		String id = GENERATED_ENTITY_ID_PREFIX + currentID;
		return new VariableID(id);
	}

	public Map<Integer, Set<VariableID>> getTokenToEntityMapping() {
		return tokenToEntities;
	}

	/**
	 * Returns all those entities that are not marked as prior knowledge. This
	 * function is especially useful for the explorers (since they should not
	 * alter fixed variables).
	 * 
	 * @return
	 */
	public Collection<EntityAnnotation> getEditableEntities() {
		return entities.values().stream().filter(e -> !e.isPriorKnowledge).collect(Collectors.toList());
	}

	/**
	 * Returns all those entity IDs for which the entities are not marked as
	 * prior knowledge. This function is especially useful for the explorers
	 * (since they should not alter fixed variables).
	 * 
	 * @return
	 */
	public Set<VariableID> getEditableEntityIDs() {
		return entities.values().stream().filter(e -> !e.isPriorKnowledge).map(e -> e.getID())
				.collect(Collectors.toSet());
	}

	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ID:");
		builder.append(id);
		builder.append(" [");
		builder.append(scoreFormat.format(modelScore));
		builder.append("]: ");
		builder.append(" [");
		builder.append(scoreFormat.format(objectiveScore));
		builder.append("]: ");
		for (Token t : document.getTokens()) {
			Set<VariableID> entities = getAnnotationsForToken(t);
			List<EntityAnnotation> begin = new ArrayList<>();
			List<EntityAnnotation> end = new ArrayList<>();
			for (VariableID entityID : entities) {
				EntityAnnotation e = getEntity(entityID);
				if (e.getBeginTokenIndex() == t.getIndex())
					begin.add(e);
				if (e.getEndTokenIndex() == t.getIndex() + 1)
					end.add(e);
			}
			if (!begin.isEmpty())
				buildTokenPrefix(builder, begin);
			builder.append(t.getText());
			builder.append(" ");
			if (!end.isEmpty())
				buildTokenSuffix(builder, end);
		}
		return builder.toString();
	}

	private void buildTokenPrefix(StringBuilder builder, List<EntityAnnotation> begin) {
		builder.append("[");
		for (EntityAnnotation e : begin) {
			builder.append(e.getID());
			builder.append("-");
			builder.append(e.getType().getName());
			builder.append("(");
			builder.append(e.getReadOnlyArguments());
			builder.append("):");
		}
		builder.append(" ");
	}

	private void buildTokenSuffix(StringBuilder builder, List<EntityAnnotation> end) {
		for (EntityAnnotation e : end) {
			builder.append(":");
			builder.append(e.getID());
		}
		builder.append("]");
		builder.append(" ");
	}

	public String toDetailedString() {
		StringBuilder builder = new StringBuilder();
		for (EntityAnnotation e : getEntities()) {
			builder.append(e);
			builder.append("\n");
		}
		for (Entry<Integer, Set<VariableID>> e : getTokenToEntityMapping().entrySet()) {
			builder.append(e);
			builder.append("\n");
		}
		return builder.toString().trim();
	}
}
