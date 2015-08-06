package com.std4453.jbc.util;

import java.util.Iterator;
import java.util.Objects;

public class Interval implements Cloneable {
	public static final Interval EMPTY_INTERVAL = new Interval();

	public int start;
	public int end;

	/**
	 * Constructs a safe {@code Interval} object that won't be negated if
	 * {@code start > end}.
	 */
	public static Interval constructOrderedInterval(int start, int end) {
		if (start >= end)
			return EMPTY_INTERVAL;
		return new Interval(start, end);
	}

	/**
	 * Constructs an {@code Interval}. If {@code start < end} then their values
	 * will be swapped. Checks whether both arguments are positive.
	 */
	public Interval(int start, int end) {
		if (start < 0 || end < 0)
			start = end = 0;

		if (start > end) {
			start = start ^ end;
			end = end ^ start;
			start = start ^ end;
		}

		this.start = start;
		this.end = end;
	}

	public Interval(Interval interval) {
		this.start = interval.start;
		this.end = interval.end;
	}

	public Interval() {
		this.start = 0;
		this.end = 0;
	}

	public Interval copy() {
		return new Interval(this);
	}

	public Interval combine(Interval interval) {
		Objects.requireNonNull(interval, "The other operator can't be null!");
		return new Interval(Math.min(this.start, interval.start), Math.max(
				this.end, interval.end));
	}

	public Interval intersection(Interval interval) {
		Objects.requireNonNull(interval, "The other operator can't be null!");
		return constructOrderedInterval(Math.max(this.start, interval.start),
				Math.min(this.end, interval.end));
	}

	public int count() {
		return end - start;
	}

	public boolean isEmpty() {
		return count() <= 0;
	}

	public <T> boolean checkBounds(T[] values) {
		if (values == null)
			return false;

		// XXX: maybe a bit slow.
		return !intersection(new Interval(0, values.length)).isEmpty();
	}

	public <T> Iterator<T> iterator(T[] values) {
		if (!checkBounds(values))
			return null;
		else
			return new IntervalArrayIterator<T>(values, this);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getStart() {
		return start;
	}

	public int getAndIncreaseStart() {
		return start++;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public <T> T getElement(T[] array, int index) {
		if (!checkBounds(array))
			return null;
		return array[start + index];
	}
}
