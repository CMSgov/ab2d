package gov.cms.ab2d.worker.processor.pauseresume.lease;

public record PauseResumeJobLeaseToken(String jobUuid, long fenceToken) {}
