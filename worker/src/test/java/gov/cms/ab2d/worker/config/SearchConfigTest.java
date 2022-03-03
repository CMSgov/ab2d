package gov.cms.ab2d.worker.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchConfigTest {
    static final String EFS_MOUNT = Path.of("tmp", "abc").toString();
    static final String EXPECTED_STREAM = Path.of(EFS_MOUNT,"jobid", "stream").toString();
    static final String FINISHED_STREAM = Path.of(EFS_MOUNT,"jobid", "finish").toString();

    @Test
    void testSearchConfig() {
        SearchConfig searchConfig = new SearchConfig(EFS_MOUNT, "stream", "finish", 0, 200, 2, 10);

        assertEquals(EXPECTED_STREAM, searchConfig.getStreamingDir("jobid").getPath());
        assertEquals(FINISHED_STREAM, searchConfig.getFinishedDir("jobid").getPath());
        assertEquals("finish", searchConfig.getFinishedDir());
        assertEquals("stream", searchConfig.getStreamingDir());
        assertEquals(EFS_MOUNT, searchConfig.getEfsMount());
        assertEquals(0, searchConfig.getBufferSize());
        assertEquals(200, searchConfig.getNdjsonRollOver());
        assertEquals(10, searchConfig.getNumberBenesPerBatch());
        assertEquals(2, searchConfig.getMultiplier());
    }
}
