package org.apache.activemq.artemis.security.keycloak;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Properties;

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

    public static Configuration initConfiguration() {
        final Properties pluginProperties = new Properties();
        try (FileInputStream fis = new FileInputStream("/amq/extra/configmaps/amq-sso-plugin-config/amq-sso-plugin-config.properties")) {
            pluginProperties.load(fis);
            LOGGER.info("trust.self.signed.certificates: {}", pluginProperties.getProperty("trust.self.signed.certificates"));
        } catch (IOException ex) {
            LOGGER.info("No /amq/extra/configmaps/amq-sso-plugin-config/amq-sso-plugin-config.properties found");
        }
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
                    .map(configuration -> {
                        if("true".equals(pluginProperties.getOrDefault("trust.self.signed.certificates", "false"))) {
                            return new Configuration(configuration.getAuthServerUrl(),
                                    configuration.getRealm(),
                                    configuration.getResource(),
                                    configuration.getCredentials(),
                                    getHttpClient(configuration.getAuthServerUrl()));
                        }
                        return configuration;
                    }) //
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
                    LOGGER.info("cert cleaned name: {}", certificateToAdd.getSubjectDN().getName()
                            .replaceAll("CN=", "")
                            .replaceAll("\\*", ""));
                    keyStore.setCertificateEntry(certificateToAdd.getSubjectDN().getName().replaceAll("CN=", "").replaceAll("\\*", ""), cert);
                }
            }
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            LOGGER.info("Http client correctly created...");
            return HttpClients.custom()
                    .setSSLContext(sslContext)
                    .build();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static Certificate[] getCertificates(final String host) throws IOException {
        LOGGER.info("Finding certificates...");
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
        LOGGER.info("Returning founded certificates...");
        return con.getServerCertificates();
    }
}
