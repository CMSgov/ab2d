package gov.cms.ab2d.worker.repository;

import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.model.JobStartedBy;
import gov.cms.ab2d.common.model.JobStatus;
import gov.cms.ab2d.common.repository.JobRepository;
import lombok.Getter;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("all")
public class StubJobRepository implements JobRepository {
    private Job job;

    @Getter
    private int updatePercentageCompletedCount;

    public StubJobRepository(Job job) {
        this.job = job;
    }


    @Override
    public void cancelJobByJobUuid(String jobUuid) {

    }

    @Override
    public Job findByJobUuid(String jobUuid) {
        return (job.getJobUuid().equals(jobUuid)) ? job : null;
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
    public int updatePercentageCompleted(String jobUuid, int percentageCompleted) {
        updatePercentageCompletedCount++;
        return 0;
    }

    @Override
    public int countJobByContractNumberAndStatus(String contractNumber, List<JobStatus> statuses) {
        return 0;
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
    public Page<Job> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public List<Job> findAllById(Iterable<Long> longs) {
        return null;
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
    public <S extends Job> S save(S entity) {
        return null;
    }

    @Override
    public <S extends Job> List<S> saveAll(Iterable<S> entities) {
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
    public <S extends Job> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
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
