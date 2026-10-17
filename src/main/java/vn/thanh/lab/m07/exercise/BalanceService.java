package vn.thanh.lab.m07.exercise;

/** Service giả lập (đã cho sẵn, không phải bài tập): lấy số dư khả dụng. */
@FunctionalInterface
public interface BalanceService {

    long getAvailableBalance(String accountId) throws Exception;
}
