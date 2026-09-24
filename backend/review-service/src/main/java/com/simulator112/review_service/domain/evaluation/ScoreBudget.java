package com.simulator112.review_service.domain.evaluation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ScoreBudget {
    private ScoreBudget() {
    }

    static int[] scale(int total, int... proportions) {
        int proportionTotal = java.util.Arrays.stream(proportions).sum();
        int[] result = new int[proportions.length];
        int allocated = 0;
        for (int index = 0; index < proportions.length; index++) {
            result[index] = total * proportions[index] / proportionTotal;
            allocated += result[index];
        }
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < proportions.length; index++) order.add(index);
        order.sort(Comparator.<Integer>comparingInt(index ->
                total * proportions[index] % proportionTotal).reversed().thenComparingInt(index -> index));
        for (int index = 0; allocated < total; index++, allocated++) {
            result[order.get(index % order.size())]++;
        }
        return result;
    }
}
