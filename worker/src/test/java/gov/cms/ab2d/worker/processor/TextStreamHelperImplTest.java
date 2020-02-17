package gov.cms.ab2d.worker.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextStreamHelperImplTest {
    @TempDir
    File tmpDirFolder;

    @Test
    void createFileName() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 20);
        assertEquals("C1111_0002.ndjson", helper.createFileName());
        helper.close();
    }

    @Test
    void addError() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 20);
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
        TextStreamHelperImpl helper = new TextStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 20);
        helper.close();
        List<Path> dataFiles = helper.getDataFiles();
        assertTrue(dataFiles.isEmpty());
    }

    @Test
    void getDataFilesWithLongData() throws IOException {
        TextStreamHelperImpl helper = new TextStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 20);
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
        TextStreamHelperImpl helper = new TextStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 20);
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
}