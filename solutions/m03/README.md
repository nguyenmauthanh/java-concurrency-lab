# Lời giải m03 – MinMaxMetrics

## Lỗi kinh điển (sai)
```java
private volatile long min = Long.MAX_VALUE;
public void addSample(long sample) {
    if (sample < min) {   // đọc
        min = sample;     // ghi  -> không nguyên tử với bước đọc!
    }
}
```
Thread A đọc `min=100`, thấy `50 < 100`; trước khi A ghi, thread B ghi `min=10`; A ghi `min=50` → **min tăng từ 10 lên 50**,
mẫu 10 bị mất. `volatile` chỉ giải quyết **visibility**, không giải quyết **atomicity** của chuỗi đọc-so sánh-ghi.

## Cách 1: synchronized ghi + volatile đọc
- `addSample` khoá → đọc-so sánh-ghi nguyên tử, hai writer không chen nhau.
- `getMin/getMax` không khoá nhưng field là `volatile` → luôn thấy giá trị mới nhất đã được ghi.
- Vì mọi lần ghi chỉ làm min nhỏ đi / max lớn lên (và được tuần tự hoá bởi lock), mọi giá trị reader thấy là đơn điệu.

## Cách 2: AtomicLong + CAS (`accumulateAndGet`)
- Mỗi lần cập nhật: đọc giá trị hiện tại `cur`, tính `Math.min(cur, sample)`, `compareAndSet(cur, new)`; thất bại thì thử lại.
- CAS chỉ thành công nếu không ai đổi giá trị kể từ lúc đọc → không bao giờ ghi đè giá trị tốt hơn.
- Nhanh hơn khi tranh chấp vừa phải; khi tranh chấp rất cao có thể retry nhiều.

## Lưu ý
- `getMin()` và `getMax()` là hai lần đọc riêng → không phải một *snapshot* nhất quán của cặp (min, max).
  Nếu cần snapshot, gộp hai giá trị vào một object bất biến và dùng `AtomicReference`/lock khi đọc.
