package com.ar.scheduling.service;

import com.ar.scheduling.entities.ScheduledTask;

public interface TaskHandler {

    String getTaskType();
    boolean handle(ScheduledTask task);
}