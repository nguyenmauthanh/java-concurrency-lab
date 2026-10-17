package vn.thanh.lab.m06;

import vn.thanh.lab.m06.exercise.LockFreeStack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * So sánh hiệu năng stack blocking (synchronized) với {@link LockFreeStack} - LÀ BÀI TẬP CỦA BẠN.
 * Nếu chưa làm bài tập, demo chỉ chạy phần blocking và nhắc bạn làm.
 */
public class StackBenchmarkDemo {

    private static final int THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 1_000_000;

    interface IntStack {
        void push(Integer value);

        Integer pop();
    }

    static class SynchronizedStack implements IntStack {
        private final Deque<Integer> items = new ArrayDeque<>();

        public synchronized void push(Integer value) {
            items.push(value);
        }

        public synchronized Integer pop() {
            return items.poll();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("%d thread, mỗi thread %,d cặp push/pop%n", THREADS, OPERATIONS_PER_THREAD);
        boolean lockFreeAvailable = true;
        for (int round = 1; round <= 3; round++) {
            System.out.println("\nVòng " + round + (round == 1 ? " (warm-up)" : ""));
            System.out.printf("  SynchronizedStack: %6d ms%n", run(new SynchronizedStack()));
            if (!lockFreeAvailable) {
                continue;
            }
            try {
                LockFreeStack<Integer> lockFree = new LockFreeStack<>();
                System.out.printf("  LockFreeStack    : %6d ms%n", run(new IntStack() {
                    public void push(Integer value) {
                        lockFree.push(value);
                    }

                    public Integer pop() {
                        return lockFree.pop();
                    }
                }));
            } catch (UnsupportedOperationException e) {
                lockFreeAvailable = false;
                System.out.println("  LockFreeStack    : CHƯA LÀM - hãy làm bài tập vn.thanh.lab.m06.exercise.LockFreeStack rồi chạy lại");
            }
        }
        System.out.println("""

                Lưu ý khi đọc kết quả: lock-free KHÔNG phải lúc nào cũng nhanh hơn. Với tranh chấp cực cao trên một điểm duy nhất
                (đỉnh stack), CAS thất bại và thử lại liên tục; còn intrinsic lock của JVM đã được tối ưu rất tốt.
                Lợi ích lớn nhất của lock-free: không bị deadlock, không bị "kẹt" khi thread giữ lock bị deschedule.
                """);
    }

    private static long run(IntStack stack) throws InterruptedException {
        Thread[] threads = new Thread[THREADS];
        RuntimeException[] failure = new RuntimeException[1];
        long start = System.nanoTime();
        for (int t = 0; t < THREADS; t++) {
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        stack.push(i);
                        stack.pop();
                    }
                } catch (RuntimeException e) {
                    failure[0] = e;
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        if (failure[0] != null) {
            throw failure[0];
        }
        return (System.nanoTime() - start) / 1_000_000;
    }
}
