package com.std4453.jbc.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Vector;

public class DataPool<T> {
	public static class DataPoolInterval<T> extends Interval implements
			Iterable<T> {
		private T[] data;

		public DataPoolInterval(int start, int end, T[] data) {
			super(start, end);
			this.data = data;
		}

		public DataPoolInterval(Interval interval, T[] data) {
			super(interval);
			this.data = data;
		}

		public DataPoolInterval(DataPoolInterval<T> interval) {
			super(interval);
			this.data = interval.data;
		}

		public DataPoolInterval(T[] data) {
			super(0, data.length);
			this.data = data;
		}

		private DataPoolInterval() {
			super();
		}

		@Override
		public Interval copy() {
			return super.copy();
		}

		@Override
		public DataPoolInterval<T> combine(Interval interval) {
			return new DataPoolInterval<T>(super.combine(interval), this.data);
		}

		@Override
		public DataPoolInterval<T> intersection(Interval interval) {
			return new DataPoolInterval<T>(super.intersection(interval),
					this.data);
		}

		@Override
		public Iterator<T> iterator() {
			return super.iterator(this.data);
		}

		public boolean checkBounds() {
			return super.checkBounds(this.data);
		}

		public T[] getData() {
			return data;
		}

		public void setData(T[] data) {
			this.data = data;
		}

		public T getElement(int index) {
			return super.getElement(data, index);
		}
	}

	private Class<T> clazz;

	private int startUnitSize;
	private int cacheLevels;

	private int unitCountPerLevel;

	private T[][] data;
	private boolean[][] allocated;
	private DataPoolInterval<T>[][] intervals;
	private int[] allocatedCount;

	private Vector<T[]> largeArrays;
	private Vector<DataPoolInterval<T>> largeIntervals;

	public DataPool(Class<T> clazz, int startUnit, int cacheLevels,
			int unitCountPerLevel) {
		this.clazz = clazz;

		this.startUnitSize = startUnit;
		this.cacheLevels = cacheLevels;
		this.unitCountPerLevel = unitCountPerLevel;

		this.allocatedCount = new int[cacheLevels];

		this.largeArrays = new Vector<T[]>();
		this.largeIntervals = new Vector<DataPool.DataPoolInterval<T>>();

		allocateCache();
	}

	private void allocateCache() {
		int size = startUnitSize;

		data = create2DTArray(cacheLevels);
		allocated = new boolean[cacheLevels][];
		intervals = create2DDataPoolIntervalArray(cacheLevels);
		for (int level = 0; level < cacheLevels; ++level, size *= 2) {
			data[level] = createTArray(unitCountPerLevel * size);
			allocated[level] = new boolean[unitCountPerLevel];
			intervals[level] = createDataPoolIntervalArray(unitCountPerLevel);
		}
	}

	private void scale() {
		Object[][] currentData = data;
		boolean[][] currentAllocated = allocated;
		Object[][] currentIntervals = intervals;

		unitCountPerLevel *= 2;

		allocateCache();

		for (int level = 0; level < cacheLevels; ++level) {
			System.arraycopy(currentData[level], 0, data[level], 0,
					currentData[level].length);
			System.arraycopy(currentAllocated[level], 0, allocated[level], 0,
					currentAllocated[level].length);
			System.arraycopy(currentIntervals[level], 0, intervals[level], 0,
					currentIntervals[level].length);
		}
	}

	@SuppressWarnings("unchecked")
	public void pack() {
		int maxAllocatedCount = getMaxAllocatedCount();
		maxAllocatedCount *= 2;

		int finalUnitCount = unitCountPerLevel;
		for (; finalUnitCount > maxAllocatedCount; finalUnitCount /= 2)
			;

		if (finalUnitCount == unitCountPerLevel)
			return;

		Object[][] currentData = data;
		boolean[][] currentAllocated = allocated;
		Object[][] currentIntervals = intervals;
		int currentUnitCount = unitCountPerLevel;

		unitCountPerLevel = finalUnitCount;

		Object[] levelData;
		boolean[] levelAllocated;
		Object[] levelIntervals;
		int tmpAllocatedCount;
		int levelUnitSize = startUnitSize;
		for (int level = 0; level < cacheLevels; ++level, levelUnitSize *= 2) {
			tmpAllocatedCount = 0;

			levelData = currentData[level];
			levelAllocated = currentAllocated[level];
			levelIntervals = currentIntervals[level];

			for (int unitId = 0; unitId < currentUnitCount; ++unitId) {
				if (!levelAllocated[unitId])
					continue;

				allocated[level][tmpAllocatedCount] = true;
				((DataPoolInterval<T>) levelIntervals[unitId])
						.setStart(tmpAllocatedCount * levelUnitSize);
				intervals[level][tmpAllocatedCount] = (DataPoolInterval<T>) levelIntervals[unitId];

				System.arraycopy(levelData, unitId * levelUnitSize,
						data[level], tmpAllocatedCount * levelUnitSize,
						levelUnitSize);

				++tmpAllocatedCount;
				((DataPoolInterval<T>) levelIntervals[unitId])
						.setEnd(tmpAllocatedCount * levelUnitSize);
			}
		}

		for (int i = 0; i < largeArrays.size(); ++i) {
			T[] array = largeArrays.get(i);
			DataPoolInterval<T> interval = largeIntervals.get(i);
			int count = interval.count();
			int newLength;

			if ((newLength = nearest2Power(count)) < array.length) {
				T[] newArray = createTArray(newLength);
				System.arraycopy(array, 0, newArray, 0, array.length);

				interval.setData(newArray);
				largeArrays.set(i, newArray);
			}
		}
	}

