package com.shikou.hannime.entities.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateTaskRequest {
    private List<Task> tasks;

    @Data
    public static class Task{
        private String videoCode;
        private List<String> qualities;
    }
}
