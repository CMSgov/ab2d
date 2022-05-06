package gov.cms.ab2d.worker.processor;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import gov.cms.ab2d.filter.ExplanationOfBenefitTrimmerSTU3;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class EobTestDataUtil {

    public static IBaseResource createEOB() {
        IBaseResource eob;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:" + File.separator +
                "test-data" + File.separator + "EOB-for-Carrier-Claims.json");
        InputStream inputStream = getEOBInputStream(resource);

        final EncodingEnum respType = EncodingEnum.forContentType(EncodingEnum.JSON_PLAIN_STRING);
        final IParser parser = respType.newParser(FhirContext.forDstu3());
        final org.hl7.fhir.dstu3.model.ExplanationOfBenefit explanationOfBenefit = parser.parseResource(org.hl7.fhir.dstu3.model.ExplanationOfBenefit.class, inputStream);
        eob = ExplanationOfBenefitTrimmerSTU3.getBenefit(explanationOfBenefit);
        org.hl7.fhir.dstu3.model.Period billingPeriod = new org.hl7.fhir.dstu3.model.Period();
        try {
            billingPeriod.setStart(sdf.parse("01/02/2020"));
            final LocalDate now = LocalDate.now();
            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
            final String nowFormatted = now.format(formatter);
            billingPeriod.setEnd(sdf.parse(nowFormatted));
        } catch (Exception ignored) {}
        //noinspection ConstantConditions
        ((org.hl7.fhir.dstu3.model.ExplanationOfBenefit) eob).setBillablePeriod(billingPeriod);

        return eob;
    }

    private static InputStream getEOBInputStream(Resource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
    }

    public static org.hl7.fhir.dstu3.model.Bundle createBundle(org.hl7.fhir.dstu3.model.Resource resource) {
        final org.hl7.fhir.dstu3.model.Bundle bundle = new org.hl7.fhir.dstu3.model.Bundle();
        bundle.addEntry(addEntry(resource));
        return bundle;
    }

    public static org.hl7.fhir.r4.model.Bundle createBundle(org.hl7.fhir.r4.model.Resource resource) {
        final org.hl7.fhir.r4.model.Bundle bundle = new org.hl7.fhir.r4.model.Bundle();
        bundle.addEntry(addEntry(resource));
        return bundle;
    }

    private static org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent addEntry(org.hl7.fhir.dstu3.model.Resource resource) {
        final org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent bundleEntryComponent = new org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(resource);
        return bundleEntryComponent;
    }

    private static org.hl7.fhir.r4.model.Bundle.BundleEntryComponent addEntry(org.hl7.fhir.r4.model.Resource resource) {
        final org.hl7.fhir.r4.model.Bundle.BundleEntryComponent bundleEntryComponent = new org.hl7.fhir.r4.model.Bundle.BundleEntryComponent();
        bundleEntryComponent.setResource(resource);
        return bundleEntryComponent;
    }

    public static org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent addNextLink() {
        org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent linkComponent = new org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent();
        linkComponent.setRelation(org.hl7.fhir.dstu3.model.Bundle.LINK_NEXT);
        return linkComponent;
    }
}
