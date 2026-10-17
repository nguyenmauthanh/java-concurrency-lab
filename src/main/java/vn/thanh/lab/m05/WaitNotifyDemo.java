package vn.thanh.lab.m05;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Demo wait/notifyAll: hàng đợi có giới hạn tự viết bằng monitor của object.
 * <p>
 * Quy tắc vàng:
 * <ol>
 *     <li>Chỉ gọi wait/notify khi đang giữ monitor (trong synchronized), nếu không -> IllegalMonitorStateException.</li>
 *     <li>Luôn wait trong vòng {@code while (!condition)} - không dùng {@code if} (spurious wakeup, nhiều consumer).</li>
 *     <li>Ưu tiên notifyAll: notify có thể đánh thức "nhầm" thread (producer đánh thức producer) -> treo.</li>
 * </ol>
 */
public class WaitNotifyDemo {

    static class MessageQueue {
        private final Deque<String> messages = new ArrayDeque<>();
        private final int capacity;

        MessageQueue(int capacity) {
            this.capacity = capacity;
        }

        synchronized void send(String message) throws InterruptedException {
            while (messages.size() == capacity) {
                wait(); // nhả monitor và ngủ; khi tỉnh dậy sẽ lấy lại monitor rồi kiểm tra lại điều kiện
            }
            messages.addLast(message);
            notifyAll(); // báo consumer đang chờ "có hàng"
        }

        synchronized String receive() throws InterruptedException {
            while (messages.isEmpty()) {
                wait();
            }
            String message = messages.removeFirst();
            notifyAll(); // báo producer đang chờ "có chỗ trống"
            return message;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        MessageQueue queue = new MessageQueue(2);
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    String message = "payment-" + i;
                    System.out.println("producer: gửi " + message);
                    queue.send(message);
                }
                queue.send("DONE");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "producer");

        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String message = queue.receive();
                    if (message.equals("DONE")) {
                        System.out.println("consumer: nhận tín hiệu kết thúc");
                        return;
                    }
                    System.out.println("            consumer: xử lý " + message);
                    Thread.sleep(150); // consumer chậm -> producer sẽ bị chặn khi hàng đợi đầy (capacity = 2)
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "consumer");

        producer.start();
        consumer.start();
        producer.join();
        consumer.join();
    }
}
