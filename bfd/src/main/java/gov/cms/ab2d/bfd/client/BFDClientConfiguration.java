package gov.cms.ab2d.bfd.client;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.util.Assert;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;

@Configuration
@PropertySource("classpath:application.bfd.properties")
@Slf4j
/**
 * Credits: most of the code in this class has been copied over from https://github
 * .com/CMSgov/dpc-app
 */
public class BFDClientConfiguration {

    public static final String JKS = "JKS";

    @Value("${bfd.keystore.location}")
    private String keystorePath;

    @Value("${bfd.keystore.password}")
    private String keystorePassword;

    @Value("${bfd.connectionTimeout}")
    private int connectionTimeout;

    @Value("${bfd.socketTimeout}")
    private int socketTimeout;

    @Value("${bfd.requestTimeout}")
    private int requestTimeout;

    @Value("${bfd.serverBaseUrl}")
    private String serverBaseUrl;

    @Bean
    public IGenericClient bfdFhirRestClient(FhirContext fhirContext, HttpClient httpClient) {
        fhirContext.getRestfulClientFactory().setHttpClient(httpClient);

        return fhirContext.newRestfulGenericClient(serverBaseUrl);
    }

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forDstu3();
    }

    @Bean
    public KeyStore bfdKeyStore() {
        // Will need to support key stores that could be both in classpath AND in an external file
        try (InputStream keyStoreStream = this.getClass().getResourceAsStream(keystorePath)) {
            Assert.notNull(keyStoreStream, "Could not find and load the keystore");
            KeyStore keyStore = KeyStore.getInstance(JKS);

            // keyStore.load will NOT throw an exception in case of a null keystore stream :shrug:
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            log.error(ex.getMessage(), ex);
            throw new BeanInstantiationException(KeyStore.class, ex.getMessage());
        }
    }

    /**
     * Borrowed from https://github.com/CMSgov/dpc-app
     *
     * @param keyStore
     * @return
     */
    @Bean
    public HttpClient bfdHttpClient(KeyStore keyStore) {
        return buildMutualTlsClient(keyStore, keystorePassword.toCharArray());
    }


    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the
     * Blue Button server using a client TLS certificate
     * Borrowed from https://github.com/CMSgov/dpc-app.
     *
     * @param keyStore     {@link KeyStore} containing, at a minimum, the client tls certificate
     *                     and private key
     * @param keyStorePass password for keystore (default: "changeit")
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(KeyStore keyStore, char[] keyStorePass) {
        final SSLContext sslContext;

        try {
            // BlueButton FHIR servers have a self-signed cert and require a client cert
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keyStorePass)
                    .loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
            log.error(ex.getMessage());
            throw new BeanInstantiationException(KeyStore.class, ex.getMessage());
        }

        // Configure the socket timeout for the connection, incl. ssl tunneling
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(connectionTimeout)
                .setConnectionRequestTimeout(requestTimeout)
                .setSocketTimeout(socketTimeout)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .build();
    }

}
