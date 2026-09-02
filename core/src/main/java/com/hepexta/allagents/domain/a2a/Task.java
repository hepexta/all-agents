package com.hepexta.allagents.domain.a2a;

public record Task(String id, String contextId, TaskStatus status, AgentMessage message) {

    public enum TaskState {
        submitted,
        working,
        completed,
        failed
    }

    public record TaskStatus(TaskState state, AgentMessage message) {
    }

    public static Task working(String id, String contextId) {
        return new Task(id, contextId, new TaskStatus(TaskState.working, null), null);
    }

    public Task completed(AgentMessage message) {
        return new Task(id(), contextId(), new TaskStatus(TaskState.completed, message), message);
    }

    public Task failed(AgentMessage message) {
        return new Task(id(), contextId(), new TaskStatus(TaskState.failed, message), message);
    }
}
