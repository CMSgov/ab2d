package gov.cms.ab2d.api.controller.v1;

import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.dstu3.model.CapabilityStatement;
import org.hl7.fhir.dstu3.model.CodeType;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.Enumerations;
import org.hl7.fhir.dstu3.model.Reference;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static gov.cms.ab2d.api.controller.common.ApiText.APPLICATION_JSON;

public class CapabilityStatementSTU3 {
    public static CapabilityStatement populateCS(String server) {
        SimpleDateFormat sdfLess = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        CapabilityStatement cs = new CapabilityStatement();
        cs.setPublisher("Centers for Medicare &amp; Medicaid Services");
        cs.setKind(CapabilityStatement.CapabilityStatementKind.INSTANCE);
        cs.setStatus(Enumerations.PublicationStatus.ACTIVE);

        String dt = sdfLess.format(new Date());
        try {
            cs.setDate(sdfLess.parse(dt));
        } catch (ParseException e) {
            cs.setDate(new Date());
        }
        cs.setAcceptUnknown(CapabilityStatement.UnknownContentCode.EXTENSIONS);
        cs.setFhirVersion(FhirVersionEnum.DSTU3.getFhirVersionString());

        CodeType codeType = new CodeType();
        codeType.setValue(APPLICATION_JSON);
        CodeType codeType2 = new CodeType();
        codeType2.setValue("application/fhir+json");
        cs.setFormat(List.of(codeType, codeType2));

        CapabilityStatement.CapabilityStatementSoftwareComponent cssc = new CapabilityStatement.CapabilityStatementSoftwareComponent();
        cssc.setName("AB2D");
        cssc.setVersion("V1");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            Date releaseDate = sdf.parse("08/27/2020 00:00:00");
            cssc.setReleaseDate(releaseDate);
        } catch (Exception ex) {
            cssc.setReleaseDate(new Date());
        }
        cs.setSoftware(cssc);

        CapabilityStatement.CapabilityStatementImplementationComponent implementation = new CapabilityStatement.CapabilityStatementImplementationComponent();
        implementation.setUrl(server);
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
        List<CapabilityStatement.CapabilityStatementRestOperationComponent> restComponents = new ArrayList<>();
        restComponents.add(createOperation("export", server + "/Patient/$export"));
        restComponents.add(createOperation("export by contract", server + "/Group/{contractNumber}/$export"));
        restComponents.add(createOperation("cancel", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("status", server + "/Job/{jobUuid}/$status"));
        restComponents.add(createOperation("download", server + "/Job/{jobUuid}/file/{filename}"));
        restComponents.add(createOperation("capability", server + "/metadata"));
        rest.setOperation(restComponents);
        rest.setInteraction(List.of(new CapabilityStatement.SystemInteractionComponent().setCode(CapabilityStatement.SystemRestfulInteraction.BATCH)));
        cs.setRest(List.of(rest));

        return cs;
    }

    private static CapabilityStatement.CapabilityStatementRestOperationComponent createOperation(String name, String path) {
        CapabilityStatement.CapabilityStatementRestOperationComponent operation = new CapabilityStatement.CapabilityStatementRestOperationComponent();
        operation.setName(name);
        Reference ref = new Reference();
        ref.setReference(path);
        operation.setDefinition(ref);
        return operation;
    }
}
