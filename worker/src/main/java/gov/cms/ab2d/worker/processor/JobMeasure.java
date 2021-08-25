package gov.cms.ab2d.worker.processor;

public enum JobMeasure {
    FAILURE_THRESHHOLD() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            // It's always an int where sourced, so truncation is fine.
            progressTracker.setFailureThreshold((int) value);
        }
    }, EXPECTED_BENES() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatients((int) value);
        }
    }, ACTUAL_BENES() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientRequestQueuedCount((int) value);
        }
    }, EOBS_FETCHED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addProcessedCount((int) value);
        }
    }, EOBS_WRITTEN() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addProcessedCount((int) value);
        }
    }, EOBS_EMPTY() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            throw new UnsupportedOperationException();
        }
    }, EOBS_ERROR() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract void update(ProgressTracker progressTracker, long value);
}
