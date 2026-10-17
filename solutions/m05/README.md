# Lời giải m05 – BoundedBuffer và SimpleBarrier

## BoundedBuffer
- **Một lock, hai condition**: producer chờ `notFull`, consumer chờ `notEmpty`. Nhờ tách hàng chờ,
  `signal()` (một thread) là đủ và đúng: sau `put` chỉ có thêm 1 phần tử → chỉ cần 1 consumer; producer
  không bao giờ bị đánh thức nhầm bởi producer khác.
  - So sánh: với `wait/notify` chỉ có **một** wait-set chung → phải dùng `notifyAll`, nếu dùng `notify` có thể đánh
    thức nhầm producer khi đáng ra cần consumer → mọi thread cùng ngủ (lost wakeup).
- **`while` thay vì `if`**: khi được đánh thức, thread phải lấy lại lock; lúc đó thread khác có thể đã lấy mất phần tử.
  Ngoài ra `await` có thể tỉnh giấc giả (spurious wakeup).
- **Mảng vòng**: `head`, `tail`, `count`; `items[head] = null` sau khi lấy để GC thu hồi.
- `lockInterruptibly()` + `await()` đều ném `InterruptedException` → huỷ được thread đang chờ.
- `size()` cũng khoá để có visibility (count không volatile).

## SimpleBarrier
- Biến đếm `arrived` được bảo vệ bởi lock; semaphore khởi tạo **0 permit**.
- N-1 thread đầu: tăng bộ đếm, **thả lock**, rồi `acquire()` → chặn.
- Thread thứ N: `release(N-1)` → thả đúng N-1 thread.
- Không phụ thuộc thứ tự: nếu thread cuối `release` trước khi một thread khác kịp `acquire`, permit vẫn còn đó,
  thread kia `acquire` sẽ đi qua ngay. Semaphore "nhớ" permit — khác với `notify` (đánh thức khi chưa ai chờ là mất).
- **Visibility**: `release()` happens-before `acquire()` thành công tương ứng; cộng với thứ tự trong lock → mọi ghi trước
  `await()` của mọi thread đều hiển thị sau khi qua barrier.
- Bẫy: gọi `gate.acquire()` khi vẫn đang giữ `lock` → thread cuối không vào được lock → deadlock.
- `tripped`: sau khi mở, các lần gọi sau trả về ngay (nếu không, chúng sẽ `acquire` mãi mãi vì không còn ai release).
