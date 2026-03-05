package ch.framedev.lagersystem.classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a user with a name and a list of order IDs.
 */
public class User {

    /** User display name. */
    private final String name;
    /** List of order IDs associated with the user. */
    private List<String> orders;

    /**
     * Creates a new user.
     *
     * @param name user display name
     * @param orders list of order IDs
     */
    public User(String name, List<String> orders) {
        this.name = name;
        this.orders = orders;
    }

    /** @return user display name */
    public String getName() {
        return name;
    }

    /**
     * Returns a non-null list of order IDs.
     *
     * @return list of order IDs
     */
    public List<String> getOrders() {
        if(orders == null) return new ArrayList<>();
        return orders;
    }

    /** @param orders list of order IDs */
    public void setOrders(List<String> orders) {
        this.orders = orders;
    }

    /**
     * Builds the QR code payload string using labeled fields.
     *
     * @return QR code data payload
     */
    public String getQRCodeData() {
        String ordersString = String.join(",", getOrders());
        return "name:" + name + ";" + "orders:" + ordersString;
    }
}
