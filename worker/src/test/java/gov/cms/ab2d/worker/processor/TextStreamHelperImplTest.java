package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.eventlogger.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class TextStreamHelperImplTest {
    @TempDir
    File tmpDirFolder;
    @Mock
    private LogManager eventLogger;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createFileName() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        assertEquals("C1111_0002.ndjson", helper.createFileName());
        helper.close();
    }

    @Test
    void testSomePermsAppend() throws IOException {
        Path loc = Path.of(tmpDirFolder + "/testfile");
        Files.createFile(loc);
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                Path.of(tmpDirFolder.toString()), "C1111", 10, 20, eventLogger, null);
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("r-xr-xr-x");
        Files.setPosixFilePermissions(loc, permissions);
        byte[] val = new byte[2];
        val[0] = 'a';
        val[1] = 'b';
        assertThrows(UncheckedIOException.class, () -> helper.appendToFile(loc, val));
        Set<PosixFilePermission> permissionsBack = PosixFilePermissions.fromString("rw-r-xr-x");
        // Set them back so that the junit can remove directory
        Files.setPosixFilePermissions(loc, permissionsBack);
        helper.close();
    }

    @Test
    void testSomePerms() throws IOException {
        byte[] val = new byte[2];
        val[0] = 'a';
        val[1] = 'b';
        Path loc = Path.of(tmpDirFolder + "/testdir");
        Files.createDirectory(loc);
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("r-xr-xr-x");
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                loc, "C1111", 10, 20, eventLogger, null);
        Files.setPosixFilePermissions(loc, permissions);
        helper.addData(null);
        helper.addData(val);
        assertThrows(UncheckedIOException.class, () -> helper.createErrorFile());
        Set<PosixFilePermission> permissionsBack = PosixFilePermissions.fromString("rwxr-xr-x");
        // Set them back so that the junit can remove directory
        Files.setPosixFilePermissions(loc, permissionsBack);
        helper.close();
    }

    @Test
    void appendToFileTest() throws IOException {
        assertThrows(NullPointerException.class, () -> new TextStreamHelperImpl(
                null, "C1111", 10, 20, eventLogger, null));
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 1, eventLogger, null);
        assertThrows(NullPointerException.class, () -> helper.tryLock(null));
        helper.close();
    }

    @Test
    void addError() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        List<Path> errorFiles = helper.getErrorFiles();
        assertTrue(errorFiles.isEmpty());
        helper.addError("Error Info\n");
        helper.addError("Error Info 2\n");
        helper.close();
        errorFiles = helper.getErrorFiles();
        assertEquals(errorFiles.size(), 1);
        List<String> lines = Files.readAllLines(errorFiles.get(0));
        assertEquals(2, lines.size());
        assertEquals(lines.get(0), "Error Info");
        assertEquals(lines.get(1), "Error Info 2");
    }

    @Test
    void getEmptyDataFiles() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        helper.close();
        List<Path> dataFiles = helper.getDataFiles();
        assertTrue(dataFiles.isEmpty());
    }

    @Test
    void getDataFilesWithLongData() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        String longString = "Once upon a time in America, there lived a sweet girl who wandered the planet";
        helper.addData(longString.getBytes());
        helper.close();
        List<Path> dataFiles = helper.getDataFiles();
        assertEquals(1, dataFiles.size());
        List<String> lines = Files.readAllLines(dataFiles.get(0));
        assertEquals(longString, lines.get(0));
    }

    @Test
    void getDataFiles() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        String shortString = "Hello";
        helper.addData(shortString.getBytes());
        String shortString2 = "W";
        helper.addData(shortString2.getBytes());
        String medString = "Should 8";
        helper.addData(medString.getBytes());
        String longString = "Once upon a time in America, there lived a sweet girl who wandered the planet";
        helper.addData(longString.getBytes());
        helper.close();
        List<Path> dataFiles = helper.getDataFiles();
        assertEquals(3, dataFiles.size());
        List<String> lines = Files.readAllLines(dataFiles.get(0));
        assertEquals(shortString + shortString2, lines.get(0));
        lines = Files.readAllLines(dataFiles.get(1));
        assertEquals(medString, lines.get(0));
        lines = Files.readAllLines(dataFiles.get(2));
        assertEquals(longString, lines.get(0));
    }

    @Test
    void getRolloverDataFile() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        String shortString = "Hello";

        // Did not rollover so no file
        helper.addData(shortString.getBytes());
        assertTrue(helper.getDataOutputs().isEmpty());

        String longString = "Once upon a time in America, there lived a sweet girl who wandered the planet";
        helper.addData(longString.getBytes());
        assertFalse(helper.getDataOutputs().isEmpty());
        checkStreamOutput(helper.getDataOutputs().get(0));

        helper.closeLastStream();
        assertEquals(2, helper.getDataOutputs().size());
        checkStreamOutput(helper.getDataOutputs().get(1));
        helper.close();
    }

    @Test
    void closeEmptyFileNothingReturned() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);

        helper.closeLastStream();
        assertTrue(helper.getDataOutputs().isEmpty());

        // Attempting to write data after last file
        assertThrows(IOException.class, () -> helper.addData("Hello".getBytes()));

        // Close helper file
        assertDoesNotThrow(helper::close);
    }

    @Test
    void throwTryLockExceptionTest() throws IOException, InterruptedException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(
                tmpDirFolder.toPath(), "C1111", 10, 20, eventLogger, null);
        Lock lock = Mockito.mock(Lock.class);
        when(lock.tryLock(anyLong(),any(TimeUnit.class))).thenThrow(InterruptedException.class);
        RuntimeException e =assertThrows(RuntimeException.class,() -> helper.tryLock(lock));
        assertEquals("Terminate processing. Unable to acquire lock",e.getMessage());

        when(lock.tryLock(anyLong(),any(TimeUnit.class))).thenReturn(false);
        e = assertThrows(RuntimeException.class,() -> helper.tryLock(lock));
        assertEquals("Terminate processing. Unable to acquire lock after waiting 20 seconds.",e.getMessage());

        helper.close();
    }

    private void checkStreamOutput(StreamOutput streamOutput) {
        assertFalse(streamOutput.getFilePath().isEmpty());
        assertNotNull(streamOutput.getChecksum());
        assertFalse(streamOutput.getChecksum().isEmpty());
        assertNotEquals(0, streamOutput.getFileLength());
        assertFalse(streamOutput.getError());
    }
}