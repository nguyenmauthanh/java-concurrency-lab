package vn.thanh.lab.m03;

/**
 * Demo data race về VISIBILITY: thread đọc có thể không bao giờ thấy giá trị mới của một biến không volatile.
 * <p>
 * Kết quả phụ thuộc JIT/CPU: với HotSpot, vòng lặp không volatile thường bị JIT "hoist" phần đọc cờ ra ngoài
 * vòng lặp -> lặp vô hạn. Demo tự thoát sau timeout (reader là daemon thread).
 */
public class VisibilityDemo {

    private static final long TIMEOUT_MS = 2_000;

    static class PlainFlag {
        boolean stop; // KHÔNG volatile
    }

    static class VolatileFlag {
        volatile boolean stop;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1. Cờ thường (không volatile) ===");
        PlainFlag plainFlag = new PlainFlag();
        Thread plainReader = new Thread(() -> {
            long spins = 0;
            while (!plainFlag.stop) {
                spins++; // không có lời gọi đồng bộ nào trong vòng lặp -> JIT được phép đọc stop một lần duy nhất
            }
            System.out.println("plain-reader: thấy stop=true sau " + spins + " vòng");
        }, "plain-reader");
        runScenario(plainReader, () -> plainFlag.stop = true);

        System.out.println("\n=== 2. Cờ volatile ===");
        VolatileFlag volatileFlag = new VolatileFlag();
        Thread volatileReader = new Thread(() -> {
            long spins = 0;
            while (!volatileFlag.stop) {
                spins++;
            }
            System.out.println("volatile-reader: thấy stop=true sau " + spins + " vòng");
        }, "volatile-reader");
        runScenario(volatileReader, () -> volatileFlag.stop = true);
    }

    private static void runScenario(Thread reader, Runnable stopAction) throws InterruptedException {
        reader.setDaemon(true); // nếu bị treo thì cũng không giữ JVM sống
        reader.start();
        Thread.sleep(500); // cho JIT kịp biên dịch vòng lặp nóng
        stopAction.run();
        System.out.println("main: đã đặt stop = true, chờ reader tối đa " + TIMEOUT_MS + "ms...");
        reader.join(TIMEOUT_MS);
        if (reader.isAlive()) {
            System.out.println("main: reader VẪN ĐANG CHẠY -> không nhìn thấy ghi của main (lỗi visibility). Bỏ qua, đi tiếp.");
        } else {
            System.out.println("main: reader đã dừng.");
        }
    }
}
