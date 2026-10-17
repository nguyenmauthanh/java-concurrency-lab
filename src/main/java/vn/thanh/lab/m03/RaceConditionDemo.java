package vn.thanh.lab.m03;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Demo race condition với {@code counter++} và 4 cách sửa:
 * synchronized method, synchronized block, AtomicInteger, LongAdder. Có đo thời gian.
 */
public class RaceConditionDemo {

    private static final int THREADS = 4;
    private static final int INCREMENTS_PER_THREAD = 2_000_000;

    interface Counter {
        void increment();

        long value();
    }

    /** counter++ = đọc -> cộng 1 -> ghi: 3 bước, không nguyên tử. */
    static class UnsafeCounter implements Counter {
        private int count;

        public void increment() {
            count++;
        }

        public long value() {
            return count;
        }
    }

    static class SynchronizedMethodCounter implements Counter {
        private int count;

        public synchronized void increment() { // khoá trên "this"
            count++;
        }

        public synchronized long value() {
            return count;
        }
    }

    static class SynchronizedBlockCounter implements Counter {
        private final Object lock = new Object(); // lock riêng, bên ngoài không khoá nhầm được
        private int count;

        public void increment() {
            synchronized (lock) { // chỉ khoá đúng đoạn critical section
                count++;
            }
        }

        public long value() {
            synchronized (lock) {
                return count;
            }
        }
    }

    static class AtomicCounter implements Counter {
        private final AtomicInteger count = new AtomicInteger();

        public void increment() {
            count.incrementAndGet(); // CAS, không khoá
        }

        public long value() {
            return count.get();
        }
    }

    static class LongAdderCounter implements Counter {
        private final LongAdder count = new LongAdder();

        public void increment() {
            count.increment(); // mỗi thread cộng vào "cell" riêng, tổng hợp khi đọc
        }

        public long value() {
            return count.sum();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        long expected = (long) THREADS * INCREMENTS_PER_THREAD;
        System.out.printf("%d thread, mỗi thread tăng %,d lần -> kỳ vọng %,d%n%n", THREADS, INCREMENTS_PER_THREAD, expected);
        System.out.println(" counter                   |       giá trị | đúng? | thời gian (ms)");
        System.out.println("---------------------------+---------------+-------+---------------");
        run("UnsafeCounter (count++)", new UnsafeCounter(), expected);
        run("synchronized method", new SynchronizedMethodCounter(), expected);
        run("synchronized block", new SynchronizedBlockCounter(), expected);
        run("AtomicInteger", new AtomicCounter(), expected);
        run("LongAdder", new LongAdderCounter(), expected);
    }

    private static void run(String label, Counter counter, long expected) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        long start = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                    counter.increment();
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        long value = counter.value();
        System.out.printf(" %-25s | %,13d | %-5s | %14.1f%n", label, value, value == expected ? "OK" : "SAI", elapsedMs);
    }
}
