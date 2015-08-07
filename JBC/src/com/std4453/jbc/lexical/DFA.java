package com.std4453.jbc.lexical;

import java.util.List;

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

		private char ch;

		public Edge(int inStageId, int outStasgeId, char ch) {
			this.inStageId = inStageId;
			this.outStasgeId = outStasgeId;
			this.ch = ch;
		}

		public int getInStageId() {
			return inStageId;
		}

		public int getOutStasgeId() {
			return outStasgeId;
		}

		public void setInStageId(int inStageId) {
			this.inStageId = inStageId;
		}

		public void setOutStasgeId(int outStasgeId) {
			this.outStasgeId = outStasgeId;
		}

		public char getCh() {
			return ch;
		}

		public void setCh(char ch) {
			this.ch = ch;
		}
	}

	private DFADataPool dataPool;

	private DataPoolInterval<State> statesInterval;

	public DFA() {
		this.dataPool = DFADataPool.getInstance();

		this.statesInterval = dataPool.getStagesPool().allocate(2);
		this.statesInterval.setElement(0, new State(0, dataPool.getEdgesPool()
				.allocate(0)));
		this.statesInterval.setElement(1, new State(1, dataPool.getEdgesPool()
				.allocate(0)));
	}

	public int addState() {
		int index = statesInterval.count();
		this.dataPool.getStagesPool().allocateMore(statesInterval, 1);
		this.statesInterval.setElement(index, new State(index, dataPool
				.getEdgesPool().allocate(0)));
		return index;
	}

	public void addEdge(int start, int end, char ch) {
		if (!statesInterval.checkBounds(start)
				|| !statesInterval.checkBounds(end))
			return;

		DataPoolInterval<Edge> startStateEdges = this.getState(start)
				.getEdgesOutInterval();
		this.dataPool.getEdgesPool().allocateMore(startStateEdges, 1);
		startStateEdges.setElement(startStateEdges.count() - 1, new Edge(start,
				end, ch));
	}

	private State getState(int index) {
		return this.statesInterval.getElement(index);
	}

	public int statesCount() {
		return this.statesInterval.count();
	}

	public void translateStateId(int original, int target) {
		if (getState(original) == null || getState(target) != null
				|| !this.statesInterval.checkBounds(target))
			return;

		for (Edge edge : getState(original).getEdgesOutInterval())
			edge.setInStageId(target);

		for (State state : this.statesInterval)
			for (Edge edge : state.getEdgesOutInterval())
				if (edge.getOutStasgeId() == original)
					edge.setOutStasgeId(target);

		getState(original).setId(target);
		this.statesInterval.setElement(target, getState(original));
		this.statesInterval.setElement(original, null);
	}

	public static DFA mergeDFAAnd(List<DFA> dfas) {
		DFA ret = new DFA();
		
		int currentOffset = 2;
		for (DFA dfa : dfas) {
			for (int i = 0; i < dfa.statesCount(); ++i) {
				ret.addState();
			}
			for (State state : dfa.statesInterval)
				for (Edge edge : state.getEdgesOutInterval())
					ret.addEdge(currentOffset + edge.getInStageId(),
							currentOffset + edge.getOutStasgeId(), edge.getCh());
			currentOffset += dfa.statesCount();
		}

		int lastIndex = 0;
		currentOffset = 2;
		for (DFA dfa : dfas) {
			ret.addEdge(lastIndex, currentOffset, (char) 0);
			lastIndex = currentOffset + 1;
			currentOffset += dfa.statesCount();
		}

		ret.addEdge(lastIndex, 1, (char) 0);

		return ret;
	}

	public static DFA mergeDFAOr(List<DFA> dfas) {
		DFA ret = new DFA();

		int currentOffset = 2;
		for (DFA dfa : dfas) {
			for (int i = 0; i < dfa.statesCount(); ++i) {
				ret.addState();
			}
			for (State state : dfa.statesInterval)
				for (Edge edge : state.getEdgesOutInterval())
					ret.addEdge(currentOffset + edge.getInStageId(),
							currentOffset + edge.getOutStasgeId(), edge.getCh());
			
			ret.addEdge(0, currentOffset, (char) 0);
			ret.addEdge(currentOffset + 1, 1, (char) 0);
			
			currentOffset += dfa.statesCount();
		}

		return ret;
	}

	public static final int TOSTRING_MODE_DIVIDED = 0;
	public static final int TOSTRING_MODE_TREE = 1;
	public static final int TOSTRING_MODE_TABLE = 2;

	@Override
	public String toString() {
		return toString(new StringBuilder()).toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		return toString(sb, TOSTRING_MODE_TREE);
	}

	public StringBuilder toString(StringBuilder sb, int mode) {
		if (mode == TOSTRING_MODE_DIVIDED)
			return toStringDivided(sb);
		else if (mode == TOSTRING_MODE_TREE)
			return toStringTree(sb);
		else if (mode == TOSTRING_MODE_TABLE)
			return toStringTable(sb);
		else
			return sb;
	}

	private StringBuilder toStringDivided(StringBuilder sb) {
		sb.append("DFA{\n\tstates=[\n");
		for (State state : this.statesInterval) {
			sb.append(String
					.format("\t\tState{id=%d, outEdgedInterval=DataPoolInterval{count=%d}}%s\n",
							state.id, state.edgesOutInterval.count(),
							state == getState(statesCount() - 1) ? "" : ","));
		}
		sb.append("\t],\nedges=[\n");
		for (State state : this.statesInterval)
			for (Edge edge : state.getEdgesOutInterval()) {
				sb.append(String.format(
						"\t\tEdge{fromId=%d, toId=%d, ch=%s (0x%s)}%s\n",
						edge.inStageId,
						edge.outStasgeId,
						edge.ch,
						Integer.toHexString(edge.ch),
						state == getState(statesCount() - 1)
								&& edge == state.getEdgesOutInterval()
										.getElement(
												state.getEdgesOutInterval()
														.count() - 1) ? ""
								: ","));
			}
		sb.append("\t]\n}");
		return sb;
	}

	private StringBuilder toStringTree(StringBuilder sb) {
		sb.append("DFA{\n\tstates=[\n");
		for (State state : statesInterval) {
			sb.append(String.format("\t\tState{id=%d, edges=[\n", state.id));
			for (Edge edge : state.getEdgesOutInterval()) {
				sb.append(String.format(
						"\t\t\tEdge{fromId=%d, toId=%d, ch=%s (0x%s)}%s\n",
						edge.inStageId,
						edge.outStasgeId,
						edge.ch,
						Integer.toHexString(edge.ch),
						edge == state.getEdgesOutInterval().getElement(
								state.getEdgesOutInterval().count() - 1) ? ""
								: ","));
			}
			sb.append(String.format("\t\t]}%s\n",
					state == getState(statesCount() - 1) ? "" : ","));
		}
		sb.append("\t]\n}");
		return sb;
	}

	private StringBuilder toStringTable(StringBuilder sb) {
		int statesCount = statesCount();

		sb.append("statesCount="+statesCount+"\n");
		
		boolean[][] relations = new boolean[statesCount][];
		for (int i = 0; i < statesCount; ++i)
			relations[i] = new boolean[statesCount];

		for (State state : this.statesInterval)
			for (Edge edge : state.getEdgesOutInterval())
				relations[edge.inStageId][edge.outStasgeId]=true;

		sb.append("  ");
		for (int i=0;i<statesCount;++i) {
			String istr=String.valueOf(i);
			sb.append(istr.substring(istr.length()-1));
			sb.append(" ");
		}
		sb.append("\n");
		for (int i=0;i<statesCount;++i) {
			String istr=String.valueOf(i);
			sb.append(istr.substring(istr.length()-1));
			sb.append(" ");
			for (int j=0;j<statesCount;++j) {
				sb.append(relations[j][i]?"+ ":"- ");
			}
			sb.append("\n");
		}
		
		return sb;
	}
}
