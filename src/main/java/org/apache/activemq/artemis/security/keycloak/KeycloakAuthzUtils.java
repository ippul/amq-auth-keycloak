package org.apache.activemq.artemis.security.keycloak;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.keycloak.authorization.client.AuthorizationDeniedException;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.resource.AuthorizationResource;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.function.Function;

public class KeycloakAuthzUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakAuthzUtils.class);

    private static Configuration configuration = initConfiguration();

    private final AuthzClient authzClient;
    
    public KeycloakAuthzUtils() {
        if(configuration == null){
            configuration = KeycloakAuthzUtils.initConfiguration();
        }
        this.authzClient = AuthzClient.create(configuration);
    }



//    public static Function<Configuration, Configuration> buildConfigurationWithCustomHttpClient = (configuration) -> {
//        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
//        connectionManager.setValidateAfterInactivity(10);
//        connectionManager.setMaxTotal(10);
//        SSLContextBuilder builder = new SSLContextBuilder();
//        SSLConnectionSocketFactory sslsFactory = null;
//        try {
//            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
//            sslsFactory = new SSLConnectionSocketFactory(builder.build());
//        } catch (NoSuchAlgorithmException e) {
//            e.printStackTrace();
//        } catch (KeyStoreException | KeyManagementException e) {
//            e.printStackTrace();
//        }
//        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsFactory)
//                //.setConnectionManager(connectionManager)
//                .build();
//        return new Configuration(configuration.getAuthServerUrl(),
//                configuration.getRealm(),
//                configuration.getResource(),
//                configuration.getCredentials(),
//                httpclient);
//    };

    public static Configuration initConfiguration() {
        LOGGER.info("Searching for Configuration for Red Hat SSO integration in folder {}", System.getProperty("artemis.instance.etc"));
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            return Files.list(Paths.get(System.getProperty("artemis.instance.etc"))) //
                    .filter(name -> name.toString().endsWith(".json")) //
                    .map(Path::toFile) //
                    .map(file -> {
                        try {
                            return mapper.readValue(file, Configuration.class);
                        } catch (JsonParseException e) {
                            e.printStackTrace();
                        } catch (JsonMappingException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }).filter(keycloakConfiguration -> keycloakConfiguration != null) //
                    .filter(keycloakConfiguration -> keycloakConfiguration.getCredentials().size() > 0) //
                    .findFirst() //
                    .map(buildConfigurationWithCustomHttpClient) //
                    .get();
        } catch (IOException e) {
            LOGGER.error("Unable to fine configuration file for Red Hat SSO {}", e);
            e.printStackTrace();
        }
        return null;
    }

    public AuthzClient getClient(){
        return this.authzClient;
    }

    public String[] getResources() throws IOException {
        final ProtectionResource protection = authzClient.protection();
        return protection.resource().findAll();
    }

    public ResourceRepresentation findResourceById(final String resourceId) {
        final ProtectionResource protection = authzClient.protection();
        return protection.resource().findById(resourceId);
    }

    public Boolean authorize(String authzToken, String resourceName, String scope) {
        LOGGER.info("Check permission for Resource: {} with scope: {}", resourceName, scope);
        try {
            final AuthorizationResource authResource = authzClient.authorization(authzToken);
            final AuthorizationRequest authRequest = new AuthorizationRequest();
            authRequest.addPermission(resourceName, scope);
            authResource.authorize(authRequest).getToken();
            return Boolean.TRUE;
        } catch(AuthorizationDeniedException e){
            LOGGER.error(e.getMessage(), e);
        }
        return Boolean.FALSE;
    }

    public static CloseableHttpClient getHttpClient(String url) {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            Certificate[] certs = getCertificates(url);
            for(Certificate cert : certs){
                if(cert instanceof X509Certificate) {
                    X509Certificate certificateToAdd = (X509Certificate) cert;
                    keyStore.setCertificateEntry(certificateToAdd.getSubjectDN().getName().replaceAll("CN=", "").replaceAll("\\*", ""), cert);
                }
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Function<Configuration, Configuration> buildConfigurationWithCustomHttpClient = (configuration) -> new Configuration(configuration.getAuthServerUrl(),
            configuration.getRealm(),
            configuration.getResource(),
            configuration.getCredentials(),
            getHttpClient(configuration.getAuthServerUrl()));

    public static Certificate[] getCertificates(final String host) throws IOException {
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException e) {
        }
        URL obj = new URL(host);
        HttpsURLConnection con =  (HttpsURLConnection) obj.openConnection();
        con.connect();
        return con.getServerCertificates();
    }
}
