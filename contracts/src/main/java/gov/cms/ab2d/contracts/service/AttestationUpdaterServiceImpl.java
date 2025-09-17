package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.hmsapi.HPMSEnrollment;
import gov.cms.ab2d.contracts.repository.ContractRepository;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.utils.DateUtil;
import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.SlackEvents;
import gov.cms.ab2d.contracts.hmsapi.HPMSAttestation;
import gov.cms.ab2d.contracts.hmsapi.HPMSOrganizationInfo;

import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static gov.cms.ab2d.contracts.model.Contract.FORMATTER;
import static gov.cms.ab2d.eventclient.events.SlackEvents.CONTRACT_CHANGED;

@Primary
@Service
@Slf4j
public class AttestationUpdaterServiceImpl implements AttestationUpdaterService {

    private static final int BATCH_SIZE = 100;

    private final HPMSFetcher hpmsFetcher;

    private final ContractRepository contractRepository;

    private final SQSEventClient eventLogger;

    @Autowired
    public AttestationUpdaterServiceImpl(ContractRepository contractRepository,
                                         HPMSFetcher hpmsFetcher,
                                         SQSEventClient eventLogger) {
        this.contractRepository = contractRepository;
        this.hpmsFetcher = hpmsFetcher;
        this.eventLogger = eventLogger;
    }

