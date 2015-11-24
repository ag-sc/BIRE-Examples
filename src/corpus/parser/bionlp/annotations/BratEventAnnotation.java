package corpus.parser.bionlp.annotations;

import java.util.HashMap;
import java.util.regex.Pattern;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import corpus.parser.bionlp.BratAnnotationManager;
import utility.ID;

public class BratEventAnnotation extends BratAnnotation {
	public final static Pattern pattern;
	public final static Pattern idPattern;

	static {
		String id = "(E\\d+)";
		String role = "(\\S+)";
		String trigger = "(T\\d+)";
		String argRole = "(\\S+)";
		String arg = "((T|E)\\d+)";
		String args = "( " + argRole + ":" + arg + ")*";
		String all = "^" + id + "\t" + role + ":" + trigger + args + "$";
		pattern = Pattern.compile(all);
		idPattern = Pattern.compile(id);
	}

	private String role;
	private ID<BratTextBoundAnnotation> triggerID;
	private Multimap<String, ID<? extends BratAnnotation>> arguments = HashMultimap.create();

	public String getRole() {
		return role;
	}

	public ID<BratTextBoundAnnotation> getTriggerID() {
		return triggerID;
	}

	public BratTextBoundAnnotation getTrigger() {
		return (BratTextBoundAnnotation) getAnnotationByID(triggerID);
	}

	public Multimap<String, ID<? extends BratAnnotation>> getArguments() {
		return arguments;
	}

	public BratEventAnnotation(BratAnnotationManager manager, String id, String role, String triggerID,
			Multimap<String, ID<? extends BratAnnotation>> arguments) {
		super(manager, id);
		this.role = role;
		this.triggerID = new ID<>(triggerID);
		this.arguments = arguments;
	}

	@Override
	public String toString() {
		return "EventAnnotation [id=" + id + ", role=" + role + ", trigger=" + triggerID + ", arguments=" + arguments
				+ "]";
	}

}
