package org.apache.activemq.artemis.security.keycloak.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.security.keycloak.KeycloakResourcesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakDestinationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakDestinationUtils.class);

    private final Map<String, Set<Role>> previousConfigurations = new HashMap<>();

    public Map<String, Set<Role>> getSecurityRoles() {
        final Map<String, Set<Role>> securityRoles = new HashMap<>();
        try {
            securityRoles.putAll(KeycloakResourcesUtils.getInstance().getSecurityRoles());
        } catch (Exception e) {
            LOGGER.error("Error while retrieving destination security settings from Keycloak", e);
            if (!previousConfigurations.isEmpty()) {
                LOGGER.warn("Keycloak not available, going to return cached configuration");
                return previousConfigurations;
            }
        }
        previousConfigurations.clear();
        previousConfigurations.putAll(securityRoles);
        return securityRoles;
    }

    public void checkAndApplyConfigurationsUpdate(HierarchicalRepository<Set<Role>> securityRepository) {
        final Map<String, Set<Role>> oldConfiguration = new HashMap<>(previousConfigurations);
        final Map<String, Set<Role>> newConfiguration = getSecurityRoles();
        Boolean anyChange = Boolean.FALSE;
        for (final String oldDestination : oldConfiguration.keySet()) {
            if (!newConfiguration.containsKey(oldDestination)) { // destination removed
                LOGGER.info("Removing match for {}", oldDestination);
                securityRepository.addMatch(oldDestination, Collections.EMPTY_SET);
                anyChange = Boolean.TRUE;
            } else { // roles modifyed
                final Set<Role> oldRoles = oldConfiguration.getOrDefault(oldDestination, Collections.EMPTY_SET);
                final Set<Role> newRoles = newConfiguration.getOrDefault(oldDestination, Collections.EMPTY_SET);
                if (!oldRoles.containsAll(newRoles) || !newRoles.containsAll(oldRoles)) {
                    LOGGER.info("Modifying match for {}", oldDestination);
                    securityRepository.addMatch(oldDestination, Collections.EMPTY_SET);
                    securityRepository.addMatch(oldDestination, newConfiguration.get(oldDestination));
                    anyChange = Boolean.TRUE;
                }
            }
            newConfiguration.remove(oldDestination);
        }
        // Now newConfiguration contains only added destination
        for (final String newDestination : newConfiguration.keySet()) {
            LOGGER.info("Adding match for {}", newDestination);
            securityRepository.addMatch(newDestination, newConfiguration.get(newDestination));
            anyChange = Boolean.TRUE;
        }
        if(anyChange) {
            LOGGER.debug("Detsination security settings changed send notification to KeycloakSecurityManager");
            KeycloakResourcesUtils.getInstance().notifyKeycloakSecurityManagers();
        }
    }

}
