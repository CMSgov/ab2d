package gov.cms.ab2d.eventlogger.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilMethodsTest {

    @Test
    void getJobId() {
        assertNull(UtilMethods.parseJobId(null));
        assertNull(UtilMethods.parseJobId("http://localhost:8080/abc"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080/Job/123/abc"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080/Job/123/$status"));
        assertEquals("123", UtilMethods.parseJobId("http://localhost:8080//Job/123//file/file1"));
    }
}