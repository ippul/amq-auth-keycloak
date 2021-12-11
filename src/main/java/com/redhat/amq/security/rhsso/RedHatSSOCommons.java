package com.redhat.amq.security.rhsso;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.HierarchicalObjectRepository;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.representation.TokenIntrospectionResponse;
import org.keycloak.authorization.client.resource.ProtectionResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.authorization.Permission;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedHatSSOCommons {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedHatSSOCommons.class);

    private static Configuration configuration = initConfiguration();

    private final AuthzClient authzClient;
    
    public RedHatSSOCommons() {
        if(configuration == null){
            configuration = RedHatSSOCommons.initConfiguration();
        }
        this.authzClient = AuthzClient.create(configuration);
    }

    private static Configuration initConfiguration() {
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

    public Boolean permissionCheck(String token, String rep, String scope){
        try {
           AccessTokenResponse response = authzClient.authorization(token).authorize();
           TokenIntrospectionResponse introspectionResponse = authzClient.protection().introspectRequestingPartyToken(response.getToken());
           Optional<Permission> permission = getPremission(rep, introspectionResponse);
           if(permission.isPresent()){
              return permission.get().getScopes().contains(scope);
           } 
        }catch(org.keycloak.authorization.client.AuthorizationDeniedException ex){
            ex.printStackTrace();
        }
        return Boolean.FALSE;
    }
  
     private Optional<Permission> getPremission(String rep, TokenIntrospectionResponse introspectionResponse) {
        HierarchicalRepository<Permission> tmpSecurityRepository = new HierarchicalObjectRepository<Permission>();

        if (introspectionResponse.getActive()) {
            for (Permission granted : introspectionResponse.getPermissions()) {
                tmpSecurityRepository.addMatch(granted.getResourceName(), granted);
            }
        }
        return Optional.ofNullable(tmpSecurityRepository.getMatch(rep));
     }
}
