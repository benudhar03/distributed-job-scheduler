package com.ar.scheduling.repository;

import com.ar.scheduling.entities.ScheduledTask;
import com.ar.scheduling.enums.ScheduleType;
import com.ar.scheduling.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArScheduledTaskRepository extends MongoRepository<ScheduledTask, String> {

    // Basic CRUD operations
    Optional<ScheduledTask> findByTaskId(String taskId);

    boolean existsByTaskId(String taskId);

    void deleteByTaskId(String taskId);

    // Duplicate check methods
    boolean existsByTaskNameAndStatusNot(String taskName, TaskStatus status);

    boolean existsByTaskNameAndStatus(String taskName, TaskStatus status);

    @Query("{ 'taskName': ?0, 'status': { $nin: ?1 } }")
    boolean existsByTaskNameAndStatusNotIn(String taskName, List<TaskStatus> excludedStatuses);

    @Query("{ 'taskName': ?0, 'status': { $in: ?1 } }")
    boolean existsByTaskNameAndStatusIn(String taskName, List<TaskStatus> includedStatuses);

    // Check for duplicate with additional criteria
    @Query("{ 'taskName': ?0, 'taskType': ?1, 'status': { $nin: ?2 } }")
    boolean existsByTaskNameAndTaskTypeAndStatusNotIn(String taskName, String taskType, List<TaskStatus> excludedStatuses);

    @Query("{ 'taskName': ?0, 'scheduledTime': ?1, 'status': { $nin: ?2 } }")
    boolean existsByTaskNameAndScheduledTimeAndStatusNotIn(String taskName, LocalDateTime scheduledTime, List<TaskStatus> excludedStatuses);

    // Find by task name (for duplicate validation)
    Optional<ScheduledTask> findByTaskName(String taskName);

    List<ScheduledTask> findByTaskNameContainingIgnoreCase(String taskName);

    // Status-based queries
    Page<ScheduledTask> findByStatus(TaskStatus status, Pageable pageable);

    List<ScheduledTask> findByStatus(TaskStatus status);

    long countByStatus(TaskStatus status);

    // Task type and status combination
    List<ScheduledTask> findByTaskTypeAndStatus(String taskType, TaskStatus status);

    Page<ScheduledTask> findByTaskTypeAndStatus(String taskType, TaskStatus status, Pageable pageable);

    // Schedule type queries
    List<ScheduledTask> findByScheduleType(ScheduleType scheduleType);

    Page<ScheduledTask> findByScheduleType(ScheduleType scheduleType, Pageable pageable);

    // Time-based queries
    List<ScheduledTask> findByScheduledTimeBefore(LocalDateTime dateTime);

    List<ScheduledTask> findByScheduledTimeAfter(LocalDateTime dateTime);

    List<ScheduledTask> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end);

    Page<ScheduledTask> findByScheduledTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Complex queries with @Query annotation
    @Query("{ 'status': { $in: ?0 }, 'scheduledTime': { $lte: ?1 } }")
    List<ScheduledTask> findReadyForExecution(List<TaskStatus> statuses, LocalDateTime currentTime);

    @Query("{ 'status': { $in: ['PENDING', 'SCHEDULED'] }, 'scheduledTime': { $lte: ?0 } }")
    List<ScheduledTask> findOverdueTasks(LocalDateTime currentTime);

    @Query("{ 'status': 'PENDING', 'scheduledTime': { $lte: ?0 } }")
    List<ScheduledTask> findPendingTasksDueForExecution(LocalDateTime currentTime);

    @Query("{ 'taskName': { $regex: ?0, $options: 'i' } }")
    List<ScheduledTask> findByTaskNameRegex(String taskNamePattern);

    @Query("{ 'taskName': { $regex: ?0, $options: 'i' }, 'status': ?1 }")
    List<ScheduledTask> findByTaskNameRegexAndStatus(String taskNamePattern, TaskStatus status);

    // Priority based queries
    @Query("{ 'priority': ?0, 'status': ?1 }")
    List<ScheduledTask> findByPriorityAndStatus(String priority, TaskStatus status);

    @Query("{ 'priority': ?0, 'status': { $in: ?1 } }")
    List<ScheduledTask> findByPriorityAndStatusIn(String priority, List<TaskStatus> statuses);

    // Created by user queries
    List<ScheduledTask> findByCreatedBy(String createdBy);

    Page<ScheduledTask> findByCreatedBy(String createdBy, Pageable pageable);

    List<ScheduledTask> findByCreatedByAndStatus(String createdBy, TaskStatus status);

    // Retry count queries
    @Query("{ 'status': 'FAILED', 'retryCount': { $lt: ?0 } }")
    List<ScheduledTask> findFailedTasksWithRetryAvailable(int maxRetries);

    @Query("{ 'status': 'FAILED', 'retryCount': { $gte: ?0 } }")
    List<ScheduledTask> findFailedTasksExhaustedRetries(int maxRetries);

    // Cron-based tasks
    @Query("{ 'scheduleType': 'CRON_BASED', 'status': { $in: ['PENDING', 'SCHEDULED'] } }")
    List<ScheduledTask> findActiveCronTasks();

    // Recurring tasks
    @Query("{ 'scheduleType': 'RECURRING', 'status': { $in: ['PENDING', 'SCHEDULED'] }, 'endTime': { $gt: ?0 } }")
    List<ScheduledTask> findActiveRecurringTasks(LocalDateTime currentTime);

    // Bulk status update operations
    @Query("{ 'taskId': { $in: ?0 } }")
    @Update("{ '$set': { 'status': ?1, 'updatedAt': ?2 } }")
    void updateStatusByTaskIds(List<String> taskIds, TaskStatus status, LocalDateTime updatedAt);

    @Query("{ 'status': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'updatedAt': ?2 } }")
    void updateStatusBulk(TaskStatus oldStatus, TaskStatus newStatus, LocalDateTime updatedAt);

    // Increment retry count
    @Query("{ 'taskId': ?0 }")
    @Update("{ '$inc': { 'retryCount': 1 }, '$set': { 'updatedAt': ?1 } }")
    void incrementRetryCount(String taskId, LocalDateTime updatedAt);

    // Set execution time and status
    @Query("{ 'taskId': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'executionTime': ?2, 'updatedAt': ?3 } }")
    void updateExecutionStatus(String taskId, TaskStatus status, LocalDateTime executionTime, LocalDateTime updatedAt);

    // Set completion time and status
    @Query("{ 'taskId': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'completedTime': ?2, 'updatedAt': ?3 } }")
    void updateCompletionStatus(String taskId, TaskStatus status, LocalDateTime completedTime, LocalDateTime updatedAt);

    // Set error message and status
    @Query("{ 'taskId': ?0 }")
    @Update("{ '$set': { 'status': ?1, 'errorMessage': ?2, 'updatedAt': ?3 } }")
    void updateErrorStatus(String taskId, TaskStatus status, String errorMessage, LocalDateTime updatedAt);

    // Find tasks with metadata
    @Query("{ 'metadata.?0': ?1 }")
    List<ScheduledTask> findByMetadataKeyAndValue(String key, String value);

    @Query("{ 'metadata.?0': { $exists: true } }")
    List<ScheduledTask> findByMetadataKeyExists(String key);

    // Statistics and analytics queries
    @Query(value = "{}", count = true)
    long getTotalTaskCount();

    @Query(value = "{ 'status': ?0 }", count = true)
    long getTaskCountByStatus(TaskStatus status);

    @Query(value = "{ 'taskType': ?0 }", count = true)
    long getTaskCountByType(String taskType);

    @Query(value = "{ 'createdAt': { $gte: ?0, $lte: ?1 } }", count = true)
    long getTaskCountByDateRange(LocalDateTime start, LocalDateTime end);

    // Aggregation queries for dashboard
    @Query(value = "{}", sort = "{ 'scheduledTime': 1 }")
    List<ScheduledTask> findAllOrderByScheduledTimeAsc();

    @Query(value = "{ 'status': { $in: ?0 } }", sort = "{ 'scheduledTime': 1 }")
    List<ScheduledTask> findByStatusInOrderByScheduledTimeAsc(List<TaskStatus> statuses);

    // Find tasks that need rescheduling (for recurring/cron tasks)
    @Query("{ " +
            "$or: [ " +
            "{ 'scheduleType': 'RECURRING', 'status': 'COMPLETED', 'endTime': { $gt: ?0 } }, " +
            "{ 'scheduleType': 'CRON_BASED', 'status': 'COMPLETED' } " +
            "] " +
            "}")
    List<ScheduledTask> findTasksForRescheduling(LocalDateTime currentTime);

    // Find tasks by multiple criteria
    @Query("{ " +
            "'status': { $in: ?0 }, " +
            "'taskType': { $in: ?1 }, " +
            "'scheduledTime': { $gte: ?2, $lte: ?3 } " +
            "}")
    Page<ScheduledTask> findByComplexCriteria(List<TaskStatus> statuses,
                                              List<String> taskTypes,
                                              LocalDateTime startTime,
                                              LocalDateTime endTime,
                                              Pageable pageable);

    // Find tasks that have been in progress for too long (stuck tasks)
    @Query("{ 'status': 'IN_PROGRESS', 'executionTime': { $lt: ?0 } }")
    List<ScheduledTask> findStuckTasks(LocalDateTime thresholdTime);

    // Find tasks by priority range
    @Query("{ 'priority': { $in: ?0 }, 'status': { $in: ?1 } }")
    List<ScheduledTask> findByPriorityInAndStatusIn(List<String> priorities, List<TaskStatus> statuses);
}