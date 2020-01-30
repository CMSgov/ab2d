package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JobDataWriterTest {

    @TempDir Path tempDir;

    private JobDataWriter cut;
    byte[] line;


    @BeforeEach
    void setup() throws IOException {
        Contract contract = new Contract();
        contract.setContractNumber("CONTRACT_NUMBER");
        contract.setContractName("CONTRACT_NAME");
        var OutputDirPath = Paths.get(tempDir.toString(), contract.getContractName());
        var outputDir = Files.createDirectory(OutputDirPath);
        cut = new JobDataWriterImpl(outputDir, contract, 30, 50);

        var poem = "Twinkle Twinkle Little Star";
        line = poem.getBytes();
    }

    @Test
    void addOneDataEntry_createsOneDataFile() {

        cut.addDataEntry(line);

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(1));

        var size = dataFiles.iterator().next().toFile().length();
        assertThat(size, is(Long.valueOf(line.length)));
    }

    @Test
    void addTwoEntriesThatCrossesMaxFileSize_shouldCreateMultipleFiles() {
        cut.addDataEntry(line);
        cut.addDataEntry(line);

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(2));
        dataFiles.forEach(file -> {
            var size = file.toFile().length();
            assertThat(size, is(Long.valueOf(line.length)));
        });
    }

    @Test
    void addThreeEntriesThatCrossesMaxFileSize_shouldCreateMultipleFiles() {
        cut.addDataEntry(line);
        cut.addDataEntry(line);
        cut.addDataEntry(line);

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(3));
        dataFiles.forEach(file -> {
            var size = file.toFile().length();
            assertThat(size, is(Long.valueOf(line.length)));
        });
    }

    @Test
    void addOneErrorEntry_createsOneErrorFile() {
        cut.addErrorEntry(line);
        var errorFiles = cut.getErrorFiles();
        assertThat(errorFiles.size(), is(1));
    }

    @Test
    void addMultipleErrorEntries_createsOneErrorFile() {
        cut.addErrorEntry(line);
        cut.addErrorEntry(line);
        cut.addErrorEntry(line);
        var errorFiles = cut.getErrorFiles();
        assertThat(errorFiles.size(), is(1));
    }


}