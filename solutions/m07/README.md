# Lời giải m07 – PaymentFanOut

## Ý tưởng
1. Tạo `Executors.newVirtualThreadPerTaskExecutor()` cho mỗi lần `evaluate` (virtual thread rẻ, không cần pool).
2. `submit` cả 3 lời gọi **trước**, rồi mới `get` → 3 lời gọi chạy song song.
3. Một `deadline` chung; mỗi `future.get(remaining)` chỉ chờ phần còn lại → tổng chờ ≤ timeout.
4. `finally`: `cancel(true)` các future (interrupt virtual thread đang block) và `shutdownNow()`.

## Vì sao đúng
- **Latency ≈ max**, không phải tổng: cả 3 task đã chạy từ lúc submit; chờ future thứ nhất 300ms thì 2 future kia
  (200ms, 100ms) cũng đã xong, `get` của chúng trả về ngay.
- **Timeout tổng**: nếu chờ `timeout` cho từng future thì tệ nhất là 3 × timeout.
- **Không rò thread**: `cancel(true)` interrupt virtual thread; `Thread.sleep` / IO blocking trên virtual thread phản hồi interrupt.
  Không gọi `close()` (try-with-resources) trước khi huỷ, vì `close()` **chờ** mọi task kết thúc.
- `ExecutionException` (service ném lỗi) → `SERVICE_ERROR`. Interrupt người gọi → khôi phục cờ.
- Thứ tự ưu tiên quyết định chỉ xét khi đã có đủ 3 kết quả.

## Cải tiến (thảo luận phỏng vấn)
- **Fail fast**: hiện tại nếu fraud chậm 300ms mà balance lỗi ngay ở 10ms, ta vẫn chờ fraud trước. Có thể dùng
  `ExecutorCompletionService` hoặc `CompletableFuture.anyOf` để phản ứng với kết quả/lỗi đầu tiên.
- Java 21 có `StructuredTaskScope.ShutdownOnFailure` (preview, cần `--enable-preview`): tự huỷ các subtask còn lại khi
  một subtask lỗi, và không bao giờ rò thread ra khỏi scope.
- `CompletableFuture.supplyAsync(..., virtualExecutor).orTimeout(...)` cũng là một cách, nhưng `orTimeout` **không**
  interrupt task đang chạy.
