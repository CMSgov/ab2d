package gov.cms.ab2d.worker.service.coveragesnapshot;

import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.contracts.model.Contract;
import gov.cms.ab2d.contracts.model.ContractDTO;
import gov.cms.ab2d.coverage.model.CoverageCount;
import gov.cms.ab2d.coverage.service.CoverageService;
import gov.cms.ab2d.snsclient.clients.SNSClient;
import gov.cms.ab2d.snsclient.messages.CoverageCountDTO;
import gov.cms.ab2d.snsclient.messages.AB2DServices;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.cms.ab2d.snsclient.messages.Topics.COVERAGE_COUNTS;
import static java.util.stream.Collectors.groupingBy;

@Service
@Slf4j
public class CoverageSnapshotServiceImpl implements CoverageSnapshotService {

    private final PdpClientService pdpClientService;
    private final CoverageService coverageService;
    private final ContractToContractCoverageMapping mapping;

    private final SNSClient snsClient;

    public CoverageSnapshotServiceImpl(PdpClientService pdpClientService, CoverageService coverageService, ContractToContractCoverageMapping mapping, SNSClient snsClient) {
        this.pdpClientService = pdpClientService;
        this.coverageService = coverageService;
        this.mapping = mapping;
        this.snsClient = snsClient;
    }

    /**
     * Counts beneficiaries for the given contracts and publishes the resulting coverage counts to SNS.
     * <p>
     * This runs a heavy aggregate query ({@link CoverageService#countBeneficiariesForContracts}) and is therefore
     * executed asynchronously so that callers (notably the coverage driver, which holds the coverage lock while
     * queueing stale coverage periods) are not blocked. It is dispatched on the dedicated, hard-capped
     * {@code coverageCountsExecutor} pool (see {@code WorkerConfig}) rather than the default executor so it can
     * never spawn unbounded threads. All exceptions are swallowed to protect the calling flow.
     */
    @Async("coverageCountsExecutor")
    @Override
    public void sendCoverageCounts(AB2DServices services, Set<String> contracts) {

        try {
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
                            .toList())
                    .flatMap(List::stream)
                    .toList();

            snsClient.sendMessage(COVERAGE_COUNTS.getValue(), coverageCountDTOS);
        } catch (Exception e) {
            log.error("Sending coverage count snapshot failed, swallowing all exceptions to protect coverage update", e);
        }
    }

    @Override
    public void sendCoverageCounts(AB2DServices services, String contract, int count, int year, int month) {
        try {
            snsClient.sendMessage(COVERAGE_COUNTS.getValue(), List.of(new CoverageCountDTO(contract, services.toString(),
                    count, year, month, Timestamp.from(Instant.now()))));
        } catch (Exception e) {
            log.error("Sending coverage count snapshot failed, swallowing all exceptions to protect coverage update", e);
        }
    }

}
