package gov.cms.ab2d.contracts.controller;

import gov.cms.ab2d.eventclient.clients.SQSEventClient;
import gov.cms.ab2d.eventclient.config.Ab2dEnvironment;
import gov.cms.ab2d.eventclient.events.ApiResponseEvent;
import gov.cms.ab2d.eventclient.events.ErrorEvent;
import java.util.HashMap;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


import static gov.cms.ab2d.eventclient.events.SlackEvents.API_INVALID_CONTRACT;

/**
 * Don't change exception classes without updating alerts in Splunk. Splunk alerts rely on the classname to filter
 * for these exceptions.
 * <p>
 * todo in the future use an enum instead to make it more consistent
 */
@ControllerAdvice
@Slf4j
public class ErrorHandler extends ResponseEntityExceptionHandler {
    public static final String ORGANIZATION = "organization";
    public static final String REQUEST_ID = "RequestId";


    private final SQSEventClient eventLogger;

    private static final HashMap<Class, HttpStatus> RESPONSE_MAP;

    static {
        RESPONSE_MAP = new HashMap<>();
        RESPONSE_MAP.put(InvalidContractException.class, HttpStatus.FORBIDDEN);
    }

    public ErrorHandler(SQSEventClient eventLogger) {
        this.eventLogger = eventLogger;
    }

    private static HttpStatus getErrorResponse(Class clazz) {
        HttpStatus res = RESPONSE_MAP.get(clazz);
        if (res == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return res;
    }

    @ExceptionHandler({InvalidContractException.class})
    public ResponseEntity<ErrorMessage> handleInvalidContractErrors(Exception ex, HttpServletRequest request) {
        HttpStatus status = getErrorResponse(ex.getClass());
        String description = API_INVALID_CONTRACT + " contractId: " + request.getParameter("contractId") + " contractNumber: " + request.getParameter("contractNumber");

        eventLogger.sendLogs(new ErrorEvent(null, null,
                ErrorEvent.ErrorType.UNAUTHORIZED_CONTRACT, description));

        ApiResponseEvent responseEvent = new ApiResponseEvent(null, null, status,
                "API Error", description, (String) request.getAttribute(REQUEST_ID));
        eventLogger.logAndAlert(responseEvent, Ab2dEnvironment.PROD_LIST);


        return new ResponseEntity<>(ErrorMessage.builder()
                .code(status.value())
                .message(ex.getMessage())
                .path(request.getServletPath())
                .timestamp(new java.util.Date())
                .build(), null, status);
    }
}