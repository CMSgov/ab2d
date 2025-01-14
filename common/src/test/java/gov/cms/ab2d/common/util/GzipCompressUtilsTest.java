package gov.cms.ab2d.common.util;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.persistence.SqlResultSetMapping;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GzipCompressUtilsTest {

    /**
     * Test file contains first 500 entries from https://bcda.cms.gov/assets/data/ExplanationOfBenefit.ndjson
     */
    public static final Path UNCOMPRESSED_FILE = Path.of("src/test/resources/test-data/EOB-500.ndjson");
    /**
     * Test file compressed using the 'gzip' command line utility, not {@link GzipCompressUtils#compress}
     */
    public static final Path COMPRESSED_FILE_USING_GZIP_CLI = Path.of("src/test/resources/test-data/EOB-500.ndjson.gz");

    @Test
    void testCompressFile() throws Exception {
        Path outputCompressed = newTestFile(".ndjson.gz");
        GzipCompressUtils.compress(UNCOMPRESSED_FILE, outputCompressed);

        /**
         Note that the following is not a valid assertion because the 'gzip' command line utility can produce
         output that differs from {@link GzipCompressUtils#compress}, however both outputs are valid.

         assertTrue(
             FileUtils.contentEquals(output.toFile(),
             COMPRESSED_FILE.toFile())
         );

         Instead, decompress the `outputCompressed` file and assert it matches {@link UNCOMPRESSED_FILE}
         */

        Path outputDecompressed = newTestFile(".ndjson");
        GzipCompressUtils.decompress(outputCompressed, outputDecompressed);

        assertTrue(
                FileUtils.contentEquals(
                        outputDecompressed.toFile(),
                        UNCOMPRESSED_FILE.toFile()
                )
        );
    }

    @Test
    void testCompressOutputStream() throws Exception {
        ByteArrayOutputStream outputCompressed = new ByteArrayOutputStream();
        GzipCompressUtils.compress(UNCOMPRESSED_FILE, outputCompressed);

        ByteArrayOutputStream outputUncompressed = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(
                new ByteArrayInputStream(outputCompressed.toByteArray()),
                outputUncompressed
        );

        assertArrayEquals(
                Files.readAllBytes(UNCOMPRESSED_FILE),
                outputUncompressed.toByteArray()
        );
    }

    @Test
    void testDecompressFile() throws Exception {
        Path output = newTestFile(".ndjson");
        GzipCompressUtils.decompress(COMPRESSED_FILE_USING_GZIP_CLI, output);
        assertTrue(
                FileUtils.contentEquals(
                        output.toFile(),
                        UNCOMPRESSED_FILE.toFile()
                )
        );
    }

    @Test
    void testDecompressOutputStream() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        GzipCompressUtils.decompress(COMPRESSED_FILE_USING_GZIP_CLI, output);
        assertArrayEquals(
                Files.readAllBytes(UNCOMPRESSED_FILE),
                output.toByteArray()
        );
    }

    @Test
    void testCompressFile_fileIsDeleted(@TempDir File tempDir) throws IOException {
        File file = copyFile(UNCOMPRESSED_FILE.toFile(), tempDir).toFile();
        assertTrue(file.exists());
        assertTrue(GzipCompressUtils.compressFile(file, true));

        assertTrue(new File(file.getParent(), file.getName() + ".gz").exists());
        assertFalse(file.exists());
    }

    @Test
    void testCompressFile_fileIsNotDeleted(@TempDir File tempDir) throws IOException {
        File file = copyFile(UNCOMPRESSED_FILE.toFile(), tempDir).toFile();
        assertTrue(file.exists());
        assertTrue(GzipCompressUtils.compressFile(file, false));

        assertTrue(new File(file.getParent(), file.getName() + ".gz").exists());
        assertTrue(file.exists());
    }

    @Test
    void testCompressFile_fileNotFound(@TempDir File tempDir) {
        assertFalse(GzipCompressUtils.compressFile(new File("not-a-real-file"), true));
    }

    @Test
    void testCompressFile_fileIsADirectory(@TempDir File tempDir)  {
        assertFalse(GzipCompressUtils.compressFile(tempDir, true));
    }

    @Test
    void testCompressJobOutputFiles(@TempDir File tempDir) throws IOException {
        String jobId = "job1234";
        File jobDirectory = new File(tempDir, jobId);
        jobDirectory.mkdirs();

        File testDataFile = copyFile(UNCOMPRESSED_FILE.toFile(), jobDirectory, "test.ndjson").toFile();
        File testErrorFile = copyFile(UNCOMPRESSED_FILE.toFile(), jobDirectory, "test_error.ndjson").toFile();
        File testTextFile = copyFile(UNCOMPRESSED_FILE.toFile(), jobDirectory, "test.txt").toFile();

        assertTrue(testDataFile.exists());
        assertTrue(testErrorFile.exists());
        assertTrue(testTextFile.exists());

        GzipCompressUtils.compressJobOutputFiles(jobId, tempDir.getAbsolutePath(), this::fileFilter);

        // verify files are compressed
        assertTrue(new File(jobDirectory, "test.ndjson.gz").exists());
        assertTrue(new File(jobDirectory, "test_error.ndjson.gz").exists());

        // verify original files were deleted after being compressed
        assertFalse(testDataFile.exists());
        assertFalse(testErrorFile.exists());

        // verify unrelated file (neither data nor error) is not changed
        assertTrue(testTextFile.exists());
        assertFalse(new File(jobDirectory, "test.txt.gz").exists());
    }

    @Test
    void testCompressJobOutputFiles_badInputs(@TempDir File tempDir) throws IOException {
        copyFile(UNCOMPRESSED_FILE.toFile(), tempDir).toFile();
        assertFalse(GzipCompressUtils.compressJobOutputFiles("non-existent-job-id", tempDir.getAbsolutePath(), null));
        assertFalse(GzipCompressUtils.compressJobOutputFiles(UNCOMPRESSED_FILE.toFile().getName(), tempDir.getAbsolutePath(), null));
    }

    @Test
    void compressFile_invalidInputs(@TempDir File tempDir) {
        assertFalse(GzipCompressUtils.compressFile(null, true));
        assertFalse(GzipCompressUtils.compressFile(new File("does-not-exist.ndjson"), true));
        assertFalse(GzipCompressUtils.compressFile(tempDir, true));
    }

    Path newTestFile(String suffix) throws IOException {
        Path file = Files.createTempFile(getClass().getSimpleName(), suffix);
        file.toFile().deleteOnExit();
        return file;
    }

    Path copyFile(File file, File directory) throws IOException {
        return Files.copy(file.toPath(), new File(directory, file.getName()).toPath());
    }

    Path copyFile(File file, File directory, String newFilename) throws IOException {
        return Files.copy(file.toPath(), new File(directory, newFilename).toPath());
    }

    boolean fileFilter(File file) {
        return file.getName().endsWith("_error.ndjson") || file.getName().endsWith(".ndjson");
    }
}