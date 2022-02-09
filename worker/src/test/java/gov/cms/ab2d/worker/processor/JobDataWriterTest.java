package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.eventlogger.LogManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JobDataWriterTest {

    @TempDir
    Path tempDir;

    @Mock
    private LogManager eventLogger;
    private StreamHelper cut;
    private String poem = "Twinkle Twinkle Little Star";
    private byte[] line = poem.getBytes();

    @BeforeEach
    void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        ContractWorkerDto contract = new ContractWorkerDto();
        contract.setContractNumber("CONTRACT_NUMBER");
        contract.setContractName("CONTRACT_NAME");
        var OutputDirPath = Paths.get(tempDir.toString(), contract.getContractName());
        var outputDir = Files.createDirectory(OutputDirPath);
        cut = new TextStreamHelperImpl(outputDir, contract.getContractNumber(), 30, 50,
                eventLogger, null);
    }

    @Test
    void addOneDataEntry_createsOneDataFile() throws IOException {
        cut.addData(line);
        cut.close();

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(1));

        var size = dataFiles.iterator().next().toFile().length();
        assertThat(size, is((long) line.length));
    }

    @Test
    void addTwoEntriesThatCrossesMaxFileSize_shouldCreateMultipleFiles() throws IOException {
        cut.addData(line);
        cut.addData(line);
        cut.close();

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(2));
        dataFiles.forEach(file -> {
            var size = file.toFile().length();
            assertThat(size, is((long) line.length));
        });
    }

    @Test
    void addThreeEntriesThatCrossesMaxFileSize_shouldCreateMultipleFiles() throws IOException {
        cut.addData(line);
        cut.addData(line);
        cut.addData(line);
        cut.close();

        var dataFiles = cut.getDataFiles();
        assertThat(dataFiles.size(), is(3));
        dataFiles.forEach(file -> {
            var size = file.toFile().length();
            assertThat(size, is((long) line.length));
        });
    }

    @Test
    void addOneErrorEntry_createsOneErrorFile() throws IOException {
        cut.addError(poem);
        cut.close();
        var errorFiles = cut.getErrorFiles();
        assertThat(errorFiles.size(), is(1));
    }

    @Test
    void addMultipleErrorEntries_createsOneErrorFile() throws IOException {
        cut.addError(poem);
        cut.addError(poem);
        cut.addError(poem);
        var errorFiles = cut.getErrorFiles();
        cut.close();
        assertThat(errorFiles.size(), is(1));
    }
}