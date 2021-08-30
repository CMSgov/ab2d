package gov.cms.ab2d.common.model;

import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Optional;

@ToString
public class CoveragePagingRequest {

    private final OffsetDateTime jobStartTime;
    private final Contract contract;
    private final int pageSize;
    private final Long cursor;

    public CoveragePagingRequest(int pageSize, Long cursor, Contract contract, OffsetDateTime jobStartTime) {
        this.contract = contract;
        this.pageSize = pageSize;
        this.cursor = cursor;
        this.jobStartTime = jobStartTime;
    }

    public OffsetDateTime getJobStartTime() {
        return jobStartTime;
    }

    public Contract getContract() {
        return contract;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Optional<Long> getCursor() {
        return Optional.ofNullable(cursor);
    }

    public String getContractNumber() {
        return contract.getContractNumber();
    }
}
