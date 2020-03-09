package gov.cms.ab2d.worker.bfdhealthcheck;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.dto.PropertiesDTO;
import gov.cms.ab2d.common.service.PropertiesService;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

import static gov.cms.ab2d.common.util.Constants.MAINTENANCE_MODE;

@Component
@Slf4j
class BFDHealthCheck {

    @Autowired
    private PropertiesService propertiesService;

    @Autowired
    private BFDClient bfdClient;

    @Value("${bfd.health.check.consecutive.successes}")
    private int consecutiveSuccessesToBringUp;

    @Value("${bfd.health.check.consecutive.failures}")
    private int consecutiveFailuresToTakeDown;

    // Nothing else should be calling this component except for the scheduler, so keep
    // state here
    private int consecutiveSuccesses = 0;

    private int consecutiveFailures = 0;

    void checkBFDHealth() {

        CapabilityStatement capabilityStatement = bfdClient.capabilityStatement();
        if (capabilityStatement == null || capabilityStatement.getStatus() != Enumerations.PublicationStatus.ACTIVE) {
            consecutiveFailures++;
            consecutiveSuccesses = 0;
        } else {
            consecutiveSuccesses++;
            consecutiveFailures = 0;
        }

        if(consecutiveSuccesses == consecutiveSuccessesToBringUp) {
            consecutiveSuccesses = 0;
            PropertiesDTO propertiesDTO = new PropertiesDTO();
            propertiesDTO.setKey(MAINTENANCE_MODE);
            propertiesDTO.setValue("false");
            propertiesService.updateProperties(List.of(propertiesDTO));
        } else if(consecutiveFailures == consecutiveFailuresToTakeDown) {
            consecutiveFailures = 0;
            PropertiesDTO propertiesDTO = new PropertiesDTO();
            propertiesDTO.setKey(MAINTENANCE_MODE);
            propertiesDTO.setValue("true");
            propertiesService.updateProperties(List.of(propertiesDTO));
        }


    }
}
