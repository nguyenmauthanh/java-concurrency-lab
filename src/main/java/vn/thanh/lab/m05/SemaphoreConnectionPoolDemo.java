package vn.thanh.lab.m05;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo {@link Semaphore} giới hạn số thread truy cập tài nguyên cùng lúc - giống một connection pool.
 * 10 thread tranh 3 "connection": tại mọi thời điểm tối đa 3 thread đang dùng connection.
 */
public class SemaphoreConnectionPoolDemo {

    static class FakeConnectionPool {
        private final Semaphore permits;
        private final ConcurrentLinkedQueue<String> idleConnections = new ConcurrentLinkedQueue<>();

        FakeConnectionPool(int size) {
            permits = new Semaphore(size, true); // fair: thread chờ lâu nhất được cấp trước
            for (int i = 1; i <= size; i++) {
                idleConnections.add("conn-" + i);
            }
        }

        /** Chờ tối đa timeout; hết thời gian trả null (giống HikariCP ném SQLTransientConnectionException). */
        String borrow(long timeout, TimeUnit unit) throws InterruptedException {
            if (!permits.tryAcquire(timeout, unit)) {
                return null;
            }
            return idleConnections.poll(); // có permit => chắc chắn còn connection rảnh
        }

        void release(String connection) {
            idleConnections.add(connection);
            permits.release(); // trả permit SAU khi trả connection
        }
    }

    public static void main(String[] args) throws InterruptedException {
        FakeConnectionPool pool = new FakeConnectionPool(3);
        AtomicInteger inUse = new AtomicInteger();
        AtomicInteger maxInUse = new AtomicInteger();
        AtomicInteger timeouts = new AtomicInteger();
        long start = System.nanoTime();

        List<Thread> workers = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Thread worker = new Thread(() -> {
                try {
                    String connection = pool.borrow(1_500, TimeUnit.MILLISECONDS);
                    if (connection == null) {
                        timeouts.incrementAndGet();
                        System.out.printf("%5d ms  %-10s TIMEOUT chờ connection%n", elapsedMs(start), Thread.currentThread().getName());
                        return;
                    }
                    try {
                        int current = inUse.incrementAndGet();
                        maxInUse.accumulateAndGet(current, Math::max);
                        System.out.printf("%5d ms  %-10s lấy %s (đang dùng: %d)%n", elapsedMs(start), Thread.currentThread().getName(), connection, current);
                        Thread.sleep(400); // giả lập query DB
                        inUse.decrementAndGet();
                    } finally {
                        pool.release(connection); // luôn trả trong finally
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "request-" + i);
            workers.add(worker);
            worker.start();
        }
        for (Thread worker : workers) {
            worker.join();
        }
        System.out.printf("%nSố connection dùng đồng thời tối đa: %d (pool size = 3), số request timeout: %d%n",
                maxInUse.get(), timeouts.get());
        System.out.println("10 request x 400ms / 3 connection -> ~4 đợt; request ở đợt 4 (sau ~1200ms) có thể timeout nếu chờ quá 1500ms.");
    }

    private static long elapsedMs(long start) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }
}
