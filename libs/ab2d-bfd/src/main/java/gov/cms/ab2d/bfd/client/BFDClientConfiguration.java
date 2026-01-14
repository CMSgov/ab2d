package gov.cms.ab2d.bfd.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

/**
 * Credits: most of the code in this class has been copied over from https://github.com/CMSgov/dpc-app
 */
@Configuration
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientConfiguration {

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

    @Value("${bfd.http.maxConnPerRoute}")
    private int maxConnPerRoute;

    @Value("${bfd.http.maxConnTotal}")
    private int maxConnTotal;

    @Value("${bfd.http.connTTL}")
    private int connectionTTL;

    /**
     * Get http client alloweed to connect to BFD domain only. The domain is limited by Mutual TLS cert verification.
     * @return the HTTP client
     */
    @Bean
    public HttpClient bfdHttpClient() {

        try {
            File keyStoreFile = new File(keystorePath);
            if (!keyStoreFile.exists()) {
                URL resource = BFDClientConfiguration.class.getResource(keystorePath);
                if (resource != null) {
                    keyStoreFile = new File(resource.toURI());
                }
            }

            if (!keyStoreFile.exists()) {
                throw new BeanInstantiationException(HttpClient.class, "Keystore file does not exist");
            }

            return buildMutualTlsClient(keyStoreFile, keystorePassword.toCharArray());
        } catch (URISyntaxException fnf) {
            throw new BeanInstantiationException(HttpClient.class, "Keystore file does not exist");
        }
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the
     * Blue Button server using a client TLS certificate
     *
     * @param keystoreFile file containing key and trust material
     * @param keyStorePass password for keystore (default: "changeit")
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(File keystoreFile, char[] keyStorePass) {
        final SSLContext sslContext;

        try {
            // BlueButton FHIR servers have a self-signed cert and require a client cert
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keystoreFile, keyStorePass, keyStorePass)
                    .loadTrustMaterial(keystoreFile, keyStorePass)
                    .build();

        } catch (IOException | CertificateException | KeyManagementException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyStoreException ex) {
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
                .setMaxConnPerRoute(maxConnPerRoute)
                .setMaxConnTotal(maxConnTotal)
                .setConnectionTimeToLive(connectionTTL, TimeUnit.MILLISECONDS)
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .build();
    }

}
