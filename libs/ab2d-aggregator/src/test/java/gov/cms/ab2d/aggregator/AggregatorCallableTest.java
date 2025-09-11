package gov.cms.ab2d.aggregator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static gov.cms.ab2d.aggregator.Aggregator.ONE_MEGA_BYTE;
import static gov.cms.ab2d.aggregator.FileOutputType.DATA;
import static gov.cms.ab2d.aggregator.FileOutputType.ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class AggregatorCallableTest {
    private static final String JOB_ID = "jobby";
    private static final String FILE_1 = "file1.txt";
    private static final String STREAM_DIR = "streaming";
    private static final String FINISH_DIR = "finished";
    private static final int MAX_MEG = 1;
    private static final int MULTIPLIER = 2;
    private final transient  ExecutorService executor = Executors.newFixedThreadPool(10);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MIB = 1048576;

    @Test
    void testDoItAllWithDataFile(@TempDir File tmpDirFolder) throws IOException, InterruptedException {
        long t1 = System.currentTimeMillis();
        AggregatorCallable callable = new AggregatorCallable(tmpDirFolder.getAbsolutePath(), JOB_ID, "contract",
                MAX_MEG, STREAM_DIR, FINISH_DIR, MULTIPLIER);
        JobHelper.workerSetUpJobDirectories(JOB_ID, tmpDirFolder.getAbsolutePath(), STREAM_DIR, FINISH_DIR);
        Future<Integer> future = executor.submit(callable);
        // For each batch
        for (int i = 0; i < 100; i++) {
            // Create a file for the batch of beneficiaries
            try (ClaimsStream stream = new ClaimsStream(JOB_ID, tmpDirFolder.getAbsolutePath(), DATA, STREAM_DIR, FINISH_DIR, MIB)) {
                // For each beneficiary
                for (int b = 0; b < 250; b++) {
                    int length = RANDOM.nextInt(1000);
                    stream.write(AggregatorTest.getAlphaNumericString(length) + "\n");
                }
            } catch (Exception ex) {
                fail(ex);
            }
        }
        JobHelper.workerFinishJob(tmpDirFolder.getAbsolutePath() + File.separator + JOB_ID + File.separator + STREAM_DIR);
        while (!future.isDone()) {
            Thread.sleep(1000);
        }
        long t2 = System.currentTimeMillis();
        System.out.println("Time is: " + (t2 - t1) / 1000);
    }

    @Test
    void testDoItAllWithErrorFile(@TempDir File tmpDirFolder) throws IOException, InterruptedException, ExecutionException {
        AggregatorCallable callable = new AggregatorCallable(
            tmpDirFolder.getAbsolutePath(), JOB_ID, "contract", MAX_MEG, STREAM_DIR, FINISH_DIR, MULTIPLIER
        );
        JobHelper.workerSetUpJobDirectories(JOB_ID, tmpDirFolder.getAbsolutePath(), STREAM_DIR, FINISH_DIR);
        Future<Integer> future = executor.submit(callable);
        // For each batch
        for (int i = 0; i < 100; i++) {
            // Create a file for the batch of beneficiaries
            ClaimsStream stream = new ClaimsStream(JOB_ID, tmpDirFolder.getAbsolutePath(), ERROR, STREAM_DIR, FINISH_DIR, MIB);
            // For each beneficiary
            for (int b = 0; b < 250; b++) {
                int length = RANDOM.nextInt(1000);
                stream.write(AggregatorTest.getAlphaNumericString(length) + "\n");
            }
            stream.close();
        }
        JobHelper.workerFinishJob(tmpDirFolder.getAbsolutePath() + File.separator + JOB_ID + File.separator + STREAM_DIR);
        // assert number of aggregations
        assertEquals(13, future.get());
    }

    // Disabled because it takes a long time but keeping as it is useful when you are doing performance tests
    @Disabled
    @Test
    void combineBigFiles(@TempDir File tmpDirFolder) throws IOException {
        String jobDir = tmpDirFolder.getAbsolutePath() + File.separator + JOB_ID;

        FileUtils.createADir(jobDir);
        System.out.println("Creating files");
        AggregatorTest.writeToFile(jobDir + File.separator + FILE_1, 100 * ONE_MEGA_BYTE);
        System.out.println("Now copy first file");
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file2.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file3.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file4.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file5.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file6.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file7.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file8.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file9.txt"));
        Files.copy(Path.of(jobDir, FILE_1), Path.of(jobDir, "file10.txt"));
        System.out.println("Done creating files");
        long t1 = System.currentTimeMillis();
        FileUtils.combineFiles(List.of(new File(jobDir + "/file1.txt"),
                        new File(jobDir + "/file2.txt"),
                        new File(jobDir + "/file3.txt"),
                        new File(jobDir + "/file4.txt"),
                        new File(jobDir + "/file5.txt"),
                        new File(jobDir + "/file6.txt"),
                        new File(jobDir + "/file7.txt"),
                        new File(jobDir + "/file8.txt"),
                        new File(jobDir + "/file9.txt"),
                        new File(jobDir + "/file10.txt")
                ),
                jobDir + "/outfile.txt");
        long t2 = System.currentTimeMillis();
        System.out.println("Combining files to (" + (t2 - t1) + ")");
    }
}
