package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.Sponsor;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.common.repository.SponsorRepository;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestation;
import gov.cms.ab2d.hpms.hmsapi.HPMSAttestationsHolder;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizationInfo;
import gov.cms.ab2d.hpms.hmsapi.HPMSOrganizations;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gov.cms.ab2d.hpms.hmsapi.HPMSAttestation.FORMATTER;

@Service
public class AttestationUpdaterService {

    private static final int BATCH_SIZE = 100;

    private final HPMSFetcher hpmsFetcher;

    private final SponsorRepository sponsorRepository;

    private final ContractRepository contractRepository;

//    private static final String HOURLY = "0 0 0/1 1/1 * ?";

    @Autowired
    public AttestationUpdaterService(SponsorRepository sponsorRepository, ContractRepository contractRepository,
                                     HPMSFetcher hpmsFetcher) {
        this.sponsorRepository = sponsorRepository;
        this.contractRepository = contractRepository;
        this.hpmsFetcher = hpmsFetcher;
    }

/*  todo: remove the comments and enable this code when integrating with the real hpms service
    @Scheduled(cron = AttestationUpdaterService.HOURLY)
    public void pollHmsData() {
        pollOrganizations();
    }
 */

    void pollOrganizations() {
        hpmsFetcher.retrieveSponsorInfo(this::processOrgInfo);
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

        batchAttestations(contractAttestList.stream().map(Contract::getContractNumber).collect(Collectors.toList()));
    }

    // Limit the size of the request to BATCH_SIZE, avoiding URLs that are too long and keeping the burden down
    // on the invoked service.
    private void batchAttestations(List<String> contractAttestList) {
        final int size = contractAttestList.size();
        int startIdx = 0;
        for (; startIdx < size - BATCH_SIZE; startIdx += BATCH_SIZE) {
            List<String> currentChunk = contractAttestList.subList(startIdx, startIdx + BATCH_SIZE);
            processAttestations(currentChunk);
        }

        // process the remainder (if any) - i.e. smaller than a batch
        if (size % BATCH_SIZE != 0) {
            List<String> currentChunk = contractAttestList.subList(startIdx, size);
            processAttestations(currentChunk);
        }
    }

    private void processAttestations(List<String> currentChunk) {
        String contractIdStr = currentChunk.stream().collect(Collectors.joining("\",\"", "[\"", "\"]"));
        hpmsFetcher.retrieveAttestationInfo(this::processContracts, contractIdStr);
    }

    private void processContracts(HPMSAttestationsHolder contractHolder) {
        Map<String, Contract> existingMap = buildExistingContractMap();
        contractHolder.getContracts()
                .forEach(attest -> updateContractIfChanged(attest, existingMap.get(attest.getContractId())));
    }

    private void updateContractIfChanged(HPMSAttestation attest, Contract contract) {
        if (contract.updateAttestation(attest.isAttested(), attest.getAttestationDate(), FORMATTER)) {
            contractRepository.save(contract);
        }
    }

    List<Contract> addNewContracts(List<HPMSOrganizationInfo> newContracts) {
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
        retContract.setSponsor(savedSponser);
        return contractRepository.save(retContract);
    }

    private void considerContract(List<Contract> contractAttestList, Contract contract,
                                  HPMSOrganizationInfo hpmsOrganizationInfo) {
        if (hpmsOrganizationInfo == null) {
            // Missing in refresh, need to update as having no attestation.
            if (contract.hasAttestation()) {
                contract.clearAttestation();
                contractRepository.save(contract);
            }
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
