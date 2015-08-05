package com.std4453.jbc.util;

import java.util.Iterator;
import java.util.Objects;

import static com.std4453.jbc.logging.Logger.*;

public class IntervalArrayIterator<T> implements Iterator<T> {
	private T[] values;
	private Interval interval;

	public IntervalArrayIterator(T[] values, Interval interval) {
		Objects.requireNonNull(values, "Values must be non-null!");
		Objects.requireNonNull(interval, "Interval must be non-null!");
		
		this.values=values;
		this.interval=interval.copy();
		
		if (!interval.checkBounds(values)) {
			log(WARNING, "Interval out of bounds of values!");
			this.interval=Interval.EMPTY_INTERVAL;
		}
	}

	@Override
	public boolean hasNext() {
		return !interval.isEmpty();
	}

	@Override
	public T next() {
		if (interval.isEmpty())
			return null;
		
		return values[interval.getAndIncreaseStart()];
	}
}
