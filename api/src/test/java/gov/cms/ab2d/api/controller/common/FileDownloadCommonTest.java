package gov.cms.ab2d.api.controller.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.File;
import java.io.IOException;

import static gov.cms.ab2d.api.controller.common.FileDownloadCommon.Encoding.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static java.util.Collections.enumeration;
import static java.util.Arrays.asList;

@ExtendWith(MockitoExtension.class)
class FileDownloadCommonTest {

    @Mock
    Resource downloadResource;

    @Mock
    MockHttpServletRequest request;

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
        assertEquals(FileDownloadCommon.getFileEncoding(downloadResource), UNCOMPRESSED);

        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.txt"));
        assertEquals(FileDownloadCommon.getFileEncoding(downloadResource), UNCOMPRESSED);

        when(downloadResource.getFile()).thenReturn(new File("/mnt/efs/xyz/test_1.ndjson.gz"));
        assertEquals(FileDownloadCommon.getFileEncoding(downloadResource), GZIP_COMPRESSED);
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

}
