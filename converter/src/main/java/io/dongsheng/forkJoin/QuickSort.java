package io.dongsheng.forkJoin;

import java.util.Arrays;
import java.util.concurrent.RecursiveAction;

public class QuickSort extends RecursiveAction {
    private int[] array;
    private int lo;
    private int hi;

    public QuickSort(int[] array, int lo, int hi) {
        this.array = array;
        this.lo = lo;
        this.hi = hi;
    }

    @Override
    protected void compute() {
        if (hi - lo < 8) {
            Arrays.sort(array, lo, hi + 1);
        } else {
            int p = partition(array, lo, hi);
            invokeAll(new QuickSort(array, lo, p), new QuickSort(array, p+1, hi));
        }

    }

    public int partition(int[] array, int lo, int hi) {
        int pivot = array[(lo + hi) / 2];
        int i = lo - 1;
        int j = hi + 1;
        for(;;) {
            do {
                i++;
            } while (array[i] < pivot);

            do {
                j--;
            } while (array[j] > pivot);

            if (i >= j) {
                return j;
            }

            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }
}
