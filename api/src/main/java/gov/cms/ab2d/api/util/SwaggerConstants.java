package gov.cms.ab2d.api.util;

import static gov.cms.ab2d.common.service.JobServiceImpl.ZIPFORMAT;

public final class SwaggerConstants {
    public static final String MAIN = "This API Provides Part A (Hospital Insurance) & B " +
            "(Medical Insurance) claim data to Part D (Prescription Drug Benefit) sponsors. Consistent with " +
            "CMS' Final Rule to implement Section 50354 of the Bipartisan Budget Act of 2018, CMS is providing " +
            "standalone Medicare Part D plan (PDP) sponsors the opportunity to request access to Medicare claims data. " +
            "Access to Medicare claims data for their enrollees will help plans promote the appropriate use of " +
            "medications and improve health outcomes, among other benefits.";

    // Bulk Export API
    public static final String BULK_MAIN = "API through which an authenticated and authorized " +
            "PDP sponsor may request a bulk-data export from a server.";

    public static final String BULK_EXPORT_TYPE = "String of comma-delimited FHIR resource objects. Only resources of " +
            "the specified resource types(s) SHALL be included in the response. Currently, only " +
            "ExplanationOfBenefit objects are supported";

    public static final String BULK_PREFER = "Value must be respond-async";

    public static final String BULK_ACCEPT = "Value must be application/fhir+json or " + ZIPFORMAT;

    public static final String BULK_OUTPUT_FORMAT = "The format for the " +
            "requested bulk data files to be generated. Currently, only application/fhir+json and " + ZIPFORMAT +
            " are supported.";

    public static final String BULK_CONTRACT_EXPORT = "Initiate Part A & B bulk claim export job for a given contract number, DEPRECATED";
    public static final String BULK_CANCEL = "Cancel a pending or in progress export job";
    public static final String BULK_EXPORT = "Initiate Part A & B bulk claim export job";
}
