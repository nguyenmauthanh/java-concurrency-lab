package vn.thanh.lab.m08.exercise;

/** Ném ra khi bulkhead đã đầy và không có chỗ trong thời gian chờ cho phép (đã cho sẵn, không phải bài tập). */
public class BulkheadFullException extends RuntimeException {

    public BulkheadFullException(String message) {
        super(message);
    }
}
