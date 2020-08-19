package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Date;

@Service
public class AttestationUpdaterService {

    private static final String HOURLY = "0 0 0/1 1/1 * ?";

    @Scheduled(cron = "*/5 * * * * ?")
    public void demoServiceMethod() {
        System.out.println("Method executed at every 5 seconds. Current time is :: " + new Date());
    }

    @Scheduled(cron = AttestationUpdaterService.HOURLY)
    public void pollHmsData() {
        System.out.println("HMS polling");
        pollOrganizations();
    }

    private void pollOrganizations() {

        // todo: move url into property
        Flux<HPMSOrganizations> orgInfoFlux = WebClient.create("http://localhost:8080/api/cda/orgs/info")
                .get()
                .retrieve()
                .bodyToFlux(HPMSOrganizations.class);

        orgInfoFlux.subscribe(this::processOrgInfo);
    }

    private void processOrgInfo(HPMSOrganizations orgInfo) {
        if (orgInfo.getOrgs().isEmpty()) {
            return;
        }

        if (orgInfo.getOrgs().size() == 57) {
            "57".equals(orgInfo);
            // do something
        }
        // do something else
    }
    /*
    Get set of organizations as map from hpms.
    Get set of active organizations from table.
        Iterate through -
            create set of Organizations that rows need to be updated
                if active in table but missing from hpms, mark inactive
                else update any changed fields
            compute organizations that need to be inserted

         From active set of organizations, query each contract and receive the collection of attestations.



     */
}
