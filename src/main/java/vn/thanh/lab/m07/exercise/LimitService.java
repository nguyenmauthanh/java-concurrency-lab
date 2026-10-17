package vn.thanh.lab.m07.exercise;

/** Service giả lập (đã cho sẵn, không phải bài tập): lấy hạn mức giao dịch còn lại trong ngày. */
@FunctionalInterface
public interface LimitService {

    long getRemainingDailyLimit(String accountId) throws Exception;
}
