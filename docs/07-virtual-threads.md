# 07 – Virtual Threads (Java 21)

## 1. Ý tưởng

```
 Platform thread (1:1)                 Virtual thread (M:N)

  Java Thread ── OS thread             VT1 VT2 VT3 ... VT100000   (object trên heap, stack nhỏ, co giãn)
  Java Thread ── OS thread                 \   |   /
  Java Thread ── OS thread             carrier1 carrier2 ... carrierN   (ForkJoinPool, N ≈ số core)
                                            |        |
                                         OS thread OS thread
```
- Virtual thread là thread do JVM lập lịch, chạy "trên lưng" (mounted) một **carrier** platform thread.
- Khi virtual thread **block** (sleep, socket IO, `BlockingQueue.take`, `ReentrantLock`...), JVM **unmount** nó:
  lưu stack lên heap, trả carrier cho virtual thread khác. Xong IO thì mount lại (có thể trên carrier khác).
- Kết quả: viết code blocking đơn giản (thread-per-request) nhưng scale như code reactive.

## 2. API

```java
Thread.ofVirtual().name("vt-", 0).start(task);       // builder
Thread.startVirtualThread(task);                       // nhanh gọn
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {   // mỗi task một VT mới
    executor.submit(task);
}                                                      // close() chờ mọi task xong
Thread.currentThread().isVirtual();
```
Spring Boot 3.2+: `spring.threads.virtual.enabled=true` → Tomcat, `@Async`, `@Scheduled`, Kafka listener... chạy trên virtual thread.

## 3. Khi nào lợi / không lợi

| Có lợi | Không lợi |
|---|---|
| Nhiều task **IO-bound** đồng thời (HTTP, DB, Kafka produce đồng bộ) | Task **CPU-bound** (mã hoá, nén, tính toán) |
| Thread-per-request server với hàng nghìn request đang chờ | Ít task đồng thời (pool nhỏ đã đủ) |
| Fan-out gọi nhiều service song song | Code dựa vào `ThreadLocal` nặng (mỗi VT một bản → tốn RAM) |

Virtual thread **không làm một request nhanh hơn**; nó tăng **throughput** khi bị giới hạn bởi số thread.

## 4. Pinning (Java 21–23)

Virtual thread bị **pin** vào carrier (không unmount được) khi block trong:
- khối/method `synchronized` (đã sửa ở JDK 24 – JEP 491),
- native method / foreign function.

```
 synchronized (lock) { socket.read(); }   -> carrier bị giữ suốt thời gian chờ IO
 N carrier đều bị pin                     -> mọi VT khác đứng chờ dù CPU rảnh
```
Phát hiện: `-Djdk.tracePinnedThreads=full` (Java 21–23), JFR event `jdk.VirtualThreadPinned`.
Sửa: dùng `ReentrantLock` quanh đoạn có IO; nâng cấp thư viện (MySQL Connector/J 9.x, các driver mới đã bỏ synchronized quanh IO).

## 5. Những điều cần thay đổi trong tư duy

- **Đừng pool virtual thread**. Tạo mới cho mỗi task. Pool là để tái sử dụng tài nguyên đắt — VT không đắt.
- **Giới hạn concurrency bằng Semaphore**, không bằng kích thước pool: 10.000 VT cùng gọi DB có 20 connection → giới hạn
  bằng connection pool hoặc `Semaphore` (bulkhead, m08).
- `ThreadLocal` vẫn chạy nhưng cẩn thận với cache lớn trong `ThreadLocal`; xem `ScopedValue` (preview).
- Thread dump: `jcmd <pid> Thread.dump_to_file -format=json file.json` để thấy cả virtual thread.

## Bẫy hay gặp

1. Dùng virtual thread cho CPU-bound rồi thất vọng vì không nhanh hơn.
2. `synchronized` quanh IO (hoặc trong thư viện) trên Java 21 → pinning, throughput sập.
3. Bỏ giới hạn concurrency → dồn hàng nghìn request vào DB / đối tác → bị rate-limit, connection pool timeout.
4. `try (var ex = newVirtualThreadPerTaskExecutor())` + timeout: `close()` chờ task chậm → timeout vô dụng; phải cancel trước.
5. Tạo `newFixedThreadPool(n, Thread.ofVirtual().factory())` → pool virtual thread, mất ý nghĩa.
6. `parallelStream` / `CompletableFuture.supplyAsync()` không truyền executor vẫn chạy trên common pool, không phải VT.

## Câu hỏi phỏng vấn

**1. Virtual thread khác platform thread thế nào?**
<details><summary>Đáp án</summary>

Platform thread ánh xạ 1:1 với thread OS, stack cố định (~1MB), tạo và chuyển ngữ cảnh tốn kém, thực tế chỉ vài nghìn. Virtual thread do JVM quản lý, stack nằm trên heap và co giãn, ánh xạ M:N lên một nhóm carrier nhỏ; tạo được hàng triệu; khi block thì unmount nhường carrier. Cùng API `Thread`, luôn là daemon, priority cố định.
</details>

**2. Pinning là gì và xử lý ra sao?**
<details><summary>Đáp án</summary>

Virtual thread không thể unmount khỏi carrier khi đang block bên trong `synchronized` (Java 21–23) hoặc native frame, nên chiếm carrier suốt thời gian chờ. Nếu nhiều VT cùng bị pin, scheduler hết carrier → throughput giảm mạnh, thậm chí deadlock. Phát hiện bằng `-Djdk.tracePinnedThreads` / JFR; sửa bằng `ReentrantLock`, cập nhật driver, hoặc nâng JDK 24+.
</details>

**3. Có nên dùng thread pool cho virtual threads không?**
<details><summary>Đáp án</summary>

Không. Pool tồn tại để tái sử dụng tài nguyên đắt; virtual thread rẻ, nên tạo mỗi task một cái (`newVirtualThreadPerTaskExecutor`). Nếu mục đích của pool là giới hạn concurrency (bảo vệ DB/đối tác) thì thay bằng `Semaphore` hoặc giới hạn ở connection pool.
</details>

**4. Bật virtual thread cho ứng dụng Spring Boot thì cần kiểm tra gì?**
<details><summary>Đáp án</summary>

(1) Thư viện có `synchronized` quanh IO không (JDBC driver, HTTP client, logging appender) → pinning trên Java 21; (2) giới hạn concurrency xuống DB (Hikari pool size) vì số request đồng thời không còn bị giới hạn bởi Tomcat thread pool; (3) `ThreadLocal` lớn (MDC thì ổn, cache lớn thì không); (4) CPU-bound endpoint không lợi gì; (5) đo lại p99 và connection timeout dưới tải.
</details>

**5. Virtual thread có làm code reactive (WebFlux) lỗi thời không?**
<details><summary>Đáp án</summary>

Phần lớn use case "dùng reactive chỉ để scale IO" có thể chuyển về code blocking + virtual thread, dễ đọc và debug hơn (stack trace bình thường). Reactive vẫn có giá trị khi cần back-pressure streaming, xử lý luồng sự kiện, toán tử kết hợp phức tạp. Hai cách giải quyết cùng một vấn đề (thread bị block khi chờ IO) theo hai hướng khác nhau.
</details>
