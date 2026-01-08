package gov.cms.ab2d.importer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(S3MockTestConfig.class)
@ExtendWith(S3MockAPIExtension.class)
class CoverageV3S3ImporterTest {

    private static final String FILE_NAME = "coverage_v3_test.csv";

    @Autowired
    private CoverageV3S3Importer importer;

    @BeforeEach
    void setup() {
        S3MockAPIExtension.deleteFile(FILE_NAME);
    }

    @Test
    void runOnce_happyPath_usesProperties_and_imports() throws Exception {
        // --- Arrange S3
        S3MockAPIExtension.createFile(
                "patient_id,contract,year,month,current_mbi,historic_mbis\n" +
                        "1,A1234,2025,12,MBI1,NULL\n",
                FILE_NAME
        );
        assertTrue(S3MockAPIExtension.isObjectExists(FILE_NAME));

        // --- Mock JDBC
        Connection conn = mock(Connection.class);

        PreparedStatement countPs1 = mock(PreparedStatement.class);
        PreparedStatement countPs2 = mock(PreparedStatement.class);
        ResultSet countRs1 = mock(ResultSet.class);
        ResultSet countRs2 = mock(ResultSet.class);

        when(conn.prepareStatement(startsWith("SELECT COUNT(*) FROM ")))
                .thenReturn(countPs1, countPs2);

        when(countPs1.executeQuery()).thenReturn(countRs1);
        when(countPs2.executeQuery()).thenReturn(countRs2);

        when(countRs1.next()).thenReturn(true);
        when(countRs1.getLong(1)).thenReturn(10L);

        when(countRs2.next()).thenReturn(true);
        when(countRs2.getLong(1)).thenReturn(11L);

        PreparedStatement importPs = mock(PreparedStatement.class);
        ResultSet importRs = mock(ResultSet.class);

        when(conn.prepareStatement(contains("aws_s3.table_import_from_s3")))
                .thenReturn(importPs);

        when(importPs.executeQuery()).thenReturn(importRs);
        when(importRs.next()).thenReturn(true);
        when(importRs.getInt(1)).thenReturn(1);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);

            // --- Act
            importer.runOnce();

            // --- Assert
            verify(importPs).setString(4, "ab2dimportfroms3test-bucket");
            verify(importPs).setString(5, FILE_NAME);
            verify(importPs).setString(6, "us-east-1");

            verify(importPs).executeQuery();
            verify(conn).close();
        }
    }
}
