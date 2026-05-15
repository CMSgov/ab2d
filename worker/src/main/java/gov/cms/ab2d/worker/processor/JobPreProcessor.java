package gov.cms.ab2d.worker.processor;

import gov.cms.ab2d.fhir.FhirVersion;
import gov.cms.ab2d.job.model.Job;

public interface JobPreProcessor {

    Job preprocess(String jobUuid);
    default Job preprocess(String jobUuid, FhirVersion fhirVersion) {
        return preprocess(jobUuid, fhirVersion);
    }

}
