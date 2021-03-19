package gov.cms.ab2d.bfd.client;

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

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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

    public static final String JKS = "JKS";

    private final String keystorePath;
    private final String keystorePassword;
    private final int connectionTimeout;
    private final int socketTimeout;
    private final int requestTimeout;
    private final int maxConnPerRoute;
    private final int maxConnTotal;
    private final int connectionTTL;

    @SuppressWarnings("checkstyle:parameternumber")
    public BFDClientConfiguration(@Value("${bfd.keystore.location}") String keystorePath,           // NO PMD
                                  @Value("${bfd.keystore.password}") String keystorePassword,
                                  @Value("${bfd.connectionTimeout}") int connectionTimeout,
                                  @Value("${bfd.socketTimeout}") int socketTimeout,
                                  @Value("${bfd.requestTimeout}") int requestTimeout,
                                  @Value("${bfd.http.maxConnPerRoute}") int maxConnPerRoute,
                                  @Value("${bfd.http.maxConnTotal}") int maxConnTotal,
                                  @Value("${bfd.http.connTTL}") int connectionTTL) {
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.connectionTimeout = connectionTimeout;
        this.socketTimeout = socketTimeout;
        this.requestTimeout = requestTimeout;
        this.maxConnPerRoute = maxConnPerRoute;
        this.maxConnTotal = maxConnTotal;
        this.connectionTTL = connectionTTL;
    }

    @Bean
    public KeyStore bfdKeyStore() {
        // First try to load the keystore from the classpath, if that doesn't work, try the filesystem
        try (InputStream keyStoreStream = getKeyStoreStream()) {
            KeyStore keyStore = KeyStore.getInstance(JKS);

            // keyStore.load will NOT throw an exception in case of a null keystore stream :shrug:
            keyStore.load(keyStoreStream, keystorePassword.toCharArray());
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException ex) {
            log.error(ex.getMessage(), ex);
            throw new BeanInstantiationException(KeyStore.class, ex.getMessage());
        }
    }

    private InputStream getKeyStoreStream() throws IOException {
        InputStream keyStoreStream = this.getClass().getResourceAsStream(keystorePath);
        return keyStoreStream != null ? keyStoreStream : new FileInputStream(keystorePath);
    }

    /**
     * Borrowed from https://github.com/CMSgov/dpc-app
     *
     * @param keyStore - the keystore
     * @return the HTTP client
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
                .setMaxConnPerRoute(maxConnPerRoute)
                .setMaxConnTotal(maxConnTotal)
                .setConnectionTimeToLive(connectionTTL, TimeUnit.MILLISECONDS)
                .setDefaultRequestConfig(requestConfig)
                .setSSLContext(sslContext)
                .build();
    }

}
