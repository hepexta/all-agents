package com.hepexta.allagents.tools;

import com.hepexta.allagents.ports.Clock;
import org.springframework.ai.tool.annotation.Tool;

import java.time.format.DateTimeFormatter;

public class CurrentDateTool {

    public static final String NAME = "getCurrentDate";

    private final Clock clock;

    public CurrentDateTool(Clock clock) {
        this.clock = clock;
    }

    @Tool(description = "Returns the current date and time in ISO-8601 format. Use it when the current date or time is relevant to the request.")
    public String getCurrentDate() {
        return clock.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
