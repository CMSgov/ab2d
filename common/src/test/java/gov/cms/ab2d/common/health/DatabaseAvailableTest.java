package gov.cms.ab2d.common.health;

import gov.cms.ab2d.common.SpringBootApp;
import gov.cms.ab2d.common.util.AB2DLocalstackContainer;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
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

    @Container
    private static final AB2DLocalstackContainer localstackContainer  = new AB2DLocalstackContainer();


    @Test
    void testDatasource() throws SQLException {
        assertTrue(DatabaseAvailable.isDbAvailable(dataSource));
        assertFalse(DatabaseAvailable.isDbAvailable(null));
        Mockito.when(bogusDS.getConnection()).thenReturn(null);
        assertFalse(DatabaseAvailable.isDbAvailable(bogusDS));
        Mockito.when(bogusDS.getConnection()).thenReturn(bogusCon);
        assertFalse(DatabaseAvailable.isDbAvailable(bogusDS));
    }
}