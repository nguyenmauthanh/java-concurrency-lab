package vn.thanh.lab.m08;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * HTTP server mô hình THREAD-PER-REQUEST dùng {@link HttpServer} của JDK.
 * Mỗi request được xử lý trọn vẹn trên một thread, và code xử lý BLOCK (giả lập gọi DB 50ms).
 * <p>
 * Cờ {@code mode} chọn executor:
 * <ul>
 *     <li>{@code platform}: fixed pool platform thread (giống Tomcat mặc định 200 thread)</li>
 *     <li>{@code virtual}: mỗi request một virtual thread</li>
 * </ul>
 * Chạy: {@code -Dexec.args="<platform|virtual> <port> <số giây chạy> [pool size]"}, ví dụ {@code "virtual 8080 120"}.
 * Server tự dừng sau số giây chỉ định.
 */
public class BlockingHttpServer implements AutoCloseable {

    static final long SIMULATED_DB_LATENCY_MS = 50;

    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicLong handledRequests = new AtomicLong();

    public BlockingHttpServer(String mode, int port, int platformPoolSize) throws IOException {
        executor = switch (mode) {
            case "platform" -> Executors.newFixedThreadPool(platformPoolSize);
            case "virtual" -> Executors.newVirtualThreadPerTaskExecutor();
            default -> throw new IllegalArgumentException("mode phải là platform hoặc virtual: " + mode);
        };
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 4_096);
        server.createContext("/api/payment", this::handlePayment);
        server.createContext("/health", exchange -> respond(exchange, 200, "{\"status\":\"UP\"}"));
        // Thread "dispatcher" của HttpServer chỉ accept + đọc request; xử lý được giao cho executor này.
        server.setExecutor(executor);
    }

    public BlockingHttpServer start() {
        server.start();
        return this;
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public long handledRequests() {
        return handledRequests.get();
    }

    private void handlePayment(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(SIMULATED_DB_LATENCY_MS); // giả lập SELECT ... FOR UPDATE + UPDATE
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            respond(exchange, 503, "{\"error\":\"interrupted\"}");
            return;
        }
        handledRequests.incrementAndGet();
        Thread current = Thread.currentThread();
        respond(exchange, 200, "{\"status\":\"OK\",\"virtual\":" + current.isVirtual() + ",\"thread\":\"" + current + "\"}");
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        String mode = args.length > 0 ? args[0] : "virtual";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        int seconds = args.length > 2 ? Integer.parseInt(args[2]) : 60;
        int poolSize = args.length > 3 ? Integer.parseInt(args[3]) : 200;

        try (BlockingHttpServer server = new BlockingHttpServer(mode, port, poolSize).start()) {
            System.out.printf("Server [%s%s] đang chạy tại http://localhost:%d/api/payment, tự dừng sau %d giây%n",
                    mode, mode.equals("platform") ? ", pool=" + poolSize : "", server.port(), seconds);
            System.out.println("Thử: curl http://localhost:" + server.port() + "/api/payment");
            Thread.sleep(TimeUnit.SECONDS.toMillis(seconds));
            System.out.println("Hết giờ. Đã xử lý " + server.handledRequests() + " request.");
        }
    }
}
