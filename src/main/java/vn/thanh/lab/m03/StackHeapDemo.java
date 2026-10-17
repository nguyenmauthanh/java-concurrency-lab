package vn.thanh.lab.m03;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo: biến cục bộ (stack) là riêng của từng thread; object trên heap thì được chia sẻ.
 */
public class StackHeapDemo {

    /** Object này nằm trên heap; mọi thread giữ tham chiếu tới nó đều thấy cùng một object. */
    static class SharedBox {
        final List<String> visitors = new ArrayList<>(); // KHÔNG thread-safe, nên phải khoá khi ghi
    }

    public static void main(String[] args) throws InterruptedException {
        SharedBox sharedBox = new SharedBox(); // tham chiếu nằm trên stack của main, object nằm trên heap

        Runnable task = () -> {
            // localCounter nằm trên stack của từng thread -> mỗi thread có bản sao riêng, không cần đồng bộ.
            int localCounter = 0;
            for (int i = 0; i < 1_000; i++) {
                localCounter++;
            }
            synchronized (sharedBox) {
                sharedBox.visitors.add(Thread.currentThread().getName() + " (localCounter=" + localCounter + ")");
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(task, "worker-" + i);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }

        System.out.println("Mỗi thread đếm localCounter trên stack riêng -> luôn = 1000:");
        sharedBox.visitors.forEach(visitor -> System.out.println("  " + visitor));
        System.out.println("Nhưng tất cả cùng ghi vào MỘT SharedBox trên heap -> số phần tử = " + sharedBox.visitors.size());
        System.out.println("""

                Ghi nhớ:
                  Stack (riêng): biến cục bộ, tham số, tham chiếu cục bộ, frame của method.
                  Heap (chung) : mọi object (new ...), mảng, static field, field của object.
                  => Chỉ dữ liệu trên heap mà >1 thread truy cập (và ít nhất 1 thread ghi) mới cần đồng bộ.
                """);
    }
}
