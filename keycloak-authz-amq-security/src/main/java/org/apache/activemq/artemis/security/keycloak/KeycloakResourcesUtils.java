package org.apache.activemq.artemis.security.keycloak;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.settings.HierarchicalRepository;
import org.apache.activemq.artemis.core.settings.impl.HierarchicalObjectRepository;
import org.apache.activemq.artemis.security.keycloak.model.DestinationAttributeKey;
import org.apache.activemq.artemis.security.keycloak.model.DestinationNameKey;
import org.apache.activemq.artemis.security.keycloak.model.DestinationType;
import org.keycloak.representations.idm.authorization.ResourceRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeycloakResourcesUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeycloakResourcesUtils.class);

    private static KeycloakResourcesUtils keycloakBridge;

    private PropertyChangeSupport propertyChangeSupport;

    private KeycloakAuthzUtils redHatSSOCommons;

    private HierarchicalRepository<DestinationNameKey> destinationNamesResolver;

    public static KeycloakResourcesUtils getInstance() {
        if (keycloakBridge == null) {
            keycloakBridge = new KeycloakResourcesUtils();
        }
        return keycloakBridge;
    }

    private KeycloakResourcesUtils() {
        this.redHatSSOCommons = new KeycloakAuthzUtils();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener) {
        this.propertyChangeSupport.removePropertyChangeListener(propertyChangeListener);
    }

    private Collection<ResourceRepresentation> getResourceRepresentationList() throws IOException {
        final Collection<ResourceRepresentation> result = new ArrayList<ResourceRepresentation>();
        final String[] allResources = redHatSSOCommons.getResources();
        this.destinationNamesResolver = new HierarchicalObjectRepository<DestinationNameKey> ();
        for (final String resourceId : allResources) {
            final ResourceRepresentation resourceRepresentation = redHatSSOCommons.findResourceById(resourceId);
            if (DestinationType.isDestinationType(resourceRepresentation.getType())) {
                result.add(resourceRepresentation);
                destinationNamesResolver.addMatch(resourceRepresentation.getDisplayName(), new DestinationNameKey(resourceRepresentation.getName(), resourceRepresentation.getDisplayName()));
            }
        }
        return result;
    }

    public Map<String, Set<Role>> getSecurityRoles() throws IOException {
        final Map<String, Set<Role>> securityRoles = new HashMap<>();
        final Collection<ResourceRepresentation> resourceRepresentationList = getResourceRepresentationList();
        for (final ResourceRepresentation resourceRepresentation : resourceRepresentationList) {
            populateSecurityRoles(resourceRepresentation, securityRoles);
        }
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
        createRolesForDestination(resourceRepresentation.getDisplayName(), securityRoles, groupsRolesMapping);
    }

    private void createRolesForDestination(final String destinationName, final Map<String, Set<Role>> securityRoles,
            final Map<String, boolean[]> groupsRolesMapping) {
        final Set<Role> roles = new HashSet<>();
        for (final String roleName : groupsRolesMapping.keySet()) {
            final Role role = createRole(roleName, groupsRolesMapping);
            roles.add(role);
        }
        securityRoles.put(destinationName, roles);
        LOGGER.debug("DestinationName: {} Roles: {}", destinationName, roles);
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

    public void notifyKeycloakSecurityManagers(){
        propertyChangeSupport.firePropertyChange("destinationNamesResolver", null, destinationNamesResolver);
    }

    public HierarchicalRepository<DestinationNameKey> getDestinationNamesResolver(){
        return this.destinationNamesResolver;
    }
}
