package gov.cms.ab2d.api.controller;

import lombok.Data;

@Data
public class ClearCoverageCacheRequest {

    private String contractNumber;
    private Integer month;
}
