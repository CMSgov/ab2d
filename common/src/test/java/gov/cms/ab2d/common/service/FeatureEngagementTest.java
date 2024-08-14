package gov.cms.ab2d.common.service;

import static org.junit.Assert.assertEquals;

import org.junit.jupiter.api.Test;

class FeatureEngagementTest {

  @Test
  void test1() {
    FeatureEngagement featureEngagement = FeatureEngagement.fromString("idle");
    assertEquals("NEUTRAL", featureEngagement.toString());
    assertEquals("idle", featureEngagement.getSerialValue());
  }

  @Test
  void test2() {
    FeatureEngagement featureEngagement = FeatureEngagement.fromString("random");
    assertEquals("IN_GEAR", featureEngagement.toString());
    assertEquals("engaged", featureEngagement.getSerialValue());
  }

}
