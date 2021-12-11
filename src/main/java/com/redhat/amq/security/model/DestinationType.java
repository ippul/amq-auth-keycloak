package com.redhat.amq.security.model;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DestinationType {
    JMS_QUEUE("JMS_QUEUE"),
    JMS_TOPIC("JMS_TOPIC");

    private final String destinationType;

    DestinationType(final String destinationType) {
        this.destinationType = destinationType;
    }

    private static Map<String, DestinationType> MAP = Stream.of( DestinationType.values() )
        .collect( Collectors.toMap(s -> s.getDestinationType(), Function.identity() ) );

    public static DestinationType fromString( String destinationType ) {
        return Optional.ofNullable( MAP.get( destinationType.toUpperCase() ) )
            .orElseThrow( () -> new IllegalArgumentException( destinationType ) );
    }

    public static Boolean isDestinationType(String s) {
        if(s == null){
            return Boolean.FALSE;
        }
        return MAP.containsKey(s.toUpperCase());
    }

    public String getDestinationType() {
        return destinationType;
    }
}
