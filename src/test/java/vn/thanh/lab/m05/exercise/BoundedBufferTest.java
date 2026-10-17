package vn.thanh.lab.m05.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoundedBufferTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void fifoOrderAndSize() throws InterruptedException {
        BoundedBuffer<String> buffer = new BoundedBuffer<>(3);
        assertEquals(3, buffer.capacity());
        buffer.put("a");
        buffer.put("b");
        assertEquals(2, buffer.size());
        assertEquals("a", buffer.take());
        buffer.put("c");
        buffer.put("d"); // vòng qua đầu mảng
        assertEquals("b", buffer.take());
        assertEquals("c", buffer.take());
        assertEquals("d", buffer.take());
        assertEquals(0, buffer.size());
    }

    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new BoundedBuffer<String>(0));
        BoundedBuffer<String> buffer = new BoundedBuffer<>(1);
        assertThrows(NullPointerException.class, () -> buffer.put(null));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("put chặn khi đầy, được thả khi có take")
    void putBlocksWhenFull() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(1);
        buffer.put(1);
        AtomicBoolean secondPutDone = new AtomicBoolean(false);
        Thread producer = new Thread(() -> {
            try {
                buffer.put(2);
                secondPutDone.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        producer.start();
        waitUntilWaiting(producer);
        assertFalse(secondPutDone.get(), "put phải bị chặn khi buffer đầy");
        assertEquals(1, buffer.take());
        producer.join(2_000);
        assertTrue(secondPutDone.get());
        assertEquals(2, buffer.take());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("take chặn khi rỗng và phản hồi interrupt")
    void takeBlocksWhenEmptyAndIsInterruptible() throws InterruptedException {
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(2);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread consumer = new Thread(() -> {
            try {
                buffer.take();
            } catch (Throwable t) {
                outcome.set(t);
            }
        });
        consumer.start();
        waitUntilWaiting(consumer);
        consumer.interrupt();
        consumer.join(2_000);
        assertFalse(consumer.isAlive());
        assertTrue(outcome.get() instanceof InterruptedException, "Kỳ vọng InterruptedException, nhận: " + outcome.get());
    }

    @RepeatedTest(10)
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Nhiều producer + consumer, capacity nhỏ: mỗi phần tử được lấy đúng 1 lần, không vượt capacity, không treo")
    void multipleProducersAndConsumers() throws InterruptedException {
        int capacity = 4;
        int producers = 4;
        int consumers = 4;
        int itemsPerProducer = 25_000;
        int total = producers * itemsPerProducer;
        BoundedBuffer<Integer> buffer = new BoundedBuffer<>(capacity);
        AtomicIntegerArray seen = new AtomicIntegerArray(total);
        AtomicReference<String> violation = new AtomicReference<>();
        AtomicBoolean finished = new AtomicBoolean(false);

        Thread sizeWatcher = new Thread(() -> {
            while (!finished.get()) {
                int size = buffer.size();
                if (size < 0 || size > capacity) {
                    violation.compareAndSet(null, "size() = " + size + " nằm ngoài [0, " + capacity + "]");
                }
            }
        });
        sizeWatcher.setDaemon(true);
        sizeWatcher.start();

        List<Thread> threads = new ArrayList<>();
        for (int p = 0; p < producers; p++) {
            int base = p * itemsPerProducer;
            threads.add(new Thread(() -> {
                try {
                    for (int i = 0; i < itemsPerProducer; i++) {
                        buffer.put(base + i);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        for (int c = 0; c < consumers; c++) {
            threads.add(new Thread(() -> {
                try {
                    for (int i = 0; i < total / consumers; i++) {
                        int item = buffer.take();
                        if (seen.incrementAndGet(item) != 1) {
                            violation.compareAndSet(null, "Phần tử " + item + " bị lấy nhiều lần");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        threads.forEach(thread -> {
            thread.setDaemon(true);
            thread.start();
        });
        for (Thread thread : threads) {
            thread.join(20_000);
            assertFalse(thread.isAlive(), "Thread bị treo -> có thể lost wakeup / deadlock");
        }
        finished.set(true);
        sizeWatcher.join();

        assertNull(violation.get(), violation.get());
        for (int i = 0; i < total; i++) {
            assertEquals(1, seen.get(i), "Phần tử " + i + " bị mất");
        }
        assertEquals(0, buffer.size());
    }

    private static void waitUntilWaiting(Thread thread) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (thread.getState() != Thread.State.WAITING && thread.getState() != Thread.State.TIMED_WAITING) {
            if (System.nanoTime() > deadline) {
                throw new AssertionError("Thread không bị chặn, trạng thái: " + thread.getState());
            }
            Thread.sleep(5);
        }
    }
}
