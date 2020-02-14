package gov.cms.ab2d.worker.processor;

import java.nio.file.Path;
import java.util.List;

public interface StreamHelper {
    void addData(byte[] data);
    void addError(String data);
    List<Path> getDataFiles();
    List<Path> getErrorFiles();
    void close();
}
