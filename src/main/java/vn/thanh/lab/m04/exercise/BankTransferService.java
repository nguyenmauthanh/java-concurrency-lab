package vn.thanh.lab.m04.exercise;

/**
 * <h2>Bài tập m04: Dịch vụ chuyển tiền không deadlock</h2>
 *
 * Quản lý {@code accountCount} tài khoản (id từ {@code 0} tới {@code accountCount - 1}), mỗi tài khoản
 * bắt đầu với {@code initialBalance}. Nhiều thread gọi {@link #transfer} đồng thời theo mọi chiều
 * (0→1 và 1→0 cùng lúc) – đây là công thức của deadlock nếu khoá ngây thơ.
 *
 * <h3>Yêu cầu</h3>
 * <ul>
 *     <li>Mỗi tài khoản có lock riêng (KHÔNG dùng một lock toàn cục cho mọi transfer - như vậy hết song song).</li>
 *     <li>{@link #transfer} khoá cả tài khoản nguồn và đích; tránh deadlock bằng <b>lock ordering</b>
 *         (luôn khoá tài khoản có id nhỏ hơn trước).</li>
 *     <li>Không đủ tiền -> trả {@code false}, không thay đổi số dư. Chuyển thành công -> {@code true}.</li>
 *     <li>{@code amount <= 0}, id không hợp lệ, hoặc {@code fromAccount == toAccount} -> {@link IllegalArgumentException}.</li>
 *     <li>Số dư không bao giờ âm. Tổng tiền toàn hệ thống luôn được bảo toàn.</li>
 *     <li>{@link #getTotalBalance()} phải trả về một <b>snapshot nhất quán</b> ngay cả khi đang có transfer chạy
 *         (không được đọc giữa lúc tiền đã rời tài khoản nguồn mà chưa tới tài khoản đích).
 *         Gợi ý: khoá tất cả tài khoản – theo đúng thứ tự.</li>
 * </ul>
 * Gợi ý thiết kế: một class lồng {@code Account} với {@code ReentrantLock} và {@code long balance}.
 */
public class BankTransferService {

    public BankTransferService(int accountCount, long initialBalance) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Chuyển {@code amount} từ {@code fromAccount} sang {@code toAccount}.
     *
     * @return {@code true} nếu chuyển thành công, {@code false} nếu tài khoản nguồn không đủ tiền
     */
    public boolean transfer(int fromAccount, int toAccount, long amount) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Số dư hiện tại của một tài khoản. */
    public long getBalance(int accountId) {
        throw new UnsupportedOperationException("TODO");
    }

    /** Tổng số dư mọi tài khoản, là snapshot nhất quán. */
    public long getTotalBalance() {
        throw new UnsupportedOperationException("TODO");
    }
}
