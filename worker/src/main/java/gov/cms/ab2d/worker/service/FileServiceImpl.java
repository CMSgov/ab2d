package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;

@Slf4j
@Component
public class FileServiceImpl implements FileService {


    @Override
    public Path createDirectory(Path outputDir) {
        Path outputDirectory = null;
        try {
            if (Files.exists(outputDir)) {
                throw new IOException("Directory already exists");
            }

            outputDirectory = Files.createDirectories(outputDir);
        } catch (IOException e) {
            final String errMsg = "Could not create output directory : ";
            log.error("{} : {}", errMsg, outputDir.toAbsolutePath());
            throw new UncheckedIOException(errMsg + outputDir.getFileName(), e);
        }
        return outputDirectory;
    }


    @Override
    public Path createOrReplaceFile(final Path outputDir, final String filename) {
        final Path filePath = Path.of(outputDir.toString(), filename);
        Path outputFile = null;
        try {
            //Delete file from previous run - Allows for a job can be restarted on failure.
            Files.deleteIfExists(filePath);

            // create a brand new file for the current run.
            outputFile = Files.createFile(filePath);
            log.info("Created file: {} ", outputFile.toAbsolutePath());
        } catch (IOException e) {
            final String errMsg = "Could not create output file : ";
            log.error("{} {} ", errMsg, filePath.toAbsolutePath(), e);
            throw new UncheckedIOException(errMsg + filename, e);
        }
        return outputFile;
    }


    @Override
    public void appendToFile(Path outputFile, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        Files.write(outputFile, byteArrayOutputStream.toByteArray(), APPEND);
    }
}