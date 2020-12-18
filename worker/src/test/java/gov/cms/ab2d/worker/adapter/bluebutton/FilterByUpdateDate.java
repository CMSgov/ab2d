package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.common.util.FilterOutByDate;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static gov.cms.ab2d.worker.processor.eob.BundleUtils.createIdentifierWithoutMbi;
import static org.junit.jupiter.api.Assertions.*;

// Because it has an outside dependency, ignore it but wanted to actually test that it works
@Disabled
@SpringBootTest
@Testcontainers
class FilterByUpdateDate {
    @Autowired
    private BFDClient bfdClient;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    void getAll() {
        OffsetDateTime earliest = OffsetDateTime.parse(Constants.SINCE_EARLIEST_DATE);

        String patientId = "-19990000002901";
        ContractBeneficiaries.PatientDTO patient = new ContractBeneficiaries.PatientDTO();
        patient.setIdentifiers(createIdentifierWithoutMbi(patientId));

        FilterOutByDate.DateRange d1 = FilterOutByDate.getDateRange(1, 2020, 12, 2020);
        patient.setDateRangesUnderContract(Collections.singletonList(d1));
        Bundle resourcesAll = bfdClient.requestEOBFromServer(patientId);

        assertNotNull(resourcesAll);
        assertEquals(26, resourcesAll.getEntry().size());

        List<Bundle.BundleEntryComponent> manuallyFiltered = resourcesAll.getEntry().stream()
                .filter(c -> updatedTimeAfter(c, earliest))
                .collect(Collectors.toList());
        assertEquals(4, manuallyFiltered.size());

        Bundle resourcesAfter = bfdClient.requestEOBFromServer(patientId, earliest);
        assertNotNull(resourcesAfter);
        assertEquals(4, resourcesAfter.getEntry().size());

        List<String> listOne = manuallyFiltered.stream().map(c -> c.getResource().getId()).collect(Collectors.toList());
        List<String> listTwo = resourcesAfter.getEntry().stream().map(c -> c.getResource().getId()).collect(Collectors.toList());
        Collections.sort(listOne);
        Collections.sort(listTwo);
        for (int i = 0; i < listOne.size(); i++) {
            assertEquals(listOne.get(i), listTwo.get(i));
        }
    }

    private boolean updatedTimeAfter(Bundle.BundleEntryComponent component, OffsetDateTime earliest) {
        if (component == null || component.getResource() == null || component.getResource().getMeta() == null
                || component.getResource().getMeta().getLastUpdated() == null) {
            return false;
        }
        return component.getResource().getMeta().getLastUpdated().getTime() >= earliest.toInstant().toEpochMilli();
    }
}
