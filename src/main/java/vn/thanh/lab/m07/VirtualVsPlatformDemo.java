package vn.thanh.lab.m07;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * So sánh 10.000 task BLOCKING IO giả lập (mỗi task chờ 100ms) giữa fixed thread pool và virtual threads.
 * <p>
 * Kỳ vọng: pool 100 thread -> 10.000 / 100 x 100ms ≈ 10 giây; virtual threads ≈ 100-300ms.
 */
public class VirtualVsPlatformDemo {

    private static final int TASKS = 10_000;
    private static final long IO_LATENCY_MS = 100;

    public static void main(String[] args) {
        System.out.printf("%,d task, mỗi task giả lập gọi IO mất %dms%n%n", TASKS, IO_LATENCY_MS);

        for (int poolSize : new int[]{100, 500, 1_000}) {
            try (ExecutorService pool = Executors.newFixedThreadPool(poolSize)) {
                report("Fixed pool " + poolSize + " platform thread", pool);
            }
        }
        try (ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor()) {
            report("Virtual thread per task", virtual);
        }

        System.out.println("""

                Nhận xét:
                - Với platform thread, throughput bị chặn bởi số thread: tasks/pool x latency.
                - Virtual thread khi block (sleep, socket IO, BlockingQueue...) sẽ unmount khỏi carrier,
                  nên vài carrier (= số core) phục vụ được hàng chục nghìn task đang chờ.
                - Virtual thread KHÔNG làm code CPU-bound nhanh hơn.
                """);
    }

    private static void report(String label, ExecutorService executor) {
        AtomicInteger completed = new AtomicInteger();
        long start = System.nanoTime();
        for (int i = 0; i < TASKS; i++) {
            executor.submit(() -> {
                Thread.sleep(IO_LATENCY_MS); // giả lập gọi DB / HTTP
                completed.incrementAndGet();
                return null;
            });
        }
        executor.close(); // chờ mọi task xong (try-with-resources cũng gọi lại close(), không sao)
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("%-36s: %,6d ms, %,8.0f task/giây (xong %,d)%n",
                label, elapsedMs, TASKS / (elapsedMs / 1000.0), completed.get());
    }
}
