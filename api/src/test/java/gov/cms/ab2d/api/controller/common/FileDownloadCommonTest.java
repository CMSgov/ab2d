package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.common.service.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.File;
import java.io.IOException;

import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.Encoding.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static java.util.Collections.enumeration;
import static java.util.Arrays.asList;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class FileDownloadCommonTest {

    @Mock
    Resource downloadResource;

    @Mock
    MockHttpServletRequest request;

    static final String TEST_JOB_UUID="d8af57a6-ba5d-4bde-9341-44cf825989d7";
    static final String TEST_FILE="test_001.ndjson";

    @Test
    void swagger_download_filename_() throws Exception {
        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.ndjson"));
        assertEquals("test_1.ndjson", FileDownloadCommon.getSwaggerDownloadFilename(downloadResource));
        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.ndjson.gz"));
        assertEquals("test_1.ndjson", FileDownloadCommon.getSwaggerDownloadFilename(downloadResource));
    }

    @Test
    void test_encoding_by_file_extension() throws IOException {
        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.ndjson"));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getFileEncoding(downloadResource));

        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.txt"));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getFileEncoding(downloadResource));

        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.ndjson.gz"));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getFileEncoding(downloadResource));
    }

    @Test
    void test_accept_encoding_values() {
        when(request.getHeaders("Accept-Encoding")).thenReturn(null);
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("")));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList()));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("gzip2")));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("identity")));
        assertEquals(UNCOMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("gzip")));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("GZIP")));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("test", "gzip")));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("gzip, deflate, br")));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getRequestedEncoding(request));

        when(request.getHeaders("Accept-Encoding")).thenReturn(enumeration(asList("gzip ", "deflate ", "br ")));
        assertEquals(GZIP_COMPRESSED, FileDownloadCommon.getRequestedEncoding(request));
    }

    @Test
    void test_sanitize_valid_job_uuid() {
        assertEquals(TEST_JOB_UUID, FileDownloadCommon.sanitizeJobUuid(TEST_JOB_UUID));
        assertEquals(TEST_JOB_UUID, FileDownloadCommon.sanitizeJobUuid(TEST_JOB_UUID + " "));
    }

    @Test
    void test_sanitize_valid_filename() {
        assertEquals(TEST_FILE, FileDownloadCommon.sanitizeFilename(TEST_FILE));
        assertEquals(TEST_FILE, FileDownloadCommon.sanitizeFilename(TEST_FILE + " "));
    }

    @Test
    void test_sanitize_invalid_job_uuid(CapturedOutput output) {
        // invalid UUID
        assertThrows(ResourceNotFoundException.class, () -> {
            FileDownloadCommon.sanitizeJobUuid("xyz");
        });
        assertTrue(output.getOut().contains("Invalid job UUID provided: 'xyz'"));

        assertThrows(ResourceNotFoundException.class, () -> {
            FileDownloadCommon.sanitizeJobUuid("../" + TEST_JOB_UUID);
        });
        assertTrue(output.getOut().contains("Invalid job UUID provided: '../" + TEST_JOB_UUID + "'"));

    }

    @Test
    void test_sanitize_invalid_file(CapturedOutput output) {
        // invalid UUID
        assertThrows(ResourceNotFoundException.class, () -> {
            FileDownloadCommon.sanitizeJobUuid("../" + TEST_FILE);
        });
        assertTrue(output.getOut().contains("Invalid job UUID provided: '../" + TEST_FILE + "'"));

    }

}
