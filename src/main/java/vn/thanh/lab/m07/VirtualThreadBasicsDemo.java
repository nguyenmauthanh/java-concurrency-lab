package vn.thanh.lab.m07;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Demo cơ bản về Virtual Threads (Java 21): các cách tạo, và quan sát carrier thread.
 * <p>
 * {@code toString()} của virtual thread có dạng {@code VirtualThread[#29,name]/runnable@ForkJoinPool-1-worker-3}:
 * phần sau "@" là CARRIER (platform thread trong ForkJoinPool) đang chở virtual thread đó.
 */
public class VirtualThreadBasicsDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 1. Các cách tạo virtual thread ===");
        Thread first = Thread.ofVirtual().name("vt-builder").start(() ->
                System.out.println("ofVirtual().start  : " + Thread.currentThread()));
        Thread second = Thread.startVirtualThread(() ->
                System.out.println("startVirtualThread : " + Thread.currentThread() + " (không có tên mặc định)"));
        Thread unstarted = Thread.ofVirtual().name("vt-unstarted").unstarted(() ->
                System.out.println("unstarted().start(): " + Thread.currentThread()));
        unstarted.start();
        first.join();
        second.join();
        unstarted.join();
        System.out.println("isVirtual = " + first.isVirtual() + ", isDaemon = " + first.isDaemon()
                + " (virtual thread luôn là daemon), main.isVirtual = " + Thread.currentThread().isVirtual());

        System.out.println("\n=== 2. Carrier thread thay đổi sau mỗi lần block ===");
        Thread hopper = Thread.ofVirtual().name("hopper").start(() -> {
            for (int i = 0; i < 4; i++) {
                System.out.println("  lần " + i + ": " + Thread.currentThread());
                sleep(Duration.ofMillis(20)); // unmount khỏi carrier; khi chạy lại có thể trên carrier khác
            }
        });
        hopper.join();

        System.out.println("\n=== 3. Executors.newVirtualThreadPerTaskExecutor(): mỗi task một virtual thread mới ===");
        // ExecutorService là AutoCloseable từ Java 19: close() chờ mọi task xong.
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                int taskId = i;
                futures.add(executor.submit(() -> {
                    sleep(Duration.ofMillis(50));
                    return "task-" + taskId + " chạy trên " + Thread.currentThread();
                }));
            }
            for (Future<String> future : futures) {
                System.out.println("  " + future.get());
            }
        }

        System.out.println("\n=== 4. Tạo 100.000 virtual thread ===");
        long start = System.nanoTime();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 100_000; i++) {
            threads.add(Thread.ofVirtual().start(() -> sleep(Duration.ofMillis(100))));
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.printf("100.000 virtual thread (mỗi cái ngủ 100ms) xong trong %d ms. Với platform thread: ~100GB stack ảo, OS sẽ từ chối.%n",
                (System.nanoTime() - start) / 1_000_000);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
