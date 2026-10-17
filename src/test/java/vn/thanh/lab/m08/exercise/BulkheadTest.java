package vn.thanh.lab.m08.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkheadTest {

    @Test
    void invalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new Bulkhead(0, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new Bulkhead(1, Duration.ofMillis(-1)));
        assertThrows(IllegalArgumentException.class, () -> new Bulkhead(1, null));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void returnsResultAndReleasesPermit() throws Exception {
        Bulkhead bulkhead = new Bulkhead(3, Duration.ZERO);
        assertEquals("ok", bulkhead.execute(() -> "ok"));
        assertEquals(3, bulkhead.availablePermits());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Exception của call được ném lại nguyên vẹn và chỗ vẫn được trả")
    void propagatesExceptionAndReleasesPermit() {
        Bulkhead bulkhead = new Bulkhead(2, Duration.ZERO);
        IllegalStateException failure = new IllegalStateException("core banking down");
        Exception thrown = assertThrows(Exception.class, () -> bulkhead.execute(() -> {
            throw failure;
        }));
        assertSame(failure, thrown);
        assertEquals(2, bulkhead.availablePermits());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("maxWait = 0: hết chỗ thì từ chối ngay, không chạy call")
    void rejectsImmediatelyWhenFull() throws Exception {
        Bulkhead bulkhead = new Bulkhead(2, Duration.ZERO);
        CountDownLatch holding = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        List<Thread> holders = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            holders.add(Thread.ofVirtual().start(() -> {
                try {
                    bulkhead.execute(() -> {
                        holding.countDown();
                        release.await();
                        return null;
                    });
                } catch (Exception ignored) {
                    // không quan trọng trong test này
                }
            }));
        }
        assertTrue(holding.await(2, TimeUnit.SECONDS));
        assertEquals(0, bulkhead.availablePermits());

        AtomicBoolean ran = new AtomicBoolean(false);
        long start = System.nanoTime();
        assertThrows(BulkheadFullException.class, () -> bulkhead.execute(() -> {
            ran.set(true);
            return null;
        }));
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 200, "Phải từ chối ngay");
        assertFalse(ran.get(), "call không được chạy khi bị từ chối");

        release.countDown();
        for (Thread holder : holders) {
            holder.join();
        }
        assertEquals(2, bulkhead.availablePermits());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("maxWait > 0: chờ đúng khoảng maxWait rồi mới từ chối")
    void waitsUpToMaxWaitThenRejects() throws Exception {
        Bulkhead bulkhead = new Bulkhead(1, Duration.ofMillis(200));
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = Thread.ofVirtual().start(() -> {
            try {
                bulkhead.execute(() -> {
                    holding.countDown();
                    release.await();
                    return null;
                });
            } catch (Exception ignored) {
                // không quan trọng
            }
        });
        assertTrue(holding.await(2, TimeUnit.SECONDS));
        long start = System.nanoTime();
        assertThrows(BulkheadFullException.class, () -> bulkhead.execute(() -> "never"));
        long waitedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(waitedMs >= 180 && waitedMs < 1_000, "Kỳ vọng chờ ~200ms, thực tế " + waitedMs + "ms");
        release.countDown();
        holder.join();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    @DisplayName("Bị interrupt khi đang chờ chỗ -> InterruptedException")
    void interruptWhileWaiting() throws Exception {
        Bulkhead bulkhead = new Bulkhead(1, Duration.ofSeconds(30));
        CountDownLatch holding = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = Thread.ofVirtual().start(() -> {
            try {
                bulkhead.execute(() -> {
                    holding.countDown();
                    release.await();
                    return null;
                });
            } catch (Exception ignored) {
                // không quan trọng
            }
        });
        assertTrue(holding.await(2, TimeUnit.SECONDS));
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread waiter = Thread.ofPlatform().start(() -> {
            try {
                bulkhead.execute(() -> "never");
            } catch (Throwable t) {
                outcome.set(t);
            }
        });
        Thread.sleep(200);
        waiter.interrupt();
        waiter.join(2_000);
        assertTrue(outcome.get() instanceof InterruptedException, "Kỳ vọng InterruptedException, nhận: " + outcome.get());
        release.countDown();
        holder.join();
        assertEquals(1, bulkhead.availablePermits(), "Thread bị interrupt không được làm rò/thừa permit");
    }

    @RepeatedTest(5)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("2.000 virtual thread cùng gọi: số lời gọi đồng thời không bao giờ vượt giới hạn")
    void neverExceedsLimitUnderLoad() throws InterruptedException {
        int limit = 20;
        Bulkhead bulkhead = new Bulkhead(limit, Duration.ofSeconds(10));
        AtomicInteger current = new AtomicInteger();
        AtomicInteger maxObserved = new AtomicInteger();
        AtomicInteger succeeded = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> callers = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            callers.add(Thread.ofVirtual().start(() -> {
                try {
                    bulkhead.execute(() -> {
                        int now = current.incrementAndGet();
                        maxObserved.accumulateAndGet(now, Math::max);
                        Thread.sleep(5); // giả lập gọi core banking
                        current.decrementAndGet();
                        return null;
                    });
                    succeeded.incrementAndGet();
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }));
        }
        for (Thread caller : callers) {
            caller.join();
        }
        if (failure.get() instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        assertEquals(null, failure.get());
        assertEquals(2_000, succeeded.get());
        assertTrue(maxObserved.get() <= limit, "Có " + maxObserved.get() + " lời gọi đồng thời, giới hạn là " + limit);
        assertEquals(limit, bulkhead.availablePermits());
    }
}
