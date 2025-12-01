package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Named("restaurantDashboardBean")
@ViewScoped
public class RestaurantDashboardBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private double totalRevenue;
    private int upcomingBookings;
    private int newInquiries;

    private List<BookingSummary> recentBookings;

    @PostConstruct
    public void init() {
        loadDashboardData();
    }

    // ================== LOAD DATA TỪ DATABASE ==================
    private void loadDashboardData() {
        List<Bookings> all = bookingsFacade.findAll();
        recentBookings = new ArrayList<>();

        if (all == null || all.isEmpty()) {
            totalRevenue = 0;
            upcomingBookings = 0;
            newInquiries = 0;
            return;
        }

        // TODO: sau này filter theo restaurant của manager đang đăng nhập
        List<Bookings> filtered = new ArrayList<>(all);

        // ----- Total Revenue: sum TotalAmount -----
        BigDecimal total = BigDecimal.ZERO;
        for (Bookings b : filtered) {
            if (b.getTotalAmount() != null) {
                total = total.add(b.getTotalAmount());
            }
        }
        totalRevenue = total.doubleValue();

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();

        // ----- Upcoming bookings: eventDate >= hôm nay -----
        upcomingBookings = (int) filtered.stream()
                .filter(b -> b.getEventDate() != null)
                .map(b -> b.getEventDate().toInstant().atZone(zone).toLocalDate())
                .filter(d -> !d.isBefore(today))
                .count();

        // ----- New inquiries: eventDate trong 7 ngày tới -----
        LocalDate nextWeek = today.plusDays(7);
        newInquiries = (int) filtered.stream()
                .filter(b -> b.getEventDate() != null)
                .map(b -> b.getEventDate().toInstant().atZone(zone).toLocalDate())
                .filter(d -> !d.isBefore(today) && !d.isAfter(nextWeek))
                .count();

        // ----- Recent bookings: 4 booking mới nhất theo EventDate -----
        filtered.sort(Comparator
                .comparing(Bookings::getEventDate, Comparator.nullsLast(Date::compareTo))
                .thenComparing(Bookings::getBookingId)
                .reversed());

        SimpleDateFormat fmt = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);

        int limit = Math.min(4, filtered.size());
        for (int i = 0; i < limit; i++) {
            Bookings b = filtered.get(i);

            // Booking ID / Code
            String id;
            if (b.getBookingCode() != null && !b.getBookingCode().isBlank()) {
                id = b.getBookingCode();
            } else {
                id = "#BK-" + b.getBookingId();
            }

            // Customer name
            String customerName = "Guest";
            Users u = b.getCustomerId();
            if (u != null) {
                if (u.getFullName() != null && !u.getFullName().isBlank()) {
                    customerName = u.getFullName();
                } else if (u.getEmail() != null && !u.getEmail().isBlank()) {
                    customerName = u.getEmail();
                }
            }

            // Date label
            String dateLabel = "";
            if (b.getEventDate() != null) {
                dateLabel = fmt.format(b.getEventDate());
            }

            // Status theo EventDate (để ra màu giống Figma)
            String statusLabel;
            String statusClass;

            if (b.getEventDate() == null) {
                statusLabel = "Pending";
                statusClass = "bg-yellow-100 text-yellow-700";
            } else {
                LocalDate ev = b.getEventDate().toInstant().atZone(zone).toLocalDate();
                if (ev.isBefore(today)) {
                    statusLabel = "Completed";
                    statusClass = "bg-gray-100 text-gray-700";
                } else if (ev.isEqual(today)) {
                    statusLabel = "Pending";
                    statusClass = "bg-yellow-100 text-yellow-700";
                } else {
                    statusLabel = "Confirmed";
                    statusClass = "bg-green-100 text-green-700";
                }
            }

            recentBookings.add(
                    new BookingSummary(id, customerName, dateLabel, statusLabel, statusClass)
            );
        }
    }

    // ================== GETTERS ==================
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

    // ================== INNER CLASS ==================
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
