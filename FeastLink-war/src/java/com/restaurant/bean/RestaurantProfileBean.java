/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import java.io.Serializable;

/**
 *
 * @author nghiapham
 */
@Named("restaurantProfileBean")
@ViewScoped
public class RestaurantProfileBean implements Serializable {

    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String contactPerson;
    private Integer minGuests;
    private Integer minDaysBeforeBooking;
    private String cancelPolicy;

    @PostConstruct
    public void init() {
        // Tạm thời gán dữ liệu demo
        name = "The Grand Eatery";
        description = "A modern fine-dining restaurant specializing in catering.";
        phone = "(555) 123-4567";
        email = "contact@grandeatery.com";
        address = "123 Luxury Lane, Metropolis, 10001";
        contactPerson = "Alex Doe";
        minGuests = 20;
        minDaysBeforeBooking = 7;
        cancelPolicy = "Full refund if cancelled 14 days before the event. "
                + "50% refund between 7–13 days. No refund within 7 days.";
    }

    // ========== ACTION ==========

    public String saveProfile() {
        // TODO: sau này gọi EJB để lưu xuống SQL Server
        System.out.println("Saving restaurant profile for: " + name);
        // Giữ lại trên cùng trang
        return null;
    }

    // ========== GETTER/SETTER ==========

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMinDaysBeforeBooking() { return minDaysBeforeBooking; }
    public void setMinDaysBeforeBooking(Integer minDaysBeforeBooking) {
        this.minDaysBeforeBooking = minDaysBeforeBooking;
    }

    public String getCancelPolicy() { return cancelPolicy; }
    public void setCancelPolicy(String cancelPolicy) { this.cancelPolicy = cancelPolicy; }
}
