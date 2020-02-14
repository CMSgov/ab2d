package gov.cms.ab2d.worker.processor;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipStreamHelperImplTest {

    @TempDir
    File tmpDirFolder;

    @Test
    void createFileName() throws IOException {
        ZipStreamHelperImpl helper = new ZipStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 100, 100, 20);
        assertEquals("C1111_0001.ndjson", helper.createFileName());
        assertEquals("C1111_0002.ndjson", helper.createFileName());
        assertEquals("C1111_0003.ndjson", helper.createFileName());
        assertEquals("C1111_0004.ndjson", helper.createFileName());
        assertEquals("C1111_0002.zip", helper.createZipFileName());
        assertEquals("C1111_0003.zip", helper.createZipFileName());
        assertEquals("C1111_0004.zip", helper.createZipFileName());
        assertEquals("C1111_0005.zip", helper.createZipFileName());
        helper.close();
    }

    @Test
    void addData() throws IOException {
        testZips("C0001", 5000, 2000, 100, 10, 200);
        testZips("C0002", 100000, 20000, 5000, 100, 1000);
    }

    /**
     * Convenience method to test the zip file writing. It takes in different parameters, generates fake data,
     * generates the zip file and it's entries and afterwards, verifies that the files names were correct and
     * the unzipped data was exactly the same as the fake data.
     *
     * @param contractId - a contract Id
     * @param totalBytesAllowedInFile - the total number of bytes allowed for a zip file
     * @param totalAllowedInPart - total bytes allowed in a file entry
     * @param numberStrings - the number of strings to add to the zip file
     * @param minStringSize - the minimum size of the string to add
     * @param maxStringSize - the maximum size of the string to add
     *
     * @throws FileNotFoundException - if there was an error writing the files
     */
    void testZips(String contractId, int totalBytesAllowedInFile, int totalAllowedInPart,
                  int numberStrings, int minStringSize, int maxStringSize) throws FileNotFoundException {
        System.out.println("\nRun Test: " + contractId + " - " + totalBytesAllowedInFile +
                "(" + totalAllowedInPart + ") - Num Lines - " + numberStrings);
        int zipCounter = 1;
        int partCounter = 1;

        // Create the zip helper
        ZipStreamHelperImpl helper = new ZipStreamHelperImpl(
                tmpDirFolder.toPath(), contractId, totalBytesAllowedInFile, totalAllowedInPart, 20);
        // Get the test data and write it to the zip file
        List<String> testVals = addTestData(numberStrings, minStringSize, maxStringSize, helper);
        // Close it and close any streams
        helper.close();
        // Get the zip files created
        List<Path> zipFiles = helper.getDataFiles();
        assertTrue(zipFiles.size() > 0);
        // Unzip the data and create a string with all the values. Make sure the names were written
        // correctly and we get the same data as input as was outputted.
        StringBuilder output = new StringBuilder();
        for (Path zip : zipFiles) {
            String fileName = zip.getFileName().toString();
            long size = zip.toFile().length();
            System.out.println("Unpacking " + fileName + " - size: " + size);
            assertTrue(fileName.endsWith("0" + zipCounter + ".zip"));
            try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip.toString()))) {
                ZipEntry entry = zipIn.getNextEntry();
                while (entry != null) {
                    String entryName = entry.getName();
                    assertTrue(entryName.endsWith("0" + partCounter + ".ndjson"));
                    output.append(extractFileData(entry, zipIn));
                    partCounter++;
                    entry = zipIn.getNextEntry();
                }
            } catch (Exception ex) {
                fail(ex);
            }
            zipCounter++;
        }
        System.out.println("Average Compression: " + helper.getAverageCompression());
        // Make sure we loaded the same data that we tried to write
        String testValString = String.join("", testVals);
        assertEquals(testValString, output.toString());
        // Make sure there are no errors
        List<Path> errorFiles = helper.getErrorFiles();
        assertTrue(errorFiles.isEmpty());
    }

    /**
     * Add test data to the zip file. If we create a string of length 5, it will be 4 characters and one
     * carriage return;
     *
     * @param numIters - The number of strings to write
     * @param minStringSize - the smallest size of a string
     * @param maxStringSize - the largest size of a string
     * @param helper - the helper to add data to
     * @return a list of test data
     */
    private List<String> addTestData(int numIters, int minStringSize, int maxStringSize, StreamHelper helper) {
        List<String> testVals = new ArrayList<>();
        for (int i = 0; i < numIters; i++) {
            int size = getRandomBetween(minStringSize, maxStringSize);
            String s = RandomStringUtils.randomAlphanumeric(size - 1) + "\n";
            testVals.add(s);
            helper.addData(s.getBytes());
        }
        return testVals;
    }

    @Test
    public void testRandom() {
        boolean tenFound = false;
        boolean twentyFound = false;
        for (int i = 0; i < 10000; i++) {
            int val = getRandomBetween(10, 20);
            if (val == 10) {
                tenFound = true;
            }
            if (val == 20) {
                twentyFound = true;
            }
            assertTrue(val >= 10 && val <= 20);
        }
        for (int i = 0; i < 10000; i++) {
            int val = getRandomBetween(10, 10);
            assertEquals(10, val);
        }
    }

    /**
     * Given a start and end value, get a random value between those two values (inclusively). For example,
     * between 10 and 12 could result in 10, 11, 12.
     *
     * @param start - the lowest number
     * @param end - the highest number
     * @return the random number
     */
    private int getRandomBetween(int start, int end) {
        double v = Math.random();
        int increment = (int) Math.floor(v * (end + 1 - start));
        int val = start + increment;
        if (val > end) {
            return end;
        }
        return val;
    }

    /**
     * Return the contents of a zip file entry as a string
     *
     * @param entry - the zip entry
     * @param zipIn - the zip stream
     * @return the contents of the zip file entry
     * @throws IOException if there was an error reading the file
     */
    private String extractFileData(ZipEntry entry, ZipInputStream zipIn) throws IOException {
        StringBuilder result = new StringBuilder();
        int read;
        while ((read = zipIn.read()) != -1) {
            result.append((char) read);
        }
        System.out.println("    Entry " + entry.getName() + " - size: " + result.length());
        return result.toString();
    }

    @Test
    void addError() throws IOException {
        ZipStreamHelperImpl helper = new ZipStreamHelperImpl(tmpDirFolder.toPath(), "C1111", 10, 10, 20);
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
}