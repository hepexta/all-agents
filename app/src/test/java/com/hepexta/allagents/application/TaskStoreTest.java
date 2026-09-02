package com.hepexta.allagents.application;

import com.hepexta.allagents.domain.a2a.AgentMessage;
import com.hepexta.allagents.domain.a2a.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStoreTest {

    @Test
    void putAndGetTask() {
        TaskStore store = new TaskStore();
        Task task = Task.working("task-1", "ctx-1");
        store.put(task);
        assertEquals(task, store.get("task-1").orElseThrow());
    }

    @Test
    void missingTaskReturnsEmpty() {
        assertTrue(new TaskStore().get("missing").isEmpty());
    }

    @Test
    void taskStateTransitions() {
        TaskStore store = new TaskStore();
        Task working = Task.working("task-1", "ctx");
        store.put(working);
        assertEquals(Task.TaskState.working, store.get("task-1").orElseThrow().status().state());

        AgentMessage message = AgentMessage.of("m-1", "agent", "done");
        store.put(working.completed(message));
        assertEquals(Task.TaskState.completed, store.get("task-1").orElseThrow().status().state());
        assertEquals("done", store.get("task-1").orElseThrow().message().text());

        store.put(working.failed(message));
        assertEquals(Task.TaskState.failed, store.get("task-1").orElseThrow().status().state());
    }
}
