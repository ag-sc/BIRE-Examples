package corpus.parser.bionlp.annotations;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import corpus.parser.bionlp.BratAnnotationManager;
import utility.ID;

public class BratAttributeAnnotation extends BratAnnotation {

	public final static Pattern pattern;
	public final static Pattern idPattern;

	static {
		String id = "((A|M)\\d+)";
		String role = "(\\S+)";
		String arg = "((T|E)\\d+)";
		String args = "( " + arg + ")*";
		String all = "^" + id + "\t" + role + args + "$";
		pattern = Pattern.compile(all);
		idPattern = Pattern.compile(id);
	}

	private String role;
	private List<ID<? extends BratAnnotation>> arguments = new ArrayList<>();

	public String getRole() {
		return role;
	}

	public List<ID<? extends BratAnnotation>> getArguments() {
		return arguments;
	}

	public BratAttributeAnnotation(BratAnnotationManager manager, String id, String role,
			List<ID<? extends BratAnnotation>> arguments) {
		super(manager, id);
		this.role = role;
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "AttributeAnnotation [id=" + id + ", role=" + role + ", arguments=" + arguments + "]";
	}

}
