# 06 – Lock-free: CAS, Atomic, lock-free data structure

## 1. Vấn đề của lock

- Thread giữ lock bị OS deschedule (hết time slice, page fault) → mọi thread khác phải chờ.
- Deadlock, priority inversion.
- Chi phí chặn/đánh thức thread (syscall, context switch) khi tranh chấp.

## 2. Compare-And-Swap (CAS)

Một lệnh CPU nguyên tử (x86: `LOCK CMPXCHG`):

```
CAS(địa_chỉ, expected, newValue):
    nguyên tử {
        if (*địa_chỉ == expected) { *địa_chỉ = newValue; return true }
        else return false
    }
```

Mẫu vòng CAS:
```java
while (true) {
    int current = value.get();          // 1. đọc
    int next = compute(current);        // 2. tính (không side effect!)
    if (value.compareAndSet(current, next)) break;   // 3. ghi nếu chưa ai đổi
    // thất bại -> có thread khác thắng -> thử lại
}
```

```
Thread A: đọc 5 ──────────── CAS(5→6) ✔
Thread B:     đọc 5 ── CAS(5→6) ✘ (giá trị đã là 6) ── đọc 6 ── CAS(6→7) ✔
```

## 3. Các class atomic

| Class | Dùng cho |
|---|---|
| `AtomicInteger`, `AtomicLong`, `AtomicBoolean` | Counter, cờ, id generator |
| `AtomicReference<V>` | Thay nguyên tử tham chiếu tới object (thường là **bất biến**) |
| `AtomicStampedReference<V>` | Như trên + stamp chống ABA |
| `AtomicIntegerArray`, `AtomicLongArray` | Mảng mà từng phần tử cập nhật nguyên tử |
| `LongAdder`, `LongAccumulator` | Counter/metrics ghi rất nhiều, đọc ít (chia cell) |
| `VarHandle` | API cấp thấp: CAS/acquire/release trên field bất kỳ |

`updateAndGet(fn)` / `accumulateAndGet(x, fn)`: vòng CAS dựng sẵn — hàm có thể được gọi **nhiều lần**.

## 4. Lock-free stack (Treiber)

```
 head ──► [C] ──► [B] ──► [A] ──► null

 push(D): n = [D]──►(head hiện tại)    CAS(head, C, D)
 pop():   h = head (C)                 CAS(head, C, B)  -> trả C
```

## 5. ABA

```
T1: đọc head = A (next = B) ........................ CAS(head, A, B) ✔ ??? (B đã bị pop từ lâu!)
T2:            pop A, pop B, push A (tái dùng node A)
```
Giá trị "trông như cũ" nhưng trạng thái đã đổi. Java có GC + node mới mỗi lần push → không bị với Treiber stack chuẩn.
Khi tái sử dụng object hoặc so sánh giá trị (không phải tham chiếu): dùng `AtomicStampedReference` / version.

## 6. Blocking, lock-free, wait-free

- **Blocking**: một thread bị dừng có thể chặn mọi thread khác.
- **Lock-free**: luôn có ít nhất một thread tiến triển (thread khác có thể retry mãi).
- **Wait-free**: mọi thread hoàn tất trong số bước hữu hạn (ví dụ `getAndIncrement` trên x86 là LOCK XADD).

## Bẫy hay gặp

1. Hàm trong `updateAndGet` có side effect (log, gửi message, tăng counter khác) → bị gọi lặp lại khi CAS thất bại.
2. Dùng `AtomicReference` với object **mutable** rồi sửa field bên trong → không còn nguyên tử.
3. Hai biến atomic độc lập không làm nên một trạng thái nguyên tử (`min` và `max`, `balance` của hai tài khoản).
4. Nghĩ lock-free luôn nhanh hơn: dưới tranh chấp cao trên một điểm, CAS retry + cache line ping-pong có thể chậm hơn lock.
5. `AtomicLong` cho metrics ghi cực nhiều → dùng `LongAdder`.
6. `compareAndSet` trên `AtomicReference<Integer>` so sánh **tham chiếu** (`==`), không phải `equals` → `Integer` ngoài cache [-128, 127] làm CAS thất bại bất ngờ.

## Câu hỏi phỏng vấn

**1. CAS là gì? Ưu nhược điểm so với lock?**
<details><summary>Đáp án</summary>

Lệnh CPU nguyên tử so sánh-và-thay. Ưu: không chặn thread, không deadlock, không phụ thuộc thread giữ lock bị deschedule, nhanh khi tranh chấp thấp/vừa. Nhược: tranh chấp cao → retry nhiều, tốn CPU; chỉ nguyên tử trên một biến; code phức tạp hơn; có vấn đề ABA.
</details>

**2. Vấn đề ABA là gì, khắc phục ra sao?**
<details><summary>Đáp án</summary>

CAS chỉ so sánh giá trị hiện tại với giá trị mong đợi; nếu giá trị đổi A→B→A, CAS vẫn thành công dù trạng thái liên quan đã thay đổi. Khắc phục: gắn version/stamp (`AtomicStampedReference`), không tái sử dụng node (trong Java GC giúp), hoặc hazard pointer (C++).
</details>

**3. `AtomicLong` vs `LongAdder`?**
<details><summary>Đáp án</summary>

`AtomicLong`: một biến, mọi thread CAS trên cùng cache line; đọc rẻ và chính xác tức thời. `LongAdder`: nhiều cell (mỗi thread thường đụng cell khác nhau), `sum()` cộng các cell — ghi nhanh hơn nhiều dưới tranh chấp, nhưng tốn bộ nhớ hơn và `sum()` không phải snapshot nguyên tử. Dùng `LongAdder` cho counter/metrics, `AtomicLong` khi cần CAS/sequence chính xác.
</details>

**4. `ConcurrentHashMap` dùng lock hay lock-free?**
<details><summary>Đáp án</summary>

Java 8+: đọc (`get`) hoàn toàn không khoá (dựa trên volatile). Ghi vào bucket rỗng dùng CAS; bucket đã có node thì `synchronized` trên node đầu của bucket (khoá rất nhỏ). `size()` dùng cơ chế giống `LongAdder` (`CounterCell`). Resize được nhiều thread hỗ trợ cùng lúc.
</details>

**5. Lock-free khác wait-free thế nào?**
<details><summary>Đáp án</summary>

Lock-free: bảo đảm toàn hệ thống luôn tiến triển (ít nhất một thread thành công), nhưng một thread cụ thể có thể retry vô hạn (starvation). Wait-free: bảo đảm **mỗi** thread hoàn tất trong số bước hữu hạn — mạnh hơn, khó cài đặt hơn.
</details>
