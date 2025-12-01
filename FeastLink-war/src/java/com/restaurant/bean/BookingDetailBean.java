package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Named("bookingDetailBean")
@ViewScoped
public class BookingDetailBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private Bookings booking;   // booking đang xem chi tiết

    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc == null || fc.getExternalContext() == null) {
            return;
        }

        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String code = params.get("code");   // lấy từ dashboard ?code=...

        if (code == null || code.isBlank()) {
            return;
        }

        // Lấy tất cả bookings rồi lọc theo BookingCode (ít dữ liệu nên ổn)
        List<Bookings> all = bookingsFacade.findAll();
        for (Bookings b : all) {
            if (code.equalsIgnoreCase(b.getBookingCode())) {
                this.booking = b;
                break;
            }
        }
    }

    // ========== helper cho view ==========

    public boolean isBookingFound() {
        return booking != null;
    }

    public Bookings getBooking() {
        return booking;
    }

    public String getBookingCode() {
        return booking != null ? booking.getBookingCode() : "";
    }

    public String getCustomerName() {
        if (booking == null || booking.getCustomerId() == null) {
            return "";
        }
        // Giả định entity Users có field fullName
        return booking.getCustomerId().getFullName();
    }

    public String getStatusLabel() {
        if (booking == null || booking.getBookingStatus() == null) return "";
        String s = booking.getBookingStatus().toUpperCase();
        switch (s) {
            case "CONFIRMED": return "Confirmed";
            case "PENDING":   return "Pending";
            case "COMPLETED": return "Completed";
            case "CANCELLED": return "Cancelled";
            default:          return booking.getBookingStatus();
        }
    }

    public String getStatusCss() {
        if (booking == null || booking.getBookingStatus() == null) {
            return "bg-gray-100 text-gray-700";
        }
        String s = booking.getBookingStatus().toUpperCase();
        switch (s) {
            case "CONFIRMED": return "bg-green-100 text-green-700";
            case "PENDING":   return "bg-yellow-100 text-yellow-700";
            case "COMPLETED": return "bg-gray-100 text-gray-700";
            case "CANCELLED": return "bg-red-100 text-red-700";
            default:          return "bg-gray-100 text-gray-700";
        }
    }

    public String getLocationDisplay() {
        if (booking == null || booking.getLocationType() == null) return "";
        if ("RESTAURANT".equalsIgnoreCase(booking.getLocationType())) {
            return "At restaurant";
        } else if ("OUTSIDE".equalsIgnoreCase(booking.getLocationType())) {
            return "Outside catering";
        }
        return booking.getLocationType();
    }
}
