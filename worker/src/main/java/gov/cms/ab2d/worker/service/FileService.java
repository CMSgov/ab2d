package gov.cms.ab2d.worker.service;

import java.io.File;
import java.nio.file.Path;

public interface FileService {

    Path createDirectory(Path outputDir);

    String generateChecksum(File file);
}
