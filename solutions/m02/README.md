# Lời giải m02 – ParallelArraySum

## Ý tưởng
- Số worker = `min(threadCount, length)` (tối thiểu 1) → không tạo thread rỗng vô ích.
- Chia đoạn: `chunk = length / workers`, `remainder` đoạn đầu dài thêm 1 → không bỏ sót phần tử cuối.
- Mỗi worker cộng vào **biến cục bộ** `local`, cuối cùng ghi một lần vào `partialSums[index]`.
- Thread gọi `join()` hết rồi mới cộng các phần.

## Vì sao đúng
- **Ranh giới đoạn**: `from` của đoạn sau = `to` của đoạn trước, đoạn cuối kết thúc ở `length` (tổng các độ dài = length).
- **Không race**: mỗi ô `partialSums[i]` chỉ một thread ghi; đọc sau `join()` nên có happens-before.
- **Tràn số**: phép cộng `long` quay vòng (mod 2^64) và có tính kết hợp/giao hoán theo modulo → thứ tự cộng khác
  vẫn cho cùng kết quả với cộng tuần tự.

## Vì sao không cộng chung vào một `AtomicLong`?
Mỗi phần tử một lần CAS trên cùng một cache line → các core tranh nhau cache line đó (cache line ping-pong),
chậm hơn cả cộng tuần tự. Quy tắc: **chia việc để mỗi thread làm trên dữ liệu riêng, chỉ gộp ở cuối**.

## Mở rộng
- Ghi `partialSums[index]` liên tục trong vòng lặp (thay vì biến `local`) gây **false sharing**:
  các ô kề nhau nằm cùng cache line 64 byte → chậm thấy rõ. Thử đo!
