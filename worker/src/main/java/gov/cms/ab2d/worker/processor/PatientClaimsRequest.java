package gov.cms.ab2d.worker.processor;

import com.newrelic.api.agent.Token;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Request to BFD for a single patient's claims matching the provided parameters and requirements.
 */
@Getter
@AllArgsConstructor
public class PatientClaimsRequest {

    /**
     * Identifiers associated with patient and date ranges that patient is/was enrolled in the Part D contract.
     * Used by {@link PatientClaimsCollector} to filter out claims with billable periods outside enrolled dates.
     *
     * Do not change without consulting multiple people.
     */
    private final List<CoverageSummary> coverageSummary;

    /**
     * Datetime when contract was legally attested for
     */
    private final OffsetDateTime attTime;

    /**
     * Optional datetime that PDP wants data for. Does not correspond to when services were conducted only
     */
    @Nullable
    private final OffsetDateTime sinceTime;

    /**
     * Organization name of contract that is not case sensitive
     */
    private final String organization;

    /**
     * Job UUID
     */
    private final String job;

    /**
     * Contract number
     */
    private final String contractNum;

    /**
     * Dictates how date filtering is done in {@link PatientClaimsCollector} for real vs test contracts.
     */
    private final Contract.ContractType contractType;

    /**
     * NR token corresponding to transaction. Calls are sampled to profile performance.
     */
    private final Token token;

    /**
     * Dictates which version of FHIR to use when requesting and serializing data
     */
    private final FhirVersion version;

    /**
     * The starting point for all job files
     */
    private final String efsMount;
}
