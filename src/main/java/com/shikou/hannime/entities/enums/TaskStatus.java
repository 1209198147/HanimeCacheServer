package com.shikou.hannime.entities.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    PENDING(0, "Pending"),
    PROCESSING(1, "Processing"),
    COMPLETED(2, "Completed"),
    FAILED(3, "Failed"),
    CANCELLED(4, "Cancelled");

    private final int code;
    private final String message;

    TaskStatus(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public static TaskStatus valueOf(int code) {
        for (TaskStatus taskStatus : TaskStatus.values()) {
            if (taskStatus.code == code) {
                return taskStatus;
            }
        }
        throw new IllegalArgumentException("Invalid TaskStatus code: " + code);
    }
}
