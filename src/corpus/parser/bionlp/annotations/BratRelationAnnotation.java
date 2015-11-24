package corpus.parser.bionlp.annotations;

import java.util.regex.Pattern;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.parser.bionlp.BratAnnotationManager;
import utility.ID;

public class BratRelationAnnotation extends BratAnnotation {
	public final static Pattern pattern;
	public final static Pattern idPattern;

	static {
		String id = "(R\\d+)";
		String role = "(\\S+)";
		String argRole = "(\\S+)";
		String arg = "((T|E)\\d+)";
		String args = "( " + argRole + ":" + arg + ")*";
		String all = "^" + id + "\t" + role + args + "$";
		pattern = Pattern.compile(all);
		idPattern = Pattern.compile(id);
	}

	private String role;
	private Multimap<String, ID<? extends BratAnnotation>> arguments = HashMultimap.create();

	public String getRole() {
		return role;
	}

	public Multimap<String, ID<? extends BratAnnotation>> getArguments() {
		return arguments;
	}

	public BratRelationAnnotation(BratAnnotationManager manager, String id, String role,
			Multimap<String, ID<? extends BratAnnotation>> arguments) {
		super(manager, id);
		this.role = role;
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "RelationAnnotation [id=" + id + ", role=" + role + ", arguments=" + arguments + "]";
	}

}
