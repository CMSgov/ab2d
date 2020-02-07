package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmer;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.dstu3.model.Resource;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class EobTestDataUtil {


    public static ExplanationOfBenefit createEOB() {
        ExplanationOfBenefit eob = null;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        final String testInputFile = "test-data/EOB-for-Carrier-Claims.json";
        final InputStream inputStream = EobTestDataUtil.class.getResourceAsStream("/" + testInputFile);

        final EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        final IParser parser = respType.newParser(FhirContext.forDstu3());
        final ExplanationOfBenefit explanationOfBenefit = parser.parseResource(ExplanationOfBenefit.class, inputStream);
        eob = ExplanationOfBenefitTrimmer.getBenefit(explanationOfBenefit);
        Period billingPeriod = new Period();
        try {
            billingPeriod.setStart(sdf.parse("01/02/2020"));
            final LocalDate now = LocalDate.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            final String nowFormatted = now.format(formatter);
            billingPeriod.setEnd(sdf.parse(nowFormatted));
        } catch (Exception ex) {}
        eob.setBillablePeriod(billingPeriod);

        return eob;
    }

    public static Bundle createBundle(Resource resource) {
        final Bundle bundle = new Bundle();
        bundle.addEntry(addEntry(resource));
        return bundle;
    }

    private static Bundle.BundleEntryComponent addEntry(Resource resource) {
        final Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(resource);
        return bundleEntryComponent;
    }


    public static Bundle.BundleLinkComponent addNextLink() {
        Bundle.BundleLinkComponent linkComponent = new Bundle.BundleLinkComponent();
        linkComponent.setRelation(Bundle.LINK_NEXT);
        return linkComponent;
    }

}
