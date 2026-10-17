package vn.thanh.lab.m07.exercise;

import java.time.Duration;

/**
 * <h2>Bài tập m07: Fan-out gọi 3 service song song bằng virtual threads</h2>
 *
 * Trước khi duyệt một giao dịch, cần gọi 3 service độc lập: {@link FraudService}, {@link BalanceService},
 * {@link LimitService}. Gọi tuần tự thì latency = tổng; gọi song song thì latency ≈ service chậm nhất.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Mỗi lần {@link #evaluate} gọi <b>đồng thời</b> cả 3 service, mỗi lời gọi chạy trên một <b>virtual thread</b>
 *         (ví dụ {@code Executors.newVirtualThreadPerTaskExecutor()}). Không dùng StructuredTaskScope (preview ở Java 21).</li>
 *     <li>{@code timeout} là thời gian chờ <b>tổng</b> cho cả 3 lời gọi.
 *         Hết thời gian -> trả {@code PaymentDecision.reject(TIMEOUT)} ngay (không chờ service chậm),
 *         và các lời gọi còn dở phải bị huỷ (interrupt).</li>
 *     <li>Bất kỳ service nào ném exception -> {@code reject(SERVICE_ERROR)}.</li>
 *     <li>Đủ kết quả thì quyết định theo thứ tự ưu tiên:
 *         <ol>
 *             <li>đáng ngờ -> {@code FRAUD_SUSPECTED}</li>
 *             <li>{@code balance < amount} -> {@code INSUFFICIENT_BALANCE}</li>
 *             <li>{@code remainingLimit < amount} -> {@code LIMIT_EXCEEDED}</li>
 *             <li>còn lại -> {@code approve()}</li>
 *         </ol></li>
 *     <li>Thread gọi {@code evaluate} bị interrupt khi đang chờ -> huỷ các lời gọi, khôi phục cờ interrupt,
 *         trả {@code reject(SERVICE_ERROR)}.</li>
 *     <li>Không để rò thread: khi method trả về, không còn virtual thread nào của lần gọi đó đang chạy "vô chủ" quá lâu.</li>
 * </ul>
 *
 * Bẫy: {@code try (var executor = ...)} - {@code close()} sẽ CHỜ mọi task xong. Nếu không huỷ task trước,
 * timeout của bạn sẽ vô nghĩa.
 */
public class PaymentFanOut {

    public PaymentFanOut(FraudService fraudService, BalanceService balanceService, LimitService limitService) {
        throw new UnsupportedOperationException("TODO");
    }

    public PaymentDecision evaluate(String accountId, long amount, Duration timeout) {
        throw new UnsupportedOperationException("TODO");
    }
}
