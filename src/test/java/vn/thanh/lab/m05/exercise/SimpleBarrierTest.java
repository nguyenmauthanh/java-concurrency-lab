package vn.thanh.lab.m05.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SimpleBarrierTest {

    @Test
    void invalidParties() {
        assertThrows(IllegalArgumentException.class, () -> new SimpleBarrier(0));
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    void singlePartyReturnsImmediately() throws InterruptedException {
        new SimpleBarrier(1).await();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("N-1 thread phải bị giữ lại cho tới khi thread thứ N tới")
    void holdsThreadsUntilLastArrives() throws InterruptedException {
        int parties = 5;
        SimpleBarrier barrier = new SimpleBarrier(parties);
        AtomicInteger passed = new AtomicInteger();
        List<Thread> early = new ArrayList<>();
        for (int i = 0; i < parties - 1; i++) {
            Thread thread = new Thread(() -> {
                try {
                    barrier.await();
                    passed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.setDaemon(true);
            early.add(thread);
            thread.start();
        }
        Thread.sleep(300);
        assertEquals(0, passed.get(), "Có thread qua barrier khi chưa đủ " + parties + " party");

        barrier.await(); // party cuối cùng
        for (Thread thread : early) {
            thread.join(2_000);
            assertFalse(thread.isAlive(), "Thread vẫn bị chặn sau khi đủ party");
        }
        assertEquals(parties - 1, passed.get());

        barrier.await(); // dùng một lần: đã mở thì trả về ngay
    }

    @RepeatedTest(200)
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Lặp nhiều lần: sau barrier mọi thread đều thấy đủ N lượt 'arrived'")
    void everyThreadSeesAllArrivals() throws InterruptedException {
        int parties = 8;
        SimpleBarrier barrier = new SimpleBarrier(parties);
        AtomicInteger arrived = new AtomicInteger();
        AtomicReference<String> violation = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < parties; i++) {
            Thread thread = new Thread(() -> {
                try {
                    arrived.incrementAndGet();
                    barrier.await();
                    int seen = arrived.get();
                    if (seen != parties) {
                        violation.compareAndSet(null, "Một thread qua barrier khi mới có " + seen + "/" + parties + " thread tới");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            thread.setDaemon(true);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join(5_000);
            assertFalse(thread.isAlive(), "Thread bị treo ở barrier");
        }
        assertNull(violation.get(), violation.get());
    }
}
