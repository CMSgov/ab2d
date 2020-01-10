package gov.cms.ab2d.worker.service;

import java.util.List;

public interface ShutDownService {

    void resetInProgressJobs(List<String> jobsInProgress);

}
