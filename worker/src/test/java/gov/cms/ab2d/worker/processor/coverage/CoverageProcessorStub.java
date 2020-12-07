package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.CoveragePeriod;

public class CoverageProcessorStub implements CoverageProcessor {

    public boolean discoveryCalled;
    public boolean queueingCalled;

    @Override
    public void queueStaleCoveragePeriods() {
        queueingCalled = true;
    }

    @Override
    public void discoverCoveragePeriods() {
        discoveryCalled = true;
    }

    @Override
    public void queueCoveragePeriod(CoveragePeriod period, boolean prioritize) {}

    @Override
    public void queueCoveragePeriod(CoveragePeriod period, int attempts, boolean prioritize) {}

    @Override
    public void shutdown() {}
}
