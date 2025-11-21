package org.models;

public class Admin extends User {
    public Admin(String username) {
        super(username);
    }

    @Override
    public boolean isAdmin() { return true; }
}

