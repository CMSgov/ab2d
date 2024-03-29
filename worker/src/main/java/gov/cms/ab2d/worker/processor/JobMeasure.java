package gov.cms.ab2d.worker.processor;

public enum JobMeasure {
    FAILURE_THRESHHOLD() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            // It's always an int where sourced, so truncation is fine.
            progressTracker.setFailureThreshold((int) value);
        }
    }, PATIENTS_EXPECTED() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.setPatientsExpected((int) value);
        }
    }, PATIENTS_LOADED() {
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
    }, PATIENTS_WITH_EOBS() {
        @Override
        public void update(ProgressTracker progressTracker, long value) {
            progressTracker.addPatientsWithEobsCount((int) value);
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
