package gov.cms.ab2d.api.controller.common;

import static gov.cms.ab2d.api.util.Constants.GENERIC_FHIR_ERR_MSG;
import static gov.cms.ab2d.common.service.JobService.ZIPFORMAT;
import static gov.cms.ab2d.common.util.Constants.NDJSON_FIRE_CONTENT_TYPE;
import static gov.cms.ab2d.common.util.Constants.SINCE_EARLIEST_DATE;

public class ApiText {
    public static final String BULK_DATA_API = "Bulk Data";
    public static final String STATUS_API = "API to determine the status of the job, the files to download " +
            "once the job is complete and an endpoint to cancel a job";
    public static final String EXP_PATIENT_INFO = "Export Patient Information";
    public static final String BULK_RESPONSE = "Absolute URL of an endpoint for subsequent status requests (polling location)";
    public static final String RUNNING_JOBIDS = "URL of Current Jobs that are running, To cancel one of the jobs, invoke Status DELETE call";
    public static final String BULK_SINCE = "Beginning time of query. Returns all records \"since\" this time. At this time, it must be after " + SINCE_EARLIEST_DATE;
    public static final String BULK_RESPONSE_LONG = "Absolute URL of an endpoint for subsequent status requests (polling location)";
    public static final String EXPORT_STARTED = "Export request has started";
    public static final String MAX_JOBS = "Too Many Jobs Running, either wait for the jobs to finish or cancel the job";
    public static final String JOB_COMPLETE = "The job is complete.";
    public static final String STATUS_DELAY = "A delay time in seconds before another status request will be accepted.";
    public static final String PROGRESS = "Completion percentage, such as 50%";
    public static final String FILE_EXPIRES = "Indicates when (an HTTP-date timestamp) the files " +
            "listed will no longer be available for access.";
    public static final String CANCEL_JOB = "Cancel Export Job";
    public static final String CAP_DESC = "Returns the FHIR capability statement";
    public static final String CAP_REQ = "A request for the FHIR capability statement";
    public static final String APPLICATION_JSON = "application/json";
    public static final String DOWNLOADS_EXPORT_FILE = "Downloads Export File";
    public static final String DOWNLOAD_DESC = "Downloads a file produced by an export job.";
    public static final String CONTENT_TYPE_DESC = "Content-Type header that matches the file format being delivered: ";
    public static final String BULK_DNLD = "Bulk Data File Download API";
    public static final String BULK_DNLD_DSC = "After creating a job, the API to download the generated bulk download files";
    public static final String NOT_FOUND = "Job or file not found. ";
    public static final String STILL_RUNNING = "The job is still in progress.";
    public static final String STATUS_DES = "Returns a status of an export job.";
    public static final String JOB_ID = "A job identifier";
    public static final String FILE_NAME = "A file name";
    public static final String DNLD_DESC = "Returns the requested file as " + NDJSON_FIRE_CONTENT_TYPE + " or " + ZIPFORMAT;
    public static final String EXPORT_JOB_STATUS = "Status of export job";
    public static final String JOB_NOT_FOUND = "Job not found. " + GENERIC_FHIR_ERR_MSG;
    public static final String JOB_CANCELLED_MSG = "Job canceled";
    public static final String CAP_STMT = "FHIR capability statement";
    public static final String CAP_API = "Provides the standard required capability statement";
    public static final String CAP_RET = "FHIR Capability Statement Returned";
    public static final String CONTRACT_NO = "A contract number";
    public static final String EXPORT_CLAIM = "Export Claim Data";
    public static final String ASYNC = "respond-async";
    public static final String OUT_FORMAT = "_outputFormat";
    public static final String SINCE = "_since";
    public static final String TYPE_PARAM = "_type";
    public static final String PREFER = "Prefer";
    public static final String X_PROG = "X-Progress";
}
