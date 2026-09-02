package com.hepexta.allagents.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring context for Cucumber. Profile is "mock" by default and switches to
 * "it" (real LLM) when -Dcucumber.profile=it is set (see the "it" Maven profile).
 */
@CucumberContextConfiguration
@SpringBootTest
@ActiveProfiles("${cucumber.profile:mock}")
public class CucumberSpringConfiguration {
}
