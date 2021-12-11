package com.redhat.amq.security.model;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum PermissionType {
    PUBLISH("publish"),
    SUBSCRIBE("subscribe"),
    PUBLISH_SUBSCRIBE("pub-sub");

    private final String permissionName;

    PermissionType(final String permissionName){
        this.permissionName = permissionName;
    }

    private static Map<String, PermissionType> MAP = Stream.of( PermissionType.values() )
        .collect( Collectors.toMap(s -> s.getPermissionName(), Function.identity() ) );

    public static PermissionType fromString(String permissionType ) {
        return Optional.ofNullable( MAP.get( permissionType.toLowerCase() ) )
            .orElseThrow( () -> new IllegalArgumentException( permissionType ) );
    }

    public static Boolean isPermissionType(String s) {
        if(s == null){
            return Boolean.FALSE;
        }
        return MAP.containsKey(s.toUpperCase());
    }

    public String getPermissionName() {
        return permissionName;
    }
}
