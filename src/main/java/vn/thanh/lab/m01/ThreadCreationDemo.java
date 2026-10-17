package vn.thanh.lab.m01;

/**
 * Demo các cách tạo thread: kế thừa Thread, truyền Runnable, lambda;
 * đặt tên, priority và UncaughtExceptionHandler.
 */
public class ThreadCreationDemo {

    // Cách 1: kế thừa Thread. Ít dùng vì Java chỉ cho kế thừa một class và trộn "việc cần làm" với "cơ chế chạy".
    static class GreetingThread extends Thread {
        GreetingThread() {
            super("greeting-thread");
        }

        @Override
        public void run() {
            System.out.println("[1] extends Thread  -> đang chạy trên " + Thread.currentThread().getName());
        }
    }

    // Cách 2: Runnable tách riêng "việc cần làm" khỏi Thread.
    static class PrintTask implements Runnable {
        @Override
        public void run() {
            System.out.println("[2] Runnable        -> đang chạy trên " + Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("main chạy trên thread: " + Thread.currentThread().getName());

        Thread first = new GreetingThread();
        Thread second = new Thread(new PrintTask(), "runnable-thread");
        // Cách 3: lambda (Runnable là functional interface) - cách phổ biến nhất.
        Thread third = new Thread(() ->
                System.out.println("[3] lambda          -> đang chạy trên " + Thread.currentThread().getName()),
                "lambda-thread");

        // Bẫy: gọi run() thay vì start() sẽ chạy NGAY TRÊN thread hiện tại, không tạo thread mới.
        System.out.print("Gọi run() trực tiếp: ");
        third.run();

        first.start();
        second.start();
        third.start();
        first.join();
        second.join();
        third.join();

        // Priority chỉ là "gợi ý" cho scheduler của OS, không có bảo đảm gì.
        Thread lowPriority = new Thread(() -> {}, "low-priority");
        lowPriority.setPriority(Thread.MIN_PRIORITY);
        Thread highPriority = new Thread(() -> {}, "high-priority");
        highPriority.setPriority(Thread.MAX_PRIORITY);
        System.out.printf("%nPriority: %s=%d, %s=%d, main=%d (chỉ là gợi ý cho OS)%n",
                lowPriority.getName(), lowPriority.getPriority(),
                highPriority.getName(), highPriority.getPriority(),
                Thread.currentThread().getPriority());

        // UncaughtExceptionHandler: exception thoát khỏi run() KHÔNG lan ra thread cha,
        // chỉ có handler này "bắt" được nó.
        Thread failing = new Thread(() -> {
            throw new IllegalStateException("Lỗi cố ý trong worker");
        }, "failing-worker");
        failing.setUncaughtExceptionHandler((thread, error) ->
                System.out.printf("%nUncaughtExceptionHandler: thread '%s' chết vì %s: %s%n",
                        thread.getName(), error.getClass().getSimpleName(), error.getMessage()));
        failing.start();
        failing.join();

        System.out.println("main vẫn sống bình thường sau khi worker chết -> exception không lan sang main.");
    }
}
