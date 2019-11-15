package gov.cms.ab2d.worker.adapter.bluebutton;

import org.hl7.fhir.dstu3.model.Resource;

import java.util.List;
import java.util.concurrent.Future;

public interface BfdClientAdapter {

    Future<List<Resource>> getResources(String patientId);

}
