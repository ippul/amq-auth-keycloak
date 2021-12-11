package com.redhat.amq.security.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.redhat.amq.security.model.DestinationAttributeKey;
import com.redhat.amq.security.model.DestinationType;
import com.redhat.amq.security.rhsso.RedHatSSOCommons;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AMQRedHatSSOIntegrationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(AMQRedHatSSOIntegrationUtils.class);

    private final Map<String, Set<Role>> previousConfigurations = new HashMap<>();

    public Map<String, Set<Role>> getSecurityRoles() {
        final Map<String, Set<Role>> securityRoles = new HashMap<>();
        try {
            final RedHatSSOCommons redHatSSOCommons = new RedHatSSOCommons();
            final String[] allResources = redHatSSOCommons.getResources();
            for (final String resourceId : allResources) {
                final ResourceRepresentation resourceRepresentation = redHatSSOCommons.findResourceById(resourceId);
                if (DestinationType.isDestinationType(resourceRepresentation.getType())) {
                    populateSecurityRoles(resourceRepresentation, securityRoles);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while retrieving authorization configurations from RH-SSO", e);
            if (!previousConfigurations.isEmpty()) {
                LOGGER.info("Red Hat SSO not available, going to return cached configuration");
                return previousConfigurations;
            }
        }
        previousConfigurations.clear();
        previousConfigurations.putAll(securityRoles);
        return securityRoles;
    }

    private void populateSecurityRoles(final ResourceRepresentation resourceRepresentation,
            final Map<String, Set<Role>> securityRoles) {
        final Map<String, boolean[]> groupsRolesMapping = new HashMap<>();
        for (DestinationAttributeKey destinationAttributeKey : DestinationAttributeKey.values()) {
            if (resourceRepresentation.getAttributes().containsKey(destinationAttributeKey.getPermission())) {
                final List<String> allowedClientRoles = resourceRepresentation.getAttributes()
                        .get(destinationAttributeKey.getPermission());
                for (String clientRole : allowedClientRoles) {
                    fillMatrixForRole(destinationAttributeKey.getPermission(), clientRole.trim(), groupsRolesMapping);
                }
            }
        }
        createRolesForDestination(resourceRepresentation.getName(), securityRoles, groupsRolesMapping);
    }

    private void createRolesForDestination(final String destinationName, final Map<String, Set<Role>> securityRoles,
            final Map<String, boolean[]> groupsRolesMapping) {
        final Set<Role> roles = new HashSet<>();
        for (final String roleName : groupsRolesMapping.keySet()) {
            final Role role = createRole(roleName, groupsRolesMapping);
            roles.add(role);
        }
        securityRoles.put(destinationName, roles);
    }

    private Role createRole(final String roleName, final Map<String, boolean[]> groupsRolesMapping) {
        final boolean[] matrix = groupsRolesMapping.get(roleName);
        final Role role = new Role(roleName, matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5],
                matrix[6], matrix[7], matrix[8], matrix[9]);
        return role;
    }

    private void fillMatrixForRole(String destinationAttributeKey, String clientRole,
            final Map<String, boolean[]> groupsRolesMapping) {
        boolean[] matrix = groupsRolesMapping.containsKey(clientRole) ? groupsRolesMapping.get(clientRole)
                : new boolean[10];
        DestinationAttributeKey destAttr = DestinationAttributeKey.fromString(destinationAttributeKey);
        matrix[destAttr.getIndex()] = true;
        groupsRolesMapping.put(clientRole, matrix);
    }

    public void checkAndApplyConfigurationsUpdate(HierarchicalRepository<Set<Role>> securityRepository) {
        final Map<String, Set<Role>> oldConfiguration = new HashMap<>(previousConfigurations);
        final Map<String, Set<Role>> newConfiguration = getSecurityRoles();

        for (final String oldDestination : oldConfiguration.keySet()) {
            if (!newConfiguration.containsKey(oldDestination)) { // destination removed
                LOGGER.info("Removing match for {}", oldDestination);
                securityRepository.addMatch(oldDestination, Collections.EMPTY_SET);
            } else { // roles modifyed
                final Set<Role> oldRoles = oldConfiguration.getOrDefault(oldDestination, Collections.EMPTY_SET);
                final Set<Role> newRoles = newConfiguration.getOrDefault(oldDestination, Collections.EMPTY_SET);
                if (!oldRoles.containsAll(newRoles) || !newRoles.containsAll(oldRoles)) {
                    LOGGER.info("Modifying match for {}", oldDestination);
                    securityRepository.addMatch(oldDestination, Collections.EMPTY_SET);
                    securityRepository.addMatch(oldDestination, newConfiguration.get(oldDestination));
                }
            }
            newConfiguration.remove(oldDestination);
        }
        // Now newConfiguration contains only added destination
        for (final String newDestination : newConfiguration.keySet()) {
            LOGGER.info("Adding match for {}", newDestination);
            securityRepository.addMatch(newDestination, newConfiguration.get(newDestination));
        }
    }

}
