# 04 – Locking: deadlock, ReentrantLock, ReadWriteLock

## 1. Deadlock

```
   Thread 1                         Thread 2
   giữ  [Lock A] ──── chờ ───►  [Lock B]  giữ
          ▲                          │
          └────────── chờ ───────────┘          => không ai đi tiếp được
```

Bốn điều kiện Coffman (đủ cả 4 mới deadlock):
1. **Mutual exclusion** – tài nguyên chỉ một thread giữ.
2. **Hold and wait** – giữ một lock trong khi chờ lock khác.
3. **No preemption** – không ai cướp được lock đang giữ.
4. **Circular wait** – vòng chờ.

Phòng tránh:
- **Lock ordering** (phá circular wait): luôn khoá theo thứ tự toàn cục (id tăng dần). Đơn giản, hiệu quả nhất.
- **tryLock(timeout)** (phá hold-and-wait): không lấy được thì thả hết, backoff, thử lại.
- Giảm phạm vi khoá, không gọi code lạ (callback, listener, method có thể bị override) khi đang giữ lock.

Phát hiện: `jstack <pid>` / thread dump → "Found one Java-level deadlock", hoặc `ThreadMXBean.findDeadlockedThreads()`.

## 2. ReentrantLock so với synchronized

| | `synchronized` | `ReentrantLock` |
|---|---|---|
| Cú pháp | Khối, tự unlock khi ra khỏi khối | `lock()` / `unlock()` trong `finally` – dễ quên |
| `tryLock()` / timeout | Không | Có |
| Ngắt khi đang chờ | Không | `lockInterruptibly()` |
| Fairness | Không | `new ReentrantLock(true)` (chậm hơn) |
| Nhiều condition | Một wait-set | Nhiều `Condition` (m05) |
| Giám sát | Hạn chế | `getQueueLength()`, `isLocked()`, `getHoldCount()` |
| Virtual threads (Java 21) | **Pin** carrier khi block bên trong | Không pin (m07) – đã sửa ở JDK 24 (JEP 491) |

Mẫu bắt buộc:
```java
lock.lock();
try {
    // critical section
} finally {
    lock.unlock();
}
```

## 3. ReentrantReadWriteLock

```
 readLock : R R R R  (nhiều reader cùng lúc)
 writeLock:          W (độc quyền: không reader, không writer khác)
```
- Hợp khi **đọc nhiều, ghi ít, đoạn đọc đủ dài**.
- Không thể nâng cấp read → write (sẽ tự deadlock). Hạ cấp write → read thì được.
- Writer có thể bị "đói" nếu reader liên tục (chế độ non-fair).
- Đọc cực ngắn → overhead RW lock lớn hơn lợi ích; cân nhắc `StampedLock` (optimistic read) hoặc cấu trúc bất biến + `volatile`.

## Bẫy hay gặp

1. `lock()` đặt **trong** `try` → nếu `lock()` ném exception thì `finally` gọi `unlock()` khi chưa giữ → `IllegalMonitorStateException`.
2. Quên `unlock()` ở một nhánh return/exception → lock bị giữ vĩnh viễn.
3. Lock ordering dựa trên thứ tự **tham số** (from, to) thay vì thứ tự **toàn cục** (id) → vẫn deadlock.
4. Gọi method của object khác (có lock riêng) khi đang giữ lock → deadlock "ẩn" khó thấy (lock ordering vỡ qua nhiều lớp).
5. tryLock + retry không có backoff ngẫu nhiên → livelock.
6. Nghĩ fair lock giải quyết deadlock — không, fairness chỉ ảnh hưởng thứ tự cấp lock.
7. Deadlock DB (InnoDB) cũng theo đúng logic này: update/`SELECT FOR UPDATE` nhiều dòng theo thứ tự khác nhau giữa các transaction.

## Câu hỏi phỏng vấn

**1. Deadlock là gì và cách phòng tránh?**
<details><summary>Đáp án</summary>

Hai (hoặc nhiều) thread chờ lẫn nhau vĩnh viễn vì mỗi bên giữ tài nguyên bên kia cần. Cần đủ 4 điều kiện Coffman. Phòng tránh bằng cách phá một điều kiện: lock ordering (circular wait), tryLock có timeout (hold and wait), giảm số lock giữ cùng lúc. Phát hiện bằng thread dump / ThreadMXBean.
</details>

**2. Khi nào dùng `ReentrantLock` thay vì `synchronized`?**
<details><summary>Đáp án</summary>

Khi cần: tryLock / timeout, ngắt được khi chờ, fairness, nhiều Condition, giám sát trạng thái lock; và trên Java 21–23 với virtual threads để tránh pinning khi block trong critical section. Còn lại `synchronized` gọn hơn và an toàn hơn (không thể quên unlock).
</details>

**3. "Reentrant" nghĩa là gì? Vì sao cần?**
<details><summary>Đáp án</summary>

Thread đang giữ lock có thể lấy lại lock đó (hold count tăng) mà không tự chặn mình. Cần vì method synchronized thường gọi method synchronized khác của cùng object, hoặc subclass gọi `super.method()` đều synchronized trên `this`.
</details>

**4. ReadWriteLock khi nào nhanh hơn, khi nào chậm hơn?**
<details><summary>Đáp án</summary>

Nhanh hơn khi tỉ lệ đọc rất cao và thời gian giữ lock đọc đủ dài để song song hoá có ý nghĩa. Chậm hơn khi ghi nhiều, hoặc đoạn đọc quá ngắn (overhead cập nhật bộ đếm reader trên một biến chung gây tranh chấp cache line).
</details>

**5. Livelock và starvation khác deadlock thế nào?**
<details><summary>Đáp án</summary>

Deadlock: các thread bị chặn, không chạy. Livelock: các thread vẫn chạy nhưng liên tục phản ứng lẫn nhau (cùng nhường, cùng thử lại) nên không tiến triển — sửa bằng backoff ngẫu nhiên. Starvation: một thread không bao giờ được cấp tài nguyên vì thread khác luôn được ưu tiên (ví dụ writer bị reader áp đảo) — sửa bằng fairness.
</details>
