package com.zazhi.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTest {
    @Test
    void reportsEveryMissingRequiredValue() {
        Config config = new Config(null, "", null);

        assertEquals(3, config.validate().size());
    }

    @Test
    void modelOverrideDoesNotChangeOtherSettings() {
        Config original = new Config("https://example.test", "secret", "old-model");
        Config changed = original.withModelId("new-model");

        assertTrue(changed.validate().isEmpty());
        assertEquals("https://example.test", changed.getBaseUrl());
        assertEquals("secret", changed.getApiKey());
        assertEquals("new-model", changed.getModelId());
    }
}
