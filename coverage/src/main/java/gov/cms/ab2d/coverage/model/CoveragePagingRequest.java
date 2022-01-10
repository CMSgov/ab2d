package gov.cms.ab2d.coverage.model;

import gov.cms.ab2d.common.model.Contract;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Optional;

@ToString
public class CoveragePagingRequest {

    /**
     * When a job is submitted by a PDP. If coverage data is modified after this datetime then
     * the results of the job are not reliable and the job will fail.
     *
     */
    private final OffsetDateTime jobStartTime;
    private final Contract contract;

    /**
     * Number of beneficiaries at a time to pull from the database
     */
    private final int pageSize;

    /**
     * Internal tracking of where in the data set work is being done
     *
     */
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
