package com.std4453.jbc.lexical;

import com.std4453.jbc.util.Interval;

public class DFA {
	public static class State {
		private int id;
		
		private Interval edgesOutInterval;
	}
	
	private DFADataPool dataPool;
	
	private int startStateId;
	private int endStateId;
	
	private Interval statesInterval;
	
	public DFA() {
		
	}
}
