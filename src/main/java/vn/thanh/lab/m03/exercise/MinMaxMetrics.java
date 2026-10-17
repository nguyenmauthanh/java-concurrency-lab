package vn.thanh.lab.m03.exercise;

/**
 * <h2>Bài tập m03: Thống kê min/max thread-safe</h2>
 *
 * Một component metrics nhận số đo (ví dụ latency của request thanh toán, tính bằng micro giây)
 * từ rất nhiều thread, và được đọc định kỳ bởi thread báo cáo.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>{@link #addSample(long)} có thể được gọi đồng thời từ nhiều thread.</li>
 *     <li>{@link #getMin()} / {@link #getMax()} có thể được gọi đồng thời với addSample, từ thread khác.</li>
 *     <li>Chưa có mẫu nào: {@code getMin() == Long.MAX_VALUE}, {@code getMax() == Long.MIN_VALUE}.</li>
 *     <li>Không được "mất" mẫu: sau khi mọi addSample kết thúc, min/max phải đúng tuyệt đối.</li>
 *     <li>Theo thời gian, giá trị {@code getMin()} mà một thread đọc được <b>không bao giờ tăng</b>,
 *         và {@code getMax()} <b>không bao giờ giảm</b> (nếu bạn làm "đọc - so sánh - ghi" không nguyên tử,
 *         một giá trị cũ có thể ghi đè giá trị tốt hơn và vi phạm điều này).</li>
 *     <li>Thao tác đọc nên rẻ (thread báo cáo đọc liên tục) - suy nghĩ về {@code volatile}.</li>
 * </ul>
 *
 * Bạn được tự thêm field. Có thể làm theo nhiều cách: synchronized + volatile, hoặc AtomicLong + CAS
 * (thử cả hai và so sánh!).
 */
public class MinMaxMetrics {

    /** Ghi nhận một mẫu đo mới. */
    public void addSample(long sample) {
        throw new UnsupportedOperationException("TODO");
    }

    /** @return mẫu nhỏ nhất đã ghi nhận, hoặc {@code Long.MAX_VALUE} nếu chưa có mẫu. */
    public long getMin() {
        throw new UnsupportedOperationException("TODO");
    }

    /** @return mẫu lớn nhất đã ghi nhận, hoặc {@code Long.MIN_VALUE} nếu chưa có mẫu. */
    public long getMax() {
        throw new UnsupportedOperationException("TODO");
    }
}
