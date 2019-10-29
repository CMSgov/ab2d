package gov.cms.ab2d.common.util;

import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class FileUtil {

    @VisibleForTesting
    public static void createTmpFileForDownload(String jobID, String fileName) throws IOException {
        List<String> lines = List.of("{",
                "  \"test\": \"value\",",
                "  \"array\": [",
                "    \"val1\",",
                "    \"val2\"",
                "  ]",
                "}");

        String path = System.getProperty("java.io.tmpdir") + File.separator + "jobdownloads" + File.separator + jobID;

        Files.createDirectories(Paths.get(path));

        Files.write(Paths.get(path + File.separator + fileName),
                lines,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }
}
