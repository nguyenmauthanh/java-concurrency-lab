# Lời giải m06 – LockFreeStack (Treiber stack)

## Thuật toán
```
push(v):  loop { h = head.get(); n = Node(v, h); if CAS(head, h, n) return }
pop():    loop { h = head.get(); if h == null return null; if CAS(head, h, h.next) return h.value }
```

## Vì sao đúng
- **Linearization point** của push/pop là lần CAS thành công: tại thời điểm đó head chuyển nguyên tử từ `h` sang giá trị mới.
- CAS chỉ thành công nếu head **vẫn là** `h` → không có push/pop nào khác chen vào giữa lúc đọc và lúc ghi.
  Nếu có ai chen vào, CAS thất bại và ta đọc lại → không mất phần tử, không pop trùng.
- `Node` bất biến (field `final`) + `AtomicReference` có ngữ nghĩa volatile → thread nào đọc được node từ head
  cũng thấy đầy đủ `value` và `next` (safe publication).
- Lock-free: tại mọi thời điểm, ít nhất một thread đang tiến triển (CAS thất bại của thread này nghĩa là CAS của thread khác đã thành công).

## Vì sao Java không bị ABA ở đây
ABA xảy ra khi node `h` bị pop, **giải phóng và tái sử dụng**, rồi được push lại → CAS so sánh địa chỉ thấy "vẫn là h"
nhưng `h.next` đã khác. Trong Java, chừng nào thread còn giữ tham chiếu `h`, GC không thu hồi node đó, và mỗi push
tạo `Node` mới → không thể có một node "khác" mang cùng tham chiếu. Nếu tự làm object pool để tái sử dụng node thì
ABA quay lại → cần `AtomicStampedReference`.

## Hiệu năng
Tất cả thread tranh nhau **một** điểm (head) → dưới tranh chấp rất cao, CAS thất bại nhiều. Cải tiến: exponential backoff,
elimination-backoff stack (push và pop "triệt tiêu" nhau trong một mảng phụ).
