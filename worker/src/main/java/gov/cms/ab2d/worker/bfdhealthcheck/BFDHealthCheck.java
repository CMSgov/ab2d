package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.eventloggers.slack.SlackLogger;
import gov.cms.ab2d.fhir.MetaDataUtils;
import gov.cms.ab2d.fhir.Versions;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;

@Component
@Slf4j
class BFDHealthCheck {

    private final SlackLogger slackLogger;
    private final PropertiesService propertiesService;
    private final BFDClient bfdClient;
    private final int consecutiveSuccessesToBringUp;
    private final int consecutiveFailuresToTakeDown;

    // Nothing else should be calling this component except for the scheduler, so keep
    // state here
    private Map<Versions.FhirVersions, Integer> consecutiveSuccesses = new HashMap<>();

    private Map<Versions.FhirVersions, Integer> consecutiveFailures = new HashMap<>();

    private Map<Versions.FhirVersions, Status> bfdStatus = new HashMap<>();

    BFDHealthCheck(SlackLogger slackLogger, PropertiesService propertiesService, BFDClient bfdClient,
                          @Value("${bfd.health.check.consecutive.successes}") int consecutiveSuccessesToBringUp,
                          @Value("${bfd.health.check.consecutive.failures}") int consecutiveFailuresToTakeDown) {
        this.slackLogger = slackLogger;
        this.propertiesService = propertiesService;
        this.bfdClient = bfdClient;
        this.consecutiveSuccessesToBringUp = consecutiveSuccessesToBringUp;
        this.consecutiveFailuresToTakeDown = consecutiveFailuresToTakeDown;
    }

    void checkBFDHealth(Versions.FhirVersions version) {

        boolean errorOccurred = false;
        IBaseConformance capabilityStatement = null;
        try {
            capabilityStatement = bfdClient.capabilityStatement(version);
        } catch (Exception e) {
            errorOccurred = true;
            log.error("Exception occurred while trying to retrieve capability statement", e);
            markFailure(version);
        }
        try {
            if (!errorOccurred) {
                if (!MetaDataUtils.metaDataValid(capabilityStatement, version)) {
                    markFailure(version);
                } else {
                    incrementValue(consecutiveSuccesses, version);
                    consecutiveFailures.put(version, 0);
                    log.debug("{} consecutive successes to connect to BFD", consecutiveSuccesses);
                }
            }
        } catch (Exception ex) {
            errorOccurred = true;
            log.error("Exception occurred while trying to retrieve capability statement - Invalid version", ex);
            markFailure(version);
        }

        if (getValue(consecutiveSuccesses, version) >= consecutiveSuccessesToBringUp && getStatus(version) == Status.DOWN) {
            updateMaintenanceStatus(Status.UP, "false", version);
        } else if (getValue(consecutiveFailures, version) >= consecutiveFailuresToTakeDown && getStatus(version) == Status.UP) {
            updateMaintenanceStatus(Status.DOWN, "true", version);
        }
    }

    private void incrementValue(Map<Versions.FhirVersions, Integer> map, Versions.FhirVersions version) {
        map.merge(version, 1, Integer::sum);
    }

    private Status getStatus(Versions.FhirVersions version) {
        Status status = this.bfdStatus.get(version);
        if (status == null) {
            status = Status.UP;
            bfdStatus.put(version, status);
        }
        return status;
    }

    private void updateMaintenanceStatus(Status status, String statusString, Versions.FhirVersions version) {
        bfdStatus.put(version, status);
        consecutiveFailures.put(version, 0);
        PropertiesDTO propertiesDTO = new PropertiesDTO();
        propertiesDTO.setKey(MAINTENANCE_MODE);
        propertiesDTO.setValue(statusString);
        slackLogger.logAlert("Maintenance Mode status: " + statusString);
        propertiesService.updateProperties(List.of(propertiesDTO));
        log.info("Updated the {} property to {}", MAINTENANCE_MODE, statusString);
    }

    private int getValue(Map<Versions.FhirVersions, Integer> map, Versions.FhirVersions version) {
        if (map == null) {
            return 0;
        }
        Integer val = map.get(version);
        if (val == null) {
            map.put(version, 0);
            return 0;
        }
        return val;
    }

    private void markFailure(Versions.FhirVersions version) {
        Integer consFail = getValue(consecutiveFailures, version);
        consFail++;
        consecutiveFailures.put(version, consFail);
        consecutiveSuccesses.put(version, 0);
        log.debug("{} consecutive failures to connect to BFD", consecutiveFailures.get(version) + " for version " + version.toString());
    }

    private enum Status {
        UP, DOWN
    }
}
