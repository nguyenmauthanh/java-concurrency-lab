# 03 – Data sharing: stack, heap, race condition, visibility

## 1. Cái gì được chia sẻ?

```
        Thread A stack           Heap (chung)              Thread B stack
     ┌──────────────────┐   ┌──────────────────────┐   ┌──────────────────┐
     │ int i = 3        │   │  Counter@1a2b         │   │ int i = 7        │
     │ Counter c ───────┼──►│   count = 42          │◄──┼─── Counter c     │
     │ long local = 10  │   │  static CONFIG        │   │ long local = 99  │
     └──────────────────┘   └──────────────────────┘   └──────────────────┘
```
- **Stack (riêng)**: biến cục bộ kiểu nguyên thuỷ, tham chiếu cục bộ, tham số.
- **Heap (chung)**: object, mảng, field, static.
- Chỉ cần đồng bộ khi: **dữ liệu chung + có ít nhất một thread ghi**.

## 2. Race condition: `count++`

```
count++  ==  (1) đọc count vào thanh ghi   (2) +1   (3) ghi lại count

Thread A: đọc 5 ─────────── +1 → ghi 6
Thread B:        đọc 5 ── +1 → ghi 6            => hai lần ++ nhưng chỉ tăng 1 (lost update)
```
Race condition = kết quả phụ thuộc vào thứ tự xen kẽ của các thread. Kiểu phổ biến: **read-modify-write**, **check-then-act**
(`if (!map.containsKey(k)) map.put(k, v)`).

## 3. synchronized

```java
public synchronized void inc() { count++; }        // khoá trên this (static method: khoá trên Class object)
public void inc() { synchronized (lock) { count++; } } // khoá trên object tuỳ chọn, phạm vi hẹp hơn
```
- Mutual exclusion: tại một thời điểm chỉ một thread giữ monitor của object.
- **Visibility**: unlock monitor *happens-before* lần lock tiếp theo trên **cùng monitor** → thấy mọi ghi trước đó.
- Reentrant: thread đang giữ lock có thể vào lại.
- Ưu tiên khoá trên `private final Object lock` thay vì `this` (code bên ngoài không khoá nhầm vào được).

## 4. Atomic

`AtomicInteger.incrementAndGet()` dùng CAS (compare-and-swap) của CPU — nguyên tử mà không cần lock (xem m06).
`LongAdder`: chia thành nhiều cell để giảm tranh chấp, nhanh nhất cho counter ghi nhiều / đọc ít.

## 5. Visibility và volatile

CPU có cache, store buffer; JIT có thể sắp xếp lại lệnh hoặc giữ biến trong thanh ghi.
Không có đồng bộ → **không có bảo đảm** một thread khác sẽ thấy giá trị mới, hay thấy theo đúng thứ tự.

```java
boolean stop;              // reader: while (!stop) {}  -> có thể lặp mãi mãi
volatile boolean stop;     // ghi volatile happens-before mọi lần đọc sau đó
```

`volatile` bảo đảm:
- **Visibility**: đọc luôn thấy lần ghi mới nhất.
- **Ordering**: mọi ghi *trước* lần ghi volatile được thấy bởi thread đọc biến volatile đó (safe publication).
- Đọc/ghi `long`/`double` volatile là nguyên tử.

`volatile` **không** bảo đảm: tính nguyên tử của `count++` hay check-then-act.

### Happens-before (tóm tắt)
- Trong một thread: theo thứ tự chương trình.
- Unlock monitor → lock tiếp theo trên cùng monitor.
- Ghi volatile → đọc volatile tiếp theo cùng biến.
- `Thread.start()` → mọi hành động trong thread đó; mọi hành động trong thread → `join()` trả về.
- Ghi `final` field trong constructor → thread khác thấy object (nếu `this` không bị lộ trong constructor).

## Bẫy hay gặp

1. Nghĩ `volatile` là đủ cho `count++` → vẫn mất cập nhật.
2. Khoá ghi nhưng không khoá đọc (và field không volatile) → reader có thể thấy giá trị cũ.
3. Khoá trên **hai object khác nhau** cho cùng một dữ liệu → như không khoá.
4. `synchronized` trên `Integer`/`String` literal/`Boolean` → object bị chia sẻ ngầm hoặc thay đổi sau mỗi lần ++ (autoboxing tạo object mới).
5. Double-checked locking không có `volatile` → thread khác có thể thấy object khởi tạo dở.
6. `HashMap` dùng chung giữa các thread (Spring singleton bean có field `HashMap`) → mất dữ liệu, thậm chí vòng lặp vô hạn (Java 7).
7. `SimpleDateFormat` / `DecimalFormat` là field static → không thread-safe; dùng `DateTimeFormatter`.

## Câu hỏi phỏng vấn

**1. `volatile` và `synchronized` khác nhau ra sao?**
<details><summary>Đáp án</summary>

`volatile` chỉ bảo đảm visibility và ordering cho một biến, không khoá, không cho tính nguyên tử của thao tác phức hợp. `synchronized` bảo đảm cả mutual exclusion (atomicity cho cả khối lệnh) lẫn visibility. Dùng `volatile` cho cờ trạng thái / publish object bất biến; dùng lock (hoặc atomic) cho read-modify-write.
</details>

**2. Vì sao `count++` không thread-safe dù `count` là `volatile`?**
<details><summary>Đáp án</summary>

Vì `count++` là ba bước đọc-cộng-ghi. `volatile` làm mỗi lần đọc/ghi riêng lẻ hiển thị ngay, nhưng hai thread vẫn có thể cùng đọc một giá trị rồi cùng ghi `giá trị + 1` → mất một lần tăng.
</details>

**3. Happens-before là gì? Kể vài quy tắc.**
<details><summary>Đáp án</summary>

Quan hệ trong Java Memory Model: nếu A happens-before B thì kết quả của A hiển thị với B và A được xem như xảy ra trước B. Quy tắc: thứ tự chương trình trong một thread; unlock → lock cùng monitor; ghi volatile → đọc volatile; `start()` → code trong thread; code trong thread → `join()`; tính bắc cầu.
</details>

**4. Spring bean mặc định là singleton — điều đó ảnh hưởng gì tới thread safety?**
<details><summary>Đáp án</summary>

Một instance dùng chung cho mọi request thread. Field mutable (cache `HashMap`, biến đếm, `SimpleDateFormat`) sẽ bị nhiều thread truy cập đồng thời → race. Bean nên stateless; state chia sẻ phải dùng cấu trúc thread-safe (`ConcurrentHashMap`, atomic) hoặc nằm trong biến cục bộ.
</details>

**5. Double-checked locking cho singleton viết thế nào cho đúng?**
<details><summary>Đáp án</summary>

```java
private static volatile Holder instance;
static Holder get() {
    Holder local = instance;
    if (local == null) {
        synchronized (Holder.class) {
            local = instance;
            if (local == null) instance = local = new Holder();
        }
    }
    return local;
}
```
Bắt buộc `volatile`, nếu không thread khác có thể thấy tham chiếu trước khi constructor chạy xong (do reorder). Đơn giản hơn: holder class idiom (`static class Lazy { static final Holder I = new Holder(); }`) hoặc `enum`.
</details>
