package vn.thanh.lab.m07;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo PINNING (Java 21-23): virtual thread block bên trong {@code synchronized} sẽ GIỮ LUÔN carrier thread
 * (không unmount được) -> các virtual thread khác không có carrier để chạy.
 * <p>
 * Chạy kèm {@code -Djdk.tracePinnedThreads=full} để JVM in stack trace mỗi khi pinning xảy ra. Trace này không hiện
 * khi chạy trong JVM của Maven ({@code exec:java}), nên hãy chạy bằng {@code java} trực tiếp:
 * <pre>mvn -q compile &amp;&amp; java -Djdk.tracePinnedThreads=full -cp target/classes vn.thanh.lab.m07.PinningDemo</pre>
 * Demo tự đặt property này (và giới hạn 2 carrier) trước khi tạo virtual thread đầu tiên nếu bạn chưa đặt,
 * nên phần so sánh thời gian vẫn đúng khi chạy bằng {@code exec:java}.
 * <p>
 * Lưu ý: từ JDK 24 (JEP 491), synchronized không còn pin khi block; demo này phản ánh hành vi của Java 21.
 */
public class PinningDemo {

    private static final int TASKS = 20;
    private static final long BLOCKING_MS = 100;

    public static void main(String[] args) throws InterruptedException {
        // Phải đặt TRƯỚC khi scheduler của virtual thread được khởi tạo (lần đầu tạo virtual thread).
        setIfAbsent("jdk.tracePinnedThreads", "full");
        setIfAbsent("jdk.virtualThreadScheduler.parallelism", "2");
        System.out.println("jdk.tracePinnedThreads = " + System.getProperty("jdk.tracePinnedThreads")
                + ", số carrier = " + System.getProperty("jdk.virtualThreadScheduler.parallelism"));
        System.out.printf("%d virtual thread, mỗi cái block %dms TRONG critical section (mỗi task một lock riêng, không tranh chấp)%n%n",
                TASKS, BLOCKING_MS);

        long pinnedMs = run("synchronized", () -> {
            Object lock = new Object();
            synchronized (lock) {
                sleep(); // block khi đang giữ monitor -> PIN carrier
            }
        });
        long unpinnedMs = run("ReentrantLock", () -> {
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            try {
                sleep(); // block khi giữ ReentrantLock -> unmount bình thường
            } finally {
                lock.unlock();
            }
        });

        System.out.printf("%nsynchronized : %5d ms  (~ %d task / 2 carrier x %dms vì carrier bị pin)%n", pinnedMs, TASKS, BLOCKING_MS);
        System.out.printf("ReentrantLock: %5d ms  (~ %dms: mọi task cùng chờ song song)%n", unpinnedMs, BLOCKING_MS);
        System.out.println("""

                Cách sửa pinning trên Java 21:
                  1. Thay synchronized bằng ReentrantLock ở đoạn code có blocking IO bên trong.
                  2. Kiểm tra thư viện (JDBC driver, HTTP client cũ) có dùng synchronized quanh IO không.
                  3. Theo dõi bằng -Djdk.tracePinnedThreads=full hoặc JFR event jdk.VirtualThreadPinned.
                """);
    }

    private static long run(String label, Runnable task) throws InterruptedException {
        System.out.println("--- " + label + " ---");
        List<Thread> threads = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < TASKS; i++) {
            threads.add(Thread.ofVirtual().name(label + "-" + i).start(task));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return (System.nanoTime() - start) / 1_000_000;
    }

    private static void sleep() {
        try {
            Thread.sleep(BLOCKING_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void setIfAbsent(String key, String value) {
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}
