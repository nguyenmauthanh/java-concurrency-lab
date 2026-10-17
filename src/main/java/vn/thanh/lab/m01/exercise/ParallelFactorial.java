package vn.thanh.lab.m01.exercise;

import java.math.BigInteger;
import java.util.List;

/**
 * <h2>Bài tập m01: Tính giai thừa song song</h2>
 *
 * Tính giai thừa ({@link BigInteger}) của nhiều số, <b>mỗi số chạy trên một {@link Thread} riêng</b>
 * (không dùng ExecutorService, không dùng parallel stream).
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Kết quả trả về theo <b>đúng thứ tự</b> của {@code inputs}: phần tử thứ i là {@code inputs.get(i)!}.</li>
 *     <li>{@code 0! = 1}. Số âm trong {@code inputs} -> ném {@link IllegalArgumentException} (trước khi tạo thread).</li>
 *     <li>{@code timeoutMillis} là <b>tổng</b> thời gian chờ cho cả lô (không phải mỗi thread một timeout riêng).
 *         Dùng {@link Thread#join(long)} với thời gian còn lại.</li>
 *     <li>Số nào chưa tính xong khi hết thời gian thì phần tử tương ứng là {@code null},
 *         và thread đó phải được {@link Thread#interrupt() interrupt}. Vòng lặp tính toán phải kiểm tra cờ interrupt
 *         để dừng sớm (không được tiếp tục ngốn CPU sau khi method đã trả về).</li>
 *     <li>Các thread worker nên là daemon để không giữ JVM sống.</li>
 *     <li>Nếu thread gọi method bị interrupt trong lúc chờ: interrupt hết worker, khôi phục cờ interrupt
 *         rồi ném lại {@link InterruptedException}.</li>
 * </ul>
 *
 * Gợi ý: lưu kết quả vào một mảng / list mà mỗi worker chỉ ghi vào ô của chính nó. Nghĩ xem kết quả ghi
 * trong worker có chắc chắn "nhìn thấy" được ở thread gọi sau khi {@code join()} trả về không (happens-before).
 */
public class ParallelFactorial {

    /**
     * @param inputs        danh sách số cần tính giai thừa (không null, có thể rỗng)
     * @param timeoutMillis tổng thời gian chờ tối đa (ms), phải > 0
     * @return danh sách cùng kích thước với {@code inputs}; phần tử {@code null} nghĩa là tính không kịp
     */
    public List<BigInteger> computeFactorials(List<Long> inputs, long timeoutMillis) throws InterruptedException {
        throw new UnsupportedOperationException("TODO");
    }
}
