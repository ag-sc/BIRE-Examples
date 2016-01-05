package factors;

import java.util.Set;

import com.google.common.collect.Sets;

import templates.AbstractTemplate;
import utility.VariableID;
import variables.AbstractState;

public class TokenFactor extends AbstractFactor {

	public final int tokenIndex;

	public TokenFactor(AbstractTemplate<? extends AbstractState> template, int tokenIndex) {
		super(template);
		this.tokenIndex = tokenIndex;
	}

	@Override
	public Set<VariableID> getVariableIDs() {
		return Sets.newHashSet(new VariableID("Token@" + tokenIndex));
	}
}
