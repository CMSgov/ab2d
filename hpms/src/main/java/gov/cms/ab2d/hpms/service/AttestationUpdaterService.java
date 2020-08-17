package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.hmsapi.HMSOrganizations;
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

        Flux<HMSOrganizations> orgInfoFlux = WebClient.create("http://localhost:8080/api/cda/orgs/info")
                .get()
                .retrieve()
                .bodyToFlux(HMSOrganizations.class);

        orgInfoFlux.subscribe(this::processOrgInfo);
    }

    private void processOrgInfo(HMSOrganizations orgInfo) {
        if (orgInfo.getOrgs().isEmpty()) {
            return;
        }

        if (orgInfo.getOrgs().size() == 57) {
            "57".equals(orgInfo);
            // do something
        }
        // do something else
    }
}
