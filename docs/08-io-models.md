# 08 – IO models: thread-per-request, thread pool, virtual threads

## 1. Blocking IO và thread-per-request

```
 Client ──► [accept] ──► Thread #k xử lý request:
                           đọc request
                           gọi DB  ─────────────── (block 50ms, thread NGỒI CHỜ) ──┐
                           gọi đối tác ─────────── (block 200ms)                  │
                           ghi response  ◄──────────────────────────────────────┘
```
- Dễ viết, dễ debug, stack trace rõ ràng.
- Mỗi request đang chờ IO giữ nguyên một thread.

## 2. Định luật Little

```
 Số request đồng thời (L) = Throughput (λ) × Latency (W)

 Muốn 2.000 req/s, mỗi request 50ms  ->  cần 2.000 × 0,05 = 100 request đồng thời -> 100 thread
 Muốn 20.000 req/s, mỗi request 200ms -> cần 4.000 thread  (platform thread: quá đắt)
```
Với platform thread, `throughput ≤ số_thread / latency`. Đây là lý do một pool cố định trở thành nút cổ chai,
dù CPU đang rảnh.

## 3. Các mô hình

| Mô hình | Cách làm | Ưu | Nhược |
|---|---|---|---|
| Thread-per-request, platform pool | Tomcat (200 thread mặc định) | Đơn giản | Giới hạn bởi số thread, RAM |
| Non-blocking / event loop | Netty, WebFlux, Node.js: ít thread, IO async, callback/reactive | Scale rất tốt | Code khó, stack trace khó đọc, không được block event loop |
| Thread-per-request, virtual threads | Mỗi request một VT, code blocking bình thường | Đơn giản **và** scale | Pinning (Java 21), cần chủ động giới hạn tài nguyên phía sau |

```
 Platform pool (N=100):   [100 thread bận chờ IO] + queue: ■■■■■■■■■■■■ (request xếp hàng -> latency tăng)
 Virtual threads:          [5.000 VT chờ IO trên ~20 carrier] -> không xếp hàng, CPU vẫn rảnh
```

## 4. Khi thread không còn là giới hạn, giới hạn chuyển đi đâu?

- **Connection pool DB** (Hikari 10–50 connection): request chờ `getConnection()` → timeout.
- **Rate limit đối tác** (ngân hàng, ví điện tử).
- **CPU** (serialize JSON, mã hoá TLS, ký số).
- **Bộ nhớ** (mỗi request giữ buffer, object).

→ Cần **bulkhead** (Semaphore), timeout ở mọi lời gọi ra ngoài, load shedding (trả 503/429 sớm).

## 5. Đọc kết quả `IoModelBenchmarkDemo`

- Platform pool 100, mỗi request 50ms → trần ≈ 2.000 req/s; p50 cao vì request chờ trong queue của executor.
- Virtual threads → throughput ≈ `concurrency / 50ms` cho tới khi đụng giới hạn của dispatcher thread trong `HttpServer` / CPU.
- Thử thay đổi: `PLATFORM_POOL_SIZE`, `CONCURRENCY`, độ trễ giả lập; chạy server và client ở hai terminal để thấy rõ hơn.

## Bẫy hay gặp

1. Tăng pool thread lên vài nghìn thay vì sửa mô hình → RAM, context switch, GC root tăng.
2. Block trong event loop (Netty/WebFlux) → toàn bộ server đứng.
3. Chuyển sang virtual thread nhưng giữ `synchronized` quanh IO (Java 21) → pinning.
4. Không đặt timeout cho HTTP client / JDBC → request treo giữ tài nguyên mãi.
5. Benchmark client và server cùng máy, client trở thành nút cổ chai mà không biết (theo dõi CPU của cả hai).
6. Chỉ nhìn throughput trung bình, không nhìn p99 và tỉ lệ lỗi.

## Câu hỏi phỏng vấn

**1. Vì sao CPU còn rảnh mà server thread-per-request đã không nhận thêm được request?**
<details><summary>Đáp án</summary>

Vì mọi thread trong pool đang block chờ IO (DB, HTTP). Request mới xếp hàng trong queue của executor (hoặc bị từ chối khi `acceptCount` đầy). Giới hạn là số thread, không phải CPU — theo Little's law, throughput ≤ số_thread / latency.
</details>

**2. Định luật Little áp dụng thế nào khi chọn kích thước pool?**
<details><summary>Đáp án</summary>

`L = λ × W`: số request đồng thời cần = throughput mục tiêu × latency trung bình. Ví dụ 1.000 req/s × 0,2s = 200 thread/connection. Áp dụng cả cho DB connection pool: nếu mỗi query 10ms và cần 2.000 query/s thì cần ≈ 20 connection.
</details>

**3. So sánh WebFlux (reactive) và Spring MVC + virtual threads.**
<details><summary>Đáp án</summary>

Cả hai giải quyết vấn đề thread bị block khi chờ IO. WebFlux: event loop + non-blocking IO, mọi thư viện phải reactive (R2DBC, WebClient), code dạng pipeline, debug khó; mạnh ở streaming và back-pressure. MVC + VT: giữ code blocking quen thuộc (JPA/JDBC, RestClient), stack trace bình thường; cần lưu ý pinning (Java 21) và giới hạn concurrency xuống tài nguyên phía sau.
</details>

**4. Server chuyển sang virtual thread xong, throughput tăng nhưng DB bắt đầu lỗi `Connection is not available, request timed out`. Vì sao và xử lý ra sao?**
<details><summary>Đáp án</summary>

Trước đây Tomcat pool 200 thread vô tình giới hạn số request đồng thời chạm tới DB. Với VT, hàng nghìn request cùng chờ `getConnection()` trên pool 20 connection → timeout. Xử lý: bulkhead/semaphore trước tầng DB và trả lỗi sớm (503/429), tối ưu query để giảm thời gian giữ connection, cân nhắc tăng pool trong khả năng của DB, đặt timeout hợp lý, cache.
</details>

**5. `HttpServer` của JDK trong demo xử lý request theo mô hình nào?**
<details><summary>Đáp án</summary>

Một dispatcher thread dùng NIO selector để accept và đọc request; mỗi request (`HttpExchange`) được giao cho `Executor` đã cấu hình để chạy handler. Handler code là blocking. Đổi executor từ fixed pool sang `newVirtualThreadPerTaskExecutor()` là chuyển từ "thread pool" sang "virtual thread per request" mà không đổi dòng code xử lý nào.
</details>
