package gov.cms.ab2d.bfd.client;


import org.hl7.fhir.dstu3.model.Bundle;


public interface BFDClient {
    Bundle requestEOBFromServer(String patientID);
    Bundle requestNextBundleFromServer(Bundle bundle);
    Bundle requestPatientFromServer(String patientId);

    Bundle requestPartDEnrolleesFromServer(String contractNum, int month);
}

