package com.std4453.jbc.lexical;

import java.util.List;
import java.util.Vector;

public class Regexp2DFA {
	public static DFA regexp2DFA(Regexp regexp) {
		switch (regexp.getRule()) {
		case AND: {
			List<DFA> dfas = new Vector<DFA>();
			for (Regexp child : regexp.getChildren())
				dfas.add(regexp2DFA(child));

			return mergeDFAAnd(dfas);
		}
		case ANY_OR_NONE: {
			DFA dfa = regexp2DFA(regexp.getChildren().getElement(0));
			dfa.addEdge(0, 1, (char) 0);
			dfa.addEdge(1, 0, (char) 0);
			return dfa;
		}
		case HAS_OR_NOT: {
			DFA dfa = regexp2DFA(regexp.getChildren().getElement(0));
			dfa.addEdge(0, 1, (char) 0);
			return dfa;
		}
		case LEAF: {
			DFA dfa = new DFA();
			dfa.addEdge(0, 1, regexp.getAvailableChar());
			return dfa;
		}
		case MORE_THAN_ZERO: {
			DFA dfa = regexp2DFA(regexp.getChildren().getElement(0));
			dfa.addEdge(1, 0, (char) 0);
			return dfa;
		}
		case OR: {
			List<DFA> dfas = new Vector<DFA>();
			for (Regexp child : regexp.getChildren())
				dfas.add(regexp2DFA(child));

			return mergeDFAOr(dfas);
		}
		default:
			return null;
		}
	}

	private static DFA mergeDFAAnd(List<DFA> dfas) {
		return DFA.mergeDFAAnd(dfas);
	}

	private static DFA mergeDFAOr(List<DFA> dfas) {
		return DFA.mergeDFAOr(dfas);
	}
	
	public static void main(String[] args) {
		Regexp regexp=Regexp.compile("a(a+|(b*a?bb)\\)?)");
		System.out.println(regexp);
		DFA dfa=regexp2DFA(regexp);
		System.out.println(dfa);
	}
}
