package gov.cms.ab2d.api.controller;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static gov.cms.ab2d.api.util.Constants.BASE_SANDBOX_URL;
import static gov.cms.ab2d.api.util.Constants.FHIR_SANDBOX_URL;

// These are being transformed to JSON so it looks like they aren't being used, but they do get translated
@SuppressWarnings("PMD.UnusedPrivateField")
class CapabilityStatement {

    private final String resourceType = "CapabilityStatement";

    private final String status = "active";

    private final String date = LocalDate.now().toString();

    private final String publisher = "Centers for Medicare &amp; Medicaid Services";

    private final String kind = "capability";

    private final Software software = new Software();

    private final Implementation implementation = new Implementation();

    private final String fhirVersion = "4.0.1";

    private final String acceptUnknown = "extensions";

    private final List<String> format = List.of("application/json", "application/fhir+json");

    private final List<Rest> rest = List.of(new Rest());

    private final class Software {

        private final String name = "AB2D";

        private final String version = "latest";

        private final String releaseDate = LocalDate.now().toString(); //TODO revisit this when our release schedule is figured out
    }

    private final class Implementation {

        private final String url = BASE_SANDBOX_URL;
    }

    private final class Rest {

        private final String mode = "server";

        private final Security security = new Security();

        private final List<Interaction> interaction = new ArrayList<>();

        private final List<Operation> operation = new ArrayList<>();

        Rest() {
            Operation exportOperation = new Operation("export", new Definition(FHIR_SANDBOX_URL + "/Patient/$export"));
            operation.add(exportOperation);
            Operation exportContractOperation = new Operation("export by contract", new Definition(FHIR_SANDBOX_URL + "Group/{contractNumber}/$export"));
            operation.add(exportContractOperation);
            Operation cancelOperation = new Operation("cancel", new Definition(FHIR_SANDBOX_URL + "/Job/{jobUuid}/$status"));
            operation.add(cancelOperation);
            Operation statusOperation = new Operation("status", new Definition(FHIR_SANDBOX_URL + "/Job/{jobUuid}/$status"));
            operation.add(statusOperation);
            Operation downloadOperation = new Operation("download", new Definition(FHIR_SANDBOX_URL + "/Job/{jobUuid}/file/{filename}"));
            operation.add(downloadOperation);
            Operation capabilityOperation = new Operation("capability", new Definition(FHIR_SANDBOX_URL + "/metadata"));
            operation.add(capabilityOperation);

            Interaction interactionRead = new Interaction("read");
            interaction.add(interactionRead);
            Interaction interactionCreate = new Interaction("create");
            interaction.add(interactionCreate);
            Interaction interactionDelete = new Interaction("delete");
            interaction.add(interactionDelete);
        }
    }

    @RequiredArgsConstructor
    private final class Security {

        private final String cors = "true";

        private final Service service = new Service("OAuth");
    }

    private final class Service {

        private final List<Coding> coding = new ArrayList<>();

        private final String text;

        Service(String text) {
            Coding codingObj = new Coding("http://hl7.org/fhir/ValueSet/restful-security-service", "OAuth", "OAuth");
            coding.add(codingObj);

            this.text = text;
        }
    }

    @RequiredArgsConstructor
    private final class Coding {

        private final String system;

        private final String code;

        private final String display;
    }

    @RequiredArgsConstructor
    private final class Operation {

        private final String name;

        private final Definition definition;
    }

    @RequiredArgsConstructor
    private final class Interaction {

        private final String code;
    }

    @RequiredArgsConstructor
    private final class Definition {

        private final String reference;
    }
}
