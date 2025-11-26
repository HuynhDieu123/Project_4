package com.mypack.admin;

import com.mypack.entity.Bookings;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private long totalUsers;
    private long totalRestaurants;
    private long totalBookings;
    private double monthlyRevenue;
    private double cancelRate;
    private long pendingApprovals;

    private List<Bookings> recentBookings;

    @PostConstruct
    public void init() {

        // Tổng số user
        totalUsers = usersFacade.countUsers();

        // Tổng nhà hàng
        totalRestaurants = restaurantsFacade.countRestaurants();

        // Tổng booking
        totalBookings = bookingsFacade.countAllBookings();

        // Doanh thu tháng
        monthlyRevenue = bookingsFacade.calculateMonthlyRevenue();

        // Tỷ lệ hủy
        cancelRate = bookingsFacade.calculateCancelRate();

        // Booking chờ duyệt
        pendingApprovals = bookingsFacade.countPendingApprovals();

        // Booking gần nhất
        recentBookings = bookingsFacade.findRecentBookings();
    }

    // GETTERS
    public long getTotalUsers() { return totalUsers; }
    public long getTotalRestaurants() { return totalRestaurants; }
    public long getTotalBookings() { return totalBookings; }
    public double getMonthlyRevenue() { return monthlyRevenue; }
    public double getCancelRate() { return cancelRate; }
    public long getPendingApprovals() { return pendingApprovals; }
    public List<Bookings> getRecentBookings() { return recentBookings; }
}
