package gov.cms.ab2d.worker.adapter.bluebutton;

import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.Future;

public interface BfdClientAdapter {
    @Async
    Future<BfdClientAdapterImpl.EobBundleDTO> getEobBundle(String patientId);
}
