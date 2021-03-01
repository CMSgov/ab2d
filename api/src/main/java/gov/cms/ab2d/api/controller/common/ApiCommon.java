package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.controller.InMaintenanceModeException;
import gov.cms.ab2d.api.controller.TooManyRequestsException;
import gov.cms.ab2d.common.model.Job;
import gov.cms.ab2d.common.service.InvalidUserInputException;
import gov.cms.ab2d.common.service.JobService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.util.Set;

import static gov.cms.ab2d.api.controller.common.ApiText.CONT_LOC;
import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V1;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.USERNAME;
import static gov.cms.ab2d.common.util.Constants.ZIP_SUPPORT_ON;
import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

@Service
@Slf4j
@SuppressWarnings("PMD.TooManyStaticImports")
public class ApiCommon {
    private final LogManager eventLogger;
    private final JobService jobService;
    private final PropertiesService propertiesService;

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    public static final String ALLOWABLE_OUTPUT_FORMATS =
            "application/fhir+ndjson,application/ndjson,ndjson," + ZIPFORMAT;
    public static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));
    public static final String JOB_CANCELLED_MSG = "Job canceled";
    public static final String JOB_NOT_FOUND_ERROR_MSG = "Job not found. " + GENERIC_FHIR_ERR_MSG;

    public ApiCommon(LogManager eventLogger, JobService jobService, PropertiesService propertiesService) {
        this.eventLogger = eventLogger;
        this.jobService = jobService;
        this.propertiesService = propertiesService;
    }

    public boolean shouldReplaceWithHttps(HttpServletRequest request) {
        return "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public String getCurrentUrl(HttpServletRequest request) {
        return shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequest().scheme("https").toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequest().toUriString().replace(":80/", "/");
    }

    public String getUrl(String ending, HttpServletRequest request) {
        return shouldReplaceWithHttps(request) ?
                ServletUriComponentsBuilder.fromCurrentRequestUri().scheme("https").replacePath(ending).toUriString() :
                ServletUriComponentsBuilder.fromCurrentRequestUri().replacePath(ending).toUriString().replace(":80/", "/");
    }

    public void checkSinceTime(OffsetDateTime date) {
        if (date == null) {
            return;
        }
        if (date.isAfter(OffsetDateTime.now())) {
            throw new InvalidUserInputException("You can not use a time after the current time for _since");
        }
        try {
            OffsetDateTime ed = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);
            if (date.isBefore(ed)) {
                log.error("Invalid _since time received {}", date);
                throw new InvalidUserInputException("_since must be after " + ed.format(ISO_OFFSET_DATE_TIME));
            }
        } catch (Exception ex) {
            throw new InvalidUserInputException("${api.since.date.earliest} date value '" + SINCE_EARLIEST_DATE + "' is invalid");
        }
    }

    public void checkIfInMaintenanceMode() {
        if (propertiesService.isInMaintenanceMode()) {
            throw new InMaintenanceModeException("The system is currently in maintenance mode. Please try the request again later.");
        }
    }

    public void checkIfCurrentUserCanAddJob() {
        if (!jobService.checkIfCurrentUserCanAddJob()) {
            String errorMsg = "You already have active export requests in progress. Please wait until they complete before submitting a new one.";
            log.error(errorMsg);
            throw new TooManyRequestsException(errorMsg);
        }
    }

    public ResponseEntity<Void> returnStatusForJobCreation(Job job, String requestId, HttpServletRequest request) {
        String statusURL = getUrl(API_PREFIX_V1 + FHIR_PREFIX + "/Job/" + job.getJobUuid() + "/$status", request);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CONT_LOC, statusURL);
        eventLogger.log(new ApiResponseEvent(MDC.get(USERNAME), job.getJobUuid(), HttpStatus.ACCEPTED, "Job Created",
                "Job " + job.getJobUuid() + " was created", requestId));
        return new ResponseEntity<>(null, responseHeaders,
                HttpStatus.ACCEPTED);
    }

    public void checkResourceTypesAndOutputFormat(String resourceTypes, String outputFormat) {
        if (resourceTypes != null && !resourceTypes.equals(EOB)) {
            log.error("Received invalid resourceTypes of {}", resourceTypes);
            throw new InvalidUserInputException("_type must be " + EOB);
        }

        final String errMsg = "An _outputFormat of " + outputFormat + " is not valid";

        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            log.error("Received _outputFormat {}, which is not valid", outputFormat);
            throw new InvalidUserInputException(errMsg);
        }

        final boolean zipSupportOn = propertiesService.isToggleOn(ZIP_SUPPORT_ON);
        if (!zipSupportOn && ZIPFORMAT.equalsIgnoreCase(outputFormat)) {
            throw new InvalidUserInputException(errMsg);
        }
    }

    public void logSuccessfulJobCreation(Job job) {
        MDC.put(JOB_LOG, job.getJobUuid());
        log.info("Successfully created job");
    }

    public void checkValidCreateJob(OffsetDateTime since, String resourceTypes, String outputFormat) {
        checkIfInMaintenanceMode();
        checkIfCurrentUserCanAddJob();
        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);
        checkSinceTime(since);
    }
}
