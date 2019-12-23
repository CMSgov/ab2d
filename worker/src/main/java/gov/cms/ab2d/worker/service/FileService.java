package gov.cms.ab2d.worker.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public interface FileService {

    Path createDirectory(Path outputDir);

    Path createFile(Path outputDir, String filename);

    void appendToFile(Path outputFile, ByteArrayOutputStream byteArrayOutputStream) throws IOException;
}
