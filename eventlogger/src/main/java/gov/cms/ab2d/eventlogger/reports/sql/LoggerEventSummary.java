package gov.cms.ab2d.eventlogger.reports.sql;

import gov.cms.ab2d.eventlogger.LoggableEvent;
import gov.cms.ab2d.eventlogger.events.JobSummaryEvent;
import gov.cms.ab2d.eventlogger.events.JobStatusChangeEvent;
import gov.cms.ab2d.eventlogger.events.FileEvent;
import gov.cms.ab2d.eventlogger.events.ApiResponseEvent;
import gov.cms.ab2d.eventlogger.events.ContractBeneSearchEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LoggerEventSummary {
    private final LoggerEventRepository loggerEventRepository;

    public LoggerEventSummary(LoggerEventRepository loggerEventRepository) {
        this.loggerEventRepository = loggerEventRepository;
    }

    public JobSummaryEvent getSummary(String jobId) {
        try {
            if (jobId == null || jobId.isEmpty()) {
                log.error("Can't do a job summary for an empty job id");
                return new JobSummaryEvent();
            }
            List<LoggableEvent> jobChangeEvents = loggerEventRepository.load(JobStatusChangeEvent.class, jobId);
            List<LoggableEvent> fileEvents = loggerEventRepository.load(FileEvent.class, jobId);
            List<LoggableEvent> downloadEvents =
                    loggerEventRepository.load(ApiResponseEvent.class, jobId).stream()
                            .filter(e -> "File Download".equalsIgnoreCase(((ApiResponseEvent) e).getResponseString()))
                            .collect(Collectors.toList());
            List<LoggableEvent> contractSearchData = loggerEventRepository.load(ContractBeneSearchEvent.class, jobId);
            List<LoggableEvent> allEvents = new ArrayList<>();
            allEvents.addAll(jobChangeEvents);
            allEvents.addAll(fileEvents);
            allEvents.addAll(downloadEvents);
            allEvents.addAll(contractSearchData);
            String firstUserFound = allEvents.stream()
                    .map(LoggableEvent::getOrganization)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            JobSummaryEvent jobSummaryEvent = new JobSummaryEvent();
            jobSummaryEvent.setJobId(jobId);
            jobSummaryEvent.setOrganization(firstUserFound);
            jobSummaryEvent.setSubmittedTime(getTime(jobChangeEvents, "SUBMITTED"));
            jobSummaryEvent.setInProgressTime(getTime(jobChangeEvents, "IN_PROGRESS"));
            jobSummaryEvent.setSuccessfulTime(getTime(jobChangeEvents, "SUCCESSFUL"));
            jobSummaryEvent.setCancelledTime(getTime(jobChangeEvents, "CANCELLED"));
            jobSummaryEvent.setFailedTime(getTime(jobChangeEvents, "FAILED"));
            jobSummaryEvent.setNumFilesCreated(getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.CLOSE));
            jobSummaryEvent.setNumFilesDeleted(getUniqueNumFilesOfType(fileEvents, FileEvent.FileStatus.DELETE));
            jobSummaryEvent.setNumFilesDownloaded(downloadEvents.size());
            if (!contractSearchData.isEmpty()) {
                List<ContractBeneSearchEvent> searches = new ArrayList<>();
                for (LoggableEvent event : contractSearchData) {
                    searches.add((ContractBeneSearchEvent) event);
                }
                jobSummaryEvent.setSuccessfullySearched(searches.stream().map(ContractBeneSearchEvent::getNumSearched).reduce(0, Integer::sum));
                jobSummaryEvent.setErrorSearched(searches.stream().map(ContractBeneSearchEvent::getNumErrors).reduce(0, Integer::sum));
                jobSummaryEvent.setTotalNum(searches.stream().map(ContractBeneSearchEvent::getNumInContract).reduce(0, Integer::sum));
            }
            return jobSummaryEvent;
        } catch (Exception ex) {
            // Logging shouldn ever break anything
            log.error("Error creating summary object", ex);
        }
        return null;
    }

    int getUniqueNumFilesOfType(List<LoggableEvent> events, FileEvent.FileStatus type) {
        return (int) events.stream()
                .map(c -> ((FileEvent) c).getStatus())
                .filter(c -> c == type)
                .count();
    }

    OffsetDateTime getTime(List<LoggableEvent> events, String type) {
        if (events == null || type == null || type.isEmpty()) {
            return null;
        }
        LoggableEvent event = events.stream()
                .filter(c -> type.equalsIgnoreCase(((JobStatusChangeEvent) c).getNewStatus()))
                .findFirst()
                .orElse(null);
        if (event != null) {
            return event.getTimeOfEvent();
        }
        return null;
    }
}
