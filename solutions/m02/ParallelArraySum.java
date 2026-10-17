package vn.thanh.lab.m02.exercise;

import java.util.Objects;

public class ParallelArraySum {

    public long sum(long[] values, int threadCount) {
        Objects.requireNonNull(values, "values");
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount phải >= 1");
        }
        int workers = Math.max(1, Math.min(threadCount, values.length));
        long[] partialSums = new long[workers]; // mỗi worker ghi đúng một ô của mình
        Thread[] threads = new Thread[workers];

        int chunk = values.length / workers;
        int remainder = values.length % workers;
        int from = 0;
        for (int t = 0; t < workers; t++) {
            // remainder đoạn đầu dài hơn 1 phần tử -> các đoạn chênh nhau tối đa 1.
            int to = from + chunk + (t < remainder ? 1 : 0);
            int index = t;
            int start = from;
            threads[t] = new Thread(() -> {
                long local = 0; // cộng vào biến cục bộ (trên stack) -> không chia sẻ, không tranh chấp
                for (int i = start; i < to; i++) {
                    local += values[i];
                }
                partialSums[index] = local;
            }, "array-sum-" + t);
            threads[t].start();
            from = to;
        }

        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            for (Thread thread : threads) {
                thread.interrupt();
            }
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Bị interrupt khi đang chờ worker", e);
        }

        long total = 0;
        for (long partial : partialSums) {
            total += partial; // an toàn: join() happens-before đoạn đọc này
        }
        return total;
    }
}
