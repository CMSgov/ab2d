package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.properties.service.PropertiesAPIService;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.LogManager;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.properties.util.Constants.MAINTENANCE_MODE;
import static gov.cms.ab2d.eventlogger.events.SlackEvents.MAINT_MODE;
import static gov.cms.ab2d.fhir.FhirVersion.STU3;
import static gov.cms.ab2d.worker.bfdhealthcheck.HealthCheckData.Status;

@Component
@Slf4j
class BFDHealthCheck {

    private final LogManager logManager;
    private final PropertiesAPIService propertiesApiService;
    private final BFDClient bfdClient;
    private final int consecutiveSuccessesToBringUp;
    private final int consecutiveFailuresToTakeDown;
    private final List<HealthCheckData> healthCheckData = new ArrayList<>();

    BFDHealthCheck(LogManager logManager, PropertiesAPIService propertiesApiService, BFDClient bfdClient,
                          @Value("${bfd.health.check.consecutive.successes}") int consecutiveSuccessesToBringUp,
                          @Value("${bfd.health.check.consecutive.failures}") int consecutiveFailuresToTakeDown) {
        this.logManager = logManager;
        this.propertiesApiService = propertiesApiService;
        this.bfdClient = bfdClient;
        this.consecutiveSuccessesToBringUp = consecutiveSuccessesToBringUp;
        this.consecutiveFailuresToTakeDown = consecutiveFailuresToTakeDown;

        // Eventually, we'll change this to R4, don't need both at this point
        this.healthCheckData.add(new HealthCheckData(STU3));
    }

    void checkBFDHealth() {
        this.healthCheckData.forEach(this::checkBFDHealth);
    }

    void checkBFDHealth(HealthCheckData data) {

        boolean errorOccurred = false;
        IBaseConformance capabilityStatement = null;
        try {
            capabilityStatement = bfdClient.capabilityStatement(data.getVersion());
        } catch (Exception e) {
            errorOccurred = true;
            log.error("Exception occurred while trying to retrieve capability statement", e);
            markFailure(data);
        }
        try {
            if (!errorOccurred) {
                if (!data.getVersion().metaDataValid(capabilityStatement)) {
                    markFailure(data);
                } else {
                    data.incrementSuccesses();
                    data.setConsecutiveFailures(0);
                    log.debug("{} consecutive successes to connect to BFD for {}", data.getConsecutiveSuccesses(), data.getVersion());
                }
            }
        } catch (Exception ex) {
            errorOccurred = true;
            log.error("Exception occurred while trying to retrieve capability statement - Invalid version", ex);
            markFailure(data);
        }

        if (data.getConsecutiveSuccesses() >= consecutiveSuccessesToBringUp && data.getStatus() == Status.DOWN) {
            updateMaintenanceStatus(data, Status.UP, "false");
        } else if (data.getConsecutiveFailures() >= consecutiveFailuresToTakeDown && data.getStatus() == Status.UP) {
            updateMaintenanceStatus(data, Status.DOWN, "true");
        }
    }

    private void updateMaintenanceStatus(HealthCheckData data, Status status, String statusString) {
        data.setStatus(status);
        data.setConsecutiveFailures(0);

        // Slack alert that we are going into maintenance mode
        logManager.alert(MAINT_MODE + " Maintenance Mode status for " + data.getVersion() +
                " is: " + statusString, Ab2dEnvironment.ALL);
        propertiesApiService.updateProperty(MAINTENANCE_MODE, statusString);
        log.info("Updated the {} property to {}", MAINTENANCE_MODE, statusString);
    }

    private void markFailure(HealthCheckData data) {
        data.incrementFailures();
        data.setConsecutiveSuccesses(0);
        log.debug("{} consecutive failures to connect to BFD", data.getConsecutiveFailures() + " for version " + data.getVersion().toString());
    }
}
