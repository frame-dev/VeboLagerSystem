package ch.framedev.lagersystem.classes;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Vendor {

    private String name;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String address;
    private List<String> suppliedArticles;

    public Vendor(String name, String contactPerson, String phoneNumber, String email, String address, List<String> suppliedArticles) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.suppliedArticles = suppliedArticles;
    }

    public Vendor(String name, String contactPerson, String phoneNumber, String email, String address) {
        this.name = name;
        this.contactPerson = contactPerson;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.address = address;
        this.suppliedArticles = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setSuppliedArticles(List<String> suppliedArticles) {
        this.suppliedArticles = suppliedArticles;
    }

    public List<String> getSuppliedArticles() {
        return suppliedArticles;
    }
}
