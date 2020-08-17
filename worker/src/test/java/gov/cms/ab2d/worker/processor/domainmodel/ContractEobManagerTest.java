package gov.cms.ab2d.worker.processor.domainmodel;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.adapter.bluebutton.ContractBeneficiaries;
import gov.cms.ab2d.worker.processor.EobTestDataUtil;
import gov.cms.ab2d.worker.processor.StreamHelper;
import gov.cms.ab2d.worker.processor.TextStreamHelperImpl;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(MockitoJUnitRunner.class)
class ContractEobManagerTest {
    @TempDir
    Path efsMountTmpDir;
    @Mock private LogManager eventLogger;
    private FhirContext context = FhirContext.forDstu3();
    private Date earliestDate = new Date();
    private OffsetDateTime attTime = OffsetDateTime.now();
    private ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
    private ContractBeneficiaries.PatientDTO patient;
    private String defaultPatientId = "-199900000022040";
    private StreamHelper helper;
    private String fileName = "ABCD_0001.ndjson";
    private String errorFileName = "ABCD_error.ndjson";
    private EobSearchResponse response;
    private Path filePath;
    private Path errorFilePath;

    @BeforeEach
    void setUp() throws ParseException, FileNotFoundException {
        MockitoAnnotations.initMocks(this);
        patient = new ContractBeneficiaries.PatientDTO();
        patient.setPatientId(defaultPatientId);
        filePath = Path.of(efsMountTmpDir.toString() + "/" + fileName);
        errorFilePath = Path.of(efsMountTmpDir.toString() + "/" + errorFileName);
        patient.setDateRangesUnderContract(Collections.singletonList(new FilterOutByDate.DateRange(new Date(0), new Date())));
        helper = new TextStreamHelperImpl(efsMountTmpDir, "ABCD",
                10000, 50, eventLogger, null);
        response = new EobSearchResponse(patient, Collections.singletonList(eob));
    }

    @AfterEach
    void shutdown() throws IOException {
        String data = FileUtils.readFileToString(filePath.toFile(), "UTF-8");
        System.out.println(data);
        Files.delete(filePath);
    }

    @Test
    void writeValidEobs() throws IOException {
        eob.getPatient().setReference("Patient/" + defaultPatientId);
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        assertEquals(0, cem.getUnknownEobs().size());
        assertEquals(0, cem.getValidEobs().size());
        cem.addResources(response);
        assertEquals(1, cem.getUnknownEobs().size());
        assertEquals(1, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        assertEquals(0, cem.getValidEobs().get(defaultPatientId).getResources().size());
        cem.writeValidEobs(helper);
        assertEquals(1, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        assertEquals(0, cem.getValidEobs().get(defaultPatientId).getResources().size());
        cem.validateResources(patient);
        assertEquals(0, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        assertEquals(1, cem.getValidEobs().get(defaultPatientId).getResources().size());
        cem.writeValidEobs(helper);
        helper.close();
        String data = getFileData(filePath);
        System.out.println(data.length() + " length");
        assertTrue(data != null && !data.isEmpty());
        assertEquals(0, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        assertEquals(0, cem.getValidEobs().get(defaultPatientId).getResources().size());
    }

    @Test
    void writeMultipleValidEobs() throws IOException {
        eob.getPatient().setReference("Patient/" + defaultPatientId);
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        cem.addResources(response);
        cem.addResources(response);
        cem.validateResources(patient);
        cem.writeValidEobs(helper);
        helper.close();
        String data = getFileData(filePath);
        assertTrue(data != null && !data.isEmpty());
        assertEquals(2 * 4311, data.length());
        assertEquals(0, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        assertEquals(0, cem.getValidEobs().get(defaultPatientId).getResources().size());
    }

    private String getFileData(Path file) throws IOException {
        return FileUtils.readFileToString(file.toFile(), "UTF-8");
    }

    @Test
    void writeInvalidEobs() throws IOException {
        eob.getPatient().setReference("Patient/INVALID");
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        cem.addResources(response);
        cem.validateResources(patient);
        cem.writeValidEobs(helper);
        helper.close();
        String data = getFileData(filePath);
        assertEquals(0, data.length());
    }

    @Test
    void writeInvalidEobsByDate() throws IOException, ParseException {
        patient.setDateRangesUnderContract(Collections.singletonList(new FilterOutByDate.DateRange(new Date(0), new Date(10000))));
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        cem.addResources(response);
        cem.validateResources(patient);
        cem.writeValidEobs(helper);
        helper.close();
        String data = getFileData(filePath);
        assertEquals(0, data.length());
    }

    @Test
    void addError() throws IOException {
        String val = "Test Exception";
        Exception exception = new Exception(val);
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        cem.handleException(helper, "Test Exception", exception);
        String data = getFileData(errorFilePath);
        assertEquals(val, data);
    }
}