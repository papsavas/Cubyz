package io.cubyz.util;

import java.util.Comparator;

/** 
 * A faster list implementation.<br>
 * Velocity is reached by sacrificing bound checks, by keeping some additional memory<br>
 * (When removing elements they are not necessarily cleared from the array) and through direct data access.
**/

public class BooleanFastList {

	public boolean[] array;
	public int size = 0;

	public BooleanFastList(int initialCapacity) {
		array = new boolean[ initialCapacity];
	}

	public BooleanFastList() {
		this(10);
	}

	public void increaseSize(int increment) {
		boolean[] newArray = new boolean[ array.length + increment];
		System.arraycopy(array, 0, newArray, 0, array.length);
		array = newArray;
	}

	public void trimToSize() {
		boolean[] newArray = new boolean[ size];
		System.arraycopy(array, 0, newArray, 0, size);
		array = newArray;
	}

	public boolean[] toArray() {
		boolean[] newArray = new boolean[ size];
		System.arraycopy(array, 0, newArray, 0, size);
		return newArray;
	}
	
	public void set(int index, boolean obj) {
		array[index] = obj;
	}
	
	public void add(boolean obj) {
		if (size == array.length)
			increaseSize(array.length/2 + 1);
		array[size] = obj;
		size++;
	}
	
	public void add(boolean... obj) {
		if (size + obj.length >= array.length)
			increaseSize(Math.max(array.length*3/2, array.length + obj.length));
		for(boolean o : obj) {
			array[size] = o;
			size++;
		}
	}
	
	public void remove(int index) {
		System.arraycopy(array, index+1, array, index, array.length-index-1);
		size--;
	}
	
	public void remove(boolean t) {
		for(int i = size-1; i >= 0; i--) {
			if(array[i] == t)
				remove(i); // Don't break here in case of multiple occurrence.
		}
	}
	
	public boolean contains(boolean t) {
		for(int i = size-1; i >= 0; i--) {
			if(array[i] == t)
				return true;
		}
		return false;
	}
	
	/**
	 * @param t
	 * @return -1 if t is outside this list<br>the index of t else
	 */
	public int indexOf(boolean t) {
		for(int i = size-1; i >= 0; i--) {
			if(array[i] == t)
				return i;
		}
		return -1;
	}
	
	public boolean isEmpty() {
		return size == 0;
	}
	
	/**
	 * Sort using Quick Sort algorithm.
	 * @param comp comparator
	 */
	public void sort(Comparator comp) {
		if (size > 1) {
			sort(comp, 0, size-1);
		}
	}
	
	/**
	 * Sort using Quick Sort algorithm.
	 * @param comp comparator
	 * @param l index of the left-most element in the to sorting area.
	 * @param r index of the right-most element in the to sorting area.
	 */
	public void sort(Comparator comp, int l, int r) {
		if(l >= r) return;
		int i = l, j = r;
		
		boolean x = array[(l+r)/2];
		while (true) {
			while (comp.compare( array[i], x) < 0) {
				i++;
			}
			while (comp.compare(x,  array[j]) < 0) {
				j--;
			}
			if (i <= j) {
				boolean temp = array[i];
				array[i] = array[j];
				array[j] = temp;
				i++;
				j--;
			}
			if (i > j) {
				break;
			}
		}
		if (l < j) {
			sort(comp, l, j);
		}
		if (i < r) {
			sort(comp, i, r);
		}
	}
	
	/**
	 * Sets the size to 0, meaning {@link RenderList#trimToSize()} should be called in order to free memory.
	 */
	public void clear() {
		size = 0;
	}

}
