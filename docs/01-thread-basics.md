# 01 – Thread basics

## 1. Process và Thread

```
┌──────────────────────── Process (JVM) ────────────────────────┐
│  Heap (dùng chung): objects, static fields, class metadata     │
│                                                                │
│  ┌── Thread main ──┐  ┌── Thread A ──┐  ┌── Thread B ──┐       │
│  │ Stack riêng     │  │ Stack riêng  │  │ Stack riêng  │       │
│  │ PC riêng        │  │ PC riêng     │  │ PC riêng     │       │
│  └─────────────────┘  └──────────────┘  └──────────────┘       │
└────────────────────────────────────────────────────────────────┘
```

- **Process**: có không gian bộ nhớ riêng, tài nguyên OS riêng (file, socket).
- **Thread**: đơn vị lập lịch của OS bên trong process. Các thread **chia sẻ heap**, mỗi thread có **stack** riêng.
- Platform thread của Java = 1:1 với thread của OS → tạo tốn kém (~1MB stack mặc định, syscall), context switch tốn kém.

## 2. Tạo thread

| Cách | Ví dụ | Ghi chú |
|------|-------|---------|
| Kế thừa `Thread` | `class T extends Thread { run() {...} }` | Ít dùng: trộn task với cơ chế chạy |
| `Runnable` | `new Thread(task, "name")` | Tách task khỏi thread |
| Lambda | `new Thread(() -> ..., "name")` | Phổ biến nhất |
| Builder (Java 21) | `Thread.ofPlatform().name("w-", 0).start(task)` | Gọn, xem thêm m07 |

**Luôn đặt tên thread** — khi đọc thread dump / log production, `Thread-17` vô dụng, `payment-worker-3` cực kỳ hữu ích.

## 3. Vòng đời

```
 NEW ──start()──► RUNNABLE ◄──────────────┐
                    │  ▲                   │
   synchronized     │  │ lấy được lock      │ hết sleep / được notify / unpark
   bị chặn          ▼  │                   │
                 BLOCKED          WAITING / TIMED_WAITING
                    (sleep, wait, join, LockSupport.park)
                    │
                 run() kết thúc ──► TERMINATED
```

## 4. Priority, UncaughtExceptionHandler, daemon

- `setPriority(1..10)`: chỉ là **gợi ý** cho OS; trên Linux mặc định gần như bị bỏ qua. Đừng dựa vào nó để đúng logic.
- Exception thoát khỏi `run()` **không** lan sang thread cha. Nó chỉ tới `UncaughtExceptionHandler`
  (của thread → của ThreadGroup → default handler → in stacktrace ra stderr).
- **Daemon thread**: JVM thoát khi chỉ còn daemon thread. Daemon bị bỏ ngang — **finally không được chạy**.
  Dùng cho việc nền không quan trọng (heartbeat, cache cleanup), không dùng cho việc ghi dữ liệu.

## 5. Interrupt – cơ chế dừng thread hợp tác

`thread.interrupt()` chỉ **bật một cờ**. Tuỳ thread đang làm gì:

| Thread đang... | Kết quả |
|----------------|---------|
| `sleep`, `wait`, `join`, `BlockingQueue.take`, `lockInterruptibly` | Ném `InterruptedException` **và xoá cờ** |
| Tính toán CPU | Không có gì xảy ra — phải tự kiểm tra `Thread.currentThread().isInterrupted()` |
| Chờ `synchronized` / IO cổ điển (`InputStream.read` trên socket) | Không bị ảnh hưởng |

Quy tắc khi bắt `InterruptedException` mà không ném lại được:
```java
catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // khôi phục cờ để code phía trên biết
    return;
}
```

## 6. join

- `join()` chờ thread kết thúc. **Mọi thao tác của thread đó happens-before việc join() trả về** → đọc kết quả an toàn.
- `join(ms)` chỉ là chờ có giới hạn; trả về rồi thread **vẫn có thể còn chạy** → kiểm tra `isAlive()` và interrupt nếu cần.

## Bẫy hay gặp

1. Gọi `run()` thay vì `start()` → code chạy trên thread hiện tại, không có song song.
2. Nuốt `InterruptedException` (`catch (InterruptedException e) {}`) → mất tín hiệu huỷ, service không shutdown được.
3. `interrupt()` không dừng vòng lặp CPU nếu không kiểm tra cờ.
4. `Thread.interrupted()` (static) **xoá cờ** sau khi đọc; `isInterrupted()` thì không.
5. `setDaemon(true)` sau `start()` → `IllegalThreadStateException`.
6. Gọi `start()` hai lần trên cùng một Thread → `IllegalThreadStateException`.
7. Exception trong worker "biến mất" lặng lẽ nếu không có handler/log (đặc biệt trong `ExecutorService.submit` — exception nằm trong `Future`).

## Câu hỏi phỏng vấn

**1. Khác nhau giữa `start()` và `run()`?**
<details><summary>Đáp án</summary>

`start()` yêu cầu JVM tạo thread OS mới rồi gọi `run()` trên thread đó. Gọi `run()` trực tiếp chỉ là một lời gọi method bình thường trên thread hiện tại. `start()` chỉ gọi được một lần.
</details>

**2. Làm sao dừng một thread đúng cách? Vì sao `Thread.stop()` bị deprecate?**
<details><summary>Đáp án</summary>

Dùng interrupt (hợp tác): gọi `interrupt()`, thread mục tiêu kiểm tra cờ hoặc xử lý `InterruptedException` rồi tự dọn dẹp và thoát. Có thể kết hợp cờ `volatile boolean running`. `stop()` ném `ThreadDeath` ở vị trí bất kỳ, nhả hết monitor đang giữ → object có thể bị để ở trạng thái nửa vời (vi phạm invariant), nên bị deprecate (và đã bị gỡ hẳn ở các JDK mới).
</details>

**3. Khi bắt `InterruptedException` nên làm gì?**
<details><summary>Đáp án</summary>

Hoặc ném lại (khai báo `throws InterruptedException`), hoặc nếu không thể (ví dụ trong `Runnable.run`) thì gọi `Thread.currentThread().interrupt()` để khôi phục cờ, rồi kết thúc công việc sớm. Không bao giờ nuốt im lặng.
</details>

**4. Daemon thread là gì, dùng khi nào, có rủi ro gì?**
<details><summary>Đáp án</summary>

Thread không giữ JVM sống; JVM thoát khi chỉ còn daemon. Dùng cho tác vụ nền "best effort". Rủi ro: bị cắt ngang bất kỳ lúc nào, `finally` không chạy → không dùng để ghi file/DB hay commit giao dịch.
</details>

**5. Exception ném ra trong một thread con thì main thread có bắt được không?**
<details><summary>Đáp án</summary>

Không bằng try/catch quanh `start()`. Exception kết thúc thread đó và đi tới `UncaughtExceptionHandler`. Với `ExecutorService.submit`, exception được gói trong `Future` và chỉ lộ ra khi gọi `future.get()` (dưới dạng `ExecutionException`); với `execute()` thì đi tới handler của thread trong pool.
</details>
