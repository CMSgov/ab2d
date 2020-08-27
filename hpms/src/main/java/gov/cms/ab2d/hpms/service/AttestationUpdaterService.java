package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.hpms.hmsapi.ContractHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AttestationUpdaterService {

    private final SponsorRepository sponsorRepository;

    private final ContractRepository contractRepository;

    private static final String HOURLY = "0 0 0/1 1/1 * ?";

    @Autowired
    public AttestationUpdaterService(SponsorRepository sponsorRepository, ContractRepository contractRepository) {
        this.sponsorRepository = sponsorRepository;
        this.contractRepository = contractRepository;
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void demoServiceMethod() {
        System.out.println("Method executed at every 5 seconds. Current time is :: " + new Date());
    }

    @Scheduled(cron = AttestationUpdaterService.HOURLY)
    public void pollHmsData() {
        System.out.println("HMS polling");
        pollOrganizations();
    }

    void pollOrganizations() {

        // todo: move url into property
        Flux<HPMSOrganizations> orgInfoFlux = WebClient.create("http://localhost:8080/api/cda/orgs/info")
                .get()
                .retrieve()
                .bodyToFlux(HPMSOrganizations.class);

        orgInfoFlux.subscribe(this::processOrgInfo);
    }

    private void processOrgInfo(HPMSOrganizations orgInfo) {
        Map<String, Contract> existingMap = buildExistingContractMap();
        // detect new Contracts
        List<HPMSOrganizationInfo> newContracts = orgInfo.getOrgs().stream()
                .filter(hpmsInfo -> !existingMap.containsKey(hpmsInfo.getContractId()))
                .collect(Collectors.toList());
        List<Contract> contractAttestList = addNewContracts(newContracts);

        Map<String, HPMSOrganizationInfo> refreshed = buildRefreshedMap(orgInfo);
        existingMap.forEach((contractId, contract) ->
                considerContract(contractAttestList, contract, refreshed.get(contractId)));

        processAttestations(contractAttestList.stream().map(Contract::getContractNumber).collect(Collectors.toList()));
    }

    private void processAttestations(List<String> contractAttestList) {
        // todo: chunk these requests to avoid a too long URL
//        String contractIdStr = String.join(",", contractAttestList);
        String contractIdStr = "[\"S1234\",\"S2341\"]";
        Flux<ContractHolder> contractsFlux = WebClient.create("http://localhost:8080/api/cda/contracts/status")
                .get().uri(uriBuilder -> uriBuilder.queryParam("contractIds", contractIdStr).build())
                .retrieve()
                .bodyToFlux(ContractHolder.class);

        contractsFlux.subscribe(this::processContracts);

    }

    private void processContracts(ContractHolder contractHolder) {
        System.out.println(contractHolder);
    }

    private List<Contract> addNewContracts(List<HPMSOrganizationInfo> newContracts) {
        if (newContracts.isEmpty()) {
            return new ArrayList<>();
        }
        return newContracts.stream().map(this::sponserAdd).collect(Collectors.toList());
    }

    private Contract sponserAdd(HPMSOrganizationInfo hpmsInfo) {
        Sponsor sponsor = new Sponsor();
        sponsor.setHpmsId(hpmsInfo.getParentOrgId());
        sponsor.setOrgName(hpmsInfo.getParentOrgName());
        Sponsor savedSponser = sponsorRepository.save(sponsor);

        Contract retContract = new Contract();
        retContract.setContractName(hpmsInfo.getContractName());
        retContract.setContractNumber(hpmsInfo.getContractId());
        retContract.setContractState(Contract.ContractState.UNATTESTED);
        retContract.setSponsor(savedSponser);
        return contractRepository.save(retContract);
    }

    private void considerContract(List<Contract> contractAttestList, Contract contract,
                                  HPMSOrganizationInfo hpmsOrganizationInfo) {
        if (hpmsOrganizationInfo == null) {
            // Missing in refresh and previously marked as deleted - nothing more to do
            if (contract.isDeleted())
                return;

            // Missing in refresh, need to update as deleted.
            contract.markDeleted();
            contractRepository.save(contract);
            return;
        }

        contractAttestList.add(contract);
    }

    private Map<String, Contract> buildExistingContractMap() {
        Map<String, Contract> existingMap;
        List<Contract> existing = contractRepository.findAll();
        existingMap = new HashMap<>();
        existing.forEach(contract -> existingMap.put(contract.getContractNumber(), contract));
        return existingMap;
    }

    private Map<String, HPMSOrganizationInfo> buildRefreshedMap(HPMSOrganizations orgInfo) {
        Map<String, HPMSOrganizationInfo> refreshed = new HashMap<>();
        orgInfo.getOrgs().forEach(hpmsOrg -> refreshed.put(hpmsOrg.getContractId(), hpmsOrg));
        return refreshed;
    }
}
