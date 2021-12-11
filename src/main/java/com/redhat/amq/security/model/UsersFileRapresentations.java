package com.redhat.amq.security.model;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public class UsersFileRapresentations {

    private String realm;

    private List<UserRepresentation> users;

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public List<UserRepresentation> getUsers() {
        return users;
    }

    public void setUsers(List<UserRepresentation> users) {
        this.users = users;
    }
}
