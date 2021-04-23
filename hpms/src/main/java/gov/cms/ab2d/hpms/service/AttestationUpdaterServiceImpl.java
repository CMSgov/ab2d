package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.hpms.hmsapi.*;  // NOPMD
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Primary
@Service
public class AttestationUpdaterServiceImpl implements AttestationUpdaterService {

    private static final int BATCH_SIZE = 100;

    private final HPMSFetcher hpmsFetcher;

    private final ContractRepository contractRepository;

    private final LogManager eventLogger;

    @Autowired
    public AttestationUpdaterServiceImpl(ContractRepository contractRepository,
                                         HPMSFetcher hpmsFetcher,
                                         LogManager eventLogger) {
        this.contractRepository = contractRepository;
        this.hpmsFetcher = hpmsFetcher;
        this.eventLogger = eventLogger;
    }

    @Override
    public void pollOrganizations() {
        hpmsFetcher.retrieveSponsorInfo(this::processOrgInfo);
    }

    private void processOrgInfo(HPMSOrganizations orgInfo) {
        Map<String, Contract> existingMap = buildExistingContractMap();

        // detect changed organizational information, specifically populating the new hpms fields
        List<Contract> changedContracts = orgInfo.getOrgs().stream()
                .filter(hpmsInfo -> existingMap.containsKey(hpmsInfo.getContractId()))
                .map(this::updateContract)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        if (!changedContracts.isEmpty()) {
            contractRepository.saveAll(changedContracts);
        }

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

    private Optional<Contract> updateContract(HPMSOrganizationInfo hpmsInfo) {
        Optional<Contract> contractHolder = contractRepository.findContractByContractNumber(hpmsInfo.getContractId());
        if (contractHolder.isEmpty())
            return contractHolder;

        Contract contract = contractHolder.get();
        return hpmsInfo.hasChanges(contract) ? Optional.of(hpmsInfo.updateContract(contract)) : Optional.empty();
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
        hpmsFetcher.retrieveAttestationInfo(this::processContracts, currentChunk);
    }

    private void processContracts(HPMSAttestationsHolder contractHolder) {
        Map<String, Contract> existingMap = buildExistingContractMap();
        contractHolder.getContracts()
                .forEach(attest -> updateContractIfChanged(attest, existingMap.get(attest.getContractId())));
    }

    private void updateContractIfChanged(HPMSAttestation attest, Contract contract) {
        if (contract.updateAttestation(attest.isAttested(), attest.getAttestationDate())) {
            String msg = "*Changed Contract*\n\nName: " + contract.getContractName() + "\n"
                    + "Number: " + contract.getContractNumber() + "\n"
                    + "HPMS Attested On: " + attest.getAttestationDate() + "\n"
                    + "Contract Attested On: " + contract.getAttestedOn() + "\n";
            if (eventLogger != null) {
                eventLogger.alert(msg, Ab2dEnvironment.ALL);
            }
            contractRepository.save(contract);
        }
    }

    List<Contract> addNewContracts(List<HPMSOrganizationInfo> newContracts) {
        if (newContracts.isEmpty()) {
            return new ArrayList<>();
        }
        newContracts.forEach(c -> {
                String msg = "*New Attester*\n\nId: " + c.getContractId() + "\n"
                        + "Name: " + c.getContractName() + "\n"
                        + "Id: " + c.getContractId() + "\n"
                        + "Org: " + c.getOrgMarketingName() + "\n";
                if (eventLogger != null) {
                    eventLogger.alert(msg, Ab2dEnvironment.ALL);
                }
            }
        );
        return newContracts.stream().map(this::sponsorAdd).collect(Collectors.toList());
    }

    private Contract sponsorAdd(HPMSOrganizationInfo hpmsInfo) {
        return contractRepository.save(hpmsInfo.build());
    }

    private void considerContract(List<Contract> contractAttestList, Contract contract,
                                  HPMSOrganizationInfo hpmsOrganizationInfo) {
        // Ignore Test contracts
        if (contract.isTestContract()) {
            return;
        }

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
