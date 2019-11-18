package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.StandardOpenOption.APPEND;

@Slf4j
@Component
public class FileService {


    Path createDirectory(Path outputDir) {
        Path outputDirectory = null;
        try {
            outputDirectory = Files.createDirectories(outputDir);
        } catch (IOException e) {
            final String errMsg = "Could not create output directory : ";
            log.error("{} : {}", errMsg, outputDir.toAbsolutePath());
            throw new RuntimeException(errMsg + outputDir.getFileName(), e);
        }
        return outputDirectory;
    }


    Path createFile(final Path outputDir, final String filename) {
        final Path filePath = Path.of(outputDir.toString(), filename);
        Path outputFile = null;
        try {
            outputFile = Files.createFile(filePath);
            log.info("Created file: {} ", outputFile.toAbsolutePath());
        } catch (IOException e) {
            final String errMsg = "Could not create output file : ";
            log.error("{} {} ", errMsg, filePath.toAbsolutePath(), e);
            throw new RuntimeException(errMsg + filename, e);
        }
        return outputFile;
    }


    void appendToFile(Path outputFile, ByteArrayOutputStream byteArrayOutputStream) throws IOException {
        Files.write(outputFile, byteArrayOutputStream.toByteArray(), APPEND);
    }
}