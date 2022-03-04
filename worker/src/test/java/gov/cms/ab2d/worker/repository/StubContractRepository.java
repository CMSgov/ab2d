package gov.cms.ab2d.worker.repository;

import gov.cms.ab2d.worker.model.ContractWorkerEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

@SuppressWarnings("all")
public class StubContractRepository implements ContractWorkerRepository {
    private final ContractWorkerEntity contract;

    public StubContractRepository(ContractWorkerEntity contract) {
        this.contract = contract;
    }

    @Override
    public ContractWorkerEntity findContractByContractNumber(String contractNumber) {
        return contract;
    }

    @Override
    public List<ContractWorkerEntity> findAll() {
        return null;
    }

    @Override
    public List<ContractWorkerEntity> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<ContractWorkerEntity> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public List<ContractWorkerEntity> findAllById(Iterable<Long> longs) {
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
    public void delete(ContractWorkerEntity entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends ContractWorkerEntity> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public <S extends ContractWorkerEntity> S save(S entity) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<ContractWorkerEntity> findById(Long aLong) {
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
    public <S extends ContractWorkerEntity> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<ContractWorkerEntity> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public ContractWorkerEntity getOne(Long aLong) {
        return null;
    }

    @Override
    public ContractWorkerEntity getById(Long aLong) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends ContractWorkerEntity> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends ContractWorkerEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends ContractWorkerEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends ContractWorkerEntity, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }
}
