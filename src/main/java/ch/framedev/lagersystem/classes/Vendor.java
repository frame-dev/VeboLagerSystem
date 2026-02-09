package ch.framedev.lagersystem.classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a vendor with contact details and supplied articles.
 */
@SuppressWarnings("unused")
public class Vendor {

    /** Vendor name. */
    private String name;
    /** Primary contact person. */
    private String contactPerson;
    /** Contact phone number. */
    private String phoneNumber;
    /** Contact email address. */
    private String email;
    /** Mailing or physical address. */
    private String address;
    /** List of article numbers supplied by the vendor. */
    private List<String> suppliedArticles;
    /** Minimum order value for purchases. */
    private double minOrderValue;

    /**
     * Creates a vendor with supplied articles and minimum order value.
     *
     * @param name vendor name
     * @param contactPerson primary contact person
     * @param phoneNumber contact phone number
     * @param email contact email address
     * @param address mailing or physical address
     * @param suppliedArticles list of supplied article numbers
     * @param minOrderValue minimum order value
     */
    public Vendor(String name, String contactPerson, String phoneNumber, String email, String address, List<String> suppliedArticles, double minOrderValue) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.suppliedArticles = suppliedArticles;
        this.minOrderValue = minOrderValue;
    }

    /**
     * Creates a vendor with minimum order value and no supplied-articles list.
     *
     * @param name vendor name
     * @param contactPerson primary contact person
     * @param phoneNumber contact phone number
     * @param email contact email address
     * @param address mailing or physical address
     * @param minOrderValue minimum order value
     */
    public Vendor(String name, String contactPerson, String phoneNumber, String email, String address, double minOrderValue) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.minOrderValue = minOrderValue;
    }

    /**
     * Creates a vendor with an empty supplied-articles list.
     *
     * @param name vendor name
     * @param contactPerson primary contact person
     * @param phoneNumber contact phone number
     * @param email contact email address
     * @param address mailing or physical address
     */
    public Vendor(String name, String contactPerson, String phoneNumber, String email, String address) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.suppliedArticles = new ArrayList<>();
    }

    /** @return vendor name */
    public String getName() {
        return name;
    }

    /** @param name vendor name */
    public void setName(String name) {
        this.name = name;
    }

    /** @return primary contact person */
    public String getContactPerson() {
        return contactPerson;
    }

    /** @param contactPerson primary contact person */
    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    /** @return contact phone number */
    public String getPhoneNumber() {
        return phoneNumber;
    }

    /** @param phoneNumber contact phone number */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    /** @return contact email address */
    public String getEmail() {
        return email;
    }

    /** @param email contact email address */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return mailing or physical address */
    public String getAddress() {
        return address;
    }

    /** @param address mailing or physical address */
    public void setAddress(String address) {
        this.address = address;
    }

    /** @param suppliedArticles list of supplied article numbers */
    public void setSuppliedArticles(List<String> suppliedArticles) {
        this.suppliedArticles = suppliedArticles;
    }

    /** @return list of supplied article numbers */
    public List<String> getSuppliedArticles() {
        return suppliedArticles;
    }

    /** @return minimum order value */
    public double getMinOrderValue() {
        return minOrderValue;
    }

    /** @param minOrderValue minimum order value */
    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    /**
     * Builds the QR code payload string using labeled fields.
     *
     * @return QR code data payload
     */
    public String getQRCodeData() {
        String articlesString = String.join(",", getSuppliedArticles());
        return "name:" + name + ";" +
               "contactPerson:" + contactPerson + ";" +
               "phoneNumber:" + phoneNumber + ";" +
               "email:" + email + ";" +
               "address:" + address + ";" +
               "suppliedArticles:" + articlesString + ";" +
               "minOrderValue:" + minOrderValue;
    }
}
