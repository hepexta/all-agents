package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.a2a.Task;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskStore {

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();

    public void put(Task task) {
        tasks.put(task.id(), task);
    }

    public Optional<Task> get(String id) {
        return Optional.ofNullable(tasks.get(id));
    }
}
