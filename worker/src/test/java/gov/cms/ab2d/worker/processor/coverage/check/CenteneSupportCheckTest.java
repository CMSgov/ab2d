package gov.cms.ab2d.worker.processor.coverage.check;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CenteneSupportCheckTest {

    @Test
    void test(){
        Assertions.assertTrue(CenteneSupportCheck.isCentene("S4802"));
        Assertions.assertTrue(CenteneSupportCheck.isCentene("Z1001"));
        Assertions.assertFalse(CenteneSupportCheck.isCentene("Z1000"));
    }
}
