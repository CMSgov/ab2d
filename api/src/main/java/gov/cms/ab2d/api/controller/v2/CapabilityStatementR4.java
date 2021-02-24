package gov.cms.ab2d.api.controller.v2;

import org.hl7.fhir.r4.model.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.common.util.Constants.API_PREFIX_V2;
import static gov.cms.ab2d.common.util.Constants.FHIR_PREFIX;

public class CapabilityStatementR4 {
    public static CapabilityStatement populateCS(String server) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        CapabilityStatement cs = new CapabilityStatement();
        cs.setPublisher("Centers for Medicare &amp; Medicaid Services");
        cs.setKind(CapabilityStatement.CapabilityStatementKind.REQUIREMENTS);
        cs.setStatus(Enumerations.PublicationStatus.DRAFT);
        try {
            Date lastUpdated = sdf.parse("02/22/2020 00:00:00");
            cs.setDate(lastUpdated);
        } catch (Exception ex) {
            cs.setDate(new Date());
        }
        cs.setFhirVersion(Enumerations.FHIRVersion._4_0_0);
        cs.setPurpose("Defines FHIR R4 (V2) version of AB2D bulk data download");

        CodeType codeType = new CodeType();
        codeType.setValue("application/json");
        CodeType codeType2 = new CodeType();
        codeType2.setValue("application/fhir+json");
        cs.setFormat(List.of(codeType, codeType2));

        CapabilityStatement.CapabilityStatementSoftwareComponent cssc = new CapabilityStatement.CapabilityStatementSoftwareComponent();
        cssc.setName("AB2D");
        cssc.setVersion("V2");
        try {
            Date releaseDate = sdf.parse("05/01/2020 00:00:00");
            cssc.setReleaseDate(releaseDate);
        } catch (Exception ex) {
            cssc.setReleaseDate(new Date());
        }
        cs.setSoftware(cssc);

        CapabilityStatement.CapabilityStatementImplementationComponent implementation = new CapabilityStatement.CapabilityStatementImplementationComponent();
        implementation.setDescription("AB2D FHIR R4 Bulk Data Download Implementation");
        implementation.setUrl(server + API_PREFIX_V2 + FHIR_PREFIX);
        cs.setImplementation(implementation);

        CapabilityStatement.CapabilityStatementRestComponent rest = new CapabilityStatement.CapabilityStatementRestComponent();
        rest.setMode(CapabilityStatement.RestfulCapabilityMode.SERVER);
        CapabilityStatement.CapabilityStatementRestSecurityComponent security = new CapabilityStatement.CapabilityStatementRestSecurityComponent();
        security.setCors(true);

        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
        coding.setCode("OAuth");
        coding.setDisplay("OAuth");
        codeableConcept.setCoding(List.of(coding));
        codeableConcept.setText("OAuth");
        security.setService(List.of(codeableConcept));
        rest.setSecurity(security);
        List<CapabilityStatement.CapabilityStatementRestResourceOperationComponent> restComponents = new ArrayList<>();
        restComponents.add(createOperation("export", server + API_PREFIX_V2 + FHIR_PREFIX + "/Patient/$export"));
        restComponents.add(createOperation("export by contract", server + API_PREFIX_V2 + FHIR_PREFIX + "/Group/{contractNumber}/$export"));
        restComponents.add(createOperation("cancel", server + API_PREFIX_V2 + FHIR_PREFIX + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("status", server + API_PREFIX_V2 + FHIR_PREFIX + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("download", server + API_PREFIX_V2 + FHIR_PREFIX + "/Job/{jobUuid}/file/{filename}"));
        restComponents.add(createOperation("capability", server + API_PREFIX_V2 + FHIR_PREFIX + "/metadata"));
        rest.setOperation(restComponents);
        rest.setInteraction(List.of(new CapabilityStatement.SystemInteractionComponent().setCode(CapabilityStatement.SystemRestfulInteraction.BATCH)));
        cs.setRest(List.of(rest));
        return cs;
    }

    private static CapabilityStatement.CapabilityStatementRestResourceOperationComponent createOperation(String name, String path) {
        CapabilityStatement.CapabilityStatementRestResourceOperationComponent operation = new CapabilityStatement.CapabilityStatementRestResourceOperationComponent();
        operation.setName(name);
        operation.setDefinition(path);
        return operation;
    }
}
