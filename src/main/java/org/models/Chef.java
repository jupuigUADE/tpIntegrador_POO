package org.models;

public class Chef extends User {
    public Chef(String username) {
        super(username);
    }

    @Override
    public boolean isChef() { return true; }
}

