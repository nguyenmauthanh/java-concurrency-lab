package vn.thanh.lab.m06;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Demo Compare-And-Swap (CAS): tự viết increment bằng vòng lặp compareAndSet và đếm số lần phải thử lại
 * khi nhiều thread tranh chấp.
 */
public class CasDemo {

    private static final int THREADS = 8;
    private static final int INCREMENTS_PER_THREAD = 500_000;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. Ngữ nghĩa compareAndSet(expected, newValue) ===");
        AtomicInteger value = new AtomicInteger(10);
        System.out.println("value = 10; compareAndSet(10, 11) -> " + value.compareAndSet(10, 11) + ", value = " + value.get());
        System.out.println("value = 11; compareAndSet(10, 12) -> " + value.compareAndSet(10, 12) + ", value = " + value.get()
                + "  (thất bại vì giá trị hiện tại không còn là 10)");

        System.out.printf("%n=== 2. %d thread x %,d lần tăng ===%n", THREADS, INCREMENTS_PER_THREAD);
        AtomicInteger counter = new AtomicInteger();
        LongAdder failedCas = new LongAdder();
        long elapsed = runThreads(() -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                // Vòng CAS kinh điển: đọc -> tính -> CAS; nếu ai đó chen vào thì đọc lại và thử lại.
                while (true) {
                    int current = counter.get();
                    int next = current + 1;
                    if (counter.compareAndSet(current, next)) {
                        break;
                    }
                    failedCas.increment();
                }
            }
        });
        System.out.printf("CAS loop tự viết : value = %,d, CAS thất bại %,d lần, %d ms%n", counter.get(), failedCas.sum(), elapsed);

        AtomicInteger builtIn = new AtomicInteger();
        elapsed = runThreads(() -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                builtIn.incrementAndGet(); // JIT dùng lệnh LOCK XADD trên x86 -> không cần vòng retry
            }
        });
        System.out.printf("incrementAndGet  : value = %,d, %d ms%n", builtIn.get(), elapsed);

        LongAdder adder = new LongAdder();
        elapsed = runThreads(() -> {
            for (int i = 0; i < INCREMENTS_PER_THREAD; i++) {
                adder.increment();
            }
        });
        System.out.printf("LongAdder        : value = %,d, %d ms (chia cell, gần như không tranh chấp)%n", adder.sum(), elapsed);
    }

    private static long runThreads(Runnable task) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        long start = System.nanoTime();
        for (int i = 0; i < THREADS; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
