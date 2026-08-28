package com.infobee.model;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class UserSerializationTest {
    @Test
    void doesNotSerializePasswordHash() throws Exception {
        String json = new ObjectMapper()
            .writeValueAsString(new User("admin", "$2a$10$hash", "System Administrator", "ADMIN"));

        assertFalse(json.contains("password"));
        assertFalse(json.contains("$2a$10$hash"));
    }
}
