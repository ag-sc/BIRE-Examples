package sampler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import corpus.AnnotationConfig;
import corpus.EntityTypeDefinition;
import sampling.Explorer;
import utility.VariableID;
import variables.ArgumentRole;
import variables.EntityAnnotation;
import variables.State;

public class ExhaustiveRelationExplorer implements Explorer<State> {

	private static Logger log = LogManager.getFormatterLogger(ExhaustiveRelationExplorer.class.getName());

	private AnnotationConfig corpusConfig;

	public ExhaustiveRelationExplorer(AnnotationConfig corpusConfig) {
		this.corpusConfig = corpusConfig;
	}

	public List<State> getNextStates(State previousState) {
		List<State> generatedStates = new ArrayList<State>();
		for (EntityAnnotation entity : previousState.getEditableEntities()) {
			generatedStates.addAll(addArguments(previousState, entity));
			generatedStates.addAll(removeArguments(previousState, entity));
			generatedStates.addAll(changeArgumentRoles(previousState, entity));
			generatedStates.addAll(changeArgumentEntities(previousState, entity));

		}
		generatedStates.add(new State(previousState));
		return generatedStates;

	}

	private List<State> addArguments(State previousState, EntityAnnotation previousStatesEntity) {
		List<State> generatedStates = new ArrayList<State>();
		List<EntityAnnotation> entities = new ArrayList<>(previousState.getEntities());
		entities.remove(previousStatesEntity);
		if (!entities.isEmpty()) {
			List<ArgumentRole> unassignedRoles = new ArrayList<>();
			EntityTypeDefinition entityTypeDefinition = corpusConfig
					.getEntityTypeDefinition(previousStatesEntity.getType());
			unassignedRoles.addAll(entityTypeDefinition.getCoreArguments().keySet());
			unassignedRoles.addAll(entityTypeDefinition.getOptionalArguments().keySet());
			// remove already assigned roles
			unassignedRoles.removeAll(previousStatesEntity.getReadOnlyArguments().keySet());

			if (!unassignedRoles.isEmpty()) {
				for (EntityAnnotation argumentEntity : entities) {
					for (ArgumentRole argumentRole : unassignedRoles) {
						State generatedState = new State(previousState);
						EntityAnnotation entity = generatedState.getEntity(previousStatesEntity.getID());
						entity.addArgument(argumentRole, argumentEntity.getID());
						log.debug("\t%s + %s:%s", entity.getID(), argumentRole, argumentEntity.getID());
						generatedStates.add(generatedState);
					}
				}
			} else {
				log.debug("\t%s (%s): No unassigned arguments left", previousStatesEntity.getID(),
						previousStatesEntity.getType().getName());
			}
		} else {
			log.debug("\t%s (%s): No entities for argument existing", previousStatesEntity.getID(),
					previousStatesEntity.getType().getName());
		}
		return generatedStates;
	}

	private List<State> removeArguments(State previousState, EntityAnnotation previousStatesEntity) {
		List<State> generatedStates = new ArrayList<State>();
		for (Entry<ArgumentRole, VariableID> argumentEntry : previousStatesEntity.getReadOnlyArguments().entries()) {
			State generatedState = new State(previousState);
			EntityAnnotation entity = generatedState.getEntity(previousStatesEntity.getID());
			entity.removeArgument(argumentEntry.getKey(), argumentEntry.getValue());
			generatedStates.add(generatedState);
		}
		return generatedStates;
	}

	private List<State> changeArgumentRoles(State previousState, EntityAnnotation previousStatesEntity) {
		List<State> generatedStates = new ArrayList<State>();
		EntityTypeDefinition type = corpusConfig.getEntityTypeDefinition(previousStatesEntity.getType());

		for (Entry<ArgumentRole, VariableID> argumentToChange : previousStatesEntity.getReadOnlyArguments().entries()) {
			List<ArgumentRole> possibleNewRoles = new ArrayList<>();
			possibleNewRoles.addAll(type.getCoreArguments().keySet());
			possibleNewRoles.addAll(type.getOptionalArguments().keySet());
			possibleNewRoles.remove(argumentToChange.getKey());

			for (ArgumentRole newRole : possibleNewRoles) {
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(previousStatesEntity.getID());
				entity.removeArgument(argumentToChange.getKey(), argumentToChange.getValue());
				entity.addArgument(newRole, argumentToChange.getValue());
				generatedStates.add(generatedState);
			}
		}
		return generatedStates;
	}

	private List<State> changeArgumentEntities(State previousState, EntityAnnotation previousStatesEntity) {
		List<State> generatedStates = new ArrayList<State>();
		for (Entry<ArgumentRole, VariableID> argumentToChange : previousStatesEntity.getReadOnlyArguments().entries()) {
			List<VariableID> entityIDs = new ArrayList<VariableID>(previousState.getEntityIDs());
			// remove parent entity from list of possible arguments
			entityIDs.remove(previousStatesEntity.getID());
			// remove previous entity from list of possible arguments
			entityIDs.remove(argumentToChange.getValue());

			for (VariableID newArgumentID : entityIDs) {
				State generatedState = new State(previousState);
				EntityAnnotation entity = generatedState.getEntity(previousStatesEntity.getID());
				entity.addArgument(argumentToChange.getKey(), newArgumentID);
				generatedStates.add(generatedState);
			}
		}
		return generatedStates;
	}

}
