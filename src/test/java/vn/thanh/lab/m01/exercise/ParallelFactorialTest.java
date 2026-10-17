package vn.thanh.lab.m01.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParallelFactorialTest {

    private final ParallelFactorial parallelFactorial = new ParallelFactorial();

    private static BigInteger sequentialFactorial(long n) {
        BigInteger result = BigInteger.ONE;
        for (long i = 2; i <= n; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Giai thừa các số nhỏ, đúng thứ tự")
    void computesSmallFactorialsInOrder() throws InterruptedException {
        List<BigInteger> result = parallelFactorial.computeFactorials(List.of(0L, 1L, 5L, 10L, 3L, 20L), 5_000);

        assertEquals(List.of(
                BigInteger.ONE,
                BigInteger.ONE,
                BigInteger.valueOf(120),
                BigInteger.valueOf(3_628_800),
                BigInteger.valueOf(6),
                BigInteger.valueOf(2_432_902_008_176_640_000L)), result);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void emptyInputReturnsEmptyList() throws InterruptedException {
        assertTrue(parallelFactorial.computeFactorials(List.of(), 1_000).isEmpty());
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void negativeNumberIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> parallelFactorial.computeFactorials(List.of(3L, -1L), 1_000));
    }

    @RepeatedTest(20)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Nhiều số, nhiều lần chạy: kết quả luôn khớp bản tuần tự (bắt lỗi visibility / ghi đè ô)")
    void manyNumbersMatchSequentialResult() throws InterruptedException {
        List<Long> inputs = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            inputs.add(ThreadLocalRandom.current().nextLong(0, 2_000));
        }

        List<BigInteger> result = parallelFactorial.computeFactorials(inputs, 10_000);

        assertEquals(inputs.size(), result.size());
        for (int i = 0; i < inputs.size(); i++) {
            assertEquals(sequentialFactorial(inputs.get(i)), result.get(i), "Sai ở vị trí " + i + " (n=" + inputs.get(i) + ")");
        }
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Hết timeout: số chưa xong là null, method trả về đúng hạn và worker bị dừng")
    void slowNumberTimesOutAndWorkerStops() throws InterruptedException {
        long start = System.nanoTime();
        List<BigInteger> result = parallelFactorial.computeFactorials(Arrays.asList(5L, 50_000_000L), 300);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertEquals(BigInteger.valueOf(120), result.get(0));
        assertNull(result.get(1), "Số quá lớn phải trả về null khi hết timeout");
        assertTrue(elapsedMs < 1_500, "timeout là tổng cho cả lô, nhưng method chạy tới " + elapsedMs + "ms");

        // Worker bị interrupt phải tự dừng trong thời gian ngắn.
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (workerStillRunning() && System.nanoTime() < deadline) {
            Thread.sleep(50);
        }
        assertTrue(!workerStillRunning(), "Worker vẫn đang tính sau khi bị interrupt -> chưa kiểm tra cờ interrupt");
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Timeout là tổng thời gian, không phải mỗi thread một timeout")
    void timeoutIsTotalNotPerThread() throws InterruptedException {
        List<Long> inputs = List.of(50_000_000L, 50_000_001L, 50_000_002L, 50_000_003L, 50_000_004L);
        long start = System.nanoTime();
        List<BigInteger> result = parallelFactorial.computeFactorials(inputs, 400);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        result.forEach(value -> assertNull(value));
        assertTrue(elapsedMs < 1_200, "5 thread x 400ms nếu join riêng từng cái -> ~2000ms; thực tế: " + elapsedMs + "ms");
    }

    private static boolean workerStillRunning() {
        return Thread.getAllStackTraces().entrySet().stream()
                .filter(entry -> entry.getKey() != Thread.currentThread())
                .flatMap(entry -> Arrays.stream(entry.getValue()))
                .anyMatch(frame -> frame.getClassName().startsWith(ParallelFactorial.class.getName()));
    }
}
