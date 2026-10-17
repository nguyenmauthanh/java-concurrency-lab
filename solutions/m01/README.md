# Lời giải m01 – ParallelFactorial

## Ý tưởng
1. Validate input **trước** khi tạo thread (fail fast, không để thread mồ côi).
2. Mỗi số một `Thread` daemon; worker chỉ ghi vào ô `results[i]` của chính nó.
3. Tính một `deadline` chung; mỗi lần `join` chỉ chờ phần thời gian **còn lại** → tổng thời gian chờ ≤ timeout.
4. Sau vòng join: thread nào còn `isAlive()` → `interrupt()` và trả `null`.
5. Vòng lặp giai thừa kiểm tra `isInterrupted()` mỗi bước → worker dừng ngay, không ngốn CPU.

## Vì sao đúng
- **Không có data race trên mảng**: mỗi ô chỉ một thread ghi. Thread gọi chỉ đọc ô `i` khi worker `i` **đã kết thúc**
  (`!isAlive()`). Theo JMM, mọi hành động của một thread *happens-before* việc thread khác phát hiện nó đã kết thúc
  (`join()` trả về hoặc `isAlive()` trả `false`) → giá trị đọc được chắc chắn là giá trị đã ghi.
- Nếu worker bị interrupt đúng lúc vừa ghi xong thì vẫn ổn: ta chỉ đọc ô khi thread đã chết; còn sống thì trả `null`.
- **Timeout tổng**: `join(remaining)` với `remaining = deadline - now`. Nếu join riêng `timeout` cho từng thread
  thì N thread chậm sẽ chờ N × timeout — test `timeoutIsTotalNotPerThread` bắt lỗi này.
- **Interrupt người gọi**: interrupt hết worker, khôi phục cờ (`Thread.currentThread().interrupt()`) rồi ném lại,
  để code phía trên biết mà xử lý.

## Bẫy hay gặp
- Dùng `ArrayList.add` từ nhiều worker → race condition, mất phần tử/`ArrayIndexOutOfBounds`.
- Quên kiểm tra cờ interrupt trong vòng lặp tính toán: `interrupt()` không "giết" thread đang chạy CPU.
- Dùng `List.of(...)` để trả kết quả → ném `NullPointerException` vì `List.of` không chứa `null`.
