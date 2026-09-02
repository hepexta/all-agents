package com.hepexta.allagents.domain.chat;

import java.time.LocalDateTime;

public record ChatEntry(String role, String content, LocalDateTime timestamp) {
}
