package vn.thanh.lab.m02;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Demo tối ưu THROUGHPUT: nhiều task ĐỘC LẬP chạy trên thread pool với số thread khác nhau.
 * <p>
 * Hai loại task:
 * <ul>
 *     <li>CPU-bound: throughput tăng tới ~số core rồi đứng yên.</li>
 *     <li>IO-bound (giả lập bằng sleep): throughput tiếp tục tăng khi thêm thread, vì thread chủ yếu đang chờ.</li>
 * </ul>
 */
public class ThroughputDemo {

    private static final int CPU_TASKS = 2_000;
    private static final int IO_TASKS = 1_000;
    private static final long IO_LATENCY_MS = 10;

    public static void main(String[] args) throws InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        System.out.printf("Máy có %d logical core%n", cores);
        int[] poolSizes = {1, 2, 4, cores, cores * 2, 64, 200};

        runBenchmark(cores, ThroughputDemo::cpuTask, "CPU-bound", CPU_TASKS); // warm-up
        System.out.println("\n=== CPU-bound: " + CPU_TASKS + " task tính toán ===");
        System.out.println(" pool | thời gian (ms) | task/giây");
        for (int poolSize : poolSizes) {
            runBenchmark(poolSize, ThroughputDemo::cpuTask, "CPU-bound", CPU_TASKS);
        }

        System.out.println("\n=== IO-bound: " + IO_TASKS + " task, mỗi task chờ " + IO_LATENCY_MS + "ms ===");
        System.out.println(" pool | thời gian (ms) | task/giây");
        for (int poolSize : poolSizes) {
            runBenchmark(poolSize, ThroughputDemo::ioTask, "IO-bound", IO_TASKS);
        }

        System.out.println("""

                Nhận xét:
                - CPU-bound: pool lớn hơn số core không tăng throughput (còn tốn thêm context switch).
                - IO-bound: pool càng lớn throughput càng cao, nhưng mỗi platform thread tốn ~1MB stack
                  -> đây là động lực của virtual threads (m07).
                """);
    }

    private static void runBenchmark(int poolSize, Runnable task, String label, int taskCount) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        long start = System.nanoTime();
        for (int i = 0; i < taskCount; i++) {
            pool.execute(task);
        }
        pool.shutdown();
        if (!pool.awaitTermination(2, TimeUnit.MINUTES)) {
            throw new IllegalStateException(label + " chạy quá lâu");
        }
        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf(" %4d | %14.1f | %,10.0f%n", poolSize, elapsedMs, taskCount / (elapsedMs / 1000));
    }

    private static volatile double sink; // tránh JIT loại bỏ phép tính "vô dụng"

    private static void cpuTask() {
        double accumulator = 0;
        for (int i = 1; i < 200_000; i++) {
            accumulator += Math.sqrt(i) / i;
        }
        sink = accumulator;
    }

    private static void ioTask() {
        try {
            Thread.sleep(IO_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
