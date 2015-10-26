package factors;

import java.util.HashSet;
import java.util.Set;

import templates.AbstractTemplate;
import utility.VariableID;
import variables.AbstractState;
import variables.ArgumentRole;

public class EntityAndArgumentFactor extends AbstractFactor {

	protected VariableID mainEntityID;
	protected VariableID argumentEntityID;
	protected ArgumentRole argumentRole;

	public EntityAndArgumentFactor(AbstractTemplate<? extends AbstractState> template, VariableID mainEntityID,
			ArgumentRole argumentRole, VariableID argumentEntityID) {
		super(template);
		this.mainEntityID = mainEntityID;
		this.argumentEntityID = argumentEntityID;
		this.argumentRole = argumentRole;
	}

	@Override
	public Set<VariableID> getVariableIDs() {
		Set<VariableID> entities = new HashSet<>();
		entities.add(mainEntityID);
		entities.add(argumentEntityID);
		return entities;
	}

	public VariableID getMainEntityID() {
		return mainEntityID;
	}

	public VariableID getArgumentEntityID() {
		return argumentEntityID;
	}

	public ArgumentRole getArgumentRole() {
		return argumentRole;
	}

	@Override
	public String toString() {
		return "EntityAndArgumentFactor [factorID=" + factorID + ", template=" + template + ", mainEntityID="
				+ mainEntityID + ", argumentRole=" + argumentRole + ", argumentEntityID=" + argumentEntityID + "]";
	}

}
