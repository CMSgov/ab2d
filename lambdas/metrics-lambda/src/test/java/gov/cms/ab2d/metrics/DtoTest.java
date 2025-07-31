package gov.cms.ab2d.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DtoTest {
    @Test
    void dimensionsAllArgTest() {
        Dimensions dimensions = new Dimensions("a", "a");
        assertEquals("a", dimensions.getName());
        assertEquals("a", dimensions.getValue());
    }

    @Test
    void dimensionsTest() {
        Dimensions dimensions = new Dimensions();
        dimensions.setName("a");
        dimensions.setValue("a");
        assertEquals("a", dimensions.getName());
        assertEquals("a", dimensions.getValue());
    }

    @Test
    void metricAlarmTest() {
        Trigger trigger = new Trigger();
        MetricAlarm metricAlarm = new MetricAlarm();

        metricAlarm.setAlarmName("");
        metricAlarm.setAlarmDescription("");
        metricAlarm.setAwsAccountId("");
        metricAlarm.setAlarmConfigurationUpdatedTimestamp("");
        metricAlarm.setNewStateValue("");
        metricAlarm.setNewStateReason("");
        metricAlarm.setStateChangeTime("");
        metricAlarm.setRegion("");
        metricAlarm.setAlarmArn("");
        metricAlarm.setOldStateValue("");
        metricAlarm.setAlarmActions(new ArrayList<>());
        metricAlarm.setOkActions(new ArrayList<>());
        metricAlarm.setInsufficientDataActions(new ArrayList<>());
        metricAlarm.setTrigger(trigger);

        assertEquals("", metricAlarm.getAlarmName());
        assertEquals("", metricAlarm.getAlarmDescription());
        assertEquals("", metricAlarm.getAwsAccountId());
        assertEquals("", metricAlarm.getAlarmConfigurationUpdatedTimestamp());
        assertEquals("", metricAlarm.getNewStateValue());
        assertEquals("", metricAlarm.getNewStateReason());
        assertEquals("", metricAlarm.getStateChangeTime());
        assertEquals("", metricAlarm.getRegion());
        assertEquals("", metricAlarm.getAlarmArn());
        assertEquals("", metricAlarm.getOldStateValue());
        assertEquals(new ArrayList<>(), metricAlarm.getAlarmActions());
        assertEquals(new ArrayList<>(), metricAlarm.getOkActions());
        assertEquals(new ArrayList<>(), metricAlarm.getInsufficientDataActions());
        assertEquals(trigger, metricAlarm.getTrigger());
    }

    @Test
    void triggerTest() {
        Dimensions[] dimensions = new Dimensions[]{};
        Trigger trigger = new Trigger();

        trigger.setDimensions(dimensions);
        trigger.setMetricName("test");
        trigger.setStatistic("test");
        trigger.setStatisticType("test");
        trigger.setUnit("test");
        trigger.setPeriod(1);
        trigger.setEvaluationPeriods("test");
        trigger.setComparisonOperator("test");
        trigger.setThreshold(1);
        trigger.setTreatMissingData("test");
        trigger.setEvaluateLowSampleCountPercentile("test");
        trigger.setNamespace("test");
        trigger.setComparisonOperator("test");

        assertEquals(dimensions, trigger.getDimensions());
        assertEquals("test", trigger.getMetricName());
        assertEquals("test", trigger.getStatistic());
        assertEquals("test", trigger.getStatisticType());
        assertEquals("test", trigger.getUnit());
        assertEquals(1, trigger.getPeriod());
        assertEquals("test", trigger.getEvaluationPeriods());
        assertEquals("test", trigger.getComparisonOperator());
        assertEquals(1, trigger.getThreshold());
        assertEquals("test", trigger.getTreatMissingData());
        assertEquals("test", trigger.getEvaluateLowSampleCountPercentile());
        assertEquals("test", trigger.getNamespace());
        assertEquals("test", trigger.getComparisonOperator());
    }

    @Test
    void eventDataException() {
        assertThrows(EventDataException.class, () -> {
            throw new EventDataException("test");
        });
    }

}
