package com.shikou.hannime.entities.enums;

import lombok.Getter;

@Getter
public enum EngineStatus {
    RUNNING("Running"),
    PAUSED("Paused"),
    STOPPED("Stopped");

    private final String message;

    EngineStatus(String message) {
        this.message = message;
    }
}
