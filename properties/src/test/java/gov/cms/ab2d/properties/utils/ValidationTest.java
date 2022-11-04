package gov.cms.ab2d.properties.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationTest {
    @Test
    void testValidInt() {
        assertFalse(Validation.validInteger(""));
        assertFalse(Validation.validInteger(null));
        assertFalse(Validation.validInteger("hello"));
        assertTrue(Validation.validInteger("0"));
        assertTrue(Validation.validInteger("123456677"));
    }

    @Test
    void testValidBoolean() {
        assertFalse(Validation.validBoolean(""));
        assertFalse(Validation.validBoolean(null));
        assertFalse(Validation.validBoolean("hello"));
        assertTrue(Validation.validBoolean("true"));
        assertTrue(Validation.validBoolean("false"));
        assertTrue(Validation.validBoolean("False"));
        assertTrue(Validation.validBoolean("TRUE"));
        assertFalse(Validation.validBoolean("1"));
        assertFalse(Validation.validBoolean("0"));
    }

    @Test
    void testValidFloat() {
        assertFalse(Validation.validFloat(""));
        assertFalse(Validation.validFloat(null));
        assertFalse(Validation.validFloat("hello"));
        assertTrue(Validation.validFloat("0"));
        assertTrue(Validation.validFloat("123456677"));
        assertTrue(Validation.validFloat("3.14"));
        assertTrue(Validation.validFloat("0.0000001"));
    }
}