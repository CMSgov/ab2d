package gov.cms.ab2d.coverage.dto;

import lombok.Data;

@Data
public class ClearCoverageCacheRequest {

    private String contractNumber;
    private Integer month;
    private Integer year;
}
