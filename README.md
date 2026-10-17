# java-concurrency-lab

Project học tập đi kèm khóa Udemy **"Java Multithreading, Concurrency & Performance Optimization"** (Michael Pogrebinsky),
mở rộng thêm phần **Virtual Threads (Java 21)** và **mô hình IO cho server**.

## Mục tiêu

- Nắm chắc nền tảng: tạo/dừng thread, chia sẻ dữ liệu, race condition, visibility, deadlock.
- Biết đo đạc: latency vs throughput, khi nào thêm thread là có lợi, khi nào là phản tác dụng.
- Dùng thành thạo công cụ của `java.util.concurrent`: `ReentrantLock`, `ReadWriteLock`, `Semaphore`, `Condition`, atomic, CAS.
- Hiểu Virtual Threads ở mức đủ dùng cho production: khi nào lợi, pinning, giới hạn concurrency xuống hệ thống phía sau.
- Có sẵn câu trả lời cho các câu hỏi phỏng vấn concurrency thường gặp ở vị trí Backend Java.

Chỉ dùng JDK 21 + JUnit 5, không có thư viện ngoài.

## Cấu trúc thư mục

```
java-concurrency-lab/
├── pom.xml
├── README.md
├── docs/                         # Lý thuyết + bẫy + câu hỏi phỏng vấn (mỗi module 1 file)
│   ├── 01-thread-basics.md
│   ├── 02-performance.md
│   ├── 03-data-sharing.md
│   ├── 04-locking.md
│   ├── 05-inter-thread-communication.md
│   ├── 06-lock-free.md
│   ├── 07-virtual-threads.md
│   └── 08-io-models.md
├── src/main/java/vn/thanh/lab/
│   ├── m01/ ... m08/             # Demo có main() chạy được
│   └── mXX/exercise/             # Bài tập: method đang throw UnsupportedOperationException("TODO")
├── src/test/java/vn/thanh/lab/
│   └── mXX/exercise/             # Test JUnit 5 cho từng bài tập (có test đa luồng thật)
└── solutions/                    # Lời giải + giải thích. KHÔNG nằm trong src để không lỡ nhìn thấy
    └── m01/ ... m08/
```

## Lộ trình đề xuất (4 tuần, 2 module/tuần)

| Tuần | Module | Trọng tâm |
|------|--------|-----------|
| 1 | m01 Thread basics, m02 Performance | Vòng đời thread, interrupt, join; đo latency/throughput |
| 2 | m03 Data sharing, m04 Locking | Race condition, visibility, deadlock, lock ordering |
| 3 | m05 Inter-thread communication, m06 Lock-free | Semaphore, Condition, producer–consumer, CAS |
| 4 | m07 Virtual Threads, m08 IO models | Virtual threads, pinning, thread-per-request vs virtual |

Mỗi module nên làm theo thứ tự: **đọc docs → chạy demo, đọc code demo → làm bài tập cho test xanh → trả lời câu hỏi phỏng vấn → mới mở `solutions/` để so sánh**.

## Cách chạy demo

Mọi demo chạy bằng `exec:java` (chạy trong JVM của Maven):

```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m01.ThreadCreationDemo
```

> Lưu ý: `exec:java` chạy trong JVM của Maven, nên muốn truyền cờ JVM (`-Dxxx`, `-Xmx`) thì đặt vào `MAVEN_OPTS`.
> Demo nào có thể treo (deadlock, visibility) đều tự thoát sau timeout.

### m01 – Thread basics
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m01.ThreadCreationDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m01.InterruptDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m01.DaemonAndJoinDemo
```

### m02 – Performance
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m02.LatencyDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m02.ThroughputDemo
```

### m03 – Data sharing
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m03.StackHeapDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m03.RaceConditionDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m03.VisibilityDemo
```

### m04 – Locking
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m04.DeadlockDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m04.ReentrantLockDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m04.ReadWriteLockBenchmark
```

### m05 – Inter-thread communication
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m05.SemaphoreConnectionPoolDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m05.WaitNotifyDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m05.ConditionDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m05.ProducerConsumerDemo
```

### m06 – Lock-free
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m06.CasDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m06.AtomicReferenceDemo
# So sánh blocking stack với LockFreeStack CỦA BẠN (làm bài tập m06 trước, nếu chưa thì demo chỉ chạy phần blocking)
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m06.StackBenchmarkDemo
```

### m07 – Virtual Threads
```bash
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m07.VirtualThreadBasicsDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m07.VirtualVsPlatformDemo
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m07.PinningDemo
```

