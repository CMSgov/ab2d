package gov.cms.ab2d.api.controller;

import java.time.LocalDate;
import java.util.List;

class CapabilityStatement {

    private final String resourceType = "CapabilityStatement";

    private final String status = "active";

    private final LocalDate date = LocalDate.now();

    private final String publisher = "Centers for Medicare & Medicaid Services";

    private final String kind = "capability";

    private final class Software {

        private final String name = "AB2D";

        private final String version = "latest";

        private final LocalDate releaseDate = LocalDate.now();
    }

    private final class Implementation {

        private final String url = "https://sandbox.ab2d.cms.gov";
    }

    private final String fhirVersion = "4.0.1";

    private final String acceptUnknown = "extensions";

    private final List<String> format = List.of("application/json", "application/fhir+json");

    private final List<Object> rest = List.of("");
}
