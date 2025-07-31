package gov.cms.ab2d.fetcher.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Date;

@Getter
@ToString
public class JobFetchPayload {
    private String jobId;
    private String contract;
    private String organization;
    private boolean skipBillablePeriodCheck;
    private FhirVersion version;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSz")
    private OffsetDateTime since;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSz")
    private Date attestationDate;
    private PatientCoverage[] beneficiaries;
}