	private int getMaxAllocatedCount() {
		int max = 0;

		for (int level = 0; level < cacheLevels; ++level)
			if (allocatedCount[level] > max)
				max = allocatedCount[level];

		return max;
	}

	private int getLevel(int size) {
		int level = 0;
		for (int unitSize = startUnitSize; level < cacheLevels
				&& unitSize < size; ++level, unitSize *= 2)
			;

		return level;
	}

	private void ensureCapacity(int level, int allocate) {
		if (availableUnitsInLevel(level) < allocate)
			scale();
	}

	private int availableUnitsInLevel(int level) {
		return unitCountPerLevel - allocatedCount[level];
	}

	public DataPoolInterval<T> allocate(int size) {
		int level = getLevel(size);
		ensureCapacity(level, 1);

		if (level == cacheLevels) {
			T[] newArray = createTArray(nearest2Power(size));
			largeArrays.add(newArray);
			DataPoolInterval<T> interval = new DataPoolInterval<T>(0, size,
					newArray);
			largeIntervals.add(interval);
			return interval;
		} else {
			return getFirstEmptyIntervalOfLevel(level, size);
		}
	}

	private DataPoolInterval<T> getFirstEmptyIntervalOfLevel(int level, int size) {
		for (int unitId = 0; unitId < unitCountPerLevel; ++unitId) {
			if (!allocated[level][unitId]) {
				DataPoolInterval<T> interval = new DataPoolInterval<T>(unitId
						* startUnitSize * (1 << level), unitId * startUnitSize
						* (1 << level) + size, data[level]);
				allocated[level][unitId] = true;
				intervals[level][unitId] = interval;

				return interval;
			}
		}

		return null;
	}

	public void allocateMore(DataPoolInterval<T> interval, int size) {
		if (interval == null || size <= 0)
			return;

		int level = getLevel(interval);
		if (level == -1)
			return;

		T[] array = interval.getData();
		int length = array.length;
		int count = interval.count();

		if (level == cacheLevels) {
			int newLength = nearest2Power(count + size);
			if (newLength > length) {
				T[] newArray = createTArray(newLength);
				System.arraycopy(array, 0, newArray, 0, count);
				interval.setData(newArray);
				interval.setEnd(interval.getEnd() + size);

				largeArrays.set(largeIntervals.indexOf(interval), newArray);
			}
		} else {
			int newLength = count + size;
			if (newLength > length) {
				int oldUnitId = interval.getStart() / startUnitSize
						/ (1 << level);

				DataPoolInterval<T> newInterval = allocate(newLength);
				System.arraycopy(array, 0, newInterval.getData(), 0, count);
				interval.setData(newInterval.getData());
				interval.setStart(newInterval.getStart());
				interval.setEnd(newInterval.getEnd());

				allocated[level][oldUnitId] = false;
				intervals[level][oldUnitId] = null;

				int newLevel = getLevel(newInterval);
				if (newLevel == cacheLevels) {
					largeIntervals.set(largeIntervals.indexOf(newInterval),
							interval);
				} else {
					intervals[newLevel][newInterval.getStart() / startUnitSize
							/ (1 << newLevel)] = interval;
				}
			}
		}
	}

	public void release(DataPoolInterval<T> interval) {
		int level = getLevel(interval);
		if (level == -1)
			return;

		if (level == cacheLevels) {
			int index = largeIntervals.indexOf(interval);
			largeIntervals.remove(index);
			largeArrays.remove(index);
		} else {
			int unitId = interval.getStart() / startUnitSize / (1 << level);
			allocated[level][unitId] = false;
			intervals[level][unitId] = null;
		}
	}

	private int getLevel(DataPoolInterval<T> interval) {
		if (largeArrays.contains(interval))
			return cacheLevels;

		int count = interval.getData().length;
		if (count % startUnitSize != 0)
			return -1;
		count /= startUnitSize;
		int level = -1;
		for (int level1 = 0; level1 < cacheLevels; ++level1)
			if (1 << level1 == count)
				level = level1;

		if (level < 0)
			return -1;

		for (int unitId = 0; unitId < unitCountPerLevel; ++unitId)
			if (intervals[level][unitId] == interval)
				return level;
		return -1;
	}

	private int nearest2Power(int size) {
		int n = 1;
		for (; n < size; n <<= 1)
			;
		return n;
	}

	@SuppressWarnings("unchecked")
	private T[][] create2DTArray(int length) {
		Class<?> clazz1 = Array.newInstance(clazz, 0).getClass();
		return (T[][]) Array.newInstance(clazz1, length);
	}

	@SuppressWarnings("unchecked")
	private T[] createTArray(int length) {
		return (T[]) Array.newInstance(clazz, length);
	}

	@SuppressWarnings("unchecked")
	private DataPoolInterval<T>[][] create2DDataPoolIntervalArray(int length) {
		DataPoolInterval<T> interval = new DataPoolInterval<T>();
		Class<?> clazz1 = interval.getClass();
		Class<?> class2 = Array.newInstance(clazz1, 0).getClass();
		return (DataPoolInterval<T>[][]) Array.newInstance(class2, length);
	}

	@SuppressWarnings("unchecked")
	private DataPoolInterval<T>[] createDataPoolIntervalArray(int length) {
		DataPoolInterval<T> interval = new DataPoolInterval<T>();
		return (DataPoolInterval<T>[]) Array.newInstance(interval.getClass(),
				length);
	}
}