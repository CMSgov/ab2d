package gov.cms.ab2d.bfd.client;

import java.io.IOException;
import java.time.OffsetDateTime;

public interface BFDSearch {

    String searchEOB(String patientId, OffsetDateTime since) throws IOException, InterruptedException;
}
