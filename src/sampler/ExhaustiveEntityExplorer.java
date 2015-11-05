package sampler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.Token;
import sampling.Explorer;
import utility.VariableID;
import variables.EntityAnnotation;
import variables.EntityType;
import variables.State;

public class ExhaustiveEntityExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveEntityExplorer.class.getName());
	private AnnotationConfig corpusConfig;

	public ExhaustiveEntityExplorer(AnnotationConfig corpusConfig) {
		this.corpusConfig = corpusConfig;
	}

	public List<State> getNextStates(State previousState) {
		List<State> generatedStates = new ArrayList<>();
		// Add new entities to empty tokens
		generatedStates.addAll(generateStatesForTokens(previousState));
		// Modify existing entities
		generatedStates.addAll(generateStatesForEntities(previousState));
		// add an unchanged state
		State generatedState = new State(previousState);
		generatedStates.add(generatedState);
		return generatedStates;
	}

	private List<State> generateStatesForEntities(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		Set<VariableID> previousStatesEntityIDs = previousState.getNonFixedEntityIDs();
		for (VariableID entityID : previousStatesEntityIDs) {
			EntityAnnotation previousStatesEntity = previousState.getEntity(entityID);
			Collection<EntityType> entityTypes = corpusConfig.getEntityTypes();
			// remove the type that this entity already has assigned
			entityTypes.remove(previousStatesEntity.getType());
			// change Type of every entity to every possible type
			for (EntityType entityType : entityTypes) {
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(entityID);
				entity.setType(entityType);
				generatedStates.add(generatedState);
			}
			// Create on state with that particular entity removed
			State generatedState = new State(previousState);
			EntityAnnotation entity = generatedState.getEntity(entityID);
			generatedState.removeEntity(entity);
			generatedStates.add(generatedState);
		}
		return generatedStates;
	}

	private List<State> generateStatesForTokens(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		List<Token> tokens = previousState.getDocument().getTokens();
		for (Token token : tokens) {
			if (!previousState.tokenHasAnnotation(token)) {
				// Assign new entity to empty token
				Collection<EntityType> entityTypes = corpusConfig.getEntityTypes();
				for (EntityType entityType : entityTypes) {
					State generatedState = new State(previousState);
					EntityAnnotation tokenAnnotation = new EntityAnnotation(generatedState, entityType,
							token.getIndex(), token.getIndex() + 1);
					generatedState.addEntity(tokenAnnotation);
					generatedStates.add(generatedState);
				}
			}
		}
		return generatedStates;
	}
}
