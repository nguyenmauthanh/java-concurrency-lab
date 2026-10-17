package vn.thanh.lab.m04;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo {@link ReentrantLock}: lock/unlock trong finally, tryLock có timeout để tránh deadlock,
 * lockInterruptibly để có thể huỷ thread đang chờ lock.
 */
public class ReentrantLockDemo {

    public static void main(String[] args) throws InterruptedException {
        basicUsage();
        tryLockAvoidsDeadlock();
        lockInterruptibly();
    }

    private static void basicUsage() {
        System.out.println("=== 1. lock() / unlock() trong finally, reentrant ===");
        ReentrantLock lock = new ReentrantLock();
        lock.lock();
        try {
            lock.lock(); // vào lại lần nữa: OK vì reentrant
            try {
                System.out.println("hold count = " + lock.getHoldCount() + ", isHeldByCurrentThread = " + lock.isHeldByCurrentThread());
            } finally {
                lock.unlock();
            }
        } finally {
            lock.unlock(); // luôn unlock trong finally, nếu không exception sẽ để lock bị giữ vĩnh viễn
        }
        System.out.println("sau khi unlock: isLocked = " + lock.isLocked());
    }

    private static void tryLockAvoidsDeadlock() throws InterruptedException {
        System.out.println("\n=== 2. tryLock(timeout): hai thread lấy lock ngược thứ tự nhưng KHÔNG deadlock ===");
        ReentrantLock lockA = new ReentrantLock();
        ReentrantLock lockB = new ReentrantLock();
        Thread first = new Thread(() -> transferWithTryLock(lockA, lockB, 200), "A-then-B");
        Thread second = new Thread(() -> transferWithTryLock(lockB, lockA, 200), "B-then-A");
        long start = System.nanoTime();
        first.start();
        second.start();
        first.join();
        second.join();
        System.out.printf("Cả hai hoàn tất 200 lần chuyển trong %d ms%n", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    private static void transferWithTryLock(ReentrantLock firstLock, ReentrantLock secondLock, int transfers) {
        int retries = 0;
        int done = 0;
        try {
            while (done < transfers) {
                if (tryTransfer(firstLock, secondLock)) {
                    done++;
                } else {
                    retries++;
                    // backoff ngẫu nhiên để hai thread không "lịch sự" nhường nhau mãi (livelock)
                    Thread.sleep(0, ThreadLocalRandom.current().nextInt(1, 50_000));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        System.out.println(Thread.currentThread().getName() + ": xong " + done + " lần, phải thử lại " + retries + " lần");
    }

    /** Lấy được cả hai lock thì làm việc và trả true; không lấy được thì thả lock đang giữ và trả false. */
    private static boolean tryTransfer(ReentrantLock firstLock, ReentrantLock secondLock) throws InterruptedException {
        if (!firstLock.tryLock(10, TimeUnit.MILLISECONDS)) {
            return false;
        }
        try {
            if (!secondLock.tryLock(10, TimeUnit.MILLISECONDS)) {
                return false; // finally bên ngoài sẽ thả firstLock -> thread kia có cơ hội chạy
            }
            try {
                return true; // critical section giữ cả hai lock
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }

    private static void lockInterruptibly() throws InterruptedException {
        System.out.println("\n=== 3. lockInterruptibly(): huỷ được thread đang chờ lock ===");
        ReentrantLock lock = new ReentrantLock();
        lock.lock(); // main giữ lock rất lâu
        try {
            Thread waiter = new Thread(() -> {
                try {
                    lock.lockInterruptibly(); // synchronized không làm được việc này
                    try {
                        System.out.println("waiter: lấy được lock (không nên thấy)");
                    } finally {
                        lock.unlock();
                    }
                } catch (InterruptedException e) {
                    System.out.println("waiter: bị interrupt trong khi chờ lock -> thoát");
                }
            }, "waiter");
            waiter.start();
            Thread.sleep(200);
            System.out.println("main: số thread đang chờ lock = " + lock.getQueueLength() + ", interrupt waiter");
            waiter.interrupt();
            waiter.join();
        } finally {
            lock.unlock();
        }
    }
}
