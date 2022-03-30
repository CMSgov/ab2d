package gov.cms.ab2d.audit.cleanup;

public interface FileDeletionService {

    void deleteFiles();

    // Ideally we would have a way to filter out files that have already been deleted but that's not possible currently
    // We might need to further constrain the query, so it ignores files that expired x time ago
    // since the stale job deleter will delete any files that somehow fall through the cracks
    void deleteDownloadIntervalExpiredFiles();
}
