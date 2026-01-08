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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Credits: most of the code in this class has been copied over from https://github.com/CMSgov/dpc-app
 */
@Configuration
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientConfiguration {

    @Value("${bfd.keystore.location:}")
    private String keystorePath;

    @Value("${bfd.keystore.base64:}")
    private String keystoreBase64;

    @Value("${bfd.keystore.certificate:}")
    private String trustCertificatePem;

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
     * Get http client allowed to connect to BFD domain only. The domain is limited by Mutual TLS cert verification.
     *
     * @return the HTTP client
     */
    @Bean
    public HttpClient bfdHttpClient() {

        try {
            char[] pass = keystorePassword.toCharArray();

            KeyStore keyStore = resolveClientKeyStore(pass);
            KeyStore trustStore = resolveTrustStoreOrFallback(pass, keyStore);

            return buildMutualTlsClient(keyStore, trustStore, pass);
        } catch (Exception e) {
            throw new BeanInstantiationException(
                    HttpClient.class, "Failed to initialize BFD mTLS HttpClient: " + e.getMessage(), e);
        }
    }


    private KeyStore resolveClientKeyStore(char[] password) throws Exception {
        if (hasText(keystoreBase64)) {
            return loadPkcs12FromBase64(keystoreBase64, password);
        }

        if (!hasText(keystorePath)) {
            throw new IllegalStateException("Neither bfd.keystore.base64 nor bfd.keystore.location is set");
        }

        File keyStoreFile = new File(keystorePath);
        if (!keyStoreFile.exists()) {
            URL resource = BFDClientConfiguration.class.getResource(keystorePath);
            if (resource != null) {
                try {
                    keyStoreFile = new File(resource.toURI());
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Invalid keystore resource URI for path: " + keystorePath, e);
                }
            }
        }

        if (!keyStoreFile.exists()) {
            throw new IllegalStateException("Keystore file does not exist at path: " + keystorePath);
        }

        return loadPkcs12FromFile(keyStoreFile, password);
    }

    /**
     * If PEM trust cert(s) are provided, build a trust store from them.
     * Otherwise, fall back to using the client keystore as trust material (old behavior).
     */
    private KeyStore resolveTrustStoreOrFallback(char[] password, KeyStore clientKeyStore) throws Exception {
        if (hasText(trustCertificatePem)) {
            return loadTrustStoreFromPem(trustCertificatePem);
        }
        // Backwards-compatible default: trust material comes from the same keystore
        return clientKeyStore;
    }

    private KeyStore loadPkcs12FromBase64(String base64Pkcs12, char[] password) throws Exception {
        // Secrets often contain newlines; remove whitespace safely
        String cleaned = base64Pkcs12.replaceAll("\\s+", "");
        byte[] pkcs12Bytes;
        try {
            pkcs12Bytes = Base64.getDecoder().decode(cleaned);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("bfd.keystore.base64 is not valid base64", e);
        }

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (ByteArrayInputStream in = new ByteArrayInputStream(pkcs12Bytes)) {
            ks.load(in, password);
        }
        return ks;
    }

    private KeyStore loadPkcs12FromFile(File keystoreFile, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(keystoreFile)) {
            ks.load(in, password);
        }
        return ks;
    }

    /**
     * Builds a KeyStore containing only trusted X.509 certificate parsed from PEM.
     */
    private KeyStore loadTrustStoreFromPem(String pem) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

        byte[] pemBytes = pem.getBytes(StandardCharsets.UTF_8);
        Certificate cert;
        try (ByteArrayInputStream in = new ByteArrayInputStream(pemBytes)) {
            cert = certFactory.generateCertificate(in);
        }

        if (cert == null) {
            throw new IllegalStateException("bfd.keystore.certificate did not contain a valid X.509 certificate");
        }

        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("bfd-trust-cert", cert);

        log.info("Loaded trusted certificate from bfd.keystore.certificate");
        return trustStore;
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the
     * Blue Button server using a client TLS certificate.
     */
    private HttpClient buildMutualTlsClient(KeyStore clientKeyStore, KeyStore trustStore, char[] keyStorePass) {
        final SSLContext sslContext;

        try {
            // BlueButton FHIR servers require a client cert; trust material may come from PEM or same keystore.
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(clientKeyStore, keyStorePass)
                    .loadTrustMaterial(trustStore, null)
                    .build();

        } catch (KeyManagementException | NoSuchAlgorithmException |
                 UnrecoverableKeyException | KeyStoreException ex) {
            log.error("Failed to build BFD mTLS SSLContext: {}", ex.getMessage(), ex);
            throw new BeanInstantiationException(KeyStore.class, ex.getMessage(), ex);
        }

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

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
