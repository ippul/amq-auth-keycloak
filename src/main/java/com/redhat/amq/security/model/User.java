package com.redhat.amq.security.model;

import java.util.ArrayList;
import java.util.List;

public class User {

    private final String username;

    private final String password;

    private final List<String> roles = new ArrayList<>();

    public User(final String username, final String password){
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getRoles() {
        return roles;
    }

    @Override
    public String toString() {
        return "User{" +
            "username='" + username + '\'' +
            ", password='" + password + '\'' +
            ", roles=" + roles +
            '}';
    }
}
