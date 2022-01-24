package gov.cms.ab2d.worker.repository;

import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.repository.ContractRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("all")
public class StubContractRepository implements ContractRepository {
    private final Contract contract;

    public StubContractRepository(Contract contract) {
        this.contract = contract;
    }

    @Override
    public Optional<Contract> findContractByContractNumber(String contractNumber) {
        return Optional.of(contract);
    }

    @Override
    public List<Contract> findAll() {
        return null;
    }

    @Override
    public List<Contract> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<Contract> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public List<Contract> findAllById(Iterable<Long> longs) {
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
    public void delete(Contract entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends Contract> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public <S extends Contract> S save(S entity) {
        return null;
    }

    @Override
    public <S extends Contract> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<Contract> findById(Long aLong) {
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
    public <S extends Contract> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends Contract> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<Contract> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public Contract getOne(Long aLong) {
        return null;
    }

    @Override
    public Contract getById(Long aLong) {
        return null;
    }

    @Override
    public <S extends Contract> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Contract> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends Contract> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends Contract> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Contract> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Contract> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Contract, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }
}
