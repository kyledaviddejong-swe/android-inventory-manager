package com.example.kyle_dejong_inventory_app;

import org.junit.Test;
import static org.junit.Assert.*;

public class PasswordHasherTest {
    @Test public void verifiesCorrectPasswordAndRejectsIncorrectOne() {
        String hash = PasswordHasher.hash("demo password");
        assertTrue(PasswordHasher.verify("demo password", hash));
        assertFalse(PasswordHasher.verify("wrong password", hash));
        assertFalse(hash.contains("demo password"));
    }

    @Test public void equalPasswordsReceiveDifferentSalts() {
        String first = PasswordHasher.hash("same password");
        String second = PasswordHasher.hash("same password");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.verify("same password", first));
        assertTrue(PasswordHasher.verify("same password", second));
    }

    @Test public void preservesUnicodeAndIntentionalWhitespace() {
        String password = "  倉庫の合言葉 🦊  ";
        String hash = PasswordHasher.hash(password);
        assertTrue(PasswordHasher.verify(password, hash));
        assertFalse(PasswordHasher.verify(password.trim(), hash));
    }

    @Test public void rejectsPlaintextMalformedAndUnboundedWorkFactors() {
        assertFalse(PasswordHasher.verify("demo", "demo"));
        assertFalse(PasswordHasher.verify("demo", null));
        assertFalse(PasswordHasher.verify(null, "broken"));
        assertFalse(PasswordHasher.verify("", "broken"));
        assertFalse(PasswordHasher.verify("demo", "pbkdf2-sha256$600000$?$?"));
        assertFalse(PasswordHasher.verify("demo", "pbkdf2-sha256$600000$YQ==$Yg=="));
        assertFalse(PasswordHasher.verify("demo", "pbkdf2-sha256$2147483647$a$b"));
    }

    @Test public void rejectsEmptyPasswordsAtCreation() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }
}
