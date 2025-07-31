package gov.cms.ab2d.audit;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import gov.cms.ab2d.testutils.TestContext;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuditTest {
    @Test
    void
    auditInvoke() throws IOException {
        Properties properties = PropertiesUtil.loadProps();
        File dir = new File(properties.getProperty("AB2D_EFS_MOUNT") + UUID.randomUUID());
        dir.mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        System.setProperty("audit_files_ttl_hours", "-1");
        AuditEventHandler eventHandler = new AuditEventHandler();
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(null, System.out, new TestContext());
        });
    }

    @Test
    void auditInvokeNoDelete() throws IOException {
        Properties properties = PropertiesUtil.loadProps();
        File dir = new File(properties.getProperty("AB2D_EFS_MOUNT") + UUID.randomUUID());
        dir.mkdirs();
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(dir.getAbsoluteFile() + String.format("/%s.ndjson", RandomString.make(5)))) {
            fileOutputStream.write("test".getBytes(StandardCharsets.UTF_8));
        }
        System.setProperty("audit_files_ttl_hours", "1");
        AuditEventHandler eventHandler = new AuditEventHandler();
        assertDoesNotThrow(() -> {
            eventHandler.handleRequest(null, System.out, new TestContext());
        });
    }


}
