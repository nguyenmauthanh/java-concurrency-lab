package vn.thanh.lab.m01;

import java.math.BigInteger;

/**
 * Demo interrupt trong hai tình huống:
 * <ol>
 *     <li>Thread đang BLOCK (sleep/wait/join): nhận {@link InterruptedException}.</li>
 *     <li>Thread đang BUSY (tính toán): không có exception, phải tự kiểm tra {@link Thread#isInterrupted()}.</li>
 * </ol>
 */
public class InterruptDemo {

    public static void main(String[] args) throws InterruptedException {
        blockingThread();
        busyLoopWithoutCheck();
        busyLoopWithCheck();
    }

    private static void blockingThread() throws InterruptedException {
        System.out.println("=== 1. Interrupt thread đang sleep ===");
        Thread sleeper = new Thread(() -> {
            try {
                System.out.println("sleeper: ngủ 10 giây...");
                Thread.sleep(10_000);
                System.out.println("sleeper: dậy bình thường (không nên thấy dòng này)");
            } catch (InterruptedException e) {
                // Khi InterruptedException được ném, cờ interrupt ĐÃ BỊ XÓA.
                System.out.println("sleeper: bị interrupt! isInterrupted() = " + Thread.currentThread().isInterrupted());
                // Quy ước tốt: nếu không xử lý được thì khôi phục cờ để code phía trên biết.
                Thread.currentThread().interrupt();
            }
        }, "sleeper");
        sleeper.start();
        Thread.sleep(200);
        sleeper.interrupt();
        sleeper.join();
    }

    private static void busyLoopWithoutCheck() throws InterruptedException {
        System.out.println("\n=== 2. Interrupt thread đang tính toán, KHÔNG kiểm tra cờ ===");
        Thread worker = new Thread(() -> {
            BigInteger result = BigInteger.ONE;
            for (int i = 0; i < 2_000_000; i++) {
                result = result.add(BigInteger.valueOf(i)); // không hề quan tâm tới interrupt
            }
            System.out.println("worker-no-check: chạy xong hết dù đã bị interrupt, bitLength=" + result.bitLength());
        }, "worker-no-check");
        worker.start();
        worker.interrupt();
        worker.join();
    }

    private static void busyLoopWithCheck() throws InterruptedException {
        System.out.println("\n=== 3. Interrupt thread đang tính toán, CÓ kiểm tra cờ ===");
        Thread worker = new Thread(() -> {
            long iterations = 0;
            while (true) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("worker-check: thấy cờ interrupt sau " + iterations + " vòng lặp -> dừng sạch sẽ");
                    return;
                }
                iterations++;
            }
        }, "worker-check");
        worker.start();
        Thread.sleep(100);
        worker.interrupt();
        worker.join(2_000);
        System.out.println("worker-check còn sống? " + worker.isAlive());
    }
}
