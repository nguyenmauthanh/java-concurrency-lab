package vn.thanh.lab.m05.exercise;

/**
 * <h2>Bài tập m05a: Bounded buffer với ReentrantLock + 2 Condition</h2>
 *
 * Tự viết lại phiên bản thu nhỏ của {@code ArrayBlockingQueue}.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Dùng <b>một</b> {@code ReentrantLock} và <b>hai</b> {@code Condition}: {@code notFull}, {@code notEmpty}.
 *         Không dùng synchronized/wait/notify, không dùng bất kỳ class nào trong {@code java.util.concurrent}
 *         ngoài package {@code locks}.</li>
 *     <li>Lưu phần tử trong mảng vòng (circular array) kích thước {@code capacity} – không dùng collection có sẵn.</li>
 *     <li>{@link #put(Object)} chặn khi đầy; {@link #take()} chặn khi rỗng; thứ tự FIFO.</li>
 *     <li>Cả hai đều phản hồi interrupt ({@link InterruptedException}) khi đang chờ.</li>
 *     <li>{@code put(null)} -> {@link NullPointerException}; {@code capacity < 1} -> {@link IllegalArgumentException}.</li>
 *     <li>Không được "mất tín hiệu" (lost wakeup): nhiều producer/consumer cùng lúc không bao giờ bị treo.</li>
 * </ul>
 *
 * @param <T> kiểu phần tử
 */
public class BoundedBuffer<T> {

    public BoundedBuffer(int capacity) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Thêm phần tử vào cuối, chờ nếu đầy. */
    public void put(T item) throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }

    /** Lấy phần tử ở đầu, chờ nếu rỗng. */
    public T take() throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }

    /** Số phần tử hiện có. */
    public int size() {
        throw new UnsupportedOperationException("TODO");
    }

    /** Sức chứa tối đa. */
    public int capacity() {
        throw new UnsupportedOperationException("TODO");
    }
}
