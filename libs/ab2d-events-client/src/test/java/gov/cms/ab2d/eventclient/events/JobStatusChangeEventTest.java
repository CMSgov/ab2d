package gov.cms.ab2d.eventclient.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertTrue;

class JobStatusChangeEventTest {


    @DisplayName("Label status change event correctly")
    @Test
    void labelSplitFromDescription() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "IMPORTANT status");

        assertTrue(event.asMessage().startsWith("IMPORTANT"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    void labelSplitIgnoresBlankStrings() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "    ");

        assertTrue(event.asMessage().startsWith(" (test)"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    void labelSplitIgnoresNullStrings() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", null);

        assertTrue(event.asMessage().startsWith(" (test)"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    void labelSplitHandlesOneWord() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "ONEWORD");

        assertTrue(event.asMessage().startsWith(" (test)"));
    }
}
