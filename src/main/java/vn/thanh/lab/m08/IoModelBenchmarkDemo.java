package vn.thanh.lab.m08;

import java.net.URI;

/**
 * Benchmark trọn gói: lần lượt bật {@link BlockingHttpServer} ở chế độ platform pool và virtual threads
 * (cổng ngẫu nhiên), bắn tải bằng {@link LoadGenerator} và in bảng so sánh.
 * <p>
 * Mỗi request phía server block 50ms. Lý thuyết:
 * <ul>
 *     <li>Pool N platform thread: throughput tối đa ≈ N / 0,05s (N=100 -> ~2.000 req/s).</li>
 *     <li>Virtual threads: throughput ≈ concurrency / 0,05s, tới khi đụng giới hạn CPU / dispatcher của HttpServer.</li>
 * </ul>
 */
public class IoModelBenchmarkDemo {

    private static final int PLATFORM_POOL_SIZE = 100;
    private static final int CONCURRENCY = 500;
    private static final int REQUESTS = 6_000;

    public static void main(String[] args) throws Exception {
        System.out.printf("Mỗi request server block %dms. Client: concurrency = %d, tổng %,d request.%n%n",
                BlockingHttpServer.SIMULATED_DB_LATENCY_MS, CONCURRENCY, REQUESTS);

        LoadGenerator.Result platform = benchmark("platform", PLATFORM_POOL_SIZE);
        LoadGenerator.Result virtual = benchmark("virtual", PLATFORM_POOL_SIZE);

        System.out.println("\n=== Kết quả ===");
        System.out.printf("%-26s %s%n", "platform (pool " + PLATFORM_POOL_SIZE + ")", platform);
        System.out.printf("%-26s %s%n", "virtual threads", virtual);
        System.out.printf("%nVirtual threads cho throughput gấp %.1f lần. Vì sao p50 của platform lại cao?%n",
                virtual.throughput() / platform.throughput());
        System.out.println("-> Request phải XẾP HÀNG chờ một trong " + PLATFORM_POOL_SIZE
                + " thread rảnh: latency = thời gian chờ trong queue + 50ms xử lý.");
    }

    private static LoadGenerator.Result benchmark(String mode, int poolSize) throws Exception {
        try (BlockingHttpServer server = new BlockingHttpServer(mode, 0, poolSize).start()) {
            URI uri = URI.create("http://127.0.0.1:" + server.port() + "/api/payment");
            System.out.println("[" + mode + "] warm-up...");
            LoadGenerator.run(uri, 100, 1_000);
            System.out.println("[" + mode + "] đo...");
            LoadGenerator.Result result = LoadGenerator.run(uri, CONCURRENCY, REQUESTS);
            System.out.println("[" + mode + "] " + result);
            return result;
        }
    }
}
