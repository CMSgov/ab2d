package gov.cms.ab2d.api.controller.common;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ApiText {
    public static final String STATUS_API = "API to determine the status of the job, the files to download " +
            "once the job is complete and an endpoint to cancel a job";
    public static final String BULK_RESPONSE = "Absolute URL of an endpoint for subsequent status requests (polling location)";
    public static final String RUNNING_JOBIDS = "URLs of currently running jobs. To cancel one of those jobs, invoke the Status DELETE call.";
    public static final String BULK_SINCE = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE;
    public static final String BULK_UNTIL = "Ending time of query. Returns all records \"until\" this time.";
    public static final String BULK_SINCE_DEFAULT = " If no value is provided, it will default to the last time a successful job was requested if it exists. The earliest accepted date is 2020-02-13T00:00:00.000-05:00";
    public static final String BULK_UNTIL_DEFAULT = " If no value is provided, it will default to the current date.";
    public static final String BULK_RESPONSE_LONG = "Absolute URL of an endpoint for subsequent status requests (polling location)";
    public static final String EXPORT_STARTED = "Export request has started";
    public static final String MAX_JOBS = "Too many jobs are currently running. Either wait for currently running jobs to finish or cancel some/all of those jobs.";
    public static final String JOB_COMPLETE = "The job is complete.";
    public static final String STATUS_DELAY = "A delay time in seconds before another status request will be accepted.";
    public static final String PROGRESS = "Completion percentage, such as 50%";
    public static final String FILE_EXPIRES = "Indicates when (an HTTP-date timestamp) the files " +
            "listed will no longer be available for access.";
    public static final String CAP_DESC = "A JSON FHIR capability statement matching ";
    public static final String CAP_REQ = "Request the FHIR capability statement detailing what operations this API supports ";
    public static final String APPLICATION_JSON = "application/json";
    public static final String DOWNLOAD_DESC = "Downloads a file produced by an export job.";
    public static final String CONTENT_TYPE_DESC = "Header which must match the file format being delivered: ";
    public static final String BULK_DNLD_DSC = "After creating a job, the API to download the generated bulk download files";
    public static final String NOT_FOUND = "Job or file not found. ";
    public static final String STILL_RUNNING = "The job is still in progress.";
    public static final String STATUS_DES = "Returns a status of an export job.";
    public static final String JOB_ID = "A job identifier";
    public static final String FILE_NAME = "A file name";
    public static final String DNLD_DESC = "Returns the requested file as " + NDJSON_FIRE_CONTENT_TYPE;
    public static final String JOB_NOT_FOUND = "Job not found. " + GENERIC_FHIR_ERR_MSG;
    public static final String JOB_CANCELLED_MSG = "Job canceled";
    public static final String CAP_STMT = "FHIR capability statement";
    public static final String CAP_API = "Provide the standard required FHIR capability statement";
    public static final String CAP_RET = "FHIR Capability Statement Returned";
    public static final String CONTRACT_NO = "A contract number";
    public static final String ASYNC = "respond-async";
    public static final String OUT_FORMAT = "_outputFormat";
    public static final String SINCE = "_since";
    public static final String UNTIL = "_until";
    public static final String TYPE_PARAM = "_type";
    public static final String PREFER = "Prefer";
    public static final String X_PROG = "X-Progress";
}
