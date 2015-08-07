package com.std4453.jbc.util;

public class Tri<A, B, C> {
	private A one;
	private B two;
	private C three;

	public Tri(A one, B two, C three) {
		super();
		this.one = one;
		this.two = two;
		this.three = three;
	}

	public A getOne() {
		return one;
	}

	public void setOne(A one) {
		this.one = one;
	}

	public B getTwo() {
		return two;
	}

	public void setTwo(B two) {
		this.two = two;
	}

	public C getThree() {
		return three;
	}

	public void setThree(C three) {
		this.three = three;
	}
}
