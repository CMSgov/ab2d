package gov.cms.ab2d.contracts.service;

import gov.cms.ab2d.contracts.processing.ReportProcessingException;
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
