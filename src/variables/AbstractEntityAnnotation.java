package variables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.Token;
import utility.VariableID;
import variables.Variable;

public abstract class AbstractEntityAnnotation implements Variable, Serializable {
	protected State state;
	protected VariableID id;
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
	/**
	 * We need to keep weak references (IDs only) to other entities in order to
	 * enable an efficient cloning of states and their entities during the
	 * training phase of the model. The state in which this entity lives offers
	 * methods to resolve this weak reference.
	 */
	protected Multimap<ArgumentRole, VariableID> arguments;

	public AbstractEntityAnnotation(State state, VariableID id, EntityType entityType,
			Multimap<ArgumentRole, VariableID> arguments, int start, int end) {
		this.state = state;
		this.id = id;
		this.type = entityType;
		this.arguments = arguments;
		this.beginTokenIndex = start;
		this.endTokenIndex = end;
	}

	public AbstractEntityAnnotation(State state, EntityType entityType, Multimap<ArgumentRole, VariableID> arguments,
			int start, int end) {
		this(state, state.generateEntityID(), entityType, arguments, start, end);
	}

	public AbstractEntityAnnotation(State state, String id, EntityType entityType,
			Multimap<ArgumentRole, VariableID> arguments, int start, int end) {
		this(state, new VariableID(id), entityType, arguments, start, end);
	}

	public AbstractEntityAnnotation(State state, EntityType entityType, int start, int end) {
		this(state, state.generateEntityID(), entityType, HashMultimap.create(), start, end);
	}

	public AbstractEntityAnnotation(State state, String id, EntityType entityType, int start, int end) {
		this(state, new VariableID(id), entityType, HashMultimap.create(), start, end);
	}

	@Override
	public VariableID getID() {
		return id;
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

	public Multimap<ArgumentRole, VariableID> getArguments() {
		return HashMultimap.create(arguments);
	}

	/**
	 * Returns the entity that is associated with the specified ID, using this
	 * entities's parent state.
	 * 
	 * @param id
	 * @return
	 */
	public AbstractEntityAnnotation getEntity(VariableID id) {
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
