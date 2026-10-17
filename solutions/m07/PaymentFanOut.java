package vn.thanh.lab.m07.exercise;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PaymentFanOut {

    private final FraudService fraudService;
    private final BalanceService balanceService;
    private final LimitService limitService;

    public PaymentFanOut(FraudService fraudService, BalanceService balanceService, LimitService limitService) {
        this.fraudService = Objects.requireNonNull(fraudService);
        this.balanceService = Objects.requireNonNull(balanceService);
        this.limitService = Objects.requireNonNull(limitService);
    }

    public PaymentDecision evaluate(String accountId, long amount, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        // Virtual thread rẻ -> tạo executor mới cho mỗi request là bình thường (không cần pool).
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<Boolean> fraud = executor.submit(() -> fraudService.isSuspicious(accountId, amount));
        Future<Long> balance = executor.submit(() -> balanceService.getAvailableBalance(accountId));
        Future<Long> limit = executor.submit(() -> limitService.getRemainingDailyLimit(accountId));
        try {
            // Chờ từng future với thời gian CÒN LẠI của deadline chung.
            boolean suspicious = fraud.get(remaining(deadline), TimeUnit.NANOSECONDS);
            long availableBalance = balance.get(remaining(deadline), TimeUnit.NANOSECONDS);
            long remainingLimit = limit.get(remaining(deadline), TimeUnit.NANOSECONDS);

            if (suspicious) {
                return PaymentDecision.reject(PaymentDecision.FRAUD_SUSPECTED);
            }
            if (availableBalance < amount) {
                return PaymentDecision.reject(PaymentDecision.INSUFFICIENT_BALANCE);
            }
            if (remainingLimit < amount) {
                return PaymentDecision.reject(PaymentDecision.LIMIT_EXCEEDED);
            }
            return PaymentDecision.approve();
        } catch (TimeoutException e) {
            return PaymentDecision.reject(PaymentDecision.TIMEOUT);
        } catch (ExecutionException e) {
            return PaymentDecision.reject(PaymentDecision.SERVICE_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentDecision.reject(PaymentDecision.SERVICE_ERROR);
        } finally {
            // Huỷ mọi lời gọi còn dở (interrupt virtual thread) TRƯỚC, rồi mới đóng executor.
            // Gọi executor.close() mà không huỷ sẽ chờ service chậm chạy xong -> timeout vô nghĩa.
            for (Future<?> future : List.of(fraud, balance, limit)) {
                future.cancel(true);
            }
            executor.shutdownNow();
        }
    }

    private static long remaining(long deadline) {
        return Math.max(0, deadline - System.nanoTime());
    }
}
