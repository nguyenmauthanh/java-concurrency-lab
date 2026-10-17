package vn.thanh.lab.m05;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Demo producer–consumer với {@link BlockingQueue} có giới hạn (back-pressure) và "poison pill" để dừng consumer.
 * Đây là mô hình của rất nhiều hệ thống: Kafka consumer -> queue nội bộ -> worker ghi DB.
 */
public class ProducerConsumerDemo {

    private static final int PRODUCERS = 2;
    private static final int CONSUMERS = 3;
    private static final int EVENTS_PER_PRODUCER = 50_000;
    private static final long POISON_PILL = Long.MIN_VALUE;

    public static void main(String[] args) throws InterruptedException {
        BlockingQueue<Long> queue = new ArrayBlockingQueue<>(100); // đầy -> producer bị chặn (back-pressure)
        AtomicLong consumedCount = new AtomicLong();
        AtomicLong consumedSum = new AtomicLong();

        List<Thread> producers = new ArrayList<>();
        for (int p = 0; p < PRODUCERS; p++) {
            int producerId = p;
            producers.add(new Thread(() -> {
                try {
                    for (int i = 1; i <= EVENTS_PER_PRODUCER; i++) {
                        queue.put((long) producerId * EVENTS_PER_PRODUCER + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "producer-" + p));
        }

        List<Thread> consumers = new ArrayList<>();
        for (int c = 0; c < CONSUMERS; c++) {
            consumers.add(new Thread(() -> {
                try {
                    while (true) {
                        long event = queue.take();
                        if (event == POISON_PILL) {
                            return;
                        }
                        consumedCount.incrementAndGet();
                        consumedSum.addAndGet(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "consumer-" + c));
        }

        long start = System.nanoTime();
        producers.forEach(Thread::start);
        consumers.forEach(Thread::start);
        for (Thread producer : producers) {
            producer.join();
        }
        // Mỗi consumer cần một poison pill riêng; gửi SAU khi mọi producer xong để không dừng sớm.
        for (int c = 0; c < CONSUMERS; c++) {
            queue.put(POISON_PILL);
        }
        for (Thread consumer : consumers) {
            consumer.join();
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        long total = (long) PRODUCERS * EVENTS_PER_PRODUCER;
        long expectedSum = total * (total + 1) / 2;
        System.out.printf("Đã xử lý %,d / %,d event trong %d ms, tổng kiểm tra %s%n",
                consumedCount.get(), total, elapsedMs, consumedSum.get() == expectedSum ? "KHỚP" : "SAI");
    }
}
