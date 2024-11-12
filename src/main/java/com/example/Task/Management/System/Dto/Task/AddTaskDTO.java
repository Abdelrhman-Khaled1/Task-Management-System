package com.example.Task.Management.System.Dto.Task;

import com.example.Task.Management.System.Entity.Task.Priority;
import com.example.Task.Management.System.Entity.Task.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddTaskDTO {
    private String title;
    private String description;
    private Status status;
    private Priority priority;
    private LocalDateTime dueDate;
}