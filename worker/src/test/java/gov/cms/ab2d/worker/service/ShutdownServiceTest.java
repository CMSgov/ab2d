package gov.cms.ab2d.worker.service;

import gov.cms.ab2d.eventlogger.Ab2dEnvironment;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.job.model.Job;
import gov.cms.ab2d.job.model.JobStartedBy;
import gov.cms.ab2d.job.model.JobStatus;
import gov.cms.ab2d.job.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownServiceTest {

    @Test
    void testEmptyReset() {
        List<String> activeJobs = Collections.emptyList();
        ShutDownService sds = new ShutDownServiceImpl(new MockJobRepository(), new MockEventLogger());
        sds.resetInProgressJobs(activeJobs);
        //noinspection ConstantConditions
        assertTrue(activeJobs.isEmpty());
    }

    @Test
    void testResetInProgress() {
        List<String> activeJobs = Arrays.asList("jobone", "jobtwo");
        ShutDownService sds = new ShutDownServiceImpl(new MockJobRepository(), new MockEventLogger());
        sds.resetInProgressJobs(activeJobs);
        assertEquals(2, activeJobs.size());
    }

    @Test
    void testException() {
        List<String> activeJobs = Collections.singletonList("bogus");
        ShutDownService sds = new ShutDownServiceImpl(null, null);
        // Hit the log service
        sds.resetInProgressJobs(activeJobs);
        //noinspection ConstantConditions - having one test case makes SonarLint happier.
        assertFalse(activeJobs.isEmpty());
    }

    static class MockEventLogger extends LogManager {

        public MockEventLogger() {
            super(null, null, null);
        }

        @Override
        public void logAndAlert(LoggableEvent event, List<Ab2dEnvironment> environments) {
        }
    }

    /*
     * TODO - definitely not pretty and the only implemented method that we care about is findByJobUuid.
     *
     * The use of a mock library could reduce this to just the override method.
     */
    @SuppressWarnings("all")
    static class MockJobRepository implements JobRepository {
        @Override
        public void cancelJobByJobUuid(String jobUuid) {

        }

        @Override
        public Job findByJobUuid(String jobUuid) {
            Job retJob = new Job();
            retJob.setJobUuid(jobUuid);
            retJob.setStatus(JobStatus.SUBMITTED);
            return retJob;
        }

        @Override
        public List<Job> findActiveJobsByClient(String organization) {
            return null;
        }

        @Override
        public List<Job> findByContractNumberEqualsAndStatusInAndStartedByOrderByCompletedAtDesc(String contractNumber, List<JobStatus> statuses, JobStartedBy startedBy) {
            return null;
        }

        @Override
        public List<Job> findStuckJobs(OffsetDateTime createdAt) {
            return null;
        }

        @Override
        public void resetJobsToSubmittedStatus(List<String> jobUuids) {

        }

        @Override
        public void updatePercentageCompleted(String jobUuid, int percentageCompleted) {

        }

        @Override
        public int countJobByContractNumberAndStatus(String contractNumber, List<JobStatus> statuses) {
            return 0;
        }

        @Override
        public List<Job> findByJobUuidIn(List<String> jobUuids) {
            return null;
        }

        @Override
        public List<Job> findAll() {
            return null;
        }

        @Override
        public List<Job> findAll(Sort sort) {
            return null;
        }

        @Override
        public List<Job> findAllById(Iterable<Long> longs) {
            return null;
        }

        @Override
        public <S extends Job> List<S> saveAll(Iterable<S> entities) {
            return null;
        }

        @Override
        public void flush() {

        }

        @Override
        public <S extends Job> S saveAndFlush(S entity) {
            return null;
        }

        @Override
        public <S extends Job> List<S> saveAllAndFlush(Iterable<S> entities) {
            return null;
        }

        @Override
        public void deleteAllInBatch(Iterable<Job> entities) {

        }

        @Override
        public void deleteAllByIdInBatch(Iterable<Long> longs) {

        }

        @Override
        public void deleteAllInBatch() {

        }

        @Override
        public Job getOne(Long aLong) {
            return null;
        }

        @Override
        public Job getById(Long aLong) {
            return null;
        }

        @Override
        public <S extends Job> List<S> findAll(Example<S> example) {
            return null;
        }

        @Override
        public <S extends Job> List<S> findAll(Example<S> example, Sort sort) {
            return null;
        }

        @Override
        public Page<Job> findAll(Pageable pageable) {
            return null;
        }

        @Override
        public <S extends Job> S save(S entity) {
            return null;
        }

        @Override
        public Optional<Job> findById(Long aLong) {
            return Optional.empty();
        }

        @Override
        public boolean existsById(Long aLong) {
            return false;
        }

        @Override
        public long count() {
            return 0;
        }

        @Override
        public void deleteById(Long aLong) {

        }

        @Override
        public void delete(Job entity) {

        }

        @Override
        public void deleteAllById(Iterable<? extends Long> longs) {

        }

        @Override
        public void deleteAll(Iterable<? extends Job> entities) {

        }

        @Override
        public void deleteAll() {

        }

        @Override
        public <S extends Job> Optional<S> findOne(Example<S> example) {
            return Optional.empty();
        }

        @Override
        public <S extends Job> Page<S> findAll(Example<S> example, Pageable pageable) {
            return null;
        }

        @Override
        public <S extends Job> long count(Example<S> example) {
            return 0;
        }

        @Override
        public <S extends Job> boolean exists(Example<S> example) {
            return false;
        }

        @Override
        public <S extends Job, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
            return null;
        }
    }
}
