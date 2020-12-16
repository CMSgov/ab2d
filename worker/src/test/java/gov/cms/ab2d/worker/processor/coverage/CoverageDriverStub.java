package gov.cms.ab2d.worker.processor.coverage;

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
}
