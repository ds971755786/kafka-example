package io.dongsheng.forkJoin;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

public class TestQuickSort {

    @Test
    public void test() {
        int num = 8;
        int length = 32;
        Random rand = new Random();
        int[] array = new int[length];
        int[] duizhao = new int[length];
        for (int i = 0; i < num; i++) {
            for (int j = 0; j < length; j++) {
                array[j] = rand.nextInt();
                duizhao[j] = array[j];
            }
            QuickSort quickSort = new QuickSort(array, 0, length - 1);
            quickSort.invoke();
            Arrays.sort(duizhao);
            //check
            Assert.assertArrayEquals(duizhao, array);


        }
    }
}
