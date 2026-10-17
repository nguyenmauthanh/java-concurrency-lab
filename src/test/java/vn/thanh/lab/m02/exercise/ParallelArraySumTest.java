package vn.thanh.lab.m02.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelArraySumTest {

    private final ParallelArraySum parallelArraySum = new ParallelArraySum();

    private static long sequentialSum(long[] values) {
        long total = 0;
        for (long value : values) {
            total += value;
        }
        return total;
    }

    @ParameterizedTest(name = "threadCount = {0}")
    @ValueSource(ints = {1, 2, 3, 4, 7, 8, 16, 33})
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void sumsLargeArrayCorrectly(int threadCount) {
        long[] values = new long[1_000_003]; // độ dài lẻ, không chia hết cho threadCount
        for (int i = 0; i < values.length; i++) {
            values[i] = i;
        }
        assertEquals(sequentialSum(values), parallelArraySum.sum(values, threadCount));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void moreThreadsThanElements() {
        assertEquals(6, parallelArraySum.sum(new long[]{1, 2, 3}, 10));
        assertEquals(42, parallelArraySum.sum(new long[]{42}, 4));
        assertEquals(0, parallelArraySum.sum(new long[0], 4));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void overflowWrapsLikeSequentialSum() {
        long[] values = {Long.MAX_VALUE, Long.MAX_VALUE, 5, Long.MIN_VALUE, -7};
        assertEquals(sequentialSum(values), parallelArraySum.sum(values, 3));
    }

    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> parallelArraySum.sum(new long[]{1}, 0));
        assertThrows(IllegalArgumentException.class, () -> parallelArraySum.sum(new long[]{1}, -2));
        assertThrows(NullPointerException.class, () -> parallelArraySum.sum(null, 2));
    }

    @RepeatedTest(30)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Mảng ngẫu nhiên, lặp nhiều lần: bắt lỗi ranh giới đoạn và race khi gộp kết quả")
    void randomArraysManyTimes() {
        Random random = new Random();
        long[] values = new long[random.nextInt(0, 200_000)];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextLong();
        }
        int threadCount = random.nextInt(1, 24);
        assertEquals(sequentialSum(values), parallelArraySum.sum(values, threadCount),
                "length=" + values.length + ", threadCount=" + threadCount);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Thực sự dùng nhiều Thread (không cộng tuần tự trên thread gọi)")
    void reallyUsesMultipleThreads() throws InterruptedException {
        Set<Thread> seen = ConcurrentHashMap.newKeySet();
        Thread caller = Thread.currentThread();
        Thread sampler = Thread.ofPlatform().daemon().start(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.getAllStackTraces().forEach((thread, frames) -> {
                    for (StackTraceElement frame : frames) {
                        if (thread != caller && frame.getClassName().startsWith(ParallelArraySum.class.getName())) {
                            seen.add(thread);
                        }
                    }
                });
            }
        });
        long[] values = new long[20_000_000];
        java.util.Arrays.fill(values, 1);
        try {
            for (int i = 0; i < 5 && seen.size() < 2; i++) {
                assertEquals(values.length, parallelArraySum.sum(values, 4));
            }
        } finally {
            sampler.interrupt();
            sampler.join();
        }
        assertTrue(seen.size() >= 2, "Chỉ thấy " + seen.size() + " worker thread chạy code ParallelArraySum");
    }
}
