package gov.cms.ab2d.coverage.model;

import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.ToString;

@ToString
public class CoveragePagingRequest {

    /**
     * When a job is submitted by a PDP. If coverage data is modified after this datetime then
     * the results of the job are not reliable and the job will fail.
     *
     */
    private final OffsetDateTime jobStartTime;
    private final ContractForCoverageDTO contract;

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

    private final boolean isV3;

    public CoveragePagingRequest(int pageSize, Long cursor, ContractForCoverageDTO contract, OffsetDateTime jobStartTime) {
        this(pageSize, cursor, contract, jobStartTime, false);
    }

    private CoveragePagingRequest(int pageSize, Long cursor, ContractForCoverageDTO contract, OffsetDateTime jobStartTime, boolean isV3) {
        this.contract = contract;
        this.pageSize = pageSize;
        this.cursor = cursor;
        this.jobStartTime = jobStartTime;
        this.isV3 = isV3;
    }

    public static CoveragePagingRequest ofV3(int pageSize, Long cursor, ContractForCoverageDTO contract, OffsetDateTime jobStartTime) {
        return new CoveragePagingRequest(pageSize, cursor, contract, jobStartTime, true);
    }

    public OffsetDateTime getJobStartTime() {
        return jobStartTime;
    }

    public ContractForCoverageDTO getContract() {
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

    public boolean isV3() {
        return isV3;
    }
}
