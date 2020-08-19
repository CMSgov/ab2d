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
import org.hl7.fhir.dstu3.model.Period;
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
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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

    @Test
    void testGetYearMonth() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Date d = sdf.parse("12/31/1999");
        assertEquals(1999, ContractEobManager.getYear(d));
        assertEquals(12, ContractEobManager.getMonth(d));
    }

    @Test
    void testGetCoveredMonth() throws ParseException {
        Period p = new Period();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        p.setStart(sdf.parse("12/31/1999"));
        p.setEnd(sdf.parse("01/01/2000"));
        List<Integer> covered = ContractEobManager.getCoveredMonths(p);
        assertTrue(covered.size() == 2 && covered.contains(12) && covered.contains(1));

        p.setStart(sdf.parse("02/01/1999"));
        p.setEnd(sdf.parse("02/28/1999"));
        covered = ContractEobManager.getCoveredMonths(p);
        assertTrue(covered.size() == 1 && covered.contains(2));

        p.setStart(sdf.parse("02/01/1999"));
        p.setEnd(sdf.parse("04/01/1999"));
        covered = ContractEobManager.getCoveredMonths(p);
        assertTrue(covered.size() == 3 && covered.contains(2) && covered.contains(3) && covered.contains(4));

        p.setStart(sdf.parse("02/01/1999"));
        p.setEnd(sdf.parse("04/01/2000"));
        covered = ContractEobManager.getCoveredMonths(p);
        assertTrue(covered.size() == 12);

        p.setStart(sdf.parse("12/31/2000"));
        p.setEnd(sdf.parse("01/01/2002"));
        covered = ContractEobManager.getCoveredMonths(p);
        assertTrue(covered.size() == 12);
    }

    @Test
    void testCovered() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        eob.getPatient().setReference("Patient/" + defaultPatientId);
        eob.getBillablePeriod().setStart(sdf.parse("01/01/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("01/05/2020"));
        assertTrue(ContractEobManager.covered(eob, List.of(1)));
        assertFalse(ContractEobManager.covered(eob, List.of(2, 3, 4, 5, 6, 7)));
        eob.getBillablePeriod().setStart(sdf.parse("01/01/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("03/05/2020"));
        assertTrue(ContractEobManager.covered(eob, List.of(2, 3, 4, 5, 6, 7)));
        eob.getBillablePeriod().setStart(sdf.parse("03/01/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("08/05/2020"));
        assertTrue(ContractEobManager.covered(eob, List.of(4)));
    }

    @Test
    void testRemoveInvalidPatients() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        ExplanationOfBenefit eob = EobTestDataUtil.createEOB();
        eob.getPatient().setReference("Patient/" + defaultPatientId);
        eob.getBillablePeriod().setStart(sdf.parse("01/01/2020"));
        eob.getBillablePeriod().setEnd(sdf.parse("01/05/2020"));
        ContractEobManager cem = new ContractEobManager(context, false, earliestDate, attTime);
        response = new EobSearchResponse(patient, Collections.singletonList(eob));
        cem.addResources(response);
        cem.cleanUpKnownInvalidPatients(Arrays.asList(1));
        assertEquals(0, cem.getUnknownEobs().get(defaultPatientId).getResources().size());

        response = new EobSearchResponse(patient, Collections.singletonList(eob));
        cem.addResources(response);
        cem.cleanUpKnownInvalidPatients(Arrays.asList(2));
        assertEquals(1, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        cem.cleanUpKnownInvalidPatients(Arrays.asList(2, 3, 4, 5, 6, 7, 8));
        assertEquals(1, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
        cem.cleanUpKnownInvalidPatients(Arrays.asList(1, 2, 3));
        assertEquals(0, cem.getUnknownEobs().get(defaultPatientId).getResources().size());
    }
}