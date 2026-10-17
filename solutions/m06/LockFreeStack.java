package vn.thanh.lab.m06.exercise;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class LockFreeStack<T> {

    /** Node bất biến: một khi đã publish thì không ai sửa được next -> an toàn khi đọc không khoá. */
    private static final class Node<T> {
        final T value;
        final Node<T> next;

        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }

    private final AtomicReference<Node<T>> head = new AtomicReference<>();

    public void push(T value) {
        Objects.requireNonNull(value, "value");
        while (true) {
            Node<T> currentHead = head.get();
            Node<T> newHead = new Node<>(value, currentHead);
            if (head.compareAndSet(currentHead, newHead)) {
                return; // thành công: không ai đổi head kể từ lúc ta đọc
            }
            Thread.onSpinWait(); // gợi ý CPU (lệnh PAUSE) khi spin; có thể thêm backoff
        }
    }

    public T pop() {
        while (true) {
            Node<T> currentHead = head.get();
            if (currentHead == null) {
                return null;
            }
            if (head.compareAndSet(currentHead, currentHead.next)) {
                return currentHead.value;
            }
            Thread.onSpinWait();
        }
    }

    public T peek() {
        Node<T> currentHead = head.get();
        return currentHead == null ? null : currentHead.value;
    }

    public boolean isEmpty() {
        return head.get() == null;
    }
}
