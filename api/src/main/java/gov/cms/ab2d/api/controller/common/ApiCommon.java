package gov.cms.ab2d.api.controller.common;

import gov.cms.ab2d.api.controller.InMaintenanceModeException;
import gov.cms.ab2d.api.controller.TooManyRequestsException;
import gov.cms.ab2d.api.remote.JobClient;
import gov.cms.ab2d.job.dto.StartJobDTO;
import gov.cms.ab2d.common.model.Contract;
import gov.cms.ab2d.common.model.PdpClient;
import gov.cms.ab2d.common.service.InvalidClientInputException;
import gov.cms.ab2d.common.service.InvalidContractException;
import gov.cms.ab2d.common.service.PdpClientService;
import gov.cms.ab2d.common.service.PropertiesService;
import gov.cms.ab2d.eventlogger.LogManager;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.fhir.FhirVersion;
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

import static gov.cms.ab2d.common.util.Constants.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;
import static gov.cms.ab2d.common.util.Constants.ORGANIZATION;
import static gov.cms.ab2d.common.util.Constants.ZIP_SUPPORT_ON;
import static gov.cms.ab2d.common.util.Constants.JOB_LOG;
import static gov.cms.ab2d.fhir.BundleUtils.EOB;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.springframework.http.HttpHeaders.CONTENT_LOCATION;

@Service
@Slf4j
public class ApiCommon {
    private final LogManager eventLogger;
    private final JobClient jobClient;
    private final PropertiesService propertiesService;
    private final PdpClientService pdpClientService;

    // Since this is used in an annotation, it can't be derived from the Set, otherwise it will be an error
    public static final String ALLOWABLE_OUTPUT_FORMATS =
            "application/fhir+ndjson,application/ndjson,ndjson," + ZIPFORMAT;
    public static final Set<String> ALLOWABLE_OUTPUT_FORMAT_SET = Set.of(ALLOWABLE_OUTPUT_FORMATS.split(","));
    public static final String JOB_CANCELLED_MSG = "Job canceled";

    public ApiCommon(LogManager eventLogger, JobClient jobClient, PropertiesService propertiesService,
                     PdpClientService pdpClientService) {
        this.eventLogger = eventLogger;
        this.jobClient = jobClient;
        this.propertiesService = propertiesService;
        this.pdpClientService = pdpClientService;
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
            throw new InvalidClientInputException("You can not use a time after the current time for _since");
        }
        try {
            OffsetDateTime ed = OffsetDateTime.parse(SINCE_EARLIEST_DATE, ISO_DATE_TIME);
            if (date.isBefore(ed)) {
                log.error("Invalid _since time received {}", date);
                throw new InvalidClientInputException("_since must be after " + ed.format(ISO_OFFSET_DATE_TIME));
            }
        } catch (Exception ex) {
            throw new InvalidClientInputException("${api.since.date.earliest} date value '" + SINCE_EARLIEST_DATE + "' is invalid");
        }
    }

    public void checkIfInMaintenanceMode() {
        if (propertiesService.isInMaintenanceMode()) {
            throw new InMaintenanceModeException("The system is currently in maintenance mode. Please try the request again later.");
        }
    }

    public void checkIfCurrentClientCanAddJob() {
        PdpClient pdpClient = pdpClientService.getCurrentClient();
        String organization = pdpClient.getOrganization();
        if (jobClient.activeJobs(organization) >= pdpClient.getMaxParallelJobs()) {
            String errorMsg = "You already have active export requests in progress. Please wait until they complete before submitting a new one.";
            log.error(errorMsg);
            throw new TooManyRequestsException(errorMsg, jobClient.getActiveJobIds(organization));
        }
    }

    public ResponseEntity<Void> returnStatusForJobCreation(String jobGuid, String apiPrefix, String requestId, HttpServletRequest request) {
        String statusURL = getUrl(apiPrefix + FHIR_PREFIX + "/Job/" + jobGuid + "/$status", request);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add(CONTENT_LOCATION, statusURL);
        eventLogger.log(new ApiResponseEvent(MDC.get(ORGANIZATION), jobGuid, HttpStatus.ACCEPTED, "Job Created",
                "Job " + jobGuid + " was created", requestId));
        return new ResponseEntity<>(null, responseHeaders,
                HttpStatus.ACCEPTED);
    }

    public void checkResourceTypesAndOutputFormat(String resourceTypes, String outputFormat) {
        if (resourceTypes != null && !resourceTypes.equals(EOB)) {
            log.error("Received invalid resourceTypes of {}", resourceTypes);
            throw new InvalidClientInputException("_type must be " + EOB);
        }

        final String errMsg = "An _outputFormat of " + outputFormat + " is not valid";

        if (outputFormat != null && !ALLOWABLE_OUTPUT_FORMAT_SET.contains(outputFormat)) {
            log.error("Received _outputFormat {}, which is not valid", outputFormat);
            throw new InvalidClientInputException(errMsg);
        }

        final boolean zipSupportOn = propertiesService.isToggleOn(ZIP_SUPPORT_ON);
        if (!zipSupportOn && ZIPFORMAT.equalsIgnoreCase(outputFormat)) {
            throw new InvalidClientInputException(errMsg);
        }
    }

    public void logSuccessfulJobCreation(String jobGuid) {
        MDC.put(JOB_LOG, jobGuid);
        log.info("Successfully created job");
    }

    public StartJobDTO checkValidCreateJob(HttpServletRequest request, String contractNumber, OffsetDateTime since,
                                           String resourceTypes, String outputFormat, FhirVersion version) {
        PdpClient pdpClient = pdpClientService.getCurrentClient();
        contractNumber = checkIfContractAttested(pdpClient.getContract(), contractNumber);
        checkIfInMaintenanceMode();
        checkIfCurrentClientCanAddJob();
        checkResourceTypesAndOutputFormat(resourceTypes, outputFormat);
        checkSinceTime(since);
        return new StartJobDTO(contractNumber, pdpClient.getOrganization(), resourceTypes,
                getCurrentUrl(request), outputFormat, since, version);
    }

    private String checkIfContractAttested(Contract contract, String contractNumber) {
        if (contractNumber == null) {
            contractNumber = contract.getContractNumber();
        }

        if (contract == null) {
            throw new IllegalStateException("Not sure if we should really look up a contract if we aren't bound to it.");
        }

        if (!contract.getContractNumber().equals(contractNumber)) {
            String errorMsg = "Specifying contract: " + contractNumber + " not associated with internal id: " +
                    pdpClientService.getCurrentClient().getId();
            log.error(errorMsg);
            throw new InvalidContractException(errorMsg);
        }

        if (!contract.hasAttestation()) {
            String errorMsg = "Contract: " + contractNumber + " is not attested.";
            log.error(errorMsg);
            throw new InvalidContractException(errorMsg);
        }
        // Validated contract
        return contractNumber;
    }
}
