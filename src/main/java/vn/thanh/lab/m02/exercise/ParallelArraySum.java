package vn.thanh.lab.m02.exercise;

/**
 * <h2>Bài tập m02: Cộng mảng long song song</h2>
 *
 * Cộng tất cả phần tử của mảng {@code long[]} bằng {@code threadCount} {@link Thread} thuần.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li><b>Không</b> dùng stream, parallel stream, ExecutorService, ForkJoinPool. Chỉ {@code new Thread(...)} + {@code join()}.</li>
 *     <li>Chia mảng thành các đoạn liên tiếp gần bằng nhau; mỗi thread cộng một đoạn vào biến cục bộ,
 *         rồi ghi kết quả từng phần. Thread gọi cộng các kết quả từng phần.</li>
 *     <li>{@code threadCount < 1} -> {@link IllegalArgumentException}; {@code values == null} -> {@link NullPointerException}.</li>
 *     <li>{@code threadCount} lớn hơn độ dài mảng vẫn phải chạy đúng (kể cả mảng rỗng -> 0).</li>
 *     <li>Khi tràn số, kết quả phải giống hệt cộng tuần tự (phép cộng long tự quay vòng - không cần xử lý đặc biệt).</li>
 *     <li>Nếu thread gọi bị interrupt khi đang chờ: khôi phục cờ interrupt và ném {@link IllegalStateException}.</li>
 * </ul>
 *
 * Câu hỏi suy nghĩ: vì sao KHÔNG nên để mọi thread cùng cộng dồn vào một biến chung (kể cả là AtomicLong)?
 */
public class ParallelArraySum {

    public long sum(long[] values, int threadCount) {
        throw new UnsupportedOperationException("TODO");
    }
}
