package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import com.newrelic.api.agent.Token;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.coverage.model.Identifiers;
import gov.cms.ab2d.filter.FilterOutByDate;
import gov.cms.ab2d.worker.TestUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;


import static gov.cms.ab2d.common.util.DateUtil.AB2D_ZONE;
import static gov.cms.ab2d.fhir.FhirVersion.R4;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifierWithoutMbi;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

    @DisplayName("Test to make sure that the EOB last updated is after since date")
    @Test
    void testAfterSinceDate() {
        ExplanationOfBenefit eob = new ExplanationOfBenefit();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime oneMinuteLater = now.plusMinutes(1);
        OffsetDateTime oneMinuteEarlier = now.minusMinutes(1);
        eob.getMeta().setLastUpdated(new Date());

        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID), null, List.of());

        PatientClaimsRequest request1 = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, STU3, null);
        PatientClaimsCollector collector1 = new PatientClaimsCollector(request1, EPOCH);
        assertTrue(collector1.afterSinceDate(eob));

        PatientClaimsRequest request2 = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, oneMinuteEarlier, null,"client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, STU3, null);
        PatientClaimsCollector collector2 = new PatientClaimsCollector(request2, EPOCH);
        assertTrue(collector2.afterSinceDate(eob));

        PatientClaimsRequest request3 = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, oneMinuteLater, null,"client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, STU3, null);
        PatientClaimsCollector collector3 = new PatientClaimsCollector(request3, EPOCH);
        assertFalse(collector3.afterSinceDate(eob));
    }

    @Test
    @DisplayName("See if changes to R4 data dictionary comes through properly")
    void testNewR4DD() throws IOException {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:" + File.separator +
                "test-data" + File.separator + "EOB-for-Carrier-v2.json");
        InputStream inputStream = resource.getInputStream();
        EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        IParser parser = respType.newParser(FhirContext.forR4());
        org.hl7.fhir.r4.model.ExplanationOfBenefit ab2dEob = parser.parseResource(org.hl7.fhir.r4.model.ExplanationOfBenefit.class, inputStream);

        IBaseBundle bundle = EobTestDataUtil.createBundle(ab2dEob);

        long beneId = Long.parseLong(ab2dEob.getPatient().getReference().replace("Patient/", ""));
        Identifiers ids = new Identifiers(beneId, "ABC", new LinkedHashSet<>());
        CoverageSummary coverageSummary = new CoverageSummary(ids, null, List.of());

        PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), EARLY_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, R4, null);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);

        collector.filterAndAddEntries(bundle, coverageSummary);
        assertEquals(1, collector.getEobs().size());
        org.hl7.fhir.r4.model.ExplanationOfBenefit foundEob = (org.hl7.fhir.r4.model.ExplanationOfBenefit) collector.getEobs().get(0);
        assertEquals(2, foundEob.getExtension().size());
    }

    @DisplayName("Skip billable period check changes whether eobs are filtered or not")
    @Test
    void whenSkipBillablePeriod() {

        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of());

        PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.CLASSIC_TEST, noOpToken, R4, null);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);

        collector.filterAndAddEntries(BUNDLE, coverageSummary);
        assertEquals(1, collector.getEobs().size());

        request = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.SYNTHEA, noOpToken, R4, null);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE, coverageSummary);
        assertTrue(collector.getEobs().isEmpty());

        request = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, R4, null);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE, coverageSummary);
        assertTrue(collector.getEobs().isEmpty());
    }

    @DisplayName("Patient id in eob must match request")
    @Test
    void patientIdMustMatchRequest() {
        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(1234L),
                null, List.of(TestUtil.getOpenRange()));


        PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE, coverageSummary);
        assertTrue(collector.getEobs().isEmpty());

        coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of(TestUtil.getOpenRange()));
        request = new PatientClaimsRequest(List.of(coverageSummary), LATER_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);

        collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE, coverageSummary);
        assertEquals(1, collector.getEobs().size());

    }

    @DisplayName("Happy path when EOB meets all requirements")
    @Test
    void normalBehavior() {
        CoverageSummary coverageSummary = new CoverageSummary(createIdentifierWithoutMbi(PATIENT_ID),
                null, List.of(TestUtil.getOpenRange()));

        PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), EARLY_ATT_DATE, null, null,"client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);

        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(BUNDLE, coverageSummary);
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

            PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now().minusYears(100), null, null,"client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle, coverageSummary);
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

            PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now().minusYears(100), null, null,"client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle, coverageSummary);
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

            PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now().minusYears(100), null,null, "client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle, coverageSummary);
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

            PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now(), null, null,"client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle, coverageSummary);
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

        PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now(), null, null,"client", "job",
                "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
        PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
        collector.filterAndAddEntries(null, coverageSummary);
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

            PatientClaimsRequest request = new PatientClaimsRequest(List.of(coverageSummary), OffsetDateTime.now(), null, null,"client", "job",
                    "contractNum", Contract.ContractType.NORMAL, noOpToken, STU3, null);
            PatientClaimsCollector collector = new PatientClaimsCollector(request, EPOCH);
            collector.filterAndAddEntries(oldBundle, coverageSummary);
            assertTrue(collector.getEobs().isEmpty());
        } catch (ParseException pe) {
            fail("could not parse bundle", pe);
        }
    }
}
