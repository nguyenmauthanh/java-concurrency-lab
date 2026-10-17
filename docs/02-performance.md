# 02 – Performance: Latency và Throughput

## 1. Hai thước đo

| | Latency | Throughput |
|---|---|---|
| Định nghĩa | Thời gian hoàn thành **một** task | Số task hoàn thành trong một đơn vị thời gian |
| Câu hỏi | "Một request mất bao lâu?" | "Một giây xử lý được bao nhiêu request?" |
| Cách tối ưu bằng thread | Chia **một** task thành N phần chạy song song | Chạy **nhiều** task độc lập song song |
| Ví dụ backend | Báo cáo đối soát chạy 30s → muốn 5s | API thanh toán 2.000 TPS → muốn 10.000 TPS |

## 2. Tối ưu latency: chia task

```
Tuần tự (1 thread):  [==================== T ====================]

Song song (4 thread):
 t1 [==== T/4 ====]
 t2 [==== T/4 ====]      Latency lý thuyết = T/N + chi phí (tạo thread, chia, gộp)
 t3 [==== T/4 ====]
 t4 [==== T/4 ====]
                  └─ gộp kết quả
```

- Số thread hợp lý cho task **CPU-bound** ≈ số core (`Runtime.getRuntime().availableProcessors()`).
- Thêm thread vượt số core → không nhanh hơn, có thể chậm hơn (context switch, cache bị xoá).
- Task phải **chia được** và chi phí chia/gộp nhỏ hơn lợi ích. Mảng nhỏ → chạy tuần tự còn nhanh hơn.

### Định luật Amdahl
Nếu phần có thể song song là `p` (0..1), speed-up tối đa với N core:

```
speedup(N) = 1 / ((1 - p) + p / N)        p = 0.9, N → ∞  =>  speedup ≤ 10
```
Phần tuần tự (khởi tạo, gộp, lock chung) quyết định giới hạn trên.

## 3. Tối ưu throughput: thread pool

```
            ┌─────────── Thread pool (N thread) ───────────┐
 task ──►   │  queue: [t][t][t][t] ──► worker1, worker2 ... │ ──► kết quả
            └───────────────────────────────────────────────┘
```

- Tái sử dụng thread → không trả giá tạo/huỷ thread cho mỗi task.
- **CPU-bound**: N ≈ số core.
- **IO-bound**: N ≈ `cores × (1 + thời_gian_chờ / thời_gian_tính)` (công thức Brian Goetz). Chờ IO 90% → N ≈ 10 × cores.
- Với IO-bound nhiều chờ → virtual threads (m07) thường là lựa chọn tốt hơn pool lớn.

## 4. Đo đạc đúng cách

- **Warm-up**: JIT cần thời gian biên dịch; lần chạy đầu luôn chậm.
- Lặp nhiều lần, lấy **median** / percentile, không lấy một lần đo.
- Chống **dead-code elimination**: dùng kết quả (ghi ra biến `volatile`, in ra).
- Benchmark nghiêm túc: dùng JMH (ngoài phạm vi project này vì không dùng thư viện ngoài).
- Với server: đo **p99 latency** dưới tải, không chỉ trung bình.

## Bẫy hay gặp

1. Mặc định "nhiều thread = nhanh hơn". Với CPU-bound, vượt số core là phản tác dụng.
2. Các thread cùng cập nhật một biến chung (kể cả `AtomicLong`) → tranh chấp cache line, chậm hơn tuần tự.
3. **False sharing**: các thread ghi vào ô mảng kề nhau (cùng cache line 64 byte) → chậm khó hiểu.
4. Đo không warm-up, hoặc đo cả thời gian khởi tạo dữ liệu.
5. Dùng `parallelStream()` cho IO-bound → nó chạy trên common `ForkJoinPool` (≈ số core), chặn IO làm nghẽn cả JVM.
6. Pool không giới hạn queue (`newFixedThreadPool` dùng `LinkedBlockingQueue` vô hạn) → khi quá tải, queue phình gây OOM / latency khủng.

## Câu hỏi phỏng vấn

**1. Latency và throughput khác nhau thế nào? Tối ưu cái này có làm hại cái kia không?**
<details><summary>Đáp án</summary>

Latency = thời gian cho một đơn vị công việc; throughput = số đơn vị trên giây. Có thể xung đột: batching tăng throughput nhưng tăng latency từng request; chia nhỏ một task cho nhiều thread giảm latency của task đó nhưng chiếm hết core khiến throughput tổng của hệ thống không tăng (thậm chí giảm vì overhead).
</details>

**2. Chọn kích thước thread pool thế nào?**
<details><summary>Đáp án</summary>

CPU-bound: ≈ số core (có thể +1). IO-bound: `cores × (1 + wait/compute)`, rồi đo thực tế. Phải tính cả giới hạn tài nguyên phía sau (connection pool DB, rate limit của đối tác) — pool lớn hơn connection pool DB chỉ khiến thread xếp hàng chờ connection.
</details>

**3. Định luật Amdahl nói gì?**
<details><summary>Đáp án</summary>

Speed-up bị giới hạn bởi phần tuần tự: `1 / ((1-p) + p/N)`. 10% tuần tự → tối đa 10 lần dù có bao nhiêu core. Hệ quả: giảm phần tuần tự (lock chung, gộp kết quả) quan trọng hơn thêm core.
</details>

**4. False sharing là gì, khắc phục ra sao?**
<details><summary>Đáp án</summary>

Hai biến độc lập nằm cùng một cache line; khi các core ghi vào chúng, cache line bị invalidate qua lại giữa các core dù không chia sẻ dữ liệu logic. Khắc phục: cộng vào biến cục bộ rồi ghi một lần, padding, `@jdk.internal.vm.annotation.Contended` (nội bộ JDK, cần `-XX:-RestrictContended`), hoặc dùng `LongAdder` (đã xử lý bằng cell có padding).
</details>

**5. Vì sao không nên dùng `parallelStream()` cho việc gọi HTTP/DB?**
<details><summary>Đáp án</summary>

Nó chạy trên `ForkJoinPool.commonPool()` có số thread ≈ số core - 1, dùng chung toàn JVM. Task chặn IO giữ hết worker, làm mọi `parallelStream`/`CompletableFuture.supplyAsync` khác trong JVM nghẽn theo. Dùng executor riêng hoặc virtual threads.
</details>
