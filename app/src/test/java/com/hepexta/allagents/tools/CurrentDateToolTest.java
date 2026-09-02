package com.hepexta.allagents.tools;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentDateToolTest {

    @Test
    void returnsIsoFormattedDateFromClock() {
        CurrentDateTool tool = new CurrentDateTool(() -> LocalDateTime.of(2026, 8, 31, 12, 30, 45));
        assertEquals("2026-08-31T12:30:45", tool.getCurrentDate());
    }
}
