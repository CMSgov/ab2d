package gov.cms.ab2d.common.dto;

import lombok.Data;

@Data
public class ClearCoverageCacheRequest {

    private String contractNumber;
    private Integer month;
}
