package com.std4453.jbc.lexical;

import java.util.regex.Pattern;

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

		public void setId(int id) {
			this.id = id;
		}

		public void setEdgesOutInterval(DataPoolInterval<Edge> edgesOutInterval) {
			this.edgesOutInterval = edgesOutInterval;
		}
	}

	public static class Edge {
		private int inStageId;
		private int outStasgeId;

		private Regexp regexp;

		public Edge(int inStageId, int outStasgeId, Regexp regexp) {
			this.inStageId = inStageId;
			this.outStasgeId = outStasgeId;
			this.regexp = regexp;
		}

		public int getInStageId() {
			return inStageId;
		}

		public int getOutStasgeId() {
			return outStasgeId;
		}

		public Regexp getRegexp() {
			return regexp;
		}

		public void setInStageId(int inStageId) {
			this.inStageId = inStageId;
		}

		public void setOutStasgeId(int outStasgeId) {
			this.outStasgeId = outStasgeId;
		}

		public void setRegexp(Regexp regexp) {
			this.regexp = regexp;
		}
	}

	private DFADataPool dataPool;

	private DataPoolInterval<State> statesInterval;

	public DFA() {
		this.dataPool = DFADataPool.getInstance();

		this.statesInterval = dataPool.getStagesPool().allocate(2);
		this.statesInterval.setElement(0, new State(0,dataPool
				.getEdgesPool().allocate(0)));
		this.statesInterval.setElement(0, new State(1,dataPool
				.getEdgesPool().allocate(0)));
	}

	public int addState() {
		int index = statesInterval.count();
		this.dataPool.getStagesPool().allocateMore(statesInterval, 1);
		this.statesInterval.setElement(index, new State(index, dataPool
				.getEdgesPool().allocate(0)));
		return index;
	}

	public void addEdge(int start, int end, Regexp regexp) {
		if (!statesInterval.checkBounds(start)
				|| !statesInterval.checkBounds(end))
			return;

		DataPoolInterval<Edge> startStateEdges = this.getState(start)
				.getEdgesOutInterval();
		this.dataPool.getEdgesPool().allocateMore(startStateEdges, 1);
		startStateEdges.setElement(startStateEdges.count() - 1, new Edge(start,
				end, regexp));
	}

	private State getState(int index) {
		return this.statesInterval.getElement(index);
	}

	public int statesCount() {
		return this.statesInterval.count();
	}
}
