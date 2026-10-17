package vn.thanh.lab.m04.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankTransferServiceTest {

    @Test
    void basicTransfer() {
        BankTransferService service = new BankTransferService(3, 100);
        assertTrue(service.transfer(0, 1, 30));
        assertEquals(70, service.getBalance(0));
        assertEquals(130, service.getBalance(1));
        assertEquals(100, service.getBalance(2));
        assertEquals(300, service.getTotalBalance());
    }

    @Test
    void insufficientFundsReturnsFalseAndKeepsBalances() {
        BankTransferService service = new BankTransferService(2, 100);
        assertFalse(service.transfer(0, 1, 101));
        assertEquals(100, service.getBalance(0));
        assertEquals(100, service.getBalance(1));
        assertTrue(service.transfer(0, 1, 100));
        assertEquals(0, service.getBalance(0));
    }

    @Test
    void invalidArguments() {
        BankTransferService service = new BankTransferService(2, 100);
        assertThrows(IllegalArgumentException.class, () -> service.transfer(0, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(0, 1, -5));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(0, 2, 10));
        assertThrows(IllegalArgumentException.class, () -> service.transfer(-1, 1, 10));
    }

    @RepeatedTest(5)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Hai thread chuyển chéo 0->1 và 1->0 liên tục: không deadlock")
    void pingPongTransfersDoNotDeadlock() throws InterruptedException {
        BankTransferService service = new BankTransferService(2, 1_000);
        CountDownLatch startGate = new CountDownLatch(1);
        Thread forward = new Thread(() -> repeatTransfer(service, startGate, 0, 1, 200_000));
        Thread backward = new Thread(() -> repeatTransfer(service, startGate, 1, 0, 200_000));
        forward.setDaemon(true);
        backward.setDaemon(true);
        forward.start();
        backward.start();
        startGate.countDown();
        forward.join(5_000);
        backward.join(5_000);
        assertFalse(forward.isAlive() || backward.isAlive(), "Thread vẫn treo -> nhiều khả năng deadlock");
        assertEquals(2_000, service.getTotalBalance());
    }

    @RepeatedTest(5)
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("16 thread chuyển ngẫu nhiên giữa 8 tài khoản: tổng tiền bảo toàn, không âm, auditor luôn thấy tổng đúng")
    void randomCrossTransfersPreserveTotal() throws InterruptedException {
        int accounts = 8;
        long initialBalance = 10_000;
        long expectedTotal = accounts * initialBalance;
        BankTransferService service = new BankTransferService(accounts, initialBalance);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicBoolean transfersDone = new AtomicBoolean(false);
        AtomicReference<String> violation = new AtomicReference<>();

        Thread auditor = new Thread(() -> {
            awaitQuietly(startGate);
            while (!transfersDone.get() && violation.get() == null) {
                long total = service.getTotalBalance();
                if (total != expectedTotal) {
                    violation.compareAndSet(null, "getTotalBalance() = " + total + " trong khi kỳ vọng " + expectedTotal);
                }
                for (int id = 0; id < accounts; id++) {
                    if (service.getBalance(id) < 0) {
                        violation.compareAndSet(null, "Tài khoản " + id + " bị âm");
                    }
                }
            }
        });
        auditor.setDaemon(true);
        auditor.start();

        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < 16; t++) {
            Thread worker = new Thread(() -> {
                awaitQuietly(startGate);
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < 50_000; i++) {
                    int from = random.nextInt(accounts);
                    int to = (from + 1 + random.nextInt(accounts - 1)) % accounts;
                    service.transfer(from, to, random.nextLong(1, 3_000));
                }
            });
            worker.setDaemon(true);
            workers.add(worker);
            worker.start();
        }
        startGate.countDown();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        for (Thread worker : workers) {
            worker.join(Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())));
            assertFalse(worker.isAlive(), "Worker vẫn treo -> nhiều khả năng deadlock");
        }
        transfersDone.set(true);
        auditor.join(5_000);

        assertNull(violation.get(), violation.get());
        assertEquals(expectedTotal, service.getTotalBalance());
        long sum = 0;
        for (int id = 0; id < accounts; id++) {
            assertTrue(service.getBalance(id) >= 0);
            sum += service.getBalance(id);
        }
        assertEquals(expectedTotal, sum);
    }

    private static void repeatTransfer(BankTransferService service, CountDownLatch startGate, int from, int to, int times) {
        awaitQuietly(startGate);
        for (int i = 0; i < times; i++) {
            service.transfer(from, to, 1);
        }
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
