package gov.cms.ab2d.worker.service.coveragesnapshot;

import gov.cms.ab2d.snsclient.messages.AB2DServices;

public interface CoverageSnapshotService {

    void sendCoverageCounts(AB2DServices services);
}
