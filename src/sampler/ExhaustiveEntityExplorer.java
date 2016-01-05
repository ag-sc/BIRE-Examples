package sampler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.EntityTypeDefinition;
import corpus.Token;
import sampling.Explorer;
import utility.VariableID;
import variables.EntityAnnotation;
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
		Set<VariableID> previousStatesEntityIDs = previousState.getEditableEntityIDs();
		for (VariableID entityID : previousStatesEntityIDs) {
			EntityAnnotation previousStatesEntity = previousState.getEntity(entityID);
			Collection<EntityTypeDefinition> entityTypeDefinitions = corpusConfig.getEntityTypeDefinitions();
			// remove the type that this entity already has assigned
			entityTypeDefinitions.remove(previousStatesEntity.getType());
			// change Type of every entity to every possible type
			for (EntityTypeDefinition entityTypeDefinition : entityTypeDefinitions) {
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(entityID);
				entity.setType(entityTypeDefinition.getInstance());
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
				Collection<EntityTypeDefinition> entityTypeDefinitions = corpusConfig.getEntityTypeDefinitions();
				for (EntityTypeDefinition entityTypeDefinition : entityTypeDefinitions) {
					State generatedState = new State(previousState);
					EntityAnnotation tokenAnnotation = new EntityAnnotation(generatedState,
							entityTypeDefinition.getInstance(), token.getIndex(), token.getIndex() + 1);
					generatedState.addEntity(tokenAnnotation);
					generatedStates.add(generatedState);
				}
			}
		}
		return generatedStates;
	}
}
