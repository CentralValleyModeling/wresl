package gov.ca.water.wresl.parsing;

import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.function.Consumer;

public class ParallelAction<T> extends RecursiveAction {
    private final List<T> items;
    private final int start;
    private final int end;
    private final int threshold;
    private final Consumer<T> action;

    public ParallelAction(List<T> items, int start, int end, int threshold, Consumer<T> action) {
        this.items = items;
        this.start = start;
        this.end = end;
        this.threshold = threshold;
        this.action = action;
    }

    @Override
    protected void compute() {
        if ((end - start) <= threshold) {
            // Leaf computation: apply the generic consumer
            for (int i = start; i < end; i++) {
                action.accept(items.get(i));
            }
        } else {
            // Split phase
            int mid = start + (end - start) / 2;
            ParallelAction<T> left = new ParallelAction<>(items, start, mid, threshold, action);
            ParallelAction<T> right = new ParallelAction<>(items, mid, end, threshold, action);

            invokeAll(left, right);
        }
    }
}

