package com.std4453.jbc.util;

public class Pair<A,B> {
	private A left;
	private B right;
	
	public Pair(A a,B b) {
		this.left=a;
		this.right=b;
	}
	
	public Pair() {
	}

	public A getLeft() {
		return left;
	}

	public void setLeft(A left) {
		this.left = left;
	}

	public B getRight() {
		return right;
	}

	public void setRight(B right) {
		this.right = right;
	}
}
