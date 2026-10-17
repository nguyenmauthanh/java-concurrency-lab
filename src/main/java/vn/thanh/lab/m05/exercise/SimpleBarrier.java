package vn.thanh.lab.m05.exercise;

/**
 * <h2>Bài tập m05b: Barrier dùng một lần, xây trên Semaphore</h2>
 *
 * {@code parties} thread gọi {@link #await()}; không thread nào đi tiếp cho tới khi
 * thread thứ {@code parties} tới. Khi đó tất cả cùng được thả.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Dùng {@code java.util.concurrent.Semaphore} để chặn/thả thread (cộng thêm lock hoặc biến đếm tuỳ bạn).
 *         Không dùng {@code CyclicBarrier}, {@code CountDownLatch}, {@code Phaser}.</li>
 *     <li>Dùng một lần: sau khi đã "mở", các lần gọi {@code await()} tiếp theo trả về ngay.</li>
 *     <li>{@code parties < 1} -> {@link IllegalArgumentException}. {@code parties == 1} -> await trả về ngay.</li>
 *     <li>Mọi thao tác ghi trước {@code await()} của mỗi thread phải được các thread khác nhìn thấy sau khi chúng qua barrier.</li>
 * </ul>
 */
public class SimpleBarrier {

    public SimpleBarrier(int parties) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Chờ cho tới khi đủ {@code parties} thread cùng gọi await. */
    public void await() throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }
}
