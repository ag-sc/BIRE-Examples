package variables;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class EntityType implements Serializable {

	private String type;
//	private Map<ArgumentRole, Argument> coreArguments;
//
//	private Map<ArgumentRole, Argument> optionalArguments;

	public EntityType(String type) {
		this.type = type;
//		this.coreArguments = new HashMap<ArgumentRole, Argument>();
//		this.optionalArguments = new HashMap<ArgumentRole, Argument>();
	}

//	public EntityType(String type, Map<ArgumentRole, Argument> coreArguments,
//			Map<ArgumentRole, Argument> optionalArguments) {
//		this.type = type;
//		this.coreArguments = coreArguments;
//		this.optionalArguments = optionalArguments;
//	}

	public String getName() {
		return type;
	}
//
//	public Map<ArgumentRole, Argument> getCoreArguments() {
//		return coreArguments;
//	}
//
//	public Map<ArgumentRole, Argument> getOptionalArguments() {
//		return optionalArguments;
//	}

//	@Override
//	public String toString() {
//		return "EntityType [type=" + type + ", coreArguments=" + coreArguments.values() + ", optionalArguments="
//				+ optionalArguments.values() + "]";
//	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EntityType other = (EntityType) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
