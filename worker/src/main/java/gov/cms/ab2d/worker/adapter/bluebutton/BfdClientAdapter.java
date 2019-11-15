package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.worker.adapter.bluebutton.BfdClientAdapterImpl.EobBundleDTO;

public interface BfdClientAdapter {

    EobBundleDTO getEobBundle(String patientId);
}
