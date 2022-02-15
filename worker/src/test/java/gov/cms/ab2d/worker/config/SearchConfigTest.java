package gov.cms.ab2d.worker.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchConfigTest {
    @Test
    void testSearchConfig() {
        SearchConfig searchConfig = new SearchConfig("/tmp/abc", "stream", "finish", 0, 200, 2, 10);

        assertEquals("/tmp/abc/jobid/stream", searchConfig.getStreamingDir("jobid").getAbsolutePath());
        assertEquals("/tmp/abc/jobid/finish", searchConfig.getFinishedDir("jobid").getAbsolutePath());
        assertEquals("finish", searchConfig.getFinishedDir());
        assertEquals("stream", searchConfig.getStreamingDir());
        assertEquals("/tmp/abc", searchConfig.getEfsMount());
        assertEquals(0, searchConfig.getBufferSize());
        assertEquals(200, searchConfig.getNdjsonRollOver());
        assertEquals(10, searchConfig.getNumberBenesPerBatch());
        assertEquals(2, searchConfig.getMultiplier());
    }
}
