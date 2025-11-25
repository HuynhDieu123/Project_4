package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("restaurantDashboardBean")
@ViewScoped
public class RestaurantDashboardBean implements Serializable {

    private double totalRevenue;
    private int upcomingBookings;
    private int newInquiries;

    private List<BookingSummary> recentBookings;

    @PostConstruct
    public void init() {
        totalRevenue = 45231.89;
        upcomingBookings = 12;
        newInquiries = 8;

        recentBookings = new ArrayList<>();
        recentBookings.add(new BookingSummary("#FL-84321", "Alice Johnson", "Dec 24, 2023", "Confirmed",
                "bg-green-100 text-green-700"));
        recentBookings.add(new BookingSummary("#FL-84320", "Bob Williams", "Dec 20, 2023", "Pending",
                "bg-yellow-100 text-yellow-700"));
        recentBookings.add(new BookingSummary("#FL-84319", "Charlie Brown", "Nov 15, 2023", "Completed",
                "bg-gray-100 text-gray-700"));
        recentBookings.add(new BookingSummary("#FL-84318", "Diana Miller", "Nov 10, 2023", "Cancelled",
                "bg-red-100 text-red-700"));
    }

    // ========== GETTERS ==========

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public int getUpcomingBookings() {
        return upcomingBookings;
    }

    public int getNewInquiries() {
        return newInquiries;
    }

    public List<BookingSummary> getRecentBookings() {
        return recentBookings;
    }

    // ========== INNER CLASS ==========

    public static class BookingSummary implements Serializable {
        private String id;
        private String customer;
        private String date;
        private String statusLabel;
        private String statusClass;

        public BookingSummary(String id, String customer, String date,
                              String statusLabel, String statusClass) {
            this.id = id;
            this.customer = customer;
            this.date = date;
            this.statusLabel = statusLabel;
            this.statusClass = statusClass;
        }

        public String getId() { return id; }

        public String getCustomer() { return customer; }

        public String getDate() { return date; }

        public String getStatusLabel() { return statusLabel; }

        public String getStatusClass() { return statusClass; }
    }
}
