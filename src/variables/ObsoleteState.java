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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;

import changes.StateChange;
import corpus.Document;
import corpus.Token;
import factors.FactorGraph;
import utility.StateID;
import utility.VariableID;

public class ObsoleteState extends AbstractState implements Serializable {

	private static Logger log = LogManager.getFormatterLogger(State.class.getName());

	private static final String GENERATED_ENTITY_ID_PREFIX = "G";
	private static final DecimalFormat scoreFormat = new DecimalFormat("0.00000");
	private static final DecimalFormat stateIDFormat = new DecimalFormat("00000000");

	private static int stateIdIndex = 0;
	private int entityIdIndex = 0;

	/**
	 * Since Entities only hold weak pointer via references to one another,
	 * using a Map is sensible to enable an efficient access to the entities.
	 */
	// private Map<EntityID, EntityAnnotation> entities = new HashMap<>();
	private Map<VariableID, ImmutableEntityAnnotation> immutableEntities = new HashMap<>();
	private Map<VariableID, MutableEntityAnnotation> mutableEntities = new HashMap<>();
	// TODO Maybe not really needed anymore
	private Map<Integer, Set<VariableID>> tokenToEntities = new HashMap<>();

	/**
	 * The state needs to keep track of the changes that were made to its
	 * entities in order to allow for efficient computation of factors and their
	 * features. Note: The changes are not stored in the Entity object since it
	 * is more efficient to just clear this map instead of iterating over all
	 * entities and reset a field in order to mark all entities as unchanged.
	 */
	// private Multimap<VariableID, StateChange> changedEntities =
	// HashMultimap.create();
	private Document document;

	private ObsoleteState() {
		super();
	}

	/**
	 * This Copy Constructor creates an exact copy of itself including all
	 * internal annotations.
	 * 
	 * @param state
	 */
	public ObsoleteState(State state) {
		this();
		this.entityIdIndex = state.entityIdIndex;
		this.document = state.document;
		for (ImmutableEntityAnnotation e : state.immutableEntities.values()) {
			this.immutableEntities.put(e.getID(), new ImmutableEntityAnnotation(this, e));
		}
		for (MutableEntityAnnotation e : state.mutableEntities.values()) {
			this.mutableEntities.put(e.getID(), new MutableEntityAnnotation(this, e));
		}
		for (Entry<Integer, Set<VariableID>> e : state.tokenToEntities.entrySet()) {
			this.tokenToEntities.put(e.getKey(), new HashSet<VariableID>(e.getValue()));
		}
		this.modelScore = state.modelScore;
		this.objectiveScore = state.objectiveScore;
		// this.changedEntities = HashMultimap.create(state.changedEntities);
		this.factorGraph = new FactorGraph(state.factorGraph);
	}

	public ObsoleteState(Document document) {
		this();
		this.document = document;
	}

	@Override
	public State duplicate() {
		State cloned = new State(this);
		return cloned;
	}

	public Document getDocument() {
		return document;
	}

	// @Override
	// public Multimap<VariableID, StateChange> getChangedVariables() {
	// return changedEntities;
	// }

	public void addImmutableEntity(ImmutableEntityAnnotation entity) {
		log.debug("State %s: ADD new fixed annotation: %s", this.getID(), entity);
		immutableEntities.put(entity.getID(), entity);
		addToTokenToEntityMapping(entity);
		// changedEntities.put(entity.getID(), StateChange.ADD_ANNOTATION);
	}

	public void addMutableEntity(MutableEntityAnnotation entity) {
		log.debug("State %s: ADD new annotation: %s", this.getID(), entity);
		mutableEntities.put(entity.getID(), entity);
		addToTokenToEntityMapping(entity);
		// changedEntities.put(entity.getID(), StateChange.ADD_ANNOTATION);
	}

	public void removeMutableEntity(MutableEntityAnnotation entity) {
		log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
		mutableEntities.remove(entity.getID());
		mutableEntities.put(entity.getID(), entity);
		removeFromTokenToEntityMapping(entity);
		removeReferencingArguments(entity);
		// changedEntities.put(entity.getID(), StateChange.REMOVE_ANNOTATION);
	}

	public void removeMutableEntity(VariableID entityID) {
		MutableEntityAnnotation entity = getMutableEntity(entityID);
		if (entity != null) {
			log.debug("State %s: REMOVE annotation: %s", this.getID(), entity);
			mutableEntities.remove(entityID);
			removeFromTokenToEntityMapping(entity);
			removeReferencingArguments(entity);
			// changedEntities.put(entityID, StateChange.REMOVE_ANNOTATION);
		} else {
			log.warn("Cannot remove entity %s. Entity not found!", entityID);
		}
	}

