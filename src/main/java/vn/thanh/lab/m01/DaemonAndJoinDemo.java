package vn.thanh.lab.m01;

/**
 * Demo daemon thread và join có timeout.
 * <p>
 * JVM thoát khi KHÔNG còn thread non-daemon nào chạy; daemon thread bị "bỏ lại" mà không chạy finally.
 * (Chạy bằng exec:java thì JVM của Maven có thể còn sống thêm một chút, nhưng ý tưởng giống nhau.)
 */
public class DaemonAndJoinDemo {

    public static void main(String[] args) throws InterruptedException {
        Thread heartbeat = new Thread(() -> {
            int beat = 0;
            while (true) {
                System.out.println("  heartbeat (daemon) #" + (++beat));
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "heartbeat");
        heartbeat.setDaemon(true); // phải gọi TRƯỚC start(), nếu không sẽ IllegalThreadStateException
        heartbeat.start();

        Thread slowTask = new Thread(() -> {
            try {
                Thread.sleep(5_000);
                System.out.println("slowTask: xong (không nên thấy dòng này)");
            } catch (InterruptedException e) {
                System.out.println("slowTask: bị huỷ vì quá thời gian chờ");
            }
        }, "slow-task");
        slowTask.start();

        long start = System.nanoTime();
        slowTask.join(500); // chỉ chờ tối đa 500ms
        long waitedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("join(500) trả về sau ~%d ms, slowTask.isAlive() = %s%n", waitedMs, slowTask.isAlive());

        if (slowTask.isAlive()) {
            // join có timeout KHÔNG tự dừng thread - ta phải chủ động interrupt.
            slowTask.interrupt();
            slowTask.join();
        }

        System.out.println("main kết thúc. heartbeat là daemon nên không giữ JVM sống.");
    }
}
