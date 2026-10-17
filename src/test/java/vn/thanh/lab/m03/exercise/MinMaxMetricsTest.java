package vn.thanh.lab.m03.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MinMaxMetricsTest {

    @Test
    void initialValues() {
        MinMaxMetrics metrics = new MinMaxMetrics();
        assertEquals(Long.MAX_VALUE, metrics.getMin());
        assertEquals(Long.MIN_VALUE, metrics.getMax());
    }

    @Test
    void singleThreadBasic() {
        MinMaxMetrics metrics = new MinMaxMetrics();
        metrics.addSample(50);
        assertEquals(50, metrics.getMin());
        assertEquals(50, metrics.getMax());
        metrics.addSample(10);
        metrics.addSample(90);
        metrics.addSample(-5);
        assertEquals(-5, metrics.getMin());
        assertEquals(90, metrics.getMax());
    }

    @RepeatedTest(10)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Nhiều writer đồng thời: không mất mẫu min/max")
    void concurrentWritersDoNotLoseExtremes() throws InterruptedException {
        MinMaxMetrics metrics = new MinMaxMetrics();
        int threads = 8;
        int samplesPerThread = 200_000;
        CountDownLatch startGate = new CountDownLatch(1);
        List<Thread> writers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int threadIndex = t;
            Thread writer = new Thread(() -> {
                await(startGate);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < samplesPerThread; i++) {
                    metrics.addSample(random.nextLong(-1_000_000, 1_000_000));
                }
                // Mỗi thread đóng góp đúng một giá trị cực trị duy nhất, ở thời điểm ngẫu nhiên.
                metrics.addSample(-10_000_000L - threadIndex);
                metrics.addSample(10_000_000L + threadIndex);
                for (int i = 0; i < samplesPerThread; i++) {
                    metrics.addSample(random.nextLong(-1_000_000, 1_000_000));
                }
            });
            writers.add(writer);
            writer.start();
        }
        startGate.countDown();
        for (Thread writer : writers) {
            writer.join();
        }
        assertEquals(-10_000_000L - (threads - 1), metrics.getMin());
        assertEquals(10_000_000L + (threads - 1), metrics.getMax());
    }

    @RepeatedTest(10)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Reader đọc đồng thời: min không bao giờ tăng, max không bao giờ giảm")
    void readersObserveMonotonicValues() throws InterruptedException {
        MinMaxMetrics metrics = new MinMaxMetrics();
        AtomicBoolean writersDone = new AtomicBoolean(false);
        AtomicReference<String> violation = new AtomicReference<>();
        CountDownLatch startGate = new CountDownLatch(1);

        List<Thread> readers = new ArrayList<>();
        for (int r = 0; r < 2; r++) {
            Thread reader = new Thread(() -> {
                await(startGate);
                long lastMin = Long.MAX_VALUE;
                long lastMax = Long.MIN_VALUE;
                while (!writersDone.get() && violation.get() == null) {
                    long min = metrics.getMin();
                    long max = metrics.getMax();
                    if (min > lastMin) {
                        violation.compareAndSet(null, "min tăng từ " + lastMin + " lên " + min);
                    }
                    if (max < lastMax) {
                        violation.compareAndSet(null, "max giảm từ " + lastMax + " xuống " + max);
                    }
                    lastMin = min;
                    lastMax = max;
                }
            });
            reader.setUncaughtExceptionHandler((thread, error) -> violation.compareAndSet(null, "Exception trong reader: " + error));
            readers.add(reader);
            reader.start();
        }

        List<Thread> writers = new ArrayList<>();
        for (int t = 0; t < 6; t++) {
            Thread writer = new Thread(() -> {
                await(startGate);
                // Giá trị "tiến dần" về cực trị để tạo nhiều lần cập nhật min/max chen nhau.
                for (long i = 0; i < 300_000; i++) {
                    long jitter = ThreadLocalRandom.current().nextLong(0, 1_000);
                    metrics.addSample(-i * 1_000 - jitter);
                    metrics.addSample(i * 1_000 + jitter);
                }
            });
            writer.setUncaughtExceptionHandler((thread, error) -> violation.compareAndSet(null, "Exception trong writer: " + error));
            writers.add(writer);
            writer.start();
        }
        startGate.countDown();
        for (Thread writer : writers) {
            writer.join();
        }
        writersDone.set(true);
        for (Thread reader : readers) {
            reader.join();
        }
        assertNull(violation.get(), violation.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
