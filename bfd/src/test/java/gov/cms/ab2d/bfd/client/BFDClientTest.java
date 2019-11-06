package gov.cms.ab2d.bfd.client;


import ca.uhn.fhir.context.FhirContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hl7.fhir.dstu3.model.Bundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootApp.class)
public class BFDClientTest {

    @Autowired
    private BFDClient client;

    @Autowired
    private FhirContext fhirContext;

    private static final Log LOG = LogFactory.getLog(BFDClientImpl.class);

    @Test
    public void queryByPatient() {
        Bundle bundle = client.requestEOBFromServer("19990000002902");
        final var jsonParser = fhirContext.newJsonParser();
        bundle.getEntry().forEach((entry) -> {
            final var resource = entry.getResource();
            final String str = jsonParser.encodeResourceToString(resource);
            LOG.info(str);
        });
    }

}
