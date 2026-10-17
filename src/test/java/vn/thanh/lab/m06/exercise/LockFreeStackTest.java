package vn.thanh.lab.m06.exercise;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LockFreeStackTest {

    @Test
    void lifoSingleThread() {
        LockFreeStack<String> stack = new LockFreeStack<>();
        assertTrue(stack.isEmpty());
        assertNull(stack.pop());
        assertNull(stack.peek());
        stack.push("a");
        stack.push("b");
        stack.push("c");
        assertFalse(stack.isEmpty());
        assertEquals("c", stack.peek());
        assertEquals("c", stack.pop());
        assertEquals("b", stack.pop());
        assertEquals("a", stack.pop());
        assertNull(stack.pop());
        assertTrue(stack.isEmpty());
        assertThrows(NullPointerException.class, () -> stack.push(null));
    }

    @Test
    @DisplayName("Không dùng lock: không có method synchronized, không có field Lock, có AtomicReference")
    void implementationIsLockFree() {
        new LockFreeStack<Integer>().push(1); // phải chạy được trước đã
        for (Method method : LockFreeStack.class.getDeclaredMethods()) {
            assertFalse(Modifier.isSynchronized(method.getModifiers()), "Method synchronized: " + method.getName());
        }
        boolean hasAtomicReference = false;
        for (Field field : LockFreeStack.class.getDeclaredFields()) {
            assertFalse(Lock.class.isAssignableFrom(field.getType()), "Field kiểu Lock: " + field.getName());
            hasAtomicReference |= AtomicReference.class.isAssignableFrom(field.getType());
        }
        assertTrue(hasAtomicReference, "Kỳ vọng đỉnh stack là một AtomicReference");
    }

    @RepeatedTest(5)
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    @DisplayName("Push đồng thời từ 8 thread: không mất phần tử")
    void concurrentPushesAreNotLost() throws InterruptedException {
        int threads = 8;
        int perThread = 100_000;
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        runConcurrently(threads, index -> {
            for (int i = 0; i < perThread; i++) {
                stack.push(index * perThread + i);
            }
        });
        AtomicIntegerArray seen = new AtomicIntegerArray(threads * perThread);
        Integer value;
        int count = 0;
        while ((value = stack.pop()) != null) {
            assertEquals(1, seen.incrementAndGet(value), "Phần tử trùng: " + value);
            count++;
        }
        assertEquals(threads * perThread, count);
    }

    @RepeatedTest(10)
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Push/pop xen kẽ từ 8 thread: mỗi phần tử được pop đúng một lần")
    void concurrentPushAndPop() throws InterruptedException {
        int threads = 8;
        int perThread = 100_000;
        int total = threads * perThread;
        LockFreeStack<Integer> stack = new LockFreeStack<>();
        AtomicIntegerArray popCount = new AtomicIntegerArray(total);
        runConcurrently(threads, index -> {
            for (int i = 0; i < perThread; i++) {
                stack.push(index * perThread + i);
                if (i % 2 == 1) { // pop 2 phần tử sau mỗi 2 lần push -> stack liên tục rỗng/đầy, tranh chấp cao
                    for (int k = 0; k < 2; k++) {
                        Integer popped = stack.pop();
                        if (popped != null) {
                            popCount.incrementAndGet(popped);
                        }
                    }
                }
            }
        });
        Integer remaining;
        while ((remaining = stack.pop()) != null) {
            popCount.incrementAndGet(remaining);
        }
        for (int i = 0; i < total; i++) {
            assertEquals(1, popCount.get(i), "Phần tử " + i + " bị pop " + popCount.get(i) + " lần");
        }
    }

    private interface IndexedTask {
        void run(int index);
    }

    private static void runConcurrently(int threadCount, IndexedTask task) throws InterruptedException {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        for (int t = 0; t < threadCount; t++) {
            int index = t;
            Thread thread = new Thread(() -> {
                try {
                    startGate.await();
                    task.run(index);
                } catch (Throwable e) {
                    failures.add(e);
                }
            });
            thread.setDaemon(true);
            threads.add(thread);
            thread.start();
        }
        startGate.countDown();
        for (Thread thread : threads) {
            thread.join(20_000);
            assertFalse(thread.isAlive(), "Thread bị treo");
        }
        if (!failures.isEmpty()) {
            Throwable first = failures.get(0);
            if (first instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new AssertionError(first);
        }
    }
}
