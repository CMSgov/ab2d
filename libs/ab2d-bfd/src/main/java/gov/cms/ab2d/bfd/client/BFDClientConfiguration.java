package gov.cms.ab2d.bfd.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * Credits: most of the code in this class has been copied over from https://github.com/CMSgov/dpc-app
 */
@Configuration
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientConfiguration {

    /**
     * Backward-compatible file-based keystore location (old approach).
     * Still supported as a fallback.
     */
    @Value("${bfd.keystore.location:}")
    private String keystorePath;

    /**
     * New approach: base64-encoded PKCS12 (PFX) keystore content injected via ECS secrets:
     * AB2D_BFD_KEYSTORE_BASE64 from SSM param /ab2d/${env}/worker/sensitive/bfd_keystore_base64
     */
    @Value("${bfd.keystore.base64:}")
    private String keystoreBase64;

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

    @Bean
    public HttpClient bfdHttpClient() {
        try {
        //    char[] pass = keystorePassword.toCharArray();
            char[] pass = effectivePassword();
            KeyStore clientKeyStore = resolveClientKeyStore(pass);

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(clientKeyStore, pass)
                    .loadTrustMaterial(null, (TrustStrategy) null)
                    .build();

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

        } catch (Exception e) {
            log.error("Failed to build BFD mTLS HttpClient", e);
            throw new BeanInstantiationException(HttpClient.class, "Failed to create BFD HttpClient: " + e.getMessage(), e);
        }
    }

    /**
     * Prefer base64 PKCS12 keystore if present; otherwise fall back to file location.
     */
    private KeyStore resolveClientKeyStore(char[] password) throws Exception {
        if (hasText(keystoreBase64)) {
            log.info("Loading BFD client keystore from bfd.keystore.base64 (env/SSM injected)");
            return loadPkcs12FromBase64(keystoreBase64, password);
        }

        if (!hasText(keystorePath)) {
            throw new IllegalStateException("Neither bfd.keystore.base64 nor bfd.keystore.location is set");
        }

        File keyStoreFile = resolveFile(keystorePath);
        log.info("Loading BFD client keystore from file path: {}", keyStoreFile.getAbsolutePath());
        return loadPkcs12FromFile(keyStoreFile, password);
    }

    private File resolveFile(String path) throws URISyntaxException {
        File keyStoreFile = new File(path);
        if (keyStoreFile.exists()) {
            return keyStoreFile;
        }

        URL resource = BFDClientConfiguration.class.getResource(path);
        if (resource != null) {
            File fromResource = new File(resource.toURI());
            if (fromResource.exists()) {
                return fromResource;
            }
        }

        throw new IllegalStateException("Keystore file does not exist at path: " + path);
    }

    private KeyStore loadPkcs12FromBase64(String base64, char[] password) throws Exception {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("bfd.keystore.base64 is not valid base64", e);
        }

        try (InputStream in = new ByteArrayInputStream(decoded)) {
            return loadPkcs12FromStream(in, password);
        }
    }

    private KeyStore loadPkcs12FromFile(File file, char[] password) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            return loadPkcs12FromStream(in, password);
        }
    }

    private KeyStore loadPkcs12FromStream(InputStream in, char[] password)
            throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(in, password);
        return ks;
    }

    private char[] effectivePassword() {
        // Blank or missing means "no password"
        if (keystorePassword == null || keystorePassword.trim().isEmpty()) {
            return new char[0];
        }
        return keystorePassword.toCharArray();
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
