package gov.cms.ab2d.api.util;

import java.time.DayOfWeek;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;

public final class Constants {

    public static final String GENERIC_FHIR_ERR_MSG = "The body will contain a FHIR " +
            "OperationOutcome resource in JSON format. " +
            "https://www.hl7.org/fhir/operationoutcome.html " +
            "Please refer to the body of the response for " +
            "details.";

    public static final String ADMIN_ROLE = "ADMIN";

    public static final String BASE_SANDBOX_URL = "https://sandbox.ab2d.cms.gov";

    public static final String FHIR_SANDBOX_URL = BASE_SANDBOX_URL + API_PREFIX + FHIR_PREFIX;

    public static final DayOfWeek BFD_UPDATE_SLO_DAY = DayOfWeek.TUESDAY;

    private Constants() {
    }


}
