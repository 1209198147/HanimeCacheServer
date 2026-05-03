package com.shikou.hannime.entities.request;

import lombok.Data;

import java.util.List;

@Data
public class CreateTaskRequest {
    private List<Task> tasks;

    @Data
    public class Task{
        private String videoCode;
        private String videoUrl;
        private List<String> resolutions;
    }
}
