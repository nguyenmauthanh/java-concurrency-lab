package vn.thanh.lab.m01.exercise;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ParallelFactorial {

    public List<BigInteger> computeFactorials(List<Long> inputs, long timeoutMillis) throws InterruptedException {
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("timeoutMillis phải > 0");
        }
        for (Long input : inputs) {
            if (input == null || input < 0) {
                throw new IllegalArgumentException("Không tính giai thừa cho số âm/null: " + input);
            }
        }

        // Mỗi worker chỉ ghi vào ô results[i] của nó -> không có hai thread ghi cùng một ô.
        BigInteger[] results = new BigInteger[inputs.size()];
        List<Thread> workers = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            int index = i;
            long n = inputs.get(i);
            Thread worker = new Thread(() -> {
                BigInteger value = factorial(n);
                if (value != null) {
                    results[index] = value;
                }
            }, "factorial-" + n);
            worker.setDaemon(true);
            workers.add(worker);
        }
        workers.forEach(Thread::start);

        long deadline = System.nanoTime() + timeoutMillis * 1_000_000;
        try {
            for (Thread worker : workers) {
                long remainingMillis = (deadline - System.nanoTime()) / 1_000_000;
                if (remainingMillis <= 0) {
                    break;
                }
                worker.join(remainingMillis);
            }
        } catch (InterruptedException e) {
            workers.forEach(Thread::interrupt);
            Thread.currentThread().interrupt();
            throw e;
        }

        List<BigInteger> output = new ArrayList<>(Arrays.asList(new BigInteger[inputs.size()]));
        for (int i = 0; i < workers.size(); i++) {
            Thread worker = workers.get(i);
            if (worker.isAlive()) {
                worker.interrupt(); // chưa xong -> dừng worker, trả null
            } else {
                // join() đã trả về và thread đã kết thúc => mọi ghi của worker happens-before đọc ở đây.
                output.set(i, results[i]);
            }
        }
        return output;
    }

    /** Trả về null nếu bị interrupt giữa chừng. */
    private static BigInteger factorial(long n) {
        BigInteger result = BigInteger.ONE;
        for (long i = 2; i <= n; i++) {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }
}
