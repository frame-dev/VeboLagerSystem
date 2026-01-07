package ch.framedev.lagersystem.classes;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class User {

    private final String name;
    private List<String> orders;

    public User(String name, List<String> orders) {
        this.name = name;
        this.orders = orders;
    }

    public String getName() {
        return name;
    }

    public List<String> getOrders() {
        if(orders == null) return new ArrayList<>();
        return orders;
    }

    public void setOrders(List<String> orders) {
        this.orders = orders;
    }
}
