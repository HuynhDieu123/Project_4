package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.BookingCombosFacadeLocal;
import com.mypack.sessionbean.BookingMenuItemsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("bookingDetailBean")
@ViewScoped
public class BookingDetailBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private BookingCombosFacadeLocal bookingCombosFacade;

    @EJB
    private BookingMenuItemsFacadeLocal bookingMenuItemsFacade;

    private Long bookingId;
    private Bookings booking;

    // ====== Menu & package ======
    private List<BookingCombos> bookingCombos;
    private List<BookingMenuItems> bookingMenuItems;
    private String selectedPackageName;
    private BigDecimal packageSubtotal;
    private BigDecimal menuSubtotal;

    // ---------------------------------------------------------------------
    // INIT
    // ---------------------------------------------------------------------
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
        try {
            String idStr = params.get("bookingId");
            if (idStr != null && !idStr.isBlank()) {
                bookingId = Long.valueOf(idStr);
                booking = bookingsFacade.find(bookingId);
            }
        } catch (Exception ignore) {
        }

        if (booking != null) {
            loadPackagesAndMenu();
        }
    }

    private void loadPackagesAndMenu() {
        bookingCombos = new ArrayList<>();
        bookingMenuItems = new ArrayList<>();
        packageSubtotal = BigDecimal.ZERO;
        menuSubtotal = BigDecimal.ZERO;
        selectedPackageName = null;

        if (booking == null || booking.getBookingId() == null) {
            return;
        }

        Long id = booking.getBookingId();

        // ================== PACKAGES (MenuCombos) ==================
        List<BookingCombos> combos = null;
        try {
            combos = bookingCombosFacade.findByBookingId(id);
        } catch (Exception e) {
            // có thể log nếu muốn
        }

        if (combos != null && !combos.isEmpty()) {
            bookingCombos.addAll(combos);

            // lấy tên package đầu tiên
            BookingCombos first = combos.get(0);
            if (first.getMenuCombos() != null && first.getMenuCombos().getName() != null) {
                selectedPackageName = first.getMenuCombos().getName();
            }

            // tính package subtotal
            for (BookingCombos bc : combos) {
                if (bc.getTotalPrice() != null) {
                    packageSubtotal = packageSubtotal.add(bc.getTotalPrice());
                }
            }
        }

        // ================== CUSTOM MENU (MenuItems) ==================
        List<BookingMenuItems> items = null;
        try {
            items = bookingMenuItemsFacade.findByBookingId(id);
        } catch (Exception e) {
            // có thể log nếu muốn
        }

        if (items != null && !items.isEmpty()) {
            bookingMenuItems.addAll(items);

            for (BookingMenuItems bmi : items) {
                if (bmi.getTotalPrice() != null) {
                    menuSubtotal = menuSubtotal.add(bmi.getTotalPrice());
                }
            }
        }
    }

    // ---------------------------------------------------------------------
    // BASIC HELPERS
    // ---------------------------------------------------------------------
    public boolean isBookingFound() {
        return booking != null;
    }

    public Bookings getBooking() {
        return booking;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public String getBookingCode() {
        return booking != null ? booking.getBookingCode() : "";
    }

    public String getCustomerName() {
        if (booking == null || booking.getCustomerId() == null) {
            return "";
        }
        return booking.getCustomerId().getFullName();
    }

    public String getStatusLabel() {
        if (booking == null || booking.getBookingStatus() == null) {
            return "";
        }
        String s = booking.getBookingStatus().toUpperCase();
        switch (s) {
            case "CONFIRMED":
                return "Confirmed";
            case "PENDING":
                return "Pending";
            case "COMPLETED":
                return "Completed";
            case "CANCELLED":
                return "Cancelled";
            default:
                return booking.getBookingStatus();
        }
    }

    public String getLocationDisplay() {
        if (booking == null || booking.getLocationType() == null) {
            return "";
        }
        if ("RESTAURANT".equalsIgnoreCase(booking.getLocationType())) {
            return "At restaurant";
        } else if ("OUTSIDE".equalsIgnoreCase(booking.getLocationType())) {
            return "Outside catering";
        }
        return booking.getLocationType();
    }

    public String getPaymentStatusLabel() {
        if (booking == null || booking.getPaymentStatus() == null) {
            return "";
        }
        switch (booking.getPaymentStatus().toUpperCase()) {
            case "UNPAID":
                return "Unpaid";
            case "DEPOSIT_PAID":
                return "Deposit paid";
            case "PAID":
                return "Paid in full";
            case "REFUNDED":
                return "Refunded";
            default:
                return booking.getPaymentStatus();
        }
    }

    // ---------------------------------------------------------------------
    // MENU & PACKAGE GETTERS
    // ---------------------------------------------------------------------
    public List<BookingCombos> getBookingCombos() {
        return bookingCombos;
    }

    public List<BookingMenuItems> getBookingMenuItems() {
        return bookingMenuItems;
    }

    public boolean isHasPackage() {
        return bookingCombos != null && !bookingCombos.isEmpty();
    }

    public boolean isHasMenuItems() {
        return bookingMenuItems != null && !bookingMenuItems.isEmpty();
    }

    public String getSelectedPackageName() {
        if (selectedPackageName != null && !selectedPackageName.isBlank()) {
            return selectedPackageName;
        }
        if (isHasPackage()) {
            BookingCombos bc = bookingCombos.get(0);
            if (bc.getMenuCombos() != null && bc.getMenuCombos().getName() != null) {
                return bc.getMenuCombos().getName();
            }
        }
        return null;
    }

    public BigDecimal getPackageSubtotal() {
        return packageSubtotal;
    }

    public BigDecimal getMenuSubtotal() {
        return menuSubtotal;
    }
    
        // ====== Payment breakdown helpers ======

    public BigDecimal getOtherCharges() {
        if (booking == null || booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = booking.getTotalAmount();
        BigDecimal pkg = packageSubtotal != null ? packageSubtotal : BigDecimal.ZERO;
        BigDecimal menu = menuSubtotal != null ? menuSubtotal : BigDecimal.ZERO;

        BigDecimal other = total.subtract(pkg.add(menu));
        // Không cho âm, nếu lỡ tính lệch
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return other;
    }

    public boolean isHasOtherCharges() {
        return getOtherCharges().compareTo(BigDecimal.ZERO) > 0;
    }

}
