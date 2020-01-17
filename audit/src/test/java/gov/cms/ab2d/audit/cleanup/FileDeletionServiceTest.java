package gov.cms.ab2d.audit.cleanup;

import gov.cms.ab2d.audit.SpringBootApp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = SpringBootApp.class)
@TestPropertySource(locations = "/application.common.properties")
public class FileDeletionServiceTest {

    @TempDir
    File tmpDirFolder;

    @Autowired
    private FileDeletionService fileDeletionService;

    private static final String TEST_FILE = "testFile.ndjson";

    private static final String TEST_FILE_NOT_DELETED = "testFileNotDeleted.ndjson";

    private static final String TEST_DIRECTORY_NO_PERMISSIONS = "testDirectoryNoPermissions";

    private static final String TEST_FILE_NO_PERMISSIONS = TEST_DIRECTORY_NO_PERMISSIONS + "/testFileNoPermissions.ndjson";

    private static final String TEST_DIRECTORY = "testDirectory";

    private static final String TEST_FILE_NESTED = TEST_DIRECTORY + "/testFileInDirectory.ndjson";

    private static final String REGULAR_FILE = "regularFile.txt";

    // Change the creation time so that the file will be eligible for deletion
    private void changeFileCreationDate(Path path) throws IOException {
        BasicFileAttributeView attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class);
        FileTime time = FileTime.fromMillis(LocalDate.now().minus(2, ChronoUnit.DAYS).toEpochDay());
        attributes.setTimes(time, time, time);
    }

    @Test
    public void checkToEnsureFilesDeleted() throws IOException, URISyntaxException {
        String efsMount = tmpDirFolder.toPath().toString();

        // other tests set this value, so set it to the correct one, JUnit ordering annotations don't seem to be respected
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", efsMount);

        // Don't change the creation date on this file, but do so on the next ones
        Path destinationNotDeleted = Paths.get(efsMount, TEST_FILE_NOT_DELETED);
        URL urlNotDeletedFile = this.getClass().getResource("/" + TEST_FILE_NOT_DELETED);
        Path sourceNotDeleted = Paths.get(urlNotDeletedFile.toURI());
        Files.copy(sourceNotDeleted, destinationNotDeleted, StandardCopyOption.REPLACE_EXISTING);

        Path destination = Paths.get(efsMount, TEST_FILE);
        URL url = this.getClass().getResource("/" + TEST_FILE);
        Path source = Paths.get(url.toURI());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(destination);

        final Path noPermissionsDirPath = Paths.get(efsMount, TEST_DIRECTORY_NO_PERMISSIONS);
        File noPermissionsDir = new File(noPermissionsDirPath.toString());
        if (!noPermissionsDir.exists()) noPermissionsDir.mkdirs();

        Path noPermissionsFileDestination = Paths.get(efsMount, TEST_FILE_NO_PERMISSIONS);
        URL noPermissionsFileUrl = this.getClass().getResource("/" + TEST_FILE_NO_PERMISSIONS);
        Path noPermissionsFileSource = Paths.get(noPermissionsFileUrl.toURI());
        Files.copy(noPermissionsFileSource, noPermissionsFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(noPermissionsFileDestination);

        noPermissionsDir.setWritable(false);

        final Path dirPath = Paths.get(efsMount, TEST_DIRECTORY);
        File dir = new File(dirPath.toString());
        if (!dir.exists()) dir.mkdirs();

        Path nestedFileDestination = Paths.get(efsMount, TEST_FILE_NESTED);
        URL nestedFileUrl = this.getClass().getResource("/" + TEST_FILE_NESTED);
        Path nestedFileSource = Paths.get(nestedFileUrl.toURI());
        Files.copy(nestedFileSource, nestedFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(nestedFileDestination);

        Path regularFileDestination = Paths.get(efsMount, REGULAR_FILE);
        URL regularFileUrl = this.getClass().getResource("/" + REGULAR_FILE);
        Path regularFileSource = Paths.get(regularFileUrl.toURI());
        Files.copy(regularFileSource, regularFileDestination, StandardCopyOption.REPLACE_EXISTING);

        changeFileCreationDate(regularFileDestination);

        fileDeletionService.deleteFiles();

        assertTrue(Files.notExists(destination));

        assertTrue(Files.notExists(nestedFileDestination));

        assertTrue(Files.exists(destinationNotDeleted));

        assertTrue(Files.exists(noPermissionsFileDestination));

        assertTrue(Files.exists(regularFileDestination));

        // Cleanup
        Files.delete(destinationNotDeleted);

        noPermissionsDir.setWritable(true);
        FileSystemUtils.deleteRecursively(noPermissionsDir);

        FileSystemUtils.deleteRecursively(dir);

        Files.delete(regularFileDestination);
    }

    @Test
    public void testEFSMountSlash() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "~/UsersDir");

        var exceptionThrown = assertThrows(EFSMountFormatException.class,() ->
            fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS Mount must start with a /"));
    }

    @Test
    public void testEFSMountDirectorySize() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/a");

        var exceptionThrown = assertThrows(EFSMountFormatException.class,() ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS mount must be at least 5 characters"));
    }

    @Test
    public void testEFSMountDirectoryBlacklist() {
        ReflectionTestUtils.setField(fileDeletionService, "efsMount", "/usr/baddirectory");

        var exceptionThrown = assertThrows(EFSMountFormatException.class,() ->
                fileDeletionService.deleteFiles());
        assertThat(exceptionThrown.getMessage(), is("EFS mount must not start with a directory that contains important files"));
    }
}
