package gov.cms.ab2d.common.health;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
@Testcontainers
class DatabaseAvailableTest {
    @Autowired
    private DataSource dataSource;
    @Mock
    private DataSource bogusDS;
    @Mock
    private Connection bogusCon;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    public void testDatasource() throws SQLException {
        assertTrue(DatabaseAvailable.isDbAvailable(dataSource));
        assertFalse(DatabaseAvailable.isDbAvailable(null));
        Mockito.when(bogusDS.getConnection()).thenReturn(null);
        assertFalse(DatabaseAvailable.isDbAvailable(bogusDS));
        Mockito.when(bogusDS.getConnection()).thenReturn(bogusCon);
        assertFalse(DatabaseAvailable.isDbAvailable(bogusDS));
    }
}