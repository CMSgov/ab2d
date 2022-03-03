package gov.cms.ab2d.worker.config;

import org.junit.jupiter.api.Test;

import static java.io.File.separatorChar;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchConfigTest {
    static final String EFS_MOUNT = separatorChar + "tmp" + separatorChar + "abc";
    static final String EXPECTED_STREAM = EFS_MOUNT + separatorChar + "jobid" + separatorChar + "stream";
    static final String FINISHED_STREAM = EFS_MOUNT + separatorChar + "jobid" + separatorChar + "finish";

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
