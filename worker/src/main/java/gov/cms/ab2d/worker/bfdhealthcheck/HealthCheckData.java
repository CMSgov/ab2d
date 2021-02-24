package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.fhir.FhirVersion;
import lombok.Data;

@Data
public class HealthCheckData {
    enum Status {
        UP, DOWN
    }
    private int consecutiveSuccesses;
    private int consecutiveFailures;
    private FhirVersion version;
    private Status status = Status.UP;

    public HealthCheckData(FhirVersion version) {
       this.version = version;
    }

    public void incrementFailures() {
        consecutiveFailures++;
    }

    public void incrementSuccesses() {
        consecutiveSuccesses++;
    }
}
