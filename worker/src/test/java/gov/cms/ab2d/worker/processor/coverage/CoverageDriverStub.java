package gov.cms.ab2d.worker.processor.coverage;

import gov.cms.ab2d.common.model.Job;

public class CoverageDriverStub implements CoverageDriver {

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
    public boolean isCoverageAvailable(Job job) {
        return false;
    }
}
