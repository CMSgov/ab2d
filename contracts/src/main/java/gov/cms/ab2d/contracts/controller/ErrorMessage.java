package gov.cms.ab2d.contracts.controller;

import java.util.Date;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ErrorMessage {
    private Date timestamp;
    private String path;
    private int code;
    private String message;
}
