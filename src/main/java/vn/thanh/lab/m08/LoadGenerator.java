package vn.thanh.lab.m08;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Load generator dùng {@link HttpClient} của JDK: bắn {@code totalRequests} request với tối đa {@code concurrency}
 * request đang bay cùng lúc (giới hạn bằng Semaphore), mỗi request chạy trên một virtual thread.
 * <p>
 * Chạy: {@code -Dexec.args="<url> <concurrency> <tổng số request>"}.
 */
public class LoadGenerator {

    public record Result(int requests, int errors, long elapsedMs, double throughput, double p50Ms, double p99Ms, double maxMs) {
        @Override
        public String toString() {
            return String.format("%,d request, %d lỗi, %,d ms, %,.0f req/s, p50 = %.1f ms, p99 = %.1f ms, max = %.1f ms",
                    requests, errors, elapsedMs, throughput, p50Ms, p99Ms, maxMs);
        }
    }

    public static Result run(URI uri, int concurrency, int totalRequests) throws InterruptedException {
        long[] latenciesNanos = new long[totalRequests];
        AtomicInteger errors = new AtomicInteger();
        Semaphore inFlight = new Semaphore(concurrency);

        try (ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();
             HttpClient client = HttpClient.newBuilder()
                     .version(HttpClient.Version.HTTP_1_1) // tránh thử nâng cấp lên h2c
                     .connectTimeout(Duration.ofSeconds(5))
                     .executor(clientExecutor)
                     .build();
             ExecutorService requestExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET().build();
            long start = System.nanoTime();
            for (int i = 0; i < totalRequests; i++) {
                inFlight.acquire(); // chặn khi đã có đủ "concurrency" request đang bay
                int index = i;
                requestExecutor.submit(() -> {
                    long sentAt = System.nanoTime();
                    try {
                        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                        if (response.statusCode() != 200) {
                            errors.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latenciesNanos[index] = System.nanoTime() - sentAt;
                        inFlight.release();
                    }
                });
            }
            inFlight.acquire(concurrency); // chờ mọi request xong
            long elapsedNanos = System.nanoTime() - start;

            // Mỗi task ghi một ô riêng; acquire ở trên happens-after mọi release -> đọc mảng an toàn.
            long[] sorted = latenciesNanos.clone();
            Arrays.sort(sorted);
            double elapsedMs = elapsedNanos / 1_000_000.0;
            return new Result(totalRequests, errors.get(), (long) elapsedMs, totalRequests / (elapsedMs / 1000),
                    percentileMs(sorted, 0.50), percentileMs(sorted, 0.99), sorted[sorted.length - 1] / 1_000_000.0);
        }
    }

    private static double percentileMs(long[] sorted, double percentile) {
        int index = (int) Math.ceil(percentile * sorted.length) - 1;
        return sorted[Math.max(0, index)] / 1_000_000.0;
    }

    public static void main(String[] args) throws InterruptedException {
        URI uri = URI.create(args.length > 0 ? args[0] : "http://localhost:8080/api/payment");
        int concurrency = args.length > 1 ? Integer.parseInt(args[1]) : 500;
        int totalRequests = args.length > 2 ? Integer.parseInt(args[2]) : 10_000;
        System.out.printf("Bắn %,d request tới %s, concurrency = %d%n", totalRequests, uri, concurrency);
        System.out.println(run(uri, concurrency, totalRequests));
    }
}
