package gov.cms.ab2d.lambdalibs;

import gov.cms.ab2d.lambdalibs.lib.PropertiesUtil;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PropLoadTest {
    @Test
    void loadProps() {
        Properties props = PropertiesUtil.loadProps();
        assertNotNull(props.getProperty("test.prop"));
    }

    @Test
    void overrideProps() {
        System.setProperty("test.prop.override", "test");
        Properties props = PropertiesUtil.loadProps();
        assertEquals("test", props.getProperty("test.prop.override"));
    }


}

