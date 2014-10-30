package com.mad.qut.budgetr.utils;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;

public class Lists {

    /**
     * Creates an empty ArrayList.
     *
     * @return ArrayList
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<E>();
    }

    /**
     * Creates a resizable ArrayList containing the given
     * elements.
     *
     * @param elements Elements that should be included
     * @return ArrayList containting the elements
     */
    public static <E> ArrayList<E> newArrayList(E... elements) {
        int capacity = (elements.length * 110) / 100 + 5;
        ArrayList<E> list = new ArrayList<E>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Clones a SparseArray.
     *
     * @param orig SparseArray
     * @return Cloned SparseArray
     */
    public static <E> SparseArray<E> cloneSparseArray(SparseArray<E> orig) {
        SparseArray<E> result = new SparseArray<E>();
        for (int i = 0; i < orig.size(); i++) {
            result.put(orig.keyAt(i), orig.valueAt(i));
        }
        return result;
    }
}
