package vn.thanh.lab.m06;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * Demo {@link AtomicReference}: cập nhật nguyên tử một object BẤT BIẾN (snapshot cấu hình phí giao dịch),
 * và vấn đề ABA cùng cách khắc phục bằng {@link AtomicStampedReference}.
 */
public class AtomicReferenceDemo {

    /** Object bất biến: muốn "sửa" thì tạo object mới rồi CAS tham chiếu. */
    record FeeConfig(int version, long fixedFee, double percentFee) {
        FeeConfig withFixedFee(long newFixedFee) {
            return new FeeConfig(version + 1, newFixedFee, percentFee);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        AtomicReference<FeeConfig> config = new AtomicReference<>(new FeeConfig(0, 1_000, 0.5));

        System.out.println("=== 1. 4 thread cùng tăng phí cố định 1.000 lần mỗi thread, dùng updateAndGet ===");
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            Thread thread = new Thread(() -> {
                for (int i = 0; i < 1_000; i++) {
                    // updateAndGet = vòng CAS: hàm có thể chạy NHIỀU lần khi tranh chấp -> phải không có side effect
                    config.updateAndGet(current -> current.withFixedFee(current.fixedFee() + 1));
                }
            });
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("Kết quả: " + config.get() + " (kỳ vọng fixedFee = 5000, version = 4000)");
        System.out.println("Reader luôn thấy một snapshot nhất quán (version, fixedFee, percentFee) vì object bất biến.");

        System.out.println("\n=== 2. Vấn đề ABA ===");
        AtomicReference<String> plain = new AtomicReference<>("A");
        String seenByThread1 = plain.get(); // thread 1 đọc A, rồi bị "tạm dừng"
        plain.set("B");                    // thread 2 đổi A -> B
        plain.set("A");                    // ... rồi đổi lại B -> A
        boolean swapped = plain.compareAndSet(seenByThread1, "C");
        System.out.println("AtomicReference: CAS(A -> C) thành công? " + swapped + " -> không phát hiện được A đã từng bị đổi");

        AtomicStampedReference<String> stamped = new AtomicStampedReference<>("A", 0);
        int[] stampHolder = new int[1];
        String seen = stamped.get(stampHolder);
        int seenStamp = stampHolder[0];
        stamped.compareAndSet("A", "B", 0, 1);
        stamped.compareAndSet("B", "A", 1, 2);
        boolean stampedSwapped = stamped.compareAndSet(seen, "C", seenStamp, seenStamp + 1);
        System.out.println("AtomicStampedReference: CAS(A -> C, stamp 0) thành công? " + stampedSwapped
                + " -> phát hiện được nhờ stamp đã tăng lên " + stamped.getStamp());
    }
}
