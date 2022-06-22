package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.dto.ContractDTO;
import gov.cms.ab2d.coverage.model.ContractForCoverageDTO;
import gov.cms.ab2d.coverage.model.CoveragePagingRequest;
import gov.cms.ab2d.coverage.model.CoveragePagingResult;
import gov.cms.ab2d.coverage.model.CoverageSummary;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.worker.TestUtil;
import gov.cms.ab2d.worker.config.ContractToContractCoverageMapping;
import java.util.ArrayList;
import java.util.List;


import static gov.cms.ab2d.worker.processor.BundleUtils.createIdentifier;

public class CoverageDriverStub implements CoverageDriver {

    public boolean discoveryCalled;
    public boolean queueingCalled;

    private final int pageSize;
    private final int totalRecords;
    private ContractToContractCoverageMapping mapping;

    public CoverageDriverStub() {
        this.pageSize = 10;
        this.totalRecords = 20;
        mapping = new ContractToContractCoverageMapping();
    }

    public CoverageDriverStub(int pageSize, int totalRecords) {
        super();
        this.pageSize = pageSize;
        this.totalRecords = totalRecords;
    }

    @Override
    public void queueStaleCoveragePeriods() {
        queueingCalled = true;
    }

    @Override
    public void discoverCoveragePeriods() {
        discoveryCalled = true;
    }

    @Override
    public boolean isCoverageAvailable(Job job, ContractDTO contract) {
        return false;
    }

    @Override
    public int numberOfBeneficiariesToProcess(Job job, ContractDTO contract) {
        return totalRecords;
    }

    @Override
    public CoveragePagingResult pageCoverage(Job job, ContractDTO contract) {

        CoveragePagingRequest nextRequest = getNextRequest(null, job, mapping.map(contract));
        List<CoverageSummary> results = getSummaries(null);
        return new CoveragePagingResult(results, nextRequest);
    }

    @Override
    public CoveragePagingResult pageCoverage(CoveragePagingRequest request) {

        CoveragePagingRequest nextRequest = getNextRequest(request, null, request.getContract());
        List<CoverageSummary> results = getSummaries(request);
        return new CoveragePagingResult(results, nextRequest);
    }

    @Override
    public void verifyCoverage() {}

    private CoveragePagingRequest getNextRequest(CoveragePagingRequest previousRequest, Job job, ContractForCoverageDTO contract) {
        if (previousRequest == null && pageSize < totalRecords) {
            return new CoveragePagingRequest(pageSize, (long) pageSize, contract, job.getCreatedAt());
        } else if (previousRequest != null) {
            long cursor = previousRequest.getCursor().get();

            if (cursor + pageSize < totalRecords) {
                return new CoveragePagingRequest(pageSize, (cursor + pageSize), previousRequest.getContract(), previousRequest.getJobStartTime());
            }
        }

        return null;
    }

    private List<CoverageSummary> getSummaries(CoveragePagingRequest request) {

        long startIndex = 0;
        long endIndex;

        if (request == null) {
            endIndex = Math.min(pageSize, totalRecords);
        } else {
            startIndex = request.getCursor().get();
            endIndex = Math.min(startIndex + pageSize, totalRecords);
        }

        List<CoverageSummary> summaries = new ArrayList<>();
        for (long patient = startIndex; patient < endIndex; patient++) {
            summaries.add(new CoverageSummary(
                    createIdentifier(patient, "mbi-" + patient),
                    null, List.of(TestUtil.getOpenRange())
            ));
        }

        return summaries;
    }
}
