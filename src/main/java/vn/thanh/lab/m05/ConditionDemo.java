package vn.thanh.lab.m05;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Demo {@link Condition}: tương đương wait/notify nhưng gắn với {@link ReentrantLock},
 * và một lock có thể có NHIỀU condition (mỗi điều kiện chờ một hàng riêng).
 * <p>
 * Kịch bản: các worker đối soát chờ "cổng cut-off" mở. Một worker dùng awaitNanos để không chờ quá lâu.
 */
public class ConditionDemo {

    static class CutoffGate {
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition opened = lock.newCondition();
        private boolean open;

        void awaitOpen() throws InterruptedException {
            lock.lock();
            try {
                while (!open) {
                    opened.await(); // nhả lock, chờ signal
                }
            } finally {
                lock.unlock();
            }
        }

        /** @return true nếu cổng mở trong thời gian chờ */
        boolean awaitOpen(long timeout, TimeUnit unit) throws InterruptedException {
            lock.lock();
            try {
                long remainingNanos = unit.toNanos(timeout);
                while (!open) {
                    if (remainingNanos <= 0) {
                        return false;
                    }
                    remainingNanos = opened.awaitNanos(remainingNanos); // trả về thời gian còn lại
                }
                return true;
            } finally {
                lock.unlock();
            }
        }

        void open() {
            lock.lock();
            try {
                open = true;
                opened.signalAll(); // đánh thức tất cả worker đang chờ
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CutoffGate gate = new CutoffGate();
        List<Thread> workers = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Thread worker = new Thread(() -> {
                try {
                    System.out.println(Thread.currentThread().getName() + ": chờ cut-off...");
                    gate.awaitOpen();
                    System.out.println(Thread.currentThread().getName() + ": cổng mở, bắt đầu đối soát");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "reconcile-" + i);
            workers.add(worker);
        }
        Thread impatient = new Thread(() -> {
            try {
                boolean opened = gate.awaitOpen(200, TimeUnit.MILLISECONDS);
                System.out.println("impatient: " + (opened ? "kịp thấy cổng mở" : "hết 200ms chờ, bỏ qua lượt này"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "impatient");
        workers.add(impatient);

        workers.forEach(Thread::start);
        Thread.sleep(600);
        System.out.println("main: mở cổng cut-off (signalAll)");
        gate.open();
        for (Thread worker : workers) {
            worker.join();
        }
    }
}
