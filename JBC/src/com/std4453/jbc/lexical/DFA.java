package com.std4453.jbc.lexical;

import com.std4453.jbc.util.DataPool.DataPoolInterval;

public class DFA {
	public static class State {
		private int id;

		private DataPoolInterval<Edge> edgesOutInterval;

		public State(int id, DataPoolInterval<Edge> edgesOutInterval) {
			this.id = id;
			this.edgesOutInterval = edgesOutInterval;
		}

		public State(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public DataPoolInterval<Edge> getEdgesOutInterval() {
			return edgesOutInterval;
		}
	}

	public static class Edge {
		private int inStageId;
		private int outStasgeId;
		
		private String regexp;

		public Edge(int inStageId, int outStasgeId, String regexp) {
			this.inStageId = inStageId;
			this.outStasgeId = outStasgeId;
			this.regexp = regexp;
		}
	}

	private DFADataPool dataPool;

	private int startStateId;
	private int endStateId;

	private DataPoolInterval<State> statesInterval;

	public DFA() {
		this.dataPool = DFADataPool.getInstance();
	}
}
