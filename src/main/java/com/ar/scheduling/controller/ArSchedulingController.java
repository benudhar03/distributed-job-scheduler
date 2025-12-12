package com.ar.scheduling.controller;

import com.ar.scheduling.dto.ScheduleRequest;
import com.ar.scheduling.dto.ScheduleResponse;
import com.ar.scheduling.dto.ScheduleUpdateRequest;
import com.ar.scheduling.dto.TaskStatusResponse;
import com.ar.scheduling.service.ArSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedule")
@Tag(name = "Scheduling Controller", description = "APIs for managing scheduled tasks")
public class ArSchedulingController {

    private final ArSchedulingService arSchedulingService;

    @Operation(
            summary = "Schedule Your task",
            description = "Creates a new scheduled task (one-off or recurring) and persists it.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task scheduled successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ScheduleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @PostMapping(
            value = "/tasks",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ScheduleResponse> scheduleTask(@Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = arSchedulingService.scheduleTask(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get task by ID",
            description = "Retrieves detailed information about a specific scheduled task.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ScheduleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @GetMapping(
            value = "/tasks/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ScheduleResponse> getScheduledTask(@PathVariable String taskId) {
        ScheduleResponse response = arSchedulingService.getScheduledTask(taskId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get all tasks",
            description = "Retrieves a list of all scheduled tasks with pagination support.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ScheduleResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @GetMapping(
            value = "/tasks",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ScheduleResponse>> getAllScheduledTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ScheduleResponse> responses = arSchedulingService.getAllScheduledTasks(page, size);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Update scheduled task",
            description = "Updates an existing scheduled task with new parameters.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task updated successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ScheduleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @PutMapping(
            value = "/tasks/{taskId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ScheduleResponse> updateScheduleRequest(
            @PathVariable String taskId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        ScheduleResponse response = arSchedulingService.updateScheduleRequest(taskId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel scheduled task",
            description = "Cancels an existing scheduled task and stops future executions.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task cancelled successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TaskStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Task already completed or cancelled", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @PostMapping(
            value = "/tasks/{taskId}/cancel",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskStatusResponse> cancelScheduledTask(@PathVariable String taskId) {
        TaskStatusResponse response = arSchedulingService.cancelScheduledTask(taskId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Trigger task execution",
            description = "Immediately triggers the execution of a scheduled task.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task triggered successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TaskStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Task already in progress", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @PostMapping(
            value = "/tasks/{taskId}/trigger",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskStatusResponse> triggerTask(@PathVariable String taskId) {
        TaskStatusResponse response = arSchedulingService.triggerTask(taskId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get task status",
            description = "Retrieves the current status and execution details of a task.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = TaskStatusResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @GetMapping(
            value = "/tasks/{taskId}/status",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<TaskStatusResponse> getScheduledTaskStatus(@PathVariable String taskId) {
        TaskStatusResponse response = arSchedulingService.getScheduledTaskStatus(taskId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get tasks by status",
            description = "Retrieves all tasks filtered by their current status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ScheduleResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @GetMapping(
            value = "/tasks/status/{status}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<ScheduleResponse>> getScheduledTasksByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<ScheduleResponse> responses =
                arSchedulingService.getScheduledTasksByStatus(status, page, size);
        return ResponseEntity.ok(responses);
    }

    @Operation(
            summary = "Delete task",
            description = "Permanently deletes a scheduled task from the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Task not found", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
            }
    )
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Void> deleteScheduledTask(@PathVariable String taskId) {
        arSchedulingService.deleteScheduledTask(taskId);
        return ResponseEntity.ok().build();
    }
}