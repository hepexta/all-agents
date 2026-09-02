package com.hepexta.allagents.bdd;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Integration runner: executes only @it-tagged scenarios against the real LLM.
 * Activated by the "it" Maven profile (scripts/it-test).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.hepexta.allagents.bdd")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "@it")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
public class RunItCucumberTest {
}
