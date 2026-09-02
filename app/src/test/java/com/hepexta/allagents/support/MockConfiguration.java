package com.hepexta.allagents.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("mock")
public class MockConfiguration {

    @Bean
    @Primary
    public MockChatModel mockChatModel() {
        return new MockChatModel();
    }
}
