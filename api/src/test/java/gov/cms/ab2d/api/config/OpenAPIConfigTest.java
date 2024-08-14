package gov.cms.ab2d.api.config;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OpenAPIConfigTest {

  @Test
  void testDefaultResponseMessages() {
    assertDoesNotThrow(() -> {
      OpenAPIConfig openAPIConfig = new OpenAPIConfig();
      openAPIConfig.defaultResponseMessages();
    });
  }

  @Test
  void testDetails() throws Exception {
    OpenAPIConfig.Details details = new OpenAPIConfig.Details();
    details.setText("text");
    details.setAdditionalProperty("key", "value");

    Map<String,String> map = new HashMap<String,String>() {{
      put("key", "value");
    }};

    ObjectWriter ow = new ObjectMapper().writer();
    String json = ow.writeValueAsString(details);

    assertEquals(OpenAPIConfig.Details.class, details.getClass());
    assertEquals("text", details.getText());
    assertEquals(map, details.getAdditionalProperties());
    assertEquals("{\"text\":\"text\",\"key\":\"value\"}", json);
  }

  @Test
  void testIssue() throws Exception {
    OpenAPIConfig.Details details = new OpenAPIConfig.Details();
    details.setText("text");

    OpenAPIConfig.Issue issue = new OpenAPIConfig.Issue();
    issue.setCode("code");
    issue.setSeverity("severity");
    issue.setAdditionalProperty("key", "value");
    issue.setDetails(details);

    Map<String,String> map = new HashMap<String,String>() {{
      put("key", "value");
    }};

    ObjectWriter ow = new ObjectMapper().writer();
    String json = ow.writeValueAsString(issue);

    assertEquals(OpenAPIConfig.Issue.class, issue.getClass());
    assertEquals("code", issue.getCode());
    assertEquals("severity", issue.getSeverity());
    assertEquals(details, issue.getDetails());
    assertEquals(map, issue.getAdditionalProperties());
    assertEquals("{\"severity\":\"severity\",\"code\":\"code\",\"details\":{\"text\":\"text\"},\"key\":\"value\"}", json);
  }

  @Test
  void testOperationOutcome() throws Exception {
    OpenAPIConfig.Details details = new OpenAPIConfig.Details();
    details.setText("text");

    OpenAPIConfig.Issue issue = new OpenAPIConfig.Issue();
    issue.setCode("code");
    issue.setSeverity("severity");
    issue.setDetails(details);

    OpenAPIConfig openAPIConfig = new OpenAPIConfig();
    OpenAPIConfig.OperationOutcome operationOutcome = openAPIConfig.new OperationOutcome();
    operationOutcome.setResourceType("resourceType");
    operationOutcome.setId("id");
    operationOutcome.setIssue(List.of(issue));
    operationOutcome.setAdditionalProperty("key", "value");

    Map<String,String> map = new HashMap<String,String>() {{
      put("key", "value");
    }};

    ObjectWriter ow = new ObjectMapper().writer();
    String json = ow.writeValueAsString(operationOutcome);

    assertEquals(OpenAPIConfig.OperationOutcome.class, operationOutcome.getClass());
    assertEquals("resourceType", operationOutcome.getResourceType());
    assertEquals("id", operationOutcome.getId());
    assertEquals(List.of(issue), operationOutcome.getIssue());
    assertEquals(map, operationOutcome.getAdditionalProperties());
    assertEquals("{\"resourceType\":\"resourceType\",\"id\":\"id\",\"issue\":[{\"severity\":\"severity\",\"code\":\"code\",\"details\":{\"text\":\"text\"}}],\"key\":\"value\"}", json);
  }
}