    public Contract pullFullInformation(HPMSOrganizationInfo info) {
        Contract contract = populateContract(info);
        Set<HPMSEnrollment> enrollmentSet = hpmsFetcher.retrieveEnrollmentInfo(List.of(contract.getContractNumber()));
        if (enrollmentSet != null && !enrollmentSet.isEmpty()) {
            try {
                Optional<HPMSEnrollment> enrollmentOpt = enrollmentSet.stream().findFirst();
                if (enrollmentOpt.isPresent()) {
                    HPMSEnrollment enrollment = enrollmentOpt.get();
                    int dayDiffFromNow = timeDifference(enrollment.getEnrollmentYearInt(), enrollment.getEnrollmentMonthInt());
                    if (dayDiffFromNow > 60) {
                        contract.setTotalEnrollment(0);
                        contract.setMedicareEligible(0);
                    } else {
                        contract.setTotalEnrollment(enrollment.getTotalEnrollmentInt());
                        contract.setMedicareEligible(enrollment.getMedicareEligibleInt());
                    }
                }
            } catch (Exception ex) {
                log.error("Unable to get enrollment for contract: " + contract.getContractNumber());
            }
        }
        try {
            Set<HPMSAttestation> attestationSet = hpmsFetcher.retrieveAttestationInfo(List.of(contract.getContractNumber()));
            if (attestationSet != null && !attestationSet.isEmpty()) {
                Optional<HPMSAttestation> attestionOpt = attestationSet.stream().findFirst();
                if (attestionOpt.isPresent()) {
                    HPMSAttestation attestation = attestionOpt.get();
                    if (attestation.isAttested()) {
                        String dateWithTZ = attestation.getAttestationDate() + " " + DateUtil.getESTOffset();
                        contract.setAttestedOn(OffsetDateTime.parse(dateWithTZ, FORMATTER));
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Unable to get attestation for contract: " + contract.getContractNumber());
        }
        return contract;
    }

    static int timeDifference(int year, int month) {
        if (month <= 0 || year <= 0) {
            return 10_000;
        }
        OffsetDateTime enrollmentDt = OffsetDateTime.of(
                year, month, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime now = OffsetDateTime.now();
        Period period = Period.between(enrollmentDt.toLocalDate(), now.toLocalDate());
        int yearDiff = period.getYears();
        int monthDiff = period.getMonths();
        int dayDiff = period.getDays();
        return (yearDiff * 365) + (monthDiff * 30) + dayDiff;
    }

    Contract populateContract(HPMSOrganizationInfo info) {
        Contract contract = new Contract();
        contract.setContractNumber(info.getContractId());
        if (info.getParentOrgId() != null) {
            contract.setHpmsParentOrgId(info.getParentOrgId().longValue());
        }
        contract.setContractName(info.getContractName());
        contract.setHpmsParentOrg(info.getParentOrgName());
        contract.setHpmsOrgMarketingName(info.getOrgMarketingName());
        return contract;
    }

    @Override
    public void pollOrganizations() {
        // Load the data from HPMS
        Map<String, Contract> pulledContracts = retrieveAllContractsFromHPMS();

        // Retrieve the data from the database
        Map<String, Contract> existingMap = buildExistingContractMap();

        // Get the ones you don't want to update
        List<Contract> onesNotToUpdate = existingMap.values().stream().filter(c -> !c.isAutoUpdatable()).toList();

        // Remove from both lists ones you shouldn't be updating
        for (Contract c : onesNotToUpdate) {
            if (pulledContracts.containsKey(c.getContractNumber())) {
                pulledContracts.remove(c.getContractNumber());
            }
            if (existingMap.containsKey(c.getContractNumber())) {
                existingMap.remove(c.getContractNumber());
            }
        }

        // Get contracts that were not updated and make sure their attestation is removed
        for (Contract c : existingMap.values()) {
            if (!pulledContracts.containsKey(c.getContractNumber())) {
                c.setAttestedOn(null);
                contractRepository.save(c);
            }
        }
        // Get the new contracts & updated contracts
        for (Contract contract : pulledContracts.values()) {
            if (!existingMap.containsKey(contract.getContractNumber())) {
                addNewContract(contract);
            } else {
                updateContract(contract, existingMap.get(contract.getContractNumber()));
            }
        }
    }

    public Map<String, Contract> retrieveAllContractsFromHPMS() {
        // Load the data from HPMS
        List<HPMSOrganizationInfo> orgInfo = hpmsFetcher.retrieveSponsorInfo();

        // If we don't get the data, return and don't do the updates
        if (orgInfo == null || orgInfo.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Contract> pulledContracts = new HashMap<>();
        for (HPMSOrganizationInfo org : orgInfo) {
            pulledContracts.put(org.getContractId(), pullFullInformation(org));
        }
        return pulledContracts;
    }

    private void updateContract(Contract newContract, Contract oldContract) {
        if (contractUpdated(oldContract, newContract)) {
            String msg = CONTRACT_CHANGED + " *Changed Contract*\n\nName: " + oldContract.getContractName() + "\n"
                    + "Number: " + oldContract.getContractNumber() + "\n"
                    + "HPMS Attested On: " + newContract.getAttestedOn() + "\n"
                    + "Contract Attested On: " + oldContract.getAttestedOn() + "\n";
            if (eventLogger != null) {
                eventLogger.alert(msg, Ab2dEnvironment.ALL);
            }
        }
        oldContract.setAttestedOn(newContract.getAttestedOn());
        oldContract.setContractName(newContract.getContractName());
        oldContract.setHpmsOrgMarketingName(newContract.getHpmsOrgMarketingName());
        oldContract.setHpmsParentOrgId(newContract.getHpmsParentOrgId());
        oldContract.setHpmsParentOrg(newContract.getHpmsParentOrg());
        oldContract.setTotalEnrollment(newContract.getTotalEnrollment());
        oldContract.setMedicareEligible(newContract.getMedicareEligible());

        contractRepository.save(oldContract);
    }

    private boolean contractUpdated(Contract oldContract, Contract newContract) {
        if (oldContract == null || newContract == null) {
            return false;
        }
        if (newContract.hasAttestation() != oldContract.hasAttestation()) {
            return true;
        }
        if (! newContract.hasAttestation() && ! oldContract.hasAttestation()) {
            return false;
        }
        if (newContract.getAttestedOn().isEqual(oldContract.getAttestedOn())) {
            return false;
        } else {
            return true;
        }
    }

    Contract addNewContract(Contract newContract) {
        if (newContract == null) {
            return null;
        }

        String msg = SlackEvents.CONTRACT_ADDED + " *New Contract*\n\nId: " + newContract.getContractNumber() + "\n"
                        + "Name: " + newContract.getContractName() + "\n"
                        + "Parent Org: " + newContract.getHpmsParentOrg() + "\n"
                        + "Org: " + newContract.getHpmsOrgMarketingName() + "\n";
        if (eventLogger != null) {
            eventLogger.alert(msg, Ab2dEnvironment.ALL);
        }
        return contractRepository.save(newContract);
    }

    private Map<String, Contract> buildExistingContractMap() {
        Map<String, Contract> existingMap;
        List<Contract> existing = contractRepository.findAll();
        existingMap = new HashMap<>();
        existing.forEach(contract -> existingMap.put(contract.getContractNumber(), contract));
        return existingMap;
    }
}
