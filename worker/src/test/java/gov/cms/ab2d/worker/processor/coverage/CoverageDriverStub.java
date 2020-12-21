package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.CoveragePagingRequest;
import gov.cms.ab2d.common.model.CoveragePagingResult;
import gov.cms.ab2d.common.model.CoverageSummary;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.worker.TestUtil;

import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifier;

public class CoverageDriverStub implements CoverageDriver {

    public boolean discoveryCalled;
    public boolean queueingCalled;

    private final int pageSize;
    private final int totalRecords;

    public CoverageDriverStub() {
        this.pageSize = 10;
        this.totalRecords = 20;
    }

    public CoverageDriverStub(int pageSize, int totalRecords) {
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
    public boolean isCoverageAvailable(Job job) {
        return false;
    }
    @Override
    public CoveragePagingResult pageCoverage(Job job) {

        CoveragePagingRequest nextRequest = getNextRequest(null);
        List<CoverageSummary> results = getSummaries(null);
        return new CoveragePagingResult(results, nextRequest);
    }

    @Override
    public CoveragePagingResult pageCoverage(CoveragePagingRequest request) {

        CoveragePagingRequest nextRequest = getNextRequest(request);
        List<CoverageSummary> results = getSummaries(request);
        return new CoveragePagingResult(results, nextRequest);
    }

    private CoveragePagingRequest getNextRequest(CoveragePagingRequest previousRequest) {
        if (previousRequest == null && pageSize < totalRecords) {
            return new CoveragePagingRequest(pageSize, "" + pageSize, List.of());
        } else if (previousRequest != null) {
            int cursor = Integer.parseInt(previousRequest.getCursor().get());

            if (cursor + pageSize < totalRecords) {
                return new CoveragePagingRequest(pageSize, "" + (cursor + pageSize), List.of());
            }
        }

        return null;
    }

    private List<CoverageSummary> getSummaries(CoveragePagingRequest request) {

        int startIndex = 0;
        int endIndex;

        if (request == null) {
            endIndex = Math.min(pageSize, totalRecords);
        } else {
            startIndex = Integer.parseInt(request.getCursor().get());
            endIndex = Math.min(startIndex + pageSize, totalRecords);
        }

        List<CoverageSummary> summaries = new ArrayList<>();
        for (int patient = startIndex; patient < endIndex; patient++) {
            summaries.add(new CoverageSummary(
                    createIdentifier("patient-" + patient, "mbi-" + patient),
                    null, List.of(TestUtil.getOpenRange())
            ));
        }

        return summaries;
    }
}
