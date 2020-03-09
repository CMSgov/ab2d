package gov.cms.ab2d.worker.adapter.bluebutton;

import gov.cms.ab2d.bfd.client.BFDClient;
import gov.cms.ab2d.common.util.AB2DPostgresqlContainer;
import gov.cms.ab2d.common.util.Constants;
import gov.cms.ab2d.filter.FilterOutByDate;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.text.ParseException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

// Because it has an outside dependency, ignore it but wanted to actually test that it works
@Ignore
@SpringBootTest
@Testcontainers
public class FilterByUpdateDate {
    @Autowired
    private BFDClient bfdClient;

    @Container
    private static final PostgreSQLContainer postgreSQLContainer= new AB2DPostgresqlContainer();

    @Test
    void getAll() throws ParseException {
        OffsetDateTime earliest = OffsetDateTime.parse(Constants.SINCE_EARLIEST_DATE);
        String patientId = "-19990000002901";
        GetPatientsByContractResponse.PatientDTO patient = new GetPatientsByContractResponse.PatientDTO();
        patient.setPatientId(patientId);
        FilterOutByDate.DateRange d1 = new FilterOutByDate.DateRange(new Date(0), new Date());
        patient.setDateRangesUnderContract(Arrays.asList(d1));
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
        System.out.println(component.getResource().getMeta().getLastUpdated());
        return component.getResource().getMeta().getLastUpdated().getTime() >= earliest.toInstant().toEpochMilli();
    }
}
