package gov.cms.ab2d.properties.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import kong.unirest.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProperyServiceMockTest {
    class PropertiesClientImplMockEnv extends PropertiesClientImpl {
        @Override
        public String getFromEnvironment() {
            return "http://localhost:8065";
        }
    }

    class PropertiesClientImplMockConfig extends PropertiesClientImpl {
        @Override
        public String getConfigFileName() {
            return "does.not.exist";
        }
    }

    @Test
    void testIt() {
        PropertiesClientImpl impl = new PropertiesClientImpl();
        int port = 8060;
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();

        configureFor("localhost", port);

        List<Property> propertiesToReturn = List.of(new Property("a.key", "a.value"), new Property("b.key", "b.value"));
        JSONArray jsonArray = new JSONArray(propertiesToReturn);
        stubFor(get(urlEqualTo("/properties")).willReturn(aResponse().withBody(jsonArray.toString())));
        stubFor(get(urlEqualTo("/properties/a.key"))
                .willReturn(aResponse().withBody("{ \"key\": \"a.key\", \"value\": \"a.value\"}")));
        stubFor(post(urlEqualTo("/properties"))
                .willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        stubFor(get(urlEqualTo("/properties/one"))
                .willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        stubFor(get(urlEqualTo("/properties/bogus"))
                .willReturn(aResponse().withStatus(404).withBody("{ \"key\": \"null\", \"value\": \"null\"}")));
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("true")));

        List<Property> properties = impl.getAllProperties();
        assertEquals(2, properties.size());

        Property retProperty = impl.getProperty("a.key");
        assertEquals("a.key", retProperty.getKey());
        assertEquals("a.value", retProperty.getValue());

        Property newProp = impl.setProperty("one", "two");
        assertEquals("one", newProp.getKey());
        assertEquals("two", newProp.getValue());

        assertThrows(PropertyNotFoundException.class, () -> impl.getProperty("bogus"));

        impl.deleteProperty("one");
        Property p = new Property();
        p.setKey("key");
        p.setValue("value");
        assertEquals("key", p.getKey());
        assertEquals("value", p.getValue());

        wireMockServer.stop();
    }

    @Test
    void testMockEnv() {
        PropertiesClientImplMockEnv mock = new PropertiesClientImplMockEnv();
        assertEquals("http://localhost:8065", mock.getUrl());
    }

    @Test
    void testMockConfig() {
        PropertiesClientImplMockConfig mock = new PropertiesClientImplMockConfig();
        assertEquals("http://localhost:8060", mock.getUrl());
    }

    @Test
    void testErrors() {
        int port = 8065;
        PropertiesClientImpl impl = new PropertiesClientImpl("http://localhost:" + port);
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();

        configureFor("localhost", port);

        stubFor(get(urlEqualTo("/properties")).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlEqualTo("/properties/a.key")).willReturn(aResponse().withStatus(520)));
        stubFor(post(urlEqualTo("/properties")).willReturn((aResponse().withStatus(404))));
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("false")));

        assertThrows(PropertyNotFoundException.class, () -> impl.getAllProperties());

        assertThrows(PropertyNotFoundException.class, () -> impl.getProperty("a.key"));

        Property newProp = impl.setProperty("one", "two");
        assertNull(newProp);

        assertThrows(PropertyNotFoundException.class, () -> impl.deleteProperty("one"));

        wireMockServer.stop();
    }

    @Test
    void testDeleteCases() {
        int port = 8066;
        PropertiesClientImpl impl = new PropertiesClientImpl("http://localhost:" + port);
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();

        configureFor("localhost", port);

        // test running delete with false
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("false")));
        assertThrows(PropertyNotFoundException.class, () -> impl.deleteProperty("one"));

        // test running delete with true
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("true")));
        assertDoesNotThrow(() -> impl.deleteProperty("one"));

        // test running delete with null (presumably)
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse()));
        assertThrows(PropertyNotFoundException.class, () -> impl.deleteProperty("one"));

        wireMockServer.stop();
    }

    @Test
    void testErrorsWithoutMock() {
        PropertiesClientImpl impl = new PropertiesClientImpl();
        assertThrows(PropertyNotFoundException.class, () -> impl.getAllProperties());
        assertThrows(PropertyNotFoundException.class, () -> impl.getProperty("a.key"));
        assertThrows(PropertyNotFoundException.class, () -> impl.setProperty("one", "two"));
        assertThrows(PropertyNotFoundException.class, () -> impl.deleteProperty("one"));
    }

}
