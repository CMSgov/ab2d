package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class ClaimsStreamTest {

    private static final String JOB_ID = "job1";
    private static final String STREAM_DIR = "streaming";
    private static final String FINISH_DIR = "finished";
    private static final int MIB = 1048576;

    @Test
    void testInit(@TempDir File tmpDirFolder) {
        // Tests the constructor that uses the default buffer size
        try (ClaimsStream stream = new ClaimsStream(JOB_ID, tmpDirFolder.getAbsolutePath(), DATA, STREAM_DIR, FINISH_DIR)) {
            assertTrue(stream.isOpen());
            stream.flush();
            stream.close();
            assertFalse(stream.isOpen());
        } catch (Exception ex) {
            fail(ex);
        }
    }

    @Test
    void testCreateAndWriteToStream(@TempDir File tmpDirFolder) {
        ClaimsStream savedStream = null;
        try (ClaimsStream stream = new ClaimsStream(JOB_ID, tmpDirFolder.getAbsolutePath(), DATA, STREAM_DIR, FINISH_DIR, MIB)) {
            savedStream = stream;
            for (int i = 0; i < 1000; i++) {
                stream.write(AggregatorTest.getAlphaNumericString(1000));
            }

            File tmpFile = stream.getFile();
            assertTrue(tmpFile.exists());
            assertTrue(stream.isOpen());
            File tmpFileDirectory = new File(tmpDirFolder.getAbsolutePath() + File.separator + JOB_ID + File.separator + STREAM_DIR);
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());

        } catch (Exception ex) {
            fail(ex);
        }
        if (savedStream != null) {
            File tmpFile = savedStream.getFile();
            assertTrue(tmpFile.exists());
            assertFalse(savedStream.isOpen());
            File tmpFileDirectory = new File(tmpDirFolder.getAbsolutePath() + File.separator + JOB_ID + File.separator + FINISH_DIR);
            File theFile = Path.of(tmpFileDirectory.getAbsolutePath(), tmpFile.getName()).toFile();
            assertTrue(theFile.exists());
        }
    }
}
