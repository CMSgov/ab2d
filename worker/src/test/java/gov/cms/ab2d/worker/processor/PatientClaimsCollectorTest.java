package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.util.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static org.junit.jupiter.api.Assertions.*;

public class PatientClaimsCollectorTest {

    private final static SimpleDateFormat SDF = new SimpleDateFormat("MM/dd/yyyy");
    private static final Date EPOCH = Date.from(ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, AB2D_ZONE).toInstant());
    private final static Long PATIENT_ID = -199900000022040L;

    private static final OffsetDateTime EARLY_ATT_DATE = OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime LATER_ATT_DATE = OffsetDateTime.of(2020, 2, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    static {
        SDF.setTimeZone(FilterOutByDate.TIMEZONE);
    }

    private static final ExplanationOfBenefit EOB = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
    private static final IBaseBundle BUNDLE = EobTestDataUtil.createBundle(EOB.copy());

    private final Token noOpToken = new Token() {
        @Override
        public boolean link() {
            return false;
        }

        @Override
        public boolean expire() {
            return false;
        }

        @Override
        public boolean linkAndExpire() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    };

    @DisplayName("Skip billable period check changes whether eobs are filtered or not")
    @Test
    void whenSkipBillablePeriod() {

        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of());

        PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, STU3);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);

        collector.filterAndAddEntries(BUNDLE);
        assertEquals(1, collector.getEobs().size());

        request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.SYNTHEA, noOpToken, STU3);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE);
        assertTrue(collector.getEobs().isEmpty());

        request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE);
        assertTrue(collector.getEobs().isEmpty());
    }

    @DisplayName("Patient id in eob must match request")
    @Test
    void patientIdMustMatchRequest() {
        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(1234L),
                null, List.of(TestUtil.getOpenRange()));


        PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE);
        assertTrue(collector.getEobs().isEmpty());

        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of(TestUtil.getOpenRange()));
        request = new PatientClaimsRequest(coverageSummary, LATER_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE);
        assertEquals(1, collector.getEobs().size());

    }

    @DisplayName("Happy path when EOB meets all requirements")
    @Test
    void normalBehavior() {
        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of(TestUtil.getOpenRange()));

        PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, EARLY_ATT_DATE, null, "client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE);
        assertEquals(1, collector.getEobs().size());
    }

    @DisplayName("Old since and attestation doesn't work if before ab2d epoch")
    @Test
    void oldSinceOldAttestation_thenFilterOut() {
        try {
            CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                    null, List.of(TestUtil.getOpenRange()));

            // Old billable period date and older attestation date should return nothing
            ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
            eob.getBillablePeriod().setStart(SDF.parse("10/13/1970"));
            eob.getBillablePeriod().setEnd(SDF.parse("10/13/1970"));
            IBaseBundle oldBundle = EobTestDataUtil.createBundle(eob);

            PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now().minusYears(100), null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle);
            assertTrue(collector.getEobs().isEmpty());
        } catch (ParseException pe) {
            fail("could not build dates", pe);
        }
    }

    @DisplayName("Early attestation still blocks old billable periods")
    @Test
    void whenEarlyAttestation_blockEarlyEobs() {

        try {
            CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                    null, List.of(TestUtil.getOpenRange()));

            // Old billable period date and older attestation date should return nothing
            ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
            eob.getBillablePeriod().setStart(SDF.parse("12/29/2019"));
            eob.getBillablePeriod().setEnd(SDF.parse("12/30/2019"));
            IBaseBundle oldBundle = EobTestDataUtil.createBundle(eob);

            PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now().minusYears(100), null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle);
            assertTrue(collector.getEobs().isEmpty());
        } catch (ParseException pe) {
            fail("could not build dates", pe);
        }
    }

    @DisplayName("Early attestation still allows valid billable periods")
    @Test
    void whenEarlyAttestation_allowEarlyEobs() {

        try {
            CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                    null, List.of(TestUtil.getOpenRange()));

            // Valid billable period and old attestation date
            ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
            eob.getBillablePeriod().setStart(SDF.parse("01/02/2020"));
            eob.getBillablePeriod().setEnd(SDF.parse("01/03/2020"));
            IBaseBundle oldBundle = EobTestDataUtil.createBundle(eob);

            PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now().minusYears(100), null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle);
            assertEquals(1, collector.getEobs().size());
        } catch (ParseException pe) {
            fail("could not build dates", pe);
        }
    }

    @DisplayName("Attestation today valid billable periods should be blocked")
    @Test
    void whenAttestationToday_blockEobs() {

        try {
            CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                    null, List.of(TestUtil.getOpenRange()));

            // Valid billable period and attestation day of today
            ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
            eob.getBillablePeriod().setStart(SDF.parse("01/02/2020"));
            eob.getBillablePeriod().setEnd(SDF.parse("01/03/2020"));
            IBaseBundle oldBundle = EobTestDataUtil.createBundle(eob);

            PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now(), null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle);
            assertTrue(collector.getEobs().isEmpty());
        } catch (ParseException pe) {
            fail("could not build dates", pe);
        }
    }


    @DisplayName("Fail quietly on null bundle")
    @Test
    void whenBundleNull_thenFailQuietly() {

        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of(TestUtil.getOpenRange()));

        PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now(), null, "client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(null);
        assertTrue(collector.getEobs().isEmpty());
    }

    @DisplayName("Fail quietly on null bundle entries")
    @Test
    void whenBundleEntriesNull_thenFailQuietly() {

        try {
            CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                    null, List.of(TestUtil.getOpenRange()));

            ExplanationOfBenefit eob = (ExplanationOfBenefit) EobTestDataUtil.createEOB();
            eob.getBillablePeriod().setStart(SDF.parse("01/02/2020"));
            eob.getBillablePeriod().setEnd(SDF.parse("01/03/2020"));
            IBaseBundle oldBundle = EobTestDataUtil.createBundle(eob);
            ReflectionTestUtils.setField(oldBundle, "entry", null);

            PatientClaimsRequest request = new PatientClaimsRequest(coverageSummary, OffsetDateTime.now(), null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle);
            assertTrue(collector.getEobs().isEmpty());
        } catch (ParseException pe) {
            fail("could not parse bundle", pe);
        }
    }
}
