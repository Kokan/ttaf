package dog.giraffe;

public class QuickSort {
	@FunctionalInterface
	public interface Compare {
		int compare(int index0, int index1);
	}

	public static abstract class PivotProperty implements Property {
		public final Compare compare;
		public int pivotIndex;
		
		public PivotProperty(Compare compare) {
			this.compare=compare;
		}
	}

	public static class PivotPropertyGreater extends PivotProperty {
		public PivotPropertyGreater(Compare compare) {
			super(compare);
		}

		@Override
		public boolean hasProperty(int index) {
			return 0>compare.compare(pivotIndex, index);
		}
	}

	public static class PivotPropertyGreaterOrEqual extends PivotProperty {
		public PivotPropertyGreaterOrEqual(Compare compare) {
			super(compare);
		}

		@Override
		public boolean hasProperty(int index) {
			return 0>=compare.compare(pivotIndex, index);
		}
	}

	@FunctionalInterface
	public interface Property {
		boolean hasProperty(int index);
	}

	@FunctionalInterface
	public interface Swap {
		void swap(int index0, int index1);
	}

	private QuickSort() {
	}

	public static int medianSplit(Compare compare, int from, Swap swap, int to) {
		int size=to-from;
		if (2>size) {
			return from;
		}
		boolean odd=0!=(size%2);
		PivotProperty greater=new PivotPropertyGreater(compare);
		PivotProperty greaterOrEqual=new PivotPropertyGreaterOrEqual(compare);
		int middle=(from+to)/2;
		while (true) {
			int last=to-1;
			int pivotIndex=pivotIndex(compare, from, last);
			if (pivotIndex!=last) {
				swap.swap(pivotIndex, last);
			}
			greaterOrEqual.pivotIndex=last;
			int split=split(from, greaterOrEqual, swap, last);
			if (split!=last) {
				swap.swap(split, last);
			}
			if ((middle==split)
					|| (middle-1==split)) {
				return middle;
			}
			if ((middle+1==split)
					&& odd) {
				return middle+1;
			}
			if (middle<split) {
				to=split;
			}
			else {
				from=split+1;
				greater.pivotIndex=split;
				split=split(from, greater, swap, to);
				if (middle<=split) {
					return middle;
				}
				from=split;
			}
		}
	}

	public static int pivotIndex(Compare compare, int first, int last) {
		int middle=(first+last)/2;
		int cc=compare.compare(middle, last);
		if (0==cc) {
			return middle;
		}
		int dd=compare.compare(first, middle);
		if ((0==dd)
				|| ((0<cc) && (0<dd))
				|| ((0>cc) && (0>dd))) {
			return middle;
		}
		int ee=compare.compare(first, last);
		return ((0==ee)
					|| ((0>cc)==(0<ee)))
				?last
				:first;
	}

	/**
	public static void sort(Compare compare, int from, Swap swap, int to) {
		sort(from, new PivotPropertyGreaterOrEqual(compare), swap, to);
	}
	
	public static void sort(int from, PivotProperty pivotProperty, Swap swap, int to) {
		int size=to-from;
		if (1>=size) {
			return;
		}
		if (2==size) {
			if (0<pivotProperty.compare.compare(from, from+1)) {
				swap.swap(from, from+1);
			}
			return;
		}
		int split=splitPivot(from, pivotProperty, swap, to);
		sort(from, pivotProperty, swap, split);
		sort(split+1, pivotProperty, swap, to);
	}

	 public static int splitPivot(int from, PivotProperty property, Swap swap, int to) {
	 int size=to-from;
	 if (1>=size) {
	 return from;
	 }
	 if (2==size) {
	 if (0<property.compare.compare(from, from+1)) {
	 swap.swap(from, from+1);
	 }
	 return from;
	 }
	 int last=to-1;
	 int pivotIndex=pivotIndex(property.compare, from, last);
	 if (pivotIndex!=last) {
	 swap.swap(pivotIndex, last);
	 }
	 property.pivotIndex=last;
	 int split=split(from, property, swap, last);
	 if (split!=last) {
	 swap.swap(split, last);
	 }
	 return split;
	 }
	*/

	public static int split(int from, Property property, Swap swap, int to) {
		--to;
		while (from<=to) {
			//invariant:
			//  ii<from => !property(ii)
			//  ii>to   => property(ii)
			while ((from<=to)
					&& (!property.hasProperty(from))) {
				++from;
			}
			while ((from<=to)
					&& property.hasProperty(to)) {
				--to;
			}
			if (from<to) {
				swap.swap(from, to);
				++from;
				--to;
			}
		}
		return from;
	}
}
