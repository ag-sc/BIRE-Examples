package corpus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import variables.EntityType;

public class AnnotationConfig implements Serializable {

	private Map<String, EntityTypeDefinition> entityTypes = new HashMap<String, EntityTypeDefinition>();

	public Collection<EntityTypeDefinition> getEntityTypeDefinitions() {
		return new ArrayList<EntityTypeDefinition>(entityTypes.values());
	}

	public void addEntityType(EntityTypeDefinition entityType) {
		entityTypes.put(entityType.getName(), entityType);
	}

	public EntityTypeDefinition getEntityTypeDefinition(String type) {
		return entityTypes.get(type);
	}

	public EntityTypeDefinition getEntityTypeDefinition(EntityType type) {
		return entityTypes.get(type.getName());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		for (EntityTypeDefinition entityType : entityTypes.values()) {
			b.append(entityType);
			b.append("\n");
		}
		return "AnnotationConfig:\n" + b;
	}

}
