package sampler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.Token;
import learning.Model;
import learning.ObjectiveFunction;
import learning.Scorer;
import sampling.Explorer;
import utility.VariableID;
import variables.EntityType;
import variables.MutableEntityAnnotation;
import variables.State;

public class ExhaustiveEntityExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveEntityExplorer.class.getName());
	private AnnotationConfig corpusConfig;

	public ExhaustiveEntityExplorer(AnnotationConfig corpusConfig) {
		this.corpusConfig = corpusConfig;
	}

	public List<State> getNextStates(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		List<Token> tokens = previousState.getDocument().getTokens();
		// Add new entities to empty tokens
		for (Token token : tokens) {

			if (!previousState.tokenHasAnnotation(token)) {
				// Assign new entity to empty token
				Collection<EntityType> entityTypes = corpusConfig.getEntityTypes();
				for (EntityType entityType : entityTypes) {
					State generatedState = new State(previousState);
					MutableEntityAnnotation tokenAnnotation = new MutableEntityAnnotation(generatedState, entityType,
							token.getIndex(), token.getIndex() + 1);
					generatedState.addMutableEntity(tokenAnnotation);
					generatedStates.add(generatedState);
				}
			}

		}
		// Modify existing entities
		Set<VariableID> previousStatesEntityIDs = previousState.getMutableEntityIDs();
		for (VariableID entityID : previousStatesEntityIDs) {
			MutableEntityAnnotation previousStatesEntity = previousState.getMutableEntity(entityID);
			Collection<EntityType> entityTypes = corpusConfig.getEntityTypes();
			// remove the type that this entity already has assigned
			entityTypes.remove(previousStatesEntity.getType());
			// change Type of every entity to every possible type
			for (EntityType entityType : entityTypes) {
				State generatedState = new State(previousState);
				MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
				entity.setType(entityType);
				generatedStates.add(generatedState);
			}
			// Create on state with that particular entity removed
			State generatedState = new State(previousState);
			MutableEntityAnnotation entity = generatedState.getMutableEntity(entityID);
			generatedState.removeMutableEntity(entity);
			generatedStates.add(generatedState);
		}
		// // add an unchanged state
		State generatedState = new State(previousState);
		generatedStates.add(generatedState);
		return generatedStates;
	}
}
