package gov.cms.ab2d.common.model;

import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.Optional;

@ToString
public class CoveragePagingRequest {

    /**
     * When the job was started at which is used to dictate searches of the database
     *
     * Derived from {@link Job#getCreatedAt()}
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
     * Internal beneficiary id that last page ended on {@link Identifiers#getBeneficiaryId()}
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
