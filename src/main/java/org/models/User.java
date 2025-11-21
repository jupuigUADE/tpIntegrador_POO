package org.models;

public abstract class User {
    private final String username;

    protected User(String username) {
        this.username = username;
    }

    public String getUsername() { return username; }

    // Role helpers - override in subclasses as needed
    public boolean isAdmin() { return false; }
    public boolean isWaiter() { return false; }
    public boolean isChef() { return false; }
}
