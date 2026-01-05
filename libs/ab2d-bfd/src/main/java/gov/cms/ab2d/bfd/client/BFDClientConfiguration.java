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
import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStoreException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.UnrecoverableKeyException;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
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

    @Value("${bfd.keystore.private_key}")
    private String env_private_key;

    @Value("${bfd.keystore.certificate}")
    private String env_certificate;

    @Value("${bfd.keystore.password}")
    private String keystorePassword;

    private String keystoreAlias = "AB2D_BFD_KEYSTORE";

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
     * Creates a KeyStore from private key and certificate stored in the ecs container secrets.
     *
     * @param privateKeyParam ENV parameter containing the private key
     * @param certificateParam ENV parameter name containing the certificate
     * @param keystorePassword Password to protect the keystore
     * @param keyAlias Alias for the key entry in the keystore
     * @return Configured KeyStore object
     * @throws Exception if keystore creation fails
     */
    public static KeyStore createKeyStore(
            String privateKeyParam,
            String certificateParam,
            String keystorePassword,
            String keyAlias) throws Exception {

            // Retrieve private key from container environment
            String privateKeyData = System.getenv(privateKeyParam);
            PrivateKey privateKey = parsePrivateKey(privateKeyData);

            // Retrieve certificate from container environment
            String certificateData = System.getenv(certificateParam);
            Certificate certificate = parseCertificate(certificateData);

            // Create KeyStore
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // Initialize empty keystore

            // Add private key and certificate chain to keystore
            Certificate[] certChain = new Certificate[]{certificate};
            keyStore.setKeyEntry(
                    keyAlias,
                    privateKey,
                    keystorePassword.toCharArray(),
                    certChain
            );

            return keyStore;
    }

    /**
     * Parses a private key from PEM or PKCS12 format.
     * Supports RSA, EC, and other key types.
     */
    private static PrivateKey parsePrivateKey(String keyData) throws Exception {
        // Remove PEM headers and whitespace
        String cleanedData = keyData
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN RSA PRIVATE KEY-----", "")
                .replaceAll("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("-----BEGIN EC PRIVATE KEY-----", "")
                .replaceAll("-----END EC PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanedData);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        // Try different key algorithms
        String[] algorithms = {"RSA", "EC", "DSA"};
        Exception lastException = null;

        for (String algorithm : algorithms) {
            try {
                KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception e) {
                lastException = e;
            }
        }

        throw new Exception("Unable to parse private key with any supported algorithm", lastException);
    }

    /**
     * Parses a certificate from PEM format.
     */
    private static Certificate parseCertificate(String certificateData) throws Exception {
        String cleanedData = certificateData
                .replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");

        byte[] certBytes = Base64.getDecoder().decode(cleanedData);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        ByteArrayInputStream inputStream = new ByteArrayInputStream(certBytes);

        return certFactory.generateCertificate(inputStream);
    }

    @Bean
    public HttpClient bfdHttpClient() {
        try {
            KeyStore keyStore = createKeyStore(
                    this.env_private_key,
                    this.env_certificate,
                    this.keystorePassword,
                    this.keystoreAlias
            );

            if (!keyStoreFile.exists()) {
                throw new BeanInstantiationException(HttpClient.class, "Keystore file does not exist");
            }

            return buildMutualTlsClient(keyStore, keystorePassword.toCharArray());
        } catch (URISyntaxException fnf) {
            throw new BeanInstantiationException(HttpClient.class, "Keystore does not exist");
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
    private HttpClient buildMutualTlsClient(KeyStore keyStore, char[] keyStorePass) {
         SSLContext sslContext = SSLContext.getInstance("TLS");

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
