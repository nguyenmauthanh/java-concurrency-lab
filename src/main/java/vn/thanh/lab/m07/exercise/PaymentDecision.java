package vn.thanh.lab.m07.exercise;

/**
 * Kết quả đánh giá thanh toán (đã cho sẵn, không phải bài tập).
 *
 * @param approved {@code true} nếu được duyệt
 * @param reason   một trong các hằng số bên dưới
 */
public record PaymentDecision(boolean approved, String reason) {

    public static final String APPROVED = "APPROVED";
    public static final String FRAUD_SUSPECTED = "FRAUD_SUSPECTED";
    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String SERVICE_ERROR = "SERVICE_ERROR";

    public static PaymentDecision approve() {
        return new PaymentDecision(true, APPROVED);
    }

    public static PaymentDecision reject(String reason) {
        return new PaymentDecision(false, reason);
    }
}
