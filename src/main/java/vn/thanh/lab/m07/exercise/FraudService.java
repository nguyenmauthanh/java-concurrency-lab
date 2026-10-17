package vn.thanh.lab.m07.exercise;

/** Service giả lập (đã cho sẵn, không phải bài tập): kiểm tra gian lận, có thể chậm. */
@FunctionalInterface
public interface FraudService {

    /** @return {@code true} nếu giao dịch đáng ngờ */
    boolean isSuspicious(String accountId, long amount) throws Exception;
}
