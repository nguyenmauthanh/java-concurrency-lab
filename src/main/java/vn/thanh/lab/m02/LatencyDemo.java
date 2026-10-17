package vn.thanh.lab.m02;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo tối ưu LATENCY: chia MỘT công việc lớn (xử lý mảng "pixel") cho N thread và đo thời gian hoàn thành.
 * <p>
 * Quan sát: thời gian giảm gần tuyến tính tới khoảng số core vật lý, sau đó chững lại hoặc tăng nhẹ
 * (chi phí tạo thread, context switch, băng thông bộ nhớ).
 */
public class LatencyDemo {

    private static final int PIXEL_COUNT = 16_000_000;
    private static final int RUNS_PER_CONFIG = 3;

    public static void main(String[] args) throws InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        int[] source = new int[PIXEL_COUNT];
        for (int i = 0; i < source.length; i++) {
            source[i] = i * 31; // dữ liệu giả, không quan trọng
        }
        int[] target = new int[PIXEL_COUNT];

        System.out.printf("Xử lý %,d pixel, máy có %d logical core%n", PIXEL_COUNT, cores);
        // Warm-up để JIT biên dịch xong trước khi đo, nếu không kết quả lần đầu sẽ rất "xấu".
        process(source, target, cores);
        process(source, target, 1);

        List<Integer> threadCounts = new ArrayList<>(List.of(1, 2, 4, 8, 16, 32, 64));
        threadCounts.removeIf(count -> count > cores * 4);
        if (!threadCounts.contains(cores)) {
            threadCounts.add(cores);
            threadCounts.sort(Integer::compare);
        }

        double baseline = 0;
        System.out.println(" threads | thời gian (ms) | speed-up");
        System.out.println("---------+----------------+---------");
        for (int threadCount : threadCounts) {
            long[] samples = new long[RUNS_PER_CONFIG];
            for (int run = 0; run < RUNS_PER_CONFIG; run++) {
                long start = System.nanoTime();
                process(source, target, threadCount);
                samples[run] = System.nanoTime() - start;
            }
            Arrays.sort(samples);
            double medianMs = samples[RUNS_PER_CONFIG / 2] / 1_000_000.0;
            if (threadCount == 1) {
                baseline = medianMs;
            }
            System.out.printf(" %7d | %14.1f | %6.2fx%n", threadCount, medianMs, baseline / medianMs);
        }
        System.out.println("\nNhận xét: speed-up tăng tới khoảng số core rồi chững lại -> thêm thread không còn giảm latency.");
    }

    /** Chia mảng thành threadCount đoạn liên tiếp, mỗi thread xử lý một đoạn. */
    private static void process(int[] source, int[] target, int threadCount) throws InterruptedException {
        Thread[] threads = new Thread[threadCount];
        int chunk = (source.length + threadCount - 1) / threadCount;
        for (int t = 0; t < threadCount; t++) {
            int from = Math.min(source.length, t * chunk);
            int to = Math.min(source.length, from + chunk);
            threads[t] = new Thread(() -> {
                for (int i = from; i < to; i++) {
                    target[i] = recolor(source[i]);
                }
            });
            threads[t].start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
    }

    /** Giả lập một phép biến đổi màu tốn chút CPU. */
    private static int recolor(int pixel) {
        int red = (pixel >> 16) & 0xFF;
        int green = (pixel >> 8) & 0xFF;
        int blue = pixel & 0xFF;
        double gray = 0;
        for (int pass = 1; pass <= 16; pass++) { // lặp vài lượt để mỗi pixel tốn đủ CPU cho dễ đo
            gray += Math.sqrt(0.299 * red * red + 0.587 * green * green + 0.114 * blue * blue + pass) / pass;
        }
        int value = ((int) gray) & 0xFF;
        return (value << 16) | (value << 8) | value;
    }
}
