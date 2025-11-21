package org.models;

public class Waiter extends User {
    public Waiter(String username) {
        super(username);
    }

    @Override
    public boolean isWaiter() { return true; }
}

