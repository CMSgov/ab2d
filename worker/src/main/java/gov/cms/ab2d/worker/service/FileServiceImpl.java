package gov.cms.ab2d.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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


}