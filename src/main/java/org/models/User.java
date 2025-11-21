package org.models;

public class User {
    private final String username;
    private final boolean admin;

    public User(String username, boolean admin) {
        this.username = username;
        this.admin = admin;
    }

    public String getUsername() { return username; }
    public boolean isAdmin() { return admin; }
}

