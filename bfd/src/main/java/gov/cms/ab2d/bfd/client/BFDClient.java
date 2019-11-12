package gov.cms.ab2d.bfd.client;


import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.dstu3.model.Bundle;


public interface BFDClient {


    Bundle requestEOBFromServer(String patientID) throws ResourceNotFoundException;

    Bundle requestNextBundleFromServer(Bundle bundle) throws ResourceNotFoundException;


}

