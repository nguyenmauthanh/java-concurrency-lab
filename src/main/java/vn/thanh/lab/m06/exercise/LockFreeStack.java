package vn.thanh.lab.m06.exercise;

/**
 * <h2>Bài tập m06: Lock-free stack (Treiber stack)</h2>
 *
 * Stack LIFO thread-safe <b>không dùng lock</b>: đỉnh stack là một {@code AtomicReference<Node<T>>},
 * push/pop dùng vòng lặp compare-and-set.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Không dùng {@code synchronized}, {@code Lock}, hay collection concurrent có sẵn.</li>
 *     <li>Dùng {@code java.util.concurrent.atomic.AtomicReference} cho đỉnh stack và một class {@code Node} bất biến
 *         ({@code final T value; final Node<T> next}).</li>
 *     <li>{@link #push(Object)}: {@code null} -> {@link NullPointerException}.</li>
 *     <li>{@link #pop()}: trả về và gỡ phần tử ở đỉnh; stack rỗng -> trả {@code null} (không chặn, không ném exception).</li>
 *     <li>{@link #peek()}: trả về phần tử ở đỉnh nhưng không gỡ, rỗng -> {@code null}.</li>
 *     <li>Dưới tranh chấp cao, không phần tử nào bị mất hoặc bị pop hai lần.</li>
 * </ul>
 *
 * Câu hỏi suy nghĩ: vì sao trong Java (có GC) Treiber stack không bị lỗi ABA, trong khi bản C/C++ tái sử dụng node thì bị?
 *
 * @param <T> kiểu phần tử
 */
public class LockFreeStack<T> {

    public void push(T value) {
        throw new UnsupportedOperationException("TODO");
    }

    public T pop() {
        throw new UnsupportedOperationException("TODO");
    }

    public T peek() {
        throw new UnsupportedOperationException("TODO");
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException("TODO");
    }
}
