package com.std4453.jbc.lexical;

import com.std4453.jbc.lexical.DFA.Edge;
import com.std4453.jbc.lexical.DFA.State;
import com.std4453.jbc.util.DataPool;

public class DFADataPool {
	private static DFADataPool instance;

	public static DFADataPool getInstance() {
		if (instance == null)
			instance = new DFADataPool();

		return instance;
	}

	protected DataPool<State> stagesPool;
	protected DataPool<Edge> edgesPool;

	protected DFADataPool() {
		this.stagesPool=new DataPool<DFA.State>(State.class, 4, 5, 128);
		this.edgesPool=new DataPool<DFA.Edge>(Edge.class, 8, 3, 512);
	}

	public DataPool<State> getStagesPool() {
		return stagesPool;
	}

	public DataPool<Edge> getEdgesPool() {
		return edgesPool;
	}
}
