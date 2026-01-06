package ch.framedev.lagersystem.classes;

import java.util.List;

public class User {

    private String name;
    private List<String> orders;

    public User(String name, List<String> orders) {
        this.name = name;
        this.orders = orders;
    }

    public String getName() {
        return name;
    }

    public List<String> getOrders() {
        return orders;
    }

    public void setOrders(List<String> orders) {
        this.orders = orders;
    }
}
