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
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Configuration
@PropertySource("classpath:application.bfd.properties")
@Slf4j
public class BFDClientConfiguration {

    @Value("${bfd.keystore.location}")
    private String keystorePath;

    @Value("${bfd.keystore.base64:}")
    private String keystoreBase64;

    @Value("${bfd.truststore.cert:}")
    private String trustStoreCertPem;

    @Value("${bfd.v3.truststore.cert:}")
    private String trustStoreCertPemV3;

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
            char[] pass = keystorePassword.toCharArray();

            KeyStore clientKeyStore = resolveClientKeyStore(pass);

            KeyStore trustStore = resolveTrustStoreCombined();

            SSLContext sslContext = SSLContexts.custom()
                    .loadKeyMaterial(clientKeyStore, pass)
                    .loadTrustMaterial(trustStore, null)
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
            throw new BeanInstantiationException(HttpClient.class,
                    "Failed to create BFD HttpClient: " + e.getMessage(), e);
        }
    }

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

    private KeyStore resolveTrustStoreCombined() throws Exception {
        log.info("Loading BFD truststore (combined)");

        boolean hasV1V2 = hasText(trustStoreCertPem);
        boolean hasV3 = hasText(trustStoreCertPemV3);

        if (!hasV1V2 && !hasV3) {
            return resolveClientKeyStore(keystorePassword.toCharArray());
        }

        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(null, null);

        if (hasV1V2) {
            addAllCertificatesFromPem(ts, trustStoreCertPem, "bfd-default");
        }

        if (hasV3) {
            addAllCertificatesFromPem(ts, trustStoreCertPemV3, "bfd-v3");
        }

        return ts;
    }

    private void addAllCertificatesFromPem(KeyStore ts, String pem, String aliasPrefix) throws Exception {
        if (!hasText(pem)) {
            return;
        }

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        try (InputStream in = new ByteArrayInputStream(pem.getBytes())) {
            int i = 0;
            for (Certificate cert : cf.generateCertificates(in)) {
                String alias = aliasPrefix + "-" + i++;
                ts.setCertificateEntry(alias, cert);

                if (cert instanceof java.security.cert.X509Certificate x509) {
                    logCertInfo(x509);
                } else {
                    log.warn("Loaded trust cert alias={} is not X509: {}", alias, cert.getType());
                }
            }

            if (i == 0) {
                Certificate cert = parseX509FromPem(pem);
                String alias = aliasPrefix + "-0";
                ts.setCertificateEntry(alias, cert);
                if (cert instanceof java.security.cert.X509Certificate x509) {
                    logCertInfo(x509);
                }
            }
        }
    }

    private Certificate parseX509FromPem(String pem) throws CertificateException {
        String normalized = pem
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");

        byte[] der = Base64.getDecoder().decode(normalized);

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream in = new ByteArrayInputStream(der)) {
            return cf.generateCertificate(in);
        } catch (IOException e) {
            throw new CertificateException("Failed reading certificate bytes", e);
        }
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

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void logCertInfo(java.security.cert.X509Certificate x) throws Exception {
        byte[] der = x.getEncoded();
        var md = java.security.MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(der);

        String fp = java.util.HexFormat.of().formatHex(dig);
        log.warn("BFD trust cert loaded. Subject='{}' Issuer='{}' SHA256={}",
                x.getSubjectX500Principal(), x.getIssuerX500Principal(), fp);
    }

}