Kết quả thời gian (synchronized chậm gấp ~10 lần ReentrantLock) thấy được ngay với `exec:java`. Riêng **stack trace pinning**
(`-Djdk.tracePinnedThreads=full`) không in ra khi chạy trong JVM của Maven, nên muốn xem trace hãy chạy bằng `java` trực tiếp:

```bash
mvn -q compile && java -Djdk.tracePinnedThreads=full -cp target/classes vn.thanh.lab.m07.PinningDemo
```
Bạn sẽ thấy dòng `reason:MONITOR` và frame được đánh dấu `<== monitors:1` – đó là chỗ `synchronized` đang pin carrier.

### m08 – IO models
```bash
# Chạy benchmark trọn gói: tự bật server (platform pool rồi virtual), bắn tải, in bảng so sánh
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m08.IoModelBenchmarkDemo

# Hoặc chạy tách: terminal 1 bật server (mode = platform|virtual, port, số giây chạy)
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m08.BlockingHttpServer -Dexec.args="virtual 8080 120"
# terminal 2 bắn tải (url, concurrency, tổng số request)
mvn -q compile exec:java -Dexec.mainClass=vn.thanh.lab.m08.LoadGenerator -Dexec.args="http://localhost:8080/api/payment 1000 20000"
```

## Cách làm và chạy test bài tập

Bài tập nằm trong `src/main/java/vn/thanh/lab/mXX/exercise/`. Mỗi method đang là:

```java
throw new UnsupportedOperationException("TODO");
```

Đọc Javadoc của class, cài đặt, rồi chạy test của riêng bài đó:

```bash
mvn test -Dtest=ParallelArraySumTest
```

> **Lúc đầu mọi test bài tập sẽ FAIL với `UnsupportedOperationException: TODO` — đó là bình thường**, vì bạn chưa làm.
> Vì vậy đừng chạy `mvn test` cho cả project rồi hoảng; hãy chạy từng test như trên.

Danh sách test:

| Module | Bài tập | Lệnh |
|--------|---------|------|
| m01 | `ParallelFactorial` | `mvn test -Dtest=ParallelFactorialTest` |
| m02 | `ParallelArraySum` | `mvn test -Dtest=ParallelArraySumTest` |
| m03 | `MinMaxMetrics` | `mvn test -Dtest=MinMaxMetricsTest` |
| m04 | `BankTransferService` | `mvn test -Dtest=BankTransferServiceTest` |
| m05 | `BoundedBuffer`, `SimpleBarrier` | `mvn test -Dtest='BoundedBufferTest,SimpleBarrierTest'` |
| m06 | `LockFreeStack` | `mvn test -Dtest=LockFreeStackTest` |
| m07 | `PaymentFanOut` | `mvn test -Dtest=PaymentFanOutTest` |
| m08 | `Bulkhead` | `mvn test -Dtest=BulkheadTest` |

Test đa luồng chạy nhiều thread, lặp nhiều lần và có `@Timeout` — nếu code có race condition hoặc deadlock,
test sẽ fail (hoặc bị timeout) thay vì treo vô hạn. Một lần test xanh **chưa chứng minh** code đúng;
hãy chạy lại vài lần (ví dụ `for i in 1 2 3 4 5; do mvn -q test -Dtest=MinMaxMetricsTest || break; done`) và tự suy luận.

Lời giải: `solutions/mXX/` (mỗi thư mục có file `.java` và `README.md` giải thích vì sao đúng).

## Checklist tiến độ

- [ ] m01 Thread basics — docs, demo, `ParallelFactorial`, câu hỏi phỏng vấn
- [ ] m02 Performance — docs, demo, `ParallelArraySum`, câu hỏi phỏng vấn
- [ ] m03 Data sharing — docs, demo, `MinMaxMetrics`, câu hỏi phỏng vấn
- [ ] m04 Locking — docs, demo, `BankTransferService`, câu hỏi phỏng vấn
- [ ] m05 Inter-thread communication — docs, demo, `BoundedBuffer`, `SimpleBarrier`, câu hỏi phỏng vấn
- [ ] m06 Lock-free — docs, demo, `LockFreeStack`, câu hỏi phỏng vấn
- [ ] m07 Virtual Threads — docs, demo, `PaymentFanOut`, câu hỏi phỏng vấn
- [ ] m08 IO models — docs, demo, `Bulkhead`, câu hỏi phỏng vấn
