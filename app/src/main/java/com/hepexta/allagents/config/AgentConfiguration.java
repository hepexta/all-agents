package com.hepexta.allagents.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hepexta.allagents.adapters.out.a2a.HttpA2aClient;
import com.hepexta.allagents.adapters.out.a2a.InProcessA2aClient;
import com.hepexta.allagents.ai.GuardrailAdvisor;
import com.hepexta.allagents.guardrail.CompositeGuardrail;
import com.hepexta.allagents.guardrail.Guardrail;
import com.hepexta.allagents.ports.A2aClient;
import com.hepexta.allagents.ports.AgentRuntime;
import com.hepexta.allagents.ports.Clock;
import com.hepexta.allagents.tools.CurrentDateTool;
import org.springframework.ai.tool.toolsearch.ToolIndex;
import org.springframework.ai.tool.toolsearch.ToolSearchTool;
import org.springframework.ai.tool.toolsearch.index.regex.RegexToolIndex;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
public class AgentConfiguration {

    @Bean
    public Clock systemClock() {
        return LocalDateTime::now;
    }

    @Bean
    public CurrentDateTool currentDateTool(Clock clock) {
        return new CurrentDateTool(clock);
    }

    @Bean
    public ToolIndex toolIndex() {
        return new RegexToolIndex();
    }

    @Bean
    public ToolSearchTool toolSearchTool(ToolIndex toolIndex) {
        return new ToolSearchTool(toolIndex, 5);
    }

    @Bean
    @Primary
    public CompositeGuardrail compositeGuardrail(List<Guardrail> guardrails) {
        return new CompositeGuardrail(guardrails);
    }

    @Bean
    public GuardrailAdvisor guardrailAdvisor(Guardrail guardrail) {
        return new GuardrailAdvisor(guardrail);
    }

    @Bean
    @ConditionalOnProperty(name = "app.a2a.mode", havingValue = "in-process", matchIfMissing = true)
    public A2aClient inProcessA2aClient(ObjectProvider<AgentRuntime> runtimeProvider) {
        return new InProcessA2aClient(runtimeProvider);
    }

    @Bean
    @ConditionalOnProperty(name = "app.a2a.mode", havingValue = "http")
    public A2aClient httpA2aClient(RestClient.Builder builder, AppProperties properties, ObjectMapper objectMapper) {
        return new HttpA2aClient(builder, properties.a2a().baseUrl(), objectMapper);
    }
}
