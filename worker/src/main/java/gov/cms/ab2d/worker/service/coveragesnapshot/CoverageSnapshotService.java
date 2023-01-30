package gov.cms.ab2d.worker.service.coveragesnapshot;

import gov.cms.ab2d.snsclient.messages.AB2DServices;

import java.util.Set;

public interface CoverageSnapshotService {

    void sendCoverageCounts(AB2DServices services, Set<String> contracts);

    void sendCoverageCounts(AB2DServices services, String contract, int count, int year, int month);
}
