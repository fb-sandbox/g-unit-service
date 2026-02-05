package com.fullbay.unit.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class IdGeneratorTest {

    @Test
    void shouldGenerateValidUnitId() {
        String id = IdGenerator.generateUnitId();

        assertNotNull(id);
        assertTrue(id.startsWith("unt-"));
        assertEquals(11, id.length()); // "unt-" + 7 chars = 11
    }

    @Test
    void shouldGenerateLowercaseAlphanumeric() {
        for (int i = 0; i < 100; i++) {
            String id = IdGenerator.generateUnitId();
            String suffix = id.substring(4); // Remove "unt-" prefix

            assertTrue(suffix.matches("^[a-z0-9]{7}$"));
        }
    }

    @Test
    void shouldGenerateUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            String id = IdGenerator.generateUnitId();
            ids.add(id);
        }

        assertEquals(1000, ids.size());
    }
}
