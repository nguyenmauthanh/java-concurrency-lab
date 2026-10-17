package vn.thanh.lab.m03.exercise;

/**
 * Cách 1 (theo khoá học): ghi trong synchronized, đọc qua volatile không khoá.
 * Cách 2 (lock-free) ở cuối file, trong comment.
 */
public class MinMaxMetrics {

    // volatile: thread đọc luôn thấy giá trị mới nhất mà không cần lấy lock.
    // Ghi long/double volatile là nguyên tử (long thường thì JLS không bảo đảm trên JVM 32-bit).
    private volatile long min = Long.MAX_VALUE;
    private volatile long max = Long.MIN_VALUE;

    public void addSample(long sample) {
        // "đọc - so sánh - ghi" phải nguyên tử -> khoá. Cùng một lock cho cả min và max.
        synchronized (this) {
            if (sample < min) {
                min = sample;
            }
            if (sample > max) {
                max = sample;
            }
        }
    }

    public long getMin() {
        return min;
    }

    public long getMax() {
        return max;
    }

    /*
     * Cách 2 – lock-free với AtomicLong:
     *
     * private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
     * private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);
     *
     * public void addSample(long sample) {
     *     min.accumulateAndGet(sample, Math::min); // vòng CAS bên trong: chỉ ghi nếu không ai đổi giữa chừng
     *     max.accumulateAndGet(sample, Math::max);
     * }
     * public long getMin() { return min.get(); }
     * public long getMax() { return max.get(); }
     */
}
