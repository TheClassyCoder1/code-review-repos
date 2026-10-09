package com.example.lending.loan.servicing.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

/** Compares an old and a new list of records. */
public final class CollectionDiffs {

    private CollectionDiffs() {
    }

    /** Returns three lists: records to create, records to update and records to delete. */
    public static <T> List<List<T>> diffList(List<T> oldList, List<T> newList, BiPredicate<T, T> sameFunc) {
        List<T> createList = new ArrayList<>();
        List<T> updateList = new ArrayList<>();
        List<T> deleteList = new ArrayList<>(oldList);
        for (T newItem : newList) {
            T match = null;
            for (T oldItem : deleteList) {
                if (sameFunc.test(oldItem, newItem)) {
                    match = oldItem;
                    break;
                }
            }
            if (match == null) {
                createList.add(newItem);
            } else {
                updateList.add(newItem);
                deleteList.remove(match);
            }
        }
        List<List<T>> result = new ArrayList<>(3);
        result.add(createList);
        result.add(updateList);
        result.add(deleteList);
        return result;
    }
}
