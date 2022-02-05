package org.apache.activemq.artemis.security.keycloak.model;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DestinationAttributeKey {

    CREATE_DURABLE_QUEUE("createDurableQueue", 2),
    CREATE_ADDRESS("createAddress", 8),
    CREATE_NON_DURABLE_QUEUE("createNonDurableQueue", 4),
    DELETE_NON_DURABLE_QUEUE("deleteNonDurableQueue", 5),
    CONSUME("consume", 1),
    DELETE_ADDRESS("deleteAddress", 9),
    SEND("send", 0),
    DELETE_DURABLE_QUEUE("deleteDurableQueue", 3),
    MANAGE("manage", 6),
    BROWSE("browse", 7);

    private static Map<String, DestinationAttributeKey> MAP = Stream.of( DestinationAttributeKey.values() )
        .collect( Collectors.toMap(s -> s.getPermission(), Function.identity() ) );

    private final String permission;

    private final int index;

    DestinationAttributeKey(final String permission, final int index) {
        this.permission = permission;
        this.index = index;
    }

    public String getPermission() {
        return permission;
    }

    public int getIndex() {
        return index;
    }

    public static DestinationAttributeKey fromString( String permission ) {
        return Optional.ofNullable( MAP.get( permission ) )
            .orElseThrow( () -> new IllegalArgumentException( permission ) );
    }

}
