package dog.giraffe.util;

/**
 * Implementation of Hoare's selection algorithm to select the median of a list-like collection of values.
 */
public class QuickSort {
	/**
	 * Represents the comparability of the elements of a list-like object.
	 */
	@FunctionalInterface
	public interface Compare {
		/**
		 * Compare two elements.
		 *
		 * @param index0 the index of the first element
		 * @param index1 the index of the second element
		 *
		 * @return 0 if the elements are equal, less than 0 if the first element comes before the second element,
		 *         and greater than zero if the first element comes after the second element
		 */
		int compare(int index0, int index1);
	}

	private static abstract class PivotProperty implements Property {
		public final Compare compare;
		public int pivotIndex;
		
		public PivotProperty(Compare compare) {
			this.compare=compare;
		}
	}

	private static class PivotPropertyGreater extends PivotProperty {
		public PivotPropertyGreater(Compare compare) {
			super(compare);
		}

		@Override
		public boolean hasProperty(int index) {
			return 0>compare.compare(pivotIndex, index);
		}
	}

	private static class PivotPropertyGreaterOrEqual extends PivotProperty {
		public PivotPropertyGreaterOrEqual(Compare compare) {
			super(compare);
		}

		@Override
		public boolean hasProperty(int index) {
			return 0>=compare.compare(pivotIndex, index);
		}
	}

	@FunctionalInterface
	private interface Property {
		boolean hasProperty(int index);
	}

	/**
	 * Represents the ability to swap two elements of a list-like object.
	 */
	@FunctionalInterface
	public interface Swap {
		/**
		 * Swap the two elements.
		 *
		 * @param index0 the index of the first element
		 * @param index1 the index of the second element
		 */
		void swap(int index0, int index1);
	}

	private QuickSort() {
	}

	/**
	 * Rearranges the elements of the list and returns the index of a split.
	 * The list is indirectly specified through the compare and swap parameters.
	 * The indices of the list are [from, to).
	 *
	 * The post-conditions of the method are:
	 * <ul>
	 *     <li>the list is a permutation of the list before the method call,</li>
	 *     <li>(to-from)/2 = min(split-from, to-split),</li>
	 *     <li>the maximal element in the sublist [from, split) is less than or equal to
	 *         the minimal element in the sublist [split, to)
	 *         as compare orders elements.</li>
	 * </ul>
	 */
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

	private static int pivotIndex(Compare compare, int first, int last) {
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

	private static int split(int from, Property property, Swap swap, int to) {
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
