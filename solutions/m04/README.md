# Lời giải m04 – BankTransferService

## Vì sao cách ngây thơ bị deadlock
```java
synchronized (from) { synchronized (to) { ... } }
```
Thread 1 chuyển 0→1: giữ lock 0, chờ lock 1. Thread 2 chuyển 1→0: giữ lock 1, chờ lock 0 → vòng chờ (circular wait).

Deadlock cần đủ 4 điều kiện Coffman: **mutual exclusion, hold and wait, no preemption, circular wait**.
Lock ordering phá điều kiện **circular wait**.

## Lời giải
- Mỗi `Account` có `ReentrantLock` riêng → các transfer không liên quan chạy song song.
- `transfer` luôn khoá tài khoản **id nhỏ trước**: 0→1 và 1→0 đều khoá 0 rồi 1 → không thể có vòng chờ.
- Kiểm tra số dư **bên trong** lock (check-then-act phải nguyên tử) → không bao giờ âm.
- `getTotalBalance` khoá **mọi** tài khoản theo thứ tự id tăng dần → snapshot nhất quán; vẫn tuân thủ thứ tự toàn cục nên
  không deadlock với `transfer`. Unlock theo thứ tự ngược, và chỉ unlock những lock đã lấy được (`locked`).
- `getBalance` cũng khoá: `balance` không phải volatile, nếu đọc không khoá thì không có bảo đảm visibility.

## Vì sao tổng tiền bảo toàn
Mọi thay đổi `balance` xảy ra theo cặp `-amount` / `+amount` trong cùng một critical section giữ cả hai lock.
Không thread nào (kể cả auditor) có thể quan sát trạng thái giữa hai lệnh vì nó cũng phải giữ lock của cả hai tài khoản.

## Biến thể
- Khi không có id tự nhiên: dùng `System.identityHashCode` + một "tie-breaking lock" khi hash trùng (Java Concurrency in Practice).
- `tryLock(timeout)` + backoff: không cần thứ tự, nhưng phức tạp hơn và có nguy cơ livelock.
- Trong DB thật (MySQL): cùng nguyên lý – `SELECT ... FOR UPDATE` theo thứ tự id tăng dần để tránh deadlock InnoDB.
