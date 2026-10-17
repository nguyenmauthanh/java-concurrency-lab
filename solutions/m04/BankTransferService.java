package vn.thanh.lab.m04.exercise;

import java.util.concurrent.locks.ReentrantLock;

public class BankTransferService {

    private static final class Account {
        final int id;
        final ReentrantLock lock = new ReentrantLock();
        long balance; // chỉ đọc/ghi khi đang giữ lock -> không cần volatile

        Account(int id, long balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    private final Account[] accounts; // mảng final, không đổi sau constructor -> publish an toàn

    public BankTransferService(int accountCount, long initialBalance) {
        if (accountCount < 1 || initialBalance < 0) {
            throw new IllegalArgumentException("accountCount phải >= 1 và initialBalance >= 0");
        }
        accounts = new Account[accountCount];
        for (int i = 0; i < accountCount; i++) {
            accounts[i] = new Account(i, initialBalance);
        }
    }

    public boolean transfer(int fromAccount, int toAccount, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount phải > 0");
        }
        if (fromAccount == toAccount) {
            throw new IllegalArgumentException("Không chuyển cho chính mình");
        }
        Account from = account(fromAccount);
        Account to = account(toAccount);

        // LOCK ORDERING: luôn khoá id nhỏ trước, bất kể chiều chuyển tiền.
        Account first = from.id < to.id ? from : to;
        Account second = from.id < to.id ? to : from;
        first.lock.lock();
        try {
            second.lock.lock();
            try {
                if (from.balance < amount) {
                    return false;
                }
                from.balance -= amount;
                to.balance += amount;
                return true;
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }

    public long getBalance(int accountId) {
        Account account = account(accountId);
        account.lock.lock(); // cần lock để có visibility (balance không volatile)
        try {
            return account.balance;
        } finally {
            account.lock.unlock();
        }
    }

    public long getTotalBalance() {
        // Khoá TẤT CẢ theo thứ tự id tăng dần (cùng thứ tự với transfer) -> snapshot nhất quán, không deadlock.
        int locked = 0;
        try {
            for (Account account : accounts) {
                account.lock.lock();
                locked++;
            }
            long total = 0;
            for (Account account : accounts) {
                total += account.balance;
            }
            return total;
        } finally {
            for (int i = locked - 1; i >= 0; i--) {
                accounts[i].lock.unlock();
            }
        }
    }

    private Account account(int id) {
        if (id < 0 || id >= accounts.length) {
            throw new IllegalArgumentException("Tài khoản không tồn tại: " + id);
        }
        return accounts[id];
    }
}
