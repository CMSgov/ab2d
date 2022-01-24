package gov.cms.ab2d.hpms.service;

import gov.cms.ab2d.hpms.processing.ReportProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ReportProcessingTest {
    @Test
    void instance() {
        Assertions.assertThrows(ReportProcessingException.class, () -> {
            throw new ReportProcessingException("test");
        });

    }
}
