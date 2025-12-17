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

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;

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
            ssmClient = getSSMClient();

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


    private static SsmClient getSSMClient() {
        return  SsmClient.builder()
                .region(Region.US_EAST_1)
                .build();
    }

    private static String getValueFromParameterStore(String key, SsmClient ssmClient) {
        var parameterRequest = GetParameterRequest.builder()
                .name(key)
                .withDecryption(true)
                .build();

        var parameterResponse = ssmClient.getParameter(parameterRequest);
        return parameterResponse.parameter().value();
    }


    /**
     * Get http client alloweed to connect to BFD domain only. The domain is limited by Mutual TLS cert verification.
     * Fetch mTLS material from ssm.
     * @return the HTTP client
     */
    @Bean
    public HttpClient bfdHttpClient() {

        KeyManagerFactory keyManagerFactory = getKeyManagerFactory();

        TrustManager[] acceptAllTrustManager = getTrustManager();

        try {

            return buildMutualTlsClient(keyManagerFactory, acceptAllTrustManager);

        } catch (URISyntaxException fnf) {
            throw new BeanInstantiationException(HttpClient.class, "Keystore does not exist");
        }
    }

    private static KeyManagerFactory getKeyManagerFactory() {
        // Build SSL Context
        String privateKeyPath = "/etc/crts/client.key.pkcs8";
        String publicKeyPath = "/etc/crts/client.crt";

        final byte[] publicData = Files.readAllBytes(Path.of(publicKeyPath));
        final byte[] privateData = Files.readAllBytes(Path.of(privateKeyPath));

        String privateString = new String(privateData, Charset.defaultCharset())
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll(System.lineSeparator(), "")
                .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateString);

        final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        final Collection<? extends Certificate> chain = certificateFactory.generateCertificates(new ByteArrayInputStream(publicData));

        Key key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));

        KeyStore clientKeyStore = KeyStore.getInstance("jks");
        final char[] pwdChars = "test".toCharArray();
        clientKeyStore.load(null, null);
        clientKeyStore.setKeyEntry("test", key, pwdChars, chain.toArray(new Certificate[0]));

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
        keyManagerFactory.init(clientKeyStore, pwdChars);
        return keyManagerFactory;
    }

    private static TrustManager[] getTrustManagers() {
        TrustManager[] acceptAllTrustManager = { new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }

            public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
            }
        }};
        return acceptAllTrustManager;
    }

    /**
     * Helper function to build a special {@link HttpClient} capable of authenticating with the
     * Blue Button server using a client TLS certificate
     *
     * @param KeyManagerFactory Manaages key material based on a KeyStore.
     * @param TrustManager[] TrustManager will accept self-signed certificates.
     * @return {@link HttpClient} compatible with HAPI FHIR TLS client
     */
    private HttpClient buildMutualTlsClient(KeyManagerFactory keyManagerFactory, TrustManager[] acceptAllTrustManager) {
        final SSLContext sslContext;

        try {

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), acceptAllTrustManager, new java.security.SecureRandom());

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
