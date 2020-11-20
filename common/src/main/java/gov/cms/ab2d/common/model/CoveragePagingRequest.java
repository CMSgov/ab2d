package gov.cms.ab2d.common.model;

import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ToString
public class CoveragePagingRequest {

    private final List<Integer> coveragePeriodIds;
    private final int pageSize;
    private final String cursor;

    public CoveragePagingRequest(int pageSize, String cursor, List<Integer> coveragePeriodIds) {
        this.coveragePeriodIds = new ArrayList<>(coveragePeriodIds);
        this.pageSize = pageSize;
        this.cursor = cursor;
    }

    public CoveragePagingRequest(int pageSize, String cursor, Integer... coveragePeriodIds) {
        this.coveragePeriodIds = List.of(coveragePeriodIds);
        this.pageSize = pageSize;
        this.cursor = cursor;
    }

    public List<Integer> getCoveragePeriodIds() {
        return coveragePeriodIds;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Optional<String> getCursor() {
        return Optional.ofNullable(cursor);
    }
}
