package vn.thanh.lab.m08.exercise;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * <h2>Bài tập m08: Bulkhead - giới hạn số lời gọi đồng thời xuống hệ thống phía sau</h2>
 *
 * Khi server chuyển sang virtual threads, số request đồng thời không còn bị giới hạn bởi thread pool.
 * 10.000 request cùng lúc gọi xuống core banking (chỉ chịu được 20 kết nối) sẽ đánh sập nó.
 * Bulkhead (giống Resilience4j SemaphoreBulkhead) giới hạn số lời gọi đang chạy cùng lúc.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Dùng {@code java.util.concurrent.Semaphore}.</li>
 *     <li>Tối đa {@code maxConcurrentCalls} lời gọi {@link #execute} chạy {@code call} cùng lúc.</li>
 *     <li>Hết chỗ: chờ tối đa {@code maxWait} (có thể là {@code Duration.ZERO} = không chờ);
 *         quá thời gian -> ném {@link BulkheadFullException} và <b>không</b> chạy {@code call}.</li>
 *     <li>Exception của {@code call} được ném lại nguyên vẹn cho người gọi; dù thành công hay lỗi, chỗ phải được trả lại.</li>
 *     <li>Bị interrupt khi đang chờ chỗ -> ném {@link InterruptedException} (không được nuốt).</li>
 *     <li>{@code maxConcurrentCalls < 1} hoặc {@code maxWait} âm/null -> {@link IllegalArgumentException}.</li>
 *     <li>{@link #availablePermits()} trả về số chỗ còn trống hiện tại.</li>
 * </ul>
 */
public class Bulkhead {

    public Bulkhead(int maxConcurrentCalls, Duration maxWait) {
        throw new UnsupportedOperationException("TODO");
    }

    public <T> T execute(Callable<T> call) throws Exception {
        throw new UnsupportedOperationException("TODO");
    }

    public int availablePermits() {
        throw new UnsupportedOperationException("TODO");
    }
}
