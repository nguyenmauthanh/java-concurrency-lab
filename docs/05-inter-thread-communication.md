# 05 – Inter-thread communication: Semaphore, wait/notify, Condition, producer–consumer

## 1. Semaphore

```
 Semaphore(3):   permits  ● ● ●
 acquire() -> lấy 1 permit (hết thì chờ)        release() -> trả 1 permit (đánh thức thread đang chờ)

 T1 ●  T2 ●  T3 ●  | T4 chờ... T5 chờ...   -> tối đa 3 thread trong vùng được bảo vệ
```
- Giới hạn số truy cập đồng thời: connection pool, rate limit gọi đối tác, bulkhead (m08).
- Không có khái niệm "chủ sở hữu": thread A acquire, thread B release được → dùng làm cơ chế báo hiệu (barrier, m05b).
- `tryAcquire(timeout)` để không chờ vô hạn. Fair semaphore tránh starvation.
- Semaphore với 1 permit ≈ mutex nhưng **không reentrant**.

## 2. wait / notify / notifyAll

```java
synchronized (lock) {
    while (!condition) {   // LUÔN là while
        lock.wait();       // nhả monitor + ngủ; tỉnh dậy thì lấy lại monitor
    }
    // hành động khi điều kiện đúng
}

synchronized (lock) {
    condition = true;
    lock.notifyAll();      // đánh thức mọi thread trong wait-set
}
```
- Phải gọi khi đang giữ monitor của chính object đó → nếu không: `IllegalMonitorStateException`.
- `notify()` đánh thức **một** thread bất kỳ; nếu wait-set lẫn nhiều loại điều kiện (producer và consumer) → dễ đánh thức nhầm → treo.

## 3. Condition (ReentrantLock)

```
 ReentrantLock ──┬── Condition notFull   (producer chờ)
                 └── Condition notEmpty  (consumer chờ)
```
- `await()` / `signal()` / `signalAll()` tương ứng `wait` / `notify` / `notifyAll`.
- Nhiều condition trên một lock → đánh thức đúng loại thread cần → có thể dùng `signal()` an toàn.
- `awaitNanos`, `await(time, unit)`, `awaitUntil(deadline)`: chờ có giới hạn.

## 4. Producer – consumer

```
 producer ──put──►  [ ][ ][ ][ ][ ]  ──take──► consumer
                    bounded queue
 đầy  -> producer bị chặn (back-pressure)
 rỗng -> consumer bị chặn
```
- Thực tế dùng `ArrayBlockingQueue` / `LinkedBlockingQueue` (có giới hạn!).
- Dừng consumer: **poison pill** (mỗi consumer một pill) hoặc interrupt.
- Ứng dụng: Kafka listener đẩy vào queue nội bộ cho worker pool; batch insert DB; async audit log.

## 5. Các công cụ đồng bộ khác (nên biết)

| Công cụ | Mục đích |
|---|---|
| `CountDownLatch` | Chờ N sự kiện xảy ra, dùng một lần |
| `CyclicBarrier` | N thread chờ nhau, dùng lại được, có barrier action |
| `Phaser` | Barrier linh hoạt, số party thay đổi |
| `Exchanger` | Hai thread trao đổi object |
| `CompletableFuture` | Kết hợp kết quả bất đồng bộ |

## Bẫy hay gặp

1. Dùng `if` thay vì `while` quanh `wait()`/`await()` → spurious wakeup hoặc thread khác lấy mất trước.
2. `notify()` thay vì `notifyAll()` khi wait-set có nhiều loại điều kiện → lost wakeup, treo.
3. Gọi `wait()` ngoài `synchronized` → `IllegalMonitorStateException`.
4. Quên `release()` trong `finally` → semaphore "rò" permit, cuối cùng mọi thread bị chặn (giống connection leak).
5. `release()` nhiều hơn `acquire()` → số permit vượt giới hạn mà không báo lỗi.
6. Queue không giới hạn → không có back-pressure, OOM khi consumer chậm.
7. Chờ (acquire/await) trong khi vẫn giữ một lock khác mà thread "người thả" cần → deadlock.

## Câu hỏi phỏng vấn

**1. Vì sao `wait()` phải nằm trong vòng `while`?**
<details><summary>Đáp án</summary>

Vì (1) spurious wakeup: thread có thể tỉnh mà không ai notify; (2) giữa lúc được notify và lúc lấy lại monitor, thread khác có thể đã thay đổi trạng thái (ví dụ consumer khác đã lấy mất phần tử); (3) `notifyAll` đánh thức cả những thread mà điều kiện của chúng vẫn chưa đúng. Phải kiểm tra lại điều kiện sau khi tỉnh.
</details>

**2. `Semaphore` khác `ReentrantLock` thế nào?**
<details><summary>Đáp án</summary>

Lock có chủ sở hữu (chỉ thread giữ mới unlock được), reentrant, cho đúng 1 thread. Semaphore có N permit, không có chủ sở hữu (thread khác release được), không reentrant. Semaphore dùng để giới hạn số truy cập đồng thời hoặc báo hiệu giữa các thread.
</details>

**3. `Condition` có lợi gì so với `wait/notify`?**
<details><summary>Đáp án</summary>

Nhiều condition trên một lock → các nhóm thread chờ ở hàng riêng, `signal` đúng nhóm, tránh đánh thức thừa; có các biến thể chờ có timeout/deadline, `awaitUninterruptibly`; đi cùng các tính năng của `ReentrantLock` (tryLock, fairness).
</details>

**4. `CountDownLatch` và `CyclicBarrier` khác nhau ra sao?**
<details><summary>Đáp án</summary>

`CountDownLatch`: một/nhiều thread chờ cho tới khi bộ đếm về 0; bộ đếm được giảm bởi bất kỳ thread nào (không cần chờ); dùng một lần. `CyclicBarrier`: N thread chờ **lẫn nhau** tại một điểm; tự reset để dùng lại; có barrier action; nếu một thread bị interrupt/timeout thì barrier "broken" cho tất cả.
</details>

**5. Làm sao dừng sạch một hệ thống producer–consumer?**
<details><summary>Đáp án</summary>

Dừng producer trước; sau đó gửi poison pill (mỗi consumer một cái) để consumer xử lý hết phần còn lại trong queue rồi thoát; hoặc dùng `ExecutorService.shutdown()` + `awaitTermination`, và `shutdownNow()` (interrupt) nếu quá thời hạn. Với Kafka: dừng poll, xử lý xong batch, commit offset rồi mới đóng.
</details>
