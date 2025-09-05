package gov.cms.ab2d.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.Statement;

import static gov.cms.ab2d.common.health.FileSystemCheckTest.*;
import static gov.cms.ab2d.common.health.MemoryUtilizationTest.*;
import static gov.cms.ab2d.common.health.UrlAvailableTest.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})

class HealthCheckUtilTest {

    @Mock DataSource healthyDataSource;
    @Mock Connection healthyConnection;
    @Mock Statement healthyStatement;

    @Mock DataSource unhealthyDataSource;
    @Mock org.slf4j.Logger healthyLogger;
    @Mock org.slf4j.Logger unhealthyLogger;

    @BeforeEach
    void setup() throws Exception {
        lenient().when(healthyDataSource.getConnection()).thenReturn(healthyConnection);
        lenient().when(healthyConnection.createStatement()).thenReturn(healthyStatement);

        lenient().when(unhealthyDataSource.getConnection()).thenThrow(new RuntimeException("database error"));

        lenient().when(healthyLogger.isErrorEnabled()).thenReturn(true);

        lenient().when(unhealthyLogger.isErrorEnabled()).thenReturn(false);
    }

    @Test
    void testAllHealthy()  {
        assertTrue(HealthCheckUtil.healthy(
            healthyDataSource,
                WRITABLE_DIRECTORY,
            ALLOCATABLE_MEMORY_MB,
            singletonList(EXAMPLE_AVAILABLE_URL),
            healthyLogger
        ));
    }

    @Test
    void testUnhealthyVariations() {
        assertFalse(HealthCheckUtil.healthy(
                unhealthyDataSource,
                WRITABLE_DIRECTORY,
                ALLOCATABLE_MEMORY_MB,
                singletonList(EXAMPLE_AVAILABLE_URL),
                healthyLogger
        ));

        assertFalse(HealthCheckUtil.healthy(
                healthyDataSource,
                null,
                ALLOCATABLE_MEMORY_MB,
                singletonList(EXAMPLE_AVAILABLE_URL),
                healthyLogger
        ));

        assertFalse(HealthCheckUtil.healthy(
                unhealthyDataSource,
                WRITABLE_DIRECTORY,
                UNALLOCATABLE_MEMORY_MB,
                singletonList(EXAMPLE_AVAILABLE_URL),
                healthyLogger
        ));

        assertFalse(HealthCheckUtil.healthy(
                unhealthyDataSource,
                WRITABLE_DIRECTORY,
                ALLOCATABLE_MEMORY_MB,
                singletonList(EXAMPLE_UNAVAILABLE_URL),
                healthyLogger
        ));

        assertFalse(HealthCheckUtil.healthy(
                unhealthyDataSource,
                WRITABLE_DIRECTORY,
                ALLOCATABLE_MEMORY_MB,
                singletonList(EXAMPLE_AVAILABLE_URL),
                unhealthyLogger
        ));
    }


}
