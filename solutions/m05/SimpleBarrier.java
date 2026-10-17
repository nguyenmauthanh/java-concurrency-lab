package vn.thanh.lab.m05.exercise;

import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

public class SimpleBarrier {

    private final int parties;
    private final Semaphore gate = new Semaphore(0); // 0 permit: mọi acquire đều chặn cho tới khi được release
    private final ReentrantLock lock = new ReentrantLock();
    private int arrived;     // được bảo vệ bởi lock
    private boolean tripped; // được bảo vệ bởi lock

    public SimpleBarrier(int parties) {
        if (parties < 1) {
            throw new IllegalArgumentException("parties phải >= 1");
        }
        this.parties = parties;
    }

    public void await() throws InterruptedException {
        boolean last;
        lock.lock();
        try {
            if (tripped) {
                return; // dùng một lần: đã mở thì đi qua luôn
            }
            arrived++;
            last = arrived == parties;
            if (last) {
                tripped = true;
            }
        } finally {
            lock.unlock(); // KHÔNG được acquire semaphore khi đang giữ lock (thread cuối sẽ không vào được)
        }

        if (last) {
            gate.release(parties - 1); // thả đúng N-1 thread đang (hoặc sắp) chờ
        } else {
            gate.acquire();
        }
    }
}
