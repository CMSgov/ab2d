package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class FileServiceImpl implements FileService {

    /**
     * Given a path, create a directory and return its path
     *
     * @param outputDir - the directory to create
     * @return the Path to the created directory
     */
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
    public byte[] generateChecksum(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            byte[] bytes = DigestUtils.sha256(fileInputStream);
            return bytes;
        } catch (IOException e) {
            log.error("Encountered IO Exception while generating checksum {}", e.getMessage(), e);
            throw new UncheckedIOException(e);
        }
    }
}