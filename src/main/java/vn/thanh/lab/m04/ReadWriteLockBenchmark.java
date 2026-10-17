package vn.thanh.lab.m04;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * So sánh hiệu năng khi có NHIỀU READER, ÍT WRITER:
 * synchronized (độc quyền cho cả đọc) vs {@link ReentrantReadWriteLock} (nhiều reader cùng lúc).
 * <p>
 * Kịch bản: kho hàng (giá -> số lượng). Reader đếm số mặt hàng trong một khoảng giá; writer thêm/bớt hàng.
 */
public class ReadWriteLockBenchmark {

    private static final int READERS = 7;
    private static final int READS_PER_READER = 20_000;
    private static final int WRITES = 2_000;
    private static final int MAX_PRICE = 1_000;

    interface Inventory {
        int countItemsInPriceRange(int lowerBound, int upperBound);

        void addItem(int price);
    }

    static class SynchronizedInventory implements Inventory {
        private final NavigableMap<Integer, Integer> priceToCount = new TreeMap<>();

        public synchronized int countItemsInPriceRange(int lowerBound, int upperBound) {
            int total = 0;
            for (int count : priceToCount.subMap(lowerBound, true, upperBound, true).values()) {
                total += count;
            }
            return total;
        }

        public synchronized void addItem(int price) {
            priceToCount.merge(price, 1, Integer::sum);
        }
    }

    static class ReadWriteLockInventory implements Inventory {
        private final NavigableMap<Integer, Integer> priceToCount = new TreeMap<>();
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        public int countItemsInPriceRange(int lowerBound, int upperBound) {
            readWriteLock.readLock().lock(); // nhiều reader giữ cùng lúc được
            try {
                int total = 0;
                for (int count : priceToCount.subMap(lowerBound, true, upperBound, true).values()) {
                    total += count;
                }
                return total;
            } finally {
                readWriteLock.readLock().unlock();
            }
        }

        public void addItem(int price) {
            readWriteLock.writeLock().lock(); // độc quyền: chặn mọi reader và writer khác
            try {
                priceToCount.merge(price, 1, Integer::sum);
            } finally {
                readWriteLock.writeLock().unlock();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.printf("%d reader x %,d lần đọc, 1 writer x %,d lần ghi%n", READERS, READS_PER_READER, WRITES);
        for (int round = 1; round <= 3; round++) { // vòng 1 coi như warm-up
            System.out.println("\nVòng " + round + (round == 1 ? " (warm-up)" : ""));
            System.out.printf("  synchronized          : %6d ms%n", run(new SynchronizedInventory()));
            System.out.printf("  ReentrantReadWriteLock: %6d ms%n", run(new ReadWriteLockInventory()));
        }
        System.out.println("""

                Nhận xét: khi đọc chiếm đa số và đoạn đọc đủ "nặng", ReadWriteLock cho phép reader chạy song song nên nhanh hơn.
                Nếu ghi nhiều hoặc đoạn đọc rất ngắn, chi phí quản lý RW lock có thể khiến nó CHẬM hơn synchronized.
                """);
    }

    private static long run(Inventory inventory) throws InterruptedException {
        for (int i = 0; i < 100_000; i++) {
            inventory.addItem(ThreadLocalRandom.current().nextInt(MAX_PRICE));
        }
        Thread writer = new Thread(() -> {
            for (int i = 0; i < WRITES; i++) {
                inventory.addItem(ThreadLocalRandom.current().nextInt(MAX_PRICE));
                try {
                    Thread.sleep(0, 100_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        Thread[] readers = new Thread[READERS];
        for (int r = 0; r < READERS; r++) {
            readers[r] = new Thread(() -> {
                for (int i = 0; i < READS_PER_READER; i++) {
                    int lowerBound = ThreadLocalRandom.current().nextInt(MAX_PRICE / 2);
                    inventory.countItemsInPriceRange(lowerBound, lowerBound + 200);
                }
            });
        }
        long start = System.nanoTime();
        writer.start();
        for (Thread reader : readers) {
            reader.start();
        }
        for (Thread reader : readers) {
            reader.join();
        }
        writer.join();
        return (System.nanoTime() - start) / 1_000_000;
    }
}
