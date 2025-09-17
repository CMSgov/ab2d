package gov.cms.ab2d.aggregator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * This manages all the creating and streaming of beneficiary data. To use this, you create a try
 * with resources with the ClaimsStream. For example:
 *
 * try (ClaimsStream stream = new ClaimsStream(jobId, false, DATA, "streaming", "finished")) {
 *     for (int i = 0; i < beneBatchSize; i++) {
 *         stream.write(getNdJson(benes.get(i)));
 *     }
 * } catch(Exception ex) { }
 *
 * This does all the work of creating a temporary file in the correct location to stream, allowing
 * streams to be written to and when the file is closed, it moves it to the "finished" directory, waiting
 * for the aggregator to pick it up
 */
public class ClaimsStream implements AutoCloseable {
    private static final String FILE_PREFIX = "tmp_";
    private final BufferedOutputStream bout;
    private final File tmpFile;
    private final File completeFile;
    private final FileOutputType type;
    private final String jobDir;
    private boolean open;
    private final FileOutputStream stream;
    private final String streamingDir;

    public ClaimsStream(String jobId, String baseDir, FileOutputType type, String streamingDir, String finishedDir) throws IOException {
        this(jobId, baseDir, type, streamingDir, finishedDir, 0);
    }

    public ClaimsStream(String jobId, String baseDir, FileOutputType type, String streamingDir, String finishedDir, int bufferSize) throws IOException {
        this.type = type;
        this.open = true;
        this.jobDir = Path.of(baseDir, jobId).toFile().getAbsolutePath();
        this.streamingDir = streamingDir;
        JobHelper.workerSetUpJobDirectories(jobId, baseDir, streamingDir, finishedDir);
        this.tmpFile = createNewFile();
        this.stream = new FileOutputStream(tmpFile);
        if (bufferSize > 0) {
            this.bout = new BufferedOutputStream(stream, bufferSize);
        } else {
            // Use the default buffer size
            this.bout = new BufferedOutputStream(stream);
        }
        File directory = new File(jobDir + File.separator + finishedDir);
        String file = tmpFile.getName();
        this.completeFile = Path.of(directory.getAbsolutePath(), file).toFile();
    }

    @Override
    public void close() throws IOException {
        bout.flush();
        bout.close();
        if (stream != null) {
            stream.close();
        }
        this.open = false;
        moveFileToDone();
    }

    public File getFile() {
        if (this.open) {
            return tmpFile;
        } else {
            return completeFile;
        }
    }

    public boolean isOpen() {
        return this.open;
    }

    public boolean moveFileToDone() {
        return tmpFile.renameTo(completeFile);
    }

    public void write(String eobNdJson) throws IOException {
        bout.write(eobNdJson.getBytes(StandardCharsets.UTF_8));
    }

    public void flush() throws IOException {
        bout.flush();
    }

    private File createNewFile() throws IOException {
        String suffix = type.getSuffix();
        File directory = new File(this.jobDir + File.separator + this.streamingDir);
        return File.createTempFile(FILE_PREFIX, suffix, directory);
    }
}