	public Set<VariableID> getEntityIDs() {
		Set<VariableID> entityIDs = new HashSet<>();
		entityIDs.addAll(getImmutableEntityIDs());
		entityIDs.addAll(getMutableEntityIDs());
		return entityIDs;
	}

	public Set<VariableID> getMutableEntityIDs() {
		return mutableEntities.keySet();
	}

	public Set<VariableID> getImmutableEntityIDs() {
		return immutableEntities.keySet();
	}

	public Collection<AbstractEntityAnnotation> getEntities() {
		Collection<AbstractEntityAnnotation> entites = new ArrayList<>();
		entites.addAll(getMutableEntities());
		entites.addAll(getImmutableEntities());
		return entites;
	}

	public Collection<MutableEntityAnnotation> getMutableEntities() {
		return mutableEntities.values();
	}

	public Collection<ImmutableEntityAnnotation> getImmutableEntities() {
		return immutableEntities.values();
	}

	public AbstractEntityAnnotation getEntity(VariableID id) {
		if (mutableEntities.containsKey(id))
			return mutableEntities.get(id);
		else
			return immutableEntities.get(id);
	}

	public MutableEntityAnnotation getMutableEntity(VariableID id) {
		return mutableEntities.get(id);
	}

	public ImmutableEntityAnnotation getImmutableEntity(VariableID id) {
		return immutableEntities.get(id);
	}

	public boolean tokenHasAnnotation(Token token) {
		Set<VariableID> entitiesForToken = tokenToEntities.get(token.getIndex());
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

	protected void removeFromTokenToEntityMapping(AbstractEntityAnnotation entityAnnotation) {
		for (int i = entityAnnotation.getBeginTokenIndex(); i < entityAnnotation.getEndTokenIndex(); i++) {
			Set<VariableID> entitiesForToken = tokenToEntities.get(i);
			if (entitiesForToken != null) {
				entitiesForToken.remove(entityAnnotation.getID());
			}
		}
	}

	protected void addToTokenToEntityMapping(AbstractEntityAnnotation entityAnnotation) {
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
	private void removeReferencingArguments(MutableEntityAnnotation removedEntity) {
		for (MutableEntityAnnotation e : mutableEntities.values()) {
			Multimap<ArgumentRole, VariableID> arguments = e.getArguments();
			for (Entry<ArgumentRole, VariableID> entry : arguments.entries()) {
				if (entry.getValue().equals(removedEntity.getID())) {
					e.removeArgument(entry.getKey(), entry.getValue());
					/*
					 * Note: no need to mark entity as changed here. This will
					 * happen in the entity's removeArgument-method
					 */
				}
			}
		}
	}

	protected VariableID generateEntityID() {
		String id = GENERATED_ENTITY_ID_PREFIX + entityIdIndex;
		entityIdIndex++;
		return new VariableID(id);
	}

	public Map<Integer, Set<VariableID>> getTokenToEntityMapping() {
		return tokenToEntities;
	}

	public void onEntityChanged(MutableEntityAnnotation entity, StateChange change) {
		// changedEntities.put(entity.getID(), change);
	}

	// @Override
	// public void markAsUnchanged() {
	// changedEntities.clear();
	// }

	// <T1-Protein: tumor necrosis <T2-Protein: factor :T1:T2>
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
			List<AbstractEntityAnnotation> begin = new ArrayList<>();
			List<AbstractEntityAnnotation> end = new ArrayList<>();
			for (VariableID entityID : entities) {
				AbstractEntityAnnotation e = getEntity(entityID);
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

	private void buildTokenPrefix(StringBuilder builder, List<AbstractEntityAnnotation> begin) {
		builder.append("[");
		for (AbstractEntityAnnotation e : begin) {
			builder.append(e.getID());
			builder.append("-");
			builder.append(e.getType().getName());
			builder.append("(");
			builder.append(e.getArguments());
			builder.append("):");
		}
		builder.append(" ");
	}

	private void buildTokenSuffix(StringBuilder builder, List<AbstractEntityAnnotation> end) {
		for (AbstractEntityAnnotation e : end) {
			builder.append(":");
			builder.append(e.getID());
		}
		builder.append("]");
		builder.append(" ");
	}

	public String toDetailedString() {
		StringBuilder builder = new StringBuilder();
		for (AbstractEntityAnnotation e : getEntities()) {
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
