package com.std4453.jbc.lexical;

import java.util.Vector;

public class DFADataPool {
	private static DFADataPool instance;

	public static DFADataPool getInstance() {
		if (instance == null)
			instance = new DFADataPool();

		return instance;
	}

	private DFA.State[] statesPool;

	protected DFADataPool() {
	}
}
