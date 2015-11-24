package corpus;

import java.util.List;

import variables.State;

public class SubDocument extends LabeledDocument<State, State> {
	protected String parentDocumentName;

	protected int offset = 0;

	public SubDocument(String parentDocumentName, String subDocumentName, String content, List<Token> tokens,
			int offset) {
		super(subDocumentName, content, tokens);
		this.parentDocumentName = parentDocumentName;
		this.offset = offset;
	}

	public String getParentDocumentName() {
		return parentDocumentName;
	}

	public int getOffset() {
		return offset;
	}

	@Override
	public String toString() {
		return "SubDocument [parentDocumentName=" + parentDocumentName + ", name=" + name + ", offset=" + offset
				+ ", content=" + content + ", tokens=" + tokens + ", goldQuery=" + goldResult + "]";
	}
}
