package org.apache.activemq.artemis.security.keycloak.model;

public class DestinationNameKey {

    private final String resourceName;

    private final String resourceMatch;

    public DestinationNameKey(String resourceName, String resourceMatch) {
        this.resourceName = resourceName;
        this.resourceMatch = resourceMatch;
    }
    public String getResourceName() {
        return resourceName;
    }
    public String getResourceMatch() {
        return resourceMatch;
    }

    
}
