package com.campuslab.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {
    @Test void hashedPasswordCanBeVerified() {
        PasswordUtil.PasswordRecord record = PasswordUtil.hash("ExamplePassword!123");
        assertNotNull(record.salt());
        assertNotNull(record.hash());
        assertNotEquals("ExamplePassword!123", record.hash());
        assertTrue(PasswordUtil.verify("ExamplePassword!123", record.salt(), record.hash()));
    }

    @Test void wrongPasswordIsRejected() {
        PasswordUtil.PasswordRecord record = PasswordUtil.hash("ExamplePassword!123");
        assertFalse(PasswordUtil.verify("wrong-password", record.salt(), record.hash()));
    }

    @Test void eachHashUsesAnIndependentSalt() {
        PasswordUtil.PasswordRecord first = PasswordUtil.hash("ExamplePassword!123");
        PasswordUtil.PasswordRecord second = PasswordUtil.hash("ExamplePassword!123");
        assertNotEquals(first.salt(), second.salt());
        assertNotEquals(first.hash(), second.hash());
    }

    @Test void malformedStoredValuesFailClosed() {
        assertFalse(PasswordUtil.verify("password", "not-base64!", "also-not-base64!"));
    }
}
