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
            progressTracker.setExpectedBeneficiaries((int) value);
        }
    }, META_DATA_PROCESSED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientsLoadedCount((int) value);
        }
    }, PATIENT_REQUEST_QUEUED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientRequestQueuedCount((int) value);
        }
    }, PATIENT_REQUESTS_PROCESSED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientProcessedCount((int) value);
        }
    }, PATIENT_REQUESTS_ERRORED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientFailureCount((int) value);
        }
    }, EOBS_FETCHED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addEobFetchedCount((int) value);
        }
    }, EOBS_WRITTEN() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addEobProcessedCount((int) value);
        }
    }, EOBS_EMPTY() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract void update(ProgressTracker progressTracker, long value);
}
