package vn.thanh.lab.m07.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentFanOutTest {

    private static final Duration GENEROUS_TIMEOUT = Duration.ofSeconds(5);

    private static void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Gọi song song: tổng thời gian ≈ service chậm nhất (300ms), không phải tổng (600ms)")
    void totalTimeIsCloseToSlowestService() {
        PaymentFanOut fanOut = new PaymentFanOut(
                (account, amount) -> { sleep(300); return false; },
                account -> { sleep(200); return 1_000_000; },
                account -> { sleep(100); return 1_000_000; });

        fanOut.evaluate("warm-up", 1, GENEROUS_TIMEOUT); // warm-up JIT/class loading

        long start = System.nanoTime();
        PaymentDecision decision = fanOut.evaluate("ACC-1", 50_000, GENEROUS_TIMEOUT);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(PaymentDecision.approve(), decision);
        assertTrue(elapsedMs >= 290, "Nhanh bất thường: " + elapsedMs + "ms");
        assertTrue(elapsedMs < 500, "Mất " + elapsedMs + "ms -> có vẻ đang gọi tuần tự (tổng = 600ms)");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Mỗi lời gọi service chạy trên virtual thread, và chạy đồng thời")
    void servicesRunConcurrentlyOnVirtualThreads() {
        Set<Boolean> virtualFlags = ConcurrentHashMap.newKeySet();
        CountDownLatch allThreeStarted = new CountDownLatch(3);
        AtomicInteger notConcurrent = new AtomicInteger();
        // Mỗi service chờ cho tới khi cả 3 cùng bắt đầu -> nếu gọi tuần tự thì sẽ không bao giờ đủ 3.
        FraudService fraud = (account, amount) -> {
            virtualFlags.add(Thread.currentThread().isVirtual());
            allThreeStarted.countDown();
            if (!allThreeStarted.await(2, TimeUnit.SECONDS)) {
                notConcurrent.incrementAndGet();
            }
            return false;
        };
        BalanceService balance = account -> {
            virtualFlags.add(Thread.currentThread().isVirtual());
            allThreeStarted.countDown();
            if (!allThreeStarted.await(2, TimeUnit.SECONDS)) {
                notConcurrent.incrementAndGet();
            }
            return 100;
        };
        LimitService limit = account -> {
            virtualFlags.add(Thread.currentThread().isVirtual());
            allThreeStarted.countDown();
            if (!allThreeStarted.await(2, TimeUnit.SECONDS)) {
                notConcurrent.incrementAndGet();
            }
            return 100;
        };

        PaymentDecision decision = new PaymentFanOut(fraud, balance, limit).evaluate("ACC-2", 10, GENEROUS_TIMEOUT);

        assertEquals(PaymentDecision.approve(), decision);
        assertEquals(Set.of(true), virtualFlags, "Service phải chạy trên virtual thread");
        assertEquals(0, notConcurrent.get(), "3 service không chạy đồng thời");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Thứ tự ưu tiên khi bị từ chối")
    void rejectionPriority() {
        assertEquals(PaymentDecision.reject(PaymentDecision.FRAUD_SUSPECTED),
                new PaymentFanOut((a, m) -> true, a -> 0, a -> 0).evaluate("A", 100, GENEROUS_TIMEOUT));
        assertEquals(PaymentDecision.reject(PaymentDecision.INSUFFICIENT_BALANCE),
                new PaymentFanOut((a, m) -> false, a -> 99, a -> 0).evaluate("A", 100, GENEROUS_TIMEOUT));
        assertEquals(PaymentDecision.reject(PaymentDecision.LIMIT_EXCEEDED),
                new PaymentFanOut((a, m) -> false, a -> 100, a -> 99).evaluate("A", 100, GENEROUS_TIMEOUT));
        assertEquals(PaymentDecision.approve(),
                new PaymentFanOut((a, m) -> false, a -> 100, a -> 100).evaluate("A", 100, GENEROUS_TIMEOUT));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Service ném exception -> SERVICE_ERROR")
    void serviceFailure() {
        PaymentFanOut fanOut = new PaymentFanOut(
                (a, m) -> false,
                a -> { throw new IllegalStateException("core banking down"); },
                a -> 1_000);
        assertEquals(PaymentDecision.reject(PaymentDecision.SERVICE_ERROR), fanOut.evaluate("A", 1, GENEROUS_TIMEOUT));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Timeout tổng: trả TIMEOUT đúng hạn và huỷ lời gọi chậm")
    void timeoutCancelsSlowCall() throws InterruptedException {
        CountDownLatch slowCallInterrupted = new CountDownLatch(1);
        PaymentFanOut fanOut = new PaymentFanOut(
                (a, m) -> false,
                a -> {
                    try {
                        sleep(5_000);
                        return 1_000;
                    } catch (InterruptedException e) {
                        slowCallInterrupted.countDown();
                        throw e;
                    }
                },
                a -> { sleep(50); return 1_000; });

        long start = System.nanoTime();
        PaymentDecision decision = fanOut.evaluate("A", 1, Duration.ofMillis(200));
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(PaymentDecision.reject(PaymentDecision.TIMEOUT), decision);
        assertTrue(elapsedMs < 700, "Timeout 200ms nhưng method chạy " + elapsedMs + "ms (có chờ service chậm không?)");
        assertTrue(slowCallInterrupted.await(2, TimeUnit.SECONDS), "Lời gọi chậm không bị interrupt -> rò thread");
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    @DisplayName("500 request đồng thời, mỗi service chờ 100ms: tất cả xong nhanh (virtual threads không bị giới hạn pool)")
    void manyConcurrentEvaluations() throws InterruptedException {
        PaymentFanOut fanOut = new PaymentFanOut(
                (a, m) -> { sleep(100); return false; },
                a -> { sleep(100); return 1_000; },
                a -> { sleep(100); return 1_000; });
        int requests = 500;
        AtomicInteger approved = new AtomicInteger();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> callers = new ArrayList<>();
        long start = System.nanoTime();
        for (int i = 0; i < requests; i++) {
            String account = "ACC-" + i;
            callers.add(Thread.ofVirtual().start(() -> {
                try {
                    if (fanOut.evaluate(account, 10, GENEROUS_TIMEOUT).approved()) {
                        approved.incrementAndGet();
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            }));
        }
        for (Thread caller : callers) {
            caller.join();
        }
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (failure.get() instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        assertFalse(failure.get() != null, String.valueOf(failure.get()));
        assertEquals(requests, approved.get());
        assertTrue(elapsedMs < 3_000, "500 request x (3 service song song 100ms) mất " + elapsedMs + "ms");
    }
}
