package vn.thanh.lab.m05.exercise;

import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedBuffer<T> {

    private final Object[] items;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull = lock.newCondition();  // producer chờ ở đây
    private final Condition notEmpty = lock.newCondition(); // consumer chờ ở đây
    private int head;  // vị trí take tiếp theo
    private int tail;  // vị trí put tiếp theo
    private int count; // mọi truy cập đều trong lock -> không cần volatile

    public BoundedBuffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity phải >= 1");
        }
        items = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        Objects.requireNonNull(item, "item");
        lock.lockInterruptibly();
        try {
            while (count == items.length) { // while, không phải if
                notFull.await();
            }
            items[tail] = item;
            tail = (tail + 1) % items.length;
            count++;
            notEmpty.signal(); // chỉ cần đánh thức MỘT consumer: chỉ có thêm đúng một phần tử
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) {
                notEmpty.await();
            }
            T item = (T) items[head];
            items[head] = null; // tránh giữ tham chiếu -> memory leak
            head = (head + 1) % items.length;
            count--;
            notFull.signal(); // có thêm đúng một chỗ trống -> đánh thức một producer
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }

    public int capacity() {
        return items.length;
    }
}
