# Lời giải m08 – Bulkhead

## Lời giải
- `Semaphore(maxConcurrentCalls, fair = true)`.
- `tryAcquire(maxWait)`: thành công → chạy `call` trong `try/finally { release() }`; thất bại → `BulkheadFullException`.
- `tryAcquire` ném `InterruptedException` khi bị interrupt trong lúc chờ → khai báo `throws Exception` nên ném thẳng ra.

## Vì sao đúng
- Số lời gọi đang chạy = số permit đang bị giữ ≤ `maxConcurrentCalls` (bất biến của semaphore).
- **Chỉ release khi đã acquire**: `release()` nằm trong `finally` của khối **sau** khi acquire thành công.
  Nếu đặt `release()` trong `finally` bao cả `tryAcquire`, thì nhánh bị từ chối / bị interrupt sẽ release
  một permit không hề lấy → semaphore "phình" vượt giới hạn (test `interruptWhileWaiting` bắt lỗi này).
- Exception của `call` không bị bọc lại → người gọi xử lý đúng loại lỗi.

## Liên hệ thực tế
- Virtual threads + Bulkhead = thay thế "giới hạn bằng thread pool". Tomcat 200 thread trước đây vô tình là một bulkhead;
  bật `spring.threads.virtual.enabled=true` là mất giới hạn đó → cần bulkhead/connection pool chủ động.
- Resilience4j có `SemaphoreBulkhead` (đúng mô hình này) và `ThreadPoolBulkhead`.
- Kết hợp với server m08: bọc lời gọi DB giả lập trong `Bulkhead(20, 100ms)`, trả HTTP 503 khi `BulkheadFullException`
  → server từ chối nhanh thay vì để latency phình to (load shedding).
