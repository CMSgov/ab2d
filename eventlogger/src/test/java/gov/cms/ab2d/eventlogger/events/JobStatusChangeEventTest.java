package gov.cms.ab2d.eventlogger.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class JobStatusChangeEventTest {


    @DisplayName("Label status change event correctly")
    @Test
    public void labelSplitFromDescription() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "IMPORTANT status");

        assertTrue(event.asMessage().startsWith("IMPORTANT"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    public void labelSplitIgnoresBlankStrings() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "    ");

        assertTrue(event.asMessage().startsWith(" (test)"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    public void labelSplitIgnoresNullStrings() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "    ");

        assertTrue(event.asMessage().startsWith(" (test)"));
    }

    @DisplayName("Label status change event correctly")
    @Test
    public void labelSplitHandlesOneWord() {

        JobStatusChangeEvent event = new JobStatusChangeEvent("test", "test", "test", "test", "ONEWORD");

        assertTrue(event.asMessage().startsWith(" (test)"));
    }
}
