package gov.cms.ab2d.api.util;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;

public final class Constants {

    public static final String GENERIC_FHIR_ERR_MSG = "The body will contain a FHIR " +
            "OperationOutcome resource in JSON format. " +
            "https://www.hl7.org/fhir/operationoutcome.html " +
            "Please refer to the body of the response for " +
            "details.";

    private Constants() {
    }
}
