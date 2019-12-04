package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
public class FileDeletionServiceTest {

    @Value("${efs.mount}")
    private String efsMount;

    @Autowired
    private FileDeletionService fileDeletionService;

    private static final String TEST_FILE = "testFile.ndjson";

    private static final String TEST_FILE_NOT_DELETED = "testFileNotDeleted.ndjson";

    private static final String TEST_DIRECTORY_NO_PERMISSIONS = "testDirectoryNoPermissions";

    private static final String TEST_FILE_NO_PERMISSIONS = TEST_DIRECTORY_NO_PERMISSIONS + "/testFileNoPermissions.ndjson";

    private static final String TEST_DIRECTORY = "testDirectory";

    private static final String TEST_FILE_NESTED = TEST_DIRECTORY + "/testFileInDirectory.ndjson";

    // Change the creation time so that the file will be eligible for deletion
    private void changeFileCreationDate(Path path) throws IOException {
        BasicFileAttributeView attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis(LocalDate.now().minus(2, ChronoUnit.DAYS).toEpochDay());
        attributes.setTimes(time, time, time);
    }

    @Test
    public void checkToEnsureFilesDeleted() throws IOException, URISyntaxException {
        // Don't change the creation date on this file, but do so on the next ones
        Path destinationNotDeleted = Paths.get(efsMount, TEST_FILE_NOT_DELETED);
        URL urlNotDeletedFile = this.getClass().getResource("/" + TEST_FILE_NOT_DELETED);
        Path sourceNotDeleted = Paths.get(urlNotDeletedFile.toURI());
        Files.move(sourceNotDeleted, destinationNotDeleted, StandardCopyOption.REPLACE_EXISTING);

        Path destination = Paths.get(efsMount, TEST_FILE);
        URL url = this.getClass().getResource("/" + TEST_FILE);
        Path source = Paths.get(url.toURI());
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destination);

        File noPermissionsDir = new File(efsMount + TEST_DIRECTORY_NO_PERMISSIONS);
        if (!noPermissionsDir.exists()) noPermissionsDir.mkdirs();

        Path noPermissionsFileDestination = Paths.get(efsMount, TEST_FILE_NO_PERMISSIONS);
        URL noPermissionsFileUrl = this.getClass().getResource("/" + TEST_FILE_NO_PERMISSIONS);
        Path noPermissionsFileSource = Paths.get(noPermissionsFileUrl.toURI());
        Files.move(noPermissionsFileSource, noPermissionsFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(noPermissionsFileDestination);

        noPermissionsDir.setWritable(false);

        File dir = new File(efsMount + TEST_DIRECTORY);
        if (!dir.exists()) dir.mkdirs();

        Path nestedFileDestination = Paths.get(efsMount, TEST_FILE_NESTED);
        URL nestedFileUrl = this.getClass().getResource("/" + TEST_FILE_NESTED);
        Path nestedFileSource = Paths.get(nestedFileUrl.toURI());
        Files.move(nestedFileSource, nestedFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(nestedFileDestination);

        fileDeletionService.deleteFiles();

        Assert.assertTrue(Files.notExists(destination));

        Assert.assertTrue(Files.notExists(nestedFileDestination));

        Assert.assertTrue(Files.exists(destinationNotDeleted));

        Assert.assertTrue(Files.exists(noPermissionsFileDestination));
    }
}
