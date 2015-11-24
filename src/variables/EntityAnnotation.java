package variables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.Token;
import utility.VariableID;

public class EntityAnnotation extends AbstractVariable<State>implements Serializable {
	/**
	 * This number specifies the token index (!! not character offset) of the
	 * first token that this annotation references.
	 */
	protected int beginTokenIndex;
	/**
	 * This number specifies the token index (!! not character offset) of the
	 * last token that this annotation references.
	 */
	protected int endTokenIndex;

	protected EntityType type;

	protected String originalText;
	protected int originalStart;
	protected int originalEnd;

	protected final State state;
	/**
	 * This flag can be used to declare an annotation as fixed, so that the
	 * Sampler/Explorers don't change this annotation in the processing of
	 * sample generation.
	 */
	protected boolean isPriorKnowledge = false;
	/**
	 * We need to keep weak references (IDs only) to other entities in order to
	 * enable an efficient cloning of states and their entities during the
	 * training phase of the model. The state in which this entity lives offers
	 * methods to resolve this weak reference.
	 */
	protected Multimap<ArgumentRole, VariableID> arguments;

	public EntityAnnotation(State state, VariableID id, EntityType entityType,
			Multimap<ArgumentRole, VariableID> arguments, int start, int end) {
		super(id);
		this.state = state;
		this.type = entityType;
		this.beginTokenIndex = start;
		this.endTokenIndex = end;
		this.arguments = arguments;
	}

	public EntityAnnotation(State state, EntityAnnotation e) {
		super(e.id);
		this.state = state;
		this.type = e.type;
		this.beginTokenIndex = e.beginTokenIndex;
		this.endTokenIndex = e.endTokenIndex;
		this.arguments = HashMultimap.create(e.arguments);
		this.originalText = e.originalText;
		this.originalStart = e.originalStart;
		this.originalEnd = e.originalEnd;
		this.isPriorKnowledge = e.isPriorKnowledge;
	}

	public EntityAnnotation(State state, EntityType entityType, Multimap<ArgumentRole, VariableID> arguments, int start,
			int end) {
		this(state, state.generateEntityID(), entityType, arguments, start, end);
	}

	public EntityAnnotation(State state, String id, EntityType entityType, Multimap<ArgumentRole, VariableID> arguments,
			int start, int end) {
		this(state, new VariableID(id), entityType, arguments, start, end);
	}

	public EntityAnnotation(State state, EntityType entityType, int start, int end) {
		this(state, state.generateEntityID(), entityType, HashMultimap.create(), start, end);
	}

	public EntityAnnotation(State state, String id, EntityType entityType, int start, int end) {
		this(state, new VariableID(id), entityType, HashMultimap.create(), start, end);
	}

	public State getState() {
		return state;
	}

	public EntityType getType() {
		return type;
	}

	public int getBeginTokenIndex() {
		return beginTokenIndex;
	}

	public int getEndTokenIndex() {
		return endTokenIndex;
	}

	public Multimap<ArgumentRole, VariableID> getReadOnlyArguments() {
		return HashMultimap.create(arguments);
	}

	/**
	 * Returns the entity that is associated with the specified ID, using this
	 * entities's parent state.
	 * 
	 * @param id
	 * @return
	 */
	public EntityAnnotation getEntity(VariableID id) {
		return state.getEntity(id);
	}

	public List<Token> getTokens() {
		List<Token> tokens = new ArrayList<Token>();
		for (int i = beginTokenIndex; i < endTokenIndex; i++)
			tokens.add(state.getDocument().getTokens().get(i));
		return tokens;
	}

	public String getOriginalText() {
		return originalText;
	}

	public void setOriginalText(String originalText) {
		this.originalText = originalText;
	}

	public int getOriginalStart() {
		return originalStart;
	}

	public void setOriginalStart(int originalStart) {
		this.originalStart = originalStart;
	}

	public int getOriginalEnd() {
		return originalEnd;
	}

	public void setOriginalEnd(int originalEnd) {
		this.originalEnd = originalEnd;
	}

	public void setType(EntityType type) {
		this.type = type;
	}

	/**
	 * Marks this annotation as fixed. That means, that the sampler should not
	 * changes this annotation during the sampling process. This is used to
	 * initialize a state with fixed prior knowledge.
	 * 
	 * @param b
	 */
	public void setPriorKnowledge(boolean b) {
		this.isPriorKnowledge = b;
	}

	public boolean isPriorKnowledge() {
		return isPriorKnowledge;
	}

	public void setBeginTokenIndex(int beginTokenIndex) {
		// TODO this handling of changes is not perfectly efficient and allows
		// errors and inconsistencies if applied wrongly
		state.removeFromTokenToEntityMapping(this);
		this.beginTokenIndex = beginTokenIndex;
		state.addToTokenToEntityMapping(this);
	}

	public void setEndTokenIndex(int endTokenIndex) {
		// TODO this handling of changes is not perfectly efficient and allows
		// errors and inconsistencies if applied wrongly

		state.removeFromTokenToEntityMapping(this);
		this.endTokenIndex = endTokenIndex;
		state.addToTokenToEntityMapping(this);
	}

	public void addArgument(ArgumentRole role, VariableID entityID) {
		arguments.put(role, entityID);
	}

	public void removeArgument(ArgumentRole role, VariableID entity) {
		arguments.remove(role, entity);
	}

	public String getText() {
		List<Token> tokens = getTokens();
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			Token token = tokens.get(i);
			builder.append(token.getText());

			/*
			 * Add a whitespace if the following token does not connect directly
			 * to this one (e.g not "interleukin" and "-")
			 */
			if (i < tokens.size() - 1 && tokens.get(i).getTo() < tokens.get(i + 1).getFrom()) {
				builder.append(" ");
			}
		}
		return builder.toString();
	}

	@Override
	public String toString() {
		return "EntityAnnotation [id=" + id + ", begin=" + beginTokenIndex + ", end=" + endTokenIndex + ", type="
				+ type.getName() + ", arguments=" + arguments + "]";
	}

	public String toDetailedString() {
		return "EntityAnnotation [id=" + id + ", begin=" + beginTokenIndex + ", end=" + endTokenIndex + " => \""
				+ getText() + "\", type=" + type.getName() + ", arguments=" + arguments + "]";
	}

}
