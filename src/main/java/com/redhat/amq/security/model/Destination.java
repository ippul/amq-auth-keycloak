package com.redhat.amq.security.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Destination {

    private final String name;

    private final DestinationType type;

    private final Map<DestinationAttributeKey, List<String>> configurations = new HashMap<>();

    public Destination(final String name, final DestinationType type){
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DestinationType getType() {
        return type;
    }

    public Map<DestinationAttributeKey, List<String>> getConfigurations() {
        return configurations;
    }

    @Override
    public String toString() {
        return "Destination{" +
            "name='" + name + '\'' +
            ", type=" + type +
            ", configurations=" + configurations +
            '}';
    }
}
