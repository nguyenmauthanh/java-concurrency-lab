package vn.thanh.lab.m08.exercise;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Bulkhead {

    private final Semaphore permits;
    private final long maxWaitNanos;

    public Bulkhead(int maxConcurrentCalls, Duration maxWait) {
        if (maxConcurrentCalls < 1) {
            throw new IllegalArgumentException("maxConcurrentCalls phải >= 1");
        }
        if (maxWait == null || maxWait.isNegative()) {
            throw new IllegalArgumentException("maxWait phải >= 0");
        }
        this.permits = new Semaphore(maxConcurrentCalls, true); // fair: không để request chờ lâu bị "đói"
        this.maxWaitNanos = maxWait.toNanos();
    }

    public <T> T execute(Callable<T> call) throws Exception {
        // tryAcquire(0, ...) không chờ; tryAcquire(timeout) phản hồi interrupt bằng InterruptedException.
        if (!permits.tryAcquire(maxWaitNanos, TimeUnit.NANOSECONDS)) {
            throw new BulkheadFullException("Bulkhead đầy, đã chờ " + Duration.ofNanos(maxWaitNanos));
        }
        try {
            return call.call(); // exception của call đi thẳng ra ngoài
        } finally {
            permits.release(); // chỉ release khi ĐÃ acquire thành công
        }
    }

    public int availablePermits() {
        return permits.availablePermits();
    }
}
