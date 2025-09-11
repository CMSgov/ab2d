package gov.cms.ab2d.contracts.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidContractParamException extends RuntimeException {
    public InvalidContractParamException(String msg) {
        super(msg);
    }
}
