package gov.cms.ab2d.worker.repository;

import gov.cms.ab2d.worker.model.ContractWorkerDto;
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
    private final ContractWorkerDto contract;

    public StubContractRepository(ContractWorkerDto contract) {
        this.contract = contract;
    }

    @Override
    public ContractWorkerDto findContractByContractNumber(String contractNumber) {
        return contract;
    }

    @Override
    public List<ContractWorkerDto> findAll() {
        return null;
    }

    @Override
    public List<ContractWorkerDto> findAll(Sort sort) {
        return null;
    }

    @Override
    public Page<ContractWorkerDto> findAll(Pageable pageable) {
        return null;
    }

    @Override
    public List<ContractWorkerDto> findAllById(Iterable<Long> longs) {
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
    public void delete(ContractWorkerDto entity) {

    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {

    }

    @Override
    public void deleteAll(Iterable<? extends ContractWorkerDto> entities) {

    }

    @Override
    public void deleteAll() {

    }

    @Override
    public <S extends ContractWorkerDto> S save(S entity) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> List<S> saveAll(Iterable<S> entities) {
        return null;
    }

    @Override
    public Optional<ContractWorkerDto> findById(Long aLong) {
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
    public <S extends ContractWorkerDto> S saveAndFlush(S entity) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> List<S> saveAllAndFlush(Iterable<S> entities) {
        return null;
    }

    @Override
    public void deleteAllInBatch(Iterable<ContractWorkerDto> entities) {

    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {

    }

    @Override
    public void deleteAllInBatch() {

    }

    @Override
    public ContractWorkerDto getOne(Long aLong) {
        return null;
    }

    @Override
    public ContractWorkerDto getById(Long aLong) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends ContractWorkerDto> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends ContractWorkerDto> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends ContractWorkerDto> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends ContractWorkerDto, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }
}
