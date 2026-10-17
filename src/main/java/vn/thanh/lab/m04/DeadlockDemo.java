package vn.thanh.lab.m04;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Demo deadlock kinh điển: hai thread lấy hai lock theo thứ tự ngược nhau.
 * Main thread phát hiện deadlock bằng {@link ThreadMXBean#findDeadlockedThreads()}, in thông tin rồi thoát.
 * Hai thread bị kẹt là daemon nên không giữ JVM sống.
 */
public class DeadlockDemo {

    private static final Object ACCOUNT_A = new Object();
    private static final Object ACCOUNT_B = new Object();

    public static void main(String[] args) throws InterruptedException {
        Thread first = new Thread(() -> lockBoth(ACCOUNT_A, "A", ACCOUNT_B, "B"), "transfer-A-to-B");
        Thread second = new Thread(() -> lockBoth(ACCOUNT_B, "B", ACCOUNT_A, "A"), "transfer-B-to-A");
        first.setDaemon(true);
        second.setDaemon(true);
        first.start();
        second.start();

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            long[] deadlockedIds = threadMXBean.findDeadlockedThreads();
            if (deadlockedIds != null) {
                System.out.println("\n>>> PHÁT HIỆN DEADLOCK giữa " + deadlockedIds.length + " thread:");
                for (ThreadInfo info : threadMXBean.getThreadInfo(deadlockedIds)) {
                    System.out.printf("  - '%s' (%s) đang chờ lock %s, lock đó do '%s' giữ%n",
                            info.getThreadName(), info.getThreadState(), info.getLockName(), info.getLockOwnerName());
                }
                System.out.println("""

                        Cách tránh:
                          1. Lock ordering: luôn lấy lock theo cùng một thứ tự toàn cục (ví dụ theo id tài khoản).
                          2. tryLock(timeout) và thả hết lock nếu không lấy được (xem ReentrantLockDemo).
                          3. Giảm số lock phải giữ cùng lúc.
                        Trong production: `jstack <pid>` hoặc thread dump cũng in ra "Found one Java-level deadlock".
                        """);
                return; // main thoát, hai thread daemon bị kẹt bị bỏ lại
            }
            Thread.sleep(100);
        }
        System.out.println("Không phát hiện deadlock trong 5 giây (hiếm khi xảy ra với demo này).");
    }

    private static void lockBoth(Object firstLock, String firstName, Object secondLock, String secondName) {
        String name = Thread.currentThread().getName();
        synchronized (firstLock) {
            System.out.println(name + ": đã giữ lock " + firstName + ", chuẩn bị lấy " + secondName);
            sleepQuietly(100); // tăng cơ hội để thread kia kịp giữ lock còn lại
            synchronized (secondLock) {
                System.out.println(name + ": đã giữ cả hai lock (không nên thấy dòng này)");
            }
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
