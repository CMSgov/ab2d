package gov.cms.ab2d.worker.service.coveragesnapshot;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.snsclient.clients.SNSClient;
import gov.cms.ab2d.snsclient.messages.AB2DServices;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static gov.cms.ab2d.snsclient.messages.Topics.COVERAGE_COUNTS;
import static java.util.stream.Collectors.groupingBy;

@Service
@Slf4j
public class CoverageSnapshotServiceImpl implements CoverageSnapshotService {

    private final PdpClientService pdpClientService;
    private final CoverageService coverageService;
    private final ContractToContractCoverageMapping mapping;

    private final SNSClient snsClient;

    public CoverageSnapshotServiceImpl(PdpClientService pdpClientService, CoverageService coverageService, ContractToContractCoverageMapping mapping, SNSClient snsClient, Ab2dEnvironment environment) {
        this.pdpClientService = pdpClientService;
        this.coverageService = coverageService;
        this.mapping = mapping;
        this.snsClient = snsClient;
    }

    @Override
    public void sendCoverageCounts(AB2DServices services, Set<String> contracts) {
        List<ContractDTO> enabledContracts = pdpClientService.getAllEnabledContracts()
                .stream()
                .filter(contract -> contracts.contains(contract.getContractNumber()))
                .map(Contract::toDTO)
                .toList();
        Map<String, List<CoverageCount>> coverageCounts = coverageService.countBeneficiariesForContracts(enabledContracts.stream()
                        .map(mapping::map)
                        .toList())
                .stream()
                .collect(groupingBy(CoverageCount::getContractNumber));

        Timestamp time = Timestamp.from(Instant.now());

        List<CoverageCountDTO> coverageCountDTOS = coverageCounts.entrySet()
                .stream()
                .map(count -> count.getValue()
                        .stream()
                        .map(c -> new CoverageCountDTO(count.getKey(), services.toString(),
                                c.getBeneficiaryCount(), c.getYear(), c.getMonth(), time))
                        .collect(Collectors.toList()))
                .flatMap(List::stream)
                .toList();

        try {
            snsClient.sendMessage(COVERAGE_COUNTS.getValue(), coverageCountDTOS);
        } catch (Exception e) {
            log.error("Sending coverage count snapshot failed, swallowing exception to protect coverage update", e);
        }
    }

    @Override
    public void sendCoverageCounts(AB2DServices services, String contract, int count, int year, int month) {
        try {
            snsClient.sendMessage(COVERAGE_COUNTS.getValue(), List.of(new CoverageCountDTO(contract, services.toString(),
                    count, year, month, Timestamp.from(Instant.now()))));
        } catch (Exception e) {
            log.error("Sending coverage count snapshot failed, swallowing exception to protect coverage update", e);
        }
    }

}
