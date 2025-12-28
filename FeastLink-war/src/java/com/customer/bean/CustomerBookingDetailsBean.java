package com.customer.bean;

import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;
import com.mypack.entity.Bookings;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

@Named("customerBookingDetailsBean")
@RequestScoped
public class CustomerBookingDetailsBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private Long bookingId;
    private Bookings booking;

    private final Locale locale = Locale.US;

    private final SimpleDateFormat fullDateFmt
            = new SimpleDateFormat("EEEE, dd MMMM yyyy", locale);
    private final SimpleDateFormat timeFmt
            = new SimpleDateFormat("HH:mm", locale);
    private final SimpleDateFormat dateTimeFmt
            = new SimpleDateFormat("dd MMM yyyy HH:mm", locale);
    private final SimpleDateFormat shortDateFmt
            = new SimpleDateFormat("EEE, dd MMM yyyy", locale);

    private final NumberFormat currencyFmt
            = NumberFormat.getCurrencyInstance(locale);

    // ====== Payment breakdown (aligned to Manager view) ======
    // packagePerGuest: base $/guest (sum of BookingCombos.totalPrice)
    // menuSubtotal: total $ of menu items (sum of BookingMenuItems.totalPrice)
    private BigDecimal packagePerGuest = BigDecimal.ZERO;
    private BigDecimal menuSubtotal = BigDecimal.ZERO;

    private String idParam;
    private boolean notFound;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

        // 1) ưu tiên URL ?bookingId=
        String urlId = params.get("bookingId");
        if (urlId != null && !urlId.isBlank()) {
            try {
                bookingId = Long.parseLong(urlId.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        // 2) fallback session selectedBookingId
        if (bookingId == null) {
            Object obj = ctx.getExternalContext().getSessionMap().get("selectedBookingId");
            if (obj instanceof Long) {
                bookingId = (Long) obj;
            } else if (obj instanceof Integer) {
                bookingId = ((Integer) obj).longValue();
            } else if (obj instanceof String) {
                try {
                    bookingId = Long.parseLong(((String) obj).trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (bookingId != null) {
            booking = bookingsFacade.find(bookingId);
            if (booking != null) {
                calculatePriceBreakdown();
            }
        }
    }

    // ===================== GETTERS BASIC =====================
    public Bookings getBooking() {
        return booking;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getIdParam() {
        return idParam;
    }

    public void setIdParam(String idParam) {
        this.idParam = idParam;
    }

    public boolean isNotFound() {
        return notFound;
    }

    // ===================== FORMATTER =====================
    public String formatDate() {
        if (booking == null || booking.getEventDate() == null) {
            return "";
        }
        return fullDateFmt.format(booking.getEventDate());
    }

    public String formatTimeRange() {
        if (booking == null) {
            return "";
        }
        String start = (booking.getStartTime() != null) ? timeFmt.format(booking.getStartTime()) : "";
        String end = (booking.getEndTime() != null) ? timeFmt.format(booking.getEndTime()) : "";
        if (!start.isEmpty() && !end.isEmpty()) {
            return start + " – " + end;
        }
        return start + end;
    }

    public String formatDateTime(Date d) {
        if (d == null) {
            return "";
        }
        return dateTimeFmt.format(d);
    }

    public String formatMoney(BigDecimal value) {
        if (value == null) {
            return "$0.00";
        }
        return currencyFmt.format(value);
    }

    // ===================== LABELS =====================
    public String getBookingStatusLabel() {
        if (booking == null || booking.getBookingStatus() == null) {
            return "";
        }
        String s = booking.getBookingStatus().toUpperCase();
        switch (s) {
            case "PENDING":
                return "Pending";
            case "CONFIRMED":
                return "Confirmed";
            case "COMPLETED":
                return "Completed";
            case "CANCELLED":
                return "Cancelled";
            default:
                return booking.getBookingStatus();
        }
    }

    public String getPaymentStatusLabel() {
        if (booking == null || booking.getPaymentStatus() == null) {
            return "";
        }
        String s = booking.getPaymentStatus().toUpperCase();
        switch (s) {
            case "UNPAID":
                return "Unpaid";
            case "DEPOSIT_PAID":
                return "Deposit Paid";
            case "PAID":
                return "Paid in Full";
            default:
                return booking.getPaymentStatus();
        }
    }

    // ===================== LOCATION / POLICY =====================
    public String getLocationDisplay() {
        if (booking == null) {
            return "";
        }
        String locType = safe(booking.getLocationType());

        if ("AT_RESTAURANT".equalsIgnoreCase(locType)) {
            if (booking.getRestaurantId() != null) {
                return "At " + safe(booking.getRestaurantId().getName());
            }
            return "At restaurant";
        }

        String outside = safe(booking.getOutsideAddress());
        return outside.isEmpty() ? "Outside catering" : outside;
    }

    public String getCancellationPolicyText() {
        if (booking == null || booking.getEventDate() == null) {
            return "Please contact the venue directly for detailed cancellation policy.";
        }

        Date eventDate = booking.getEventDate();
        LocalDate event;

        if (eventDate instanceof java.sql.Date) {
            event = ((java.sql.Date) eventDate).toLocalDate();
        } else {
            event = eventDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        LocalDate deadline = event.minusDays(3);
        Date deadlineDate = Date.from(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant());
        String deadlineStr = shortDateFmt.format(deadlineDate);

        return "Free cancellation is available until " + deadlineStr
                + ". After this date, your deposit may become non-refundable based on the venue's policy.";
    }

    // ===================== NOTES / CANCEL =====================
    public boolean hasCustomerNote() {
        return booking != null && booking.getNote() != null && !booking.getNote().trim().isEmpty();
    }

    public boolean hasCancelInfo() {
        return booking != null && booking.getCancelReason() != null && !booking.getCancelReason().trim().isEmpty();
    }

    public boolean isCancelled() {
        return booking != null
                && booking.getBookingStatus() != null
                && "CANCELLED".equalsIgnoreCase(booking.getBookingStatus());
    }

    public String getCancelReasonDisplay() {
        if (!isCancelled()) {
            return "";
        }

        String r = safe(booking.getCancelReason());
        if (r.isEmpty()) {
            try {
                r = safe(booking.getRejectReason());
            } catch (Exception ignore) {
            }
        }
        return r.isEmpty() ? "No reason was provided." : r;
    }

    public boolean isHasCancelTime() {
        return booking != null && booking.getCancelTime() != null;
    }

    // ===================== CONTACT INFO =====================
    private Users getCustomer() {
        return booking != null ? booking.getCustomerId() : null;
    }

    public String getContactFullName() {
        if (booking == null) {
            return "";
        }

        String name = safe(booking.getContactFullName());
        if (!name.isEmpty()) {
            return name;
        }

        Users u = getCustomer();
        return u != null ? safe(u.getFullName()) : "";
    }

    public String getContactEmail() {
        if (booking == null) {
            return "";
        }

        String email = safe(booking.getContactEmail());
        if (!email.isEmpty()) {
            return email;
        }

        Users u = getCustomer();
        return u != null ? safe(u.getEmail()) : "";
    }

    public String getContactPhone() {
        if (booking == null) {
            return "";
        }

        String phone = safe(booking.getContactPhone());
        if (!phone.isEmpty()) {
            return phone;
        }

        Users u = getCustomer();
        return u != null ? safe(u.getPhone()) : "";
    }

    public String getCompanyName() {
        return "";
    }

    public String getCompanyTaxId() {
        return "";
    }

    public String getBillingAddress() {
        Users u = getCustomer();
        return u != null ? safe(u.getAddress()) : "";
    }

    // ===================== SPECIAL REQUESTS / PAYMENT TYPE =====================
    public String getSpecialRequests() {
        if (booking == null) {
            return "";
        }
        String note = safe(booking.getNote());
        return note.isEmpty() ? "No additional notes were provided for this booking." : note;
    }

    public String getPaymentTypeLabel() {
        if (booking == null) {
            return "";
        }
        BigDecimal remaining = booking.getRemainingAmount();
        if (remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            return "Pay deposit only (30%)";
        }
        return "Pay full amount now";
    }

    // Optional: load by idParam (nếu bạn dùng ở chỗ khác)
    public void loadFromParam() {
        notFound = false;

        Long id;
        try {
            if (idParam == null || idParam.trim().isEmpty()) {
                notFound = true;
                return;
            }
            id = Long.valueOf(idParam.trim());
        } catch (Exception e) {
            notFound = true;
            return;
        }

        Bookings b = bookingsFacade.find(id);
        if (b == null) {
            notFound = true;
            return;
        }

        Object u = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("currentUser");
        if (u instanceof Users) {
            Users user = (Users) u;
            if (b.getCustomerId() == null || !b.getCustomerId().getUserId().equals(user.getUserId())) {
                notFound = true;
                return;
            }
        }

        this.booking = b;
        calculatePriceBreakdown();
    }

    // =========================================================
    //  PAYMENT SUMMARY (MATCH MANAGER)
    // =========================================================
    private void calculatePriceBreakdown() {
        packagePerGuest = BigDecimal.ZERO; // base $/guest
        menuSubtotal = BigDecimal.ZERO;    // total $ menu (all guests)

        if (booking == null) {
            return;
        }

        // Package base per guest: sum BookingCombos.totalPrice
        Collection<BookingCombos> comboColl = booking.getBookingCombosCollection();
        if (comboColl != null) {
            for (BookingCombos bc : comboColl) {
                if (bc != null && bc.getTotalPrice() != null) {
                    packagePerGuest = packagePerGuest.add(bc.getTotalPrice());
                }
            }
        }

        // Menu subtotal (all guests): sum BookingMenuItems.totalPrice
        Collection<BookingMenuItems> menuColl = booking.getBookingMenuItemsCollection();
        if (menuColl != null) {
            for (BookingMenuItems bmi : menuColl) {
                if (bmi != null && bmi.getTotalPrice() != null) {
                    menuSubtotal = menuSubtotal.add(bmi.getTotalPrice());
                }
            }
        }

        packagePerGuest = money(packagePerGuest);
        menuSubtotal = money(menuSubtotal);
    }

    public boolean isHasGuestCount() {
        if (booking == null) {
            return false;
        }
        Integer gc = booking.getGuestCount();
        return gc != null && gc > 0;
    }

    public boolean isHasPackage() {
        return getPackagePerGuest().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isHasMenuItems() {
        return getMenuSubtotal().compareTo(BigDecimal.ZERO) > 0;
    }

    // ---- Package
    public BigDecimal getPackagePerGuest() {
        return packagePerGuest != null ? money(packagePerGuest) : BigDecimal.ZERO;
    }

    public BigDecimal getPackageSubtotal() {
        if (!isHasGuestCount()) {
            return BigDecimal.ZERO;
        }
        BigDecimal perGuest = getPackagePerGuest();
        return money(perGuest.multiply(new BigDecimal(booking.getGuestCount())));
    }

    // ---- Menu
    public BigDecimal getMenuSubtotal() {
        return menuSubtotal != null ? money(menuSubtotal) : BigDecimal.ZERO;
    }

    public BigDecimal getMenuPerGuest() {
        if (!isHasGuestCount()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = getMenuSubtotal();
        return money(total.divide(new BigDecimal(booking.getGuestCount()), 2, RoundingMode.HALF_UP));
    }

    // ---- Food subtotal
    public BigDecimal getFoodSubtotal() {
        return money(getPackageSubtotal().add(getMenuSubtotal()));
    }

    // ---- Service Type (nếu có)
    public String getServiceTypeName() {
        try {
            if (booking != null && booking.getServiceTypeId() != null) {
                String n = booking.getServiceTypeId().getName();
                n = safe(n);
                return n.isEmpty() ? null : n;
            }
        } catch (Exception ignore) {
        }
        return null;
    }

    public boolean isHasServiceType() {
        String n = getServiceTypeName();
        return n != null && !n.isBlank();
    }

    public BigDecimal getServiceChargeRate() {
        String name = getServiceTypeName();
        if (name == null) {
            return BigDecimal.ZERO;
        }

        String s = name.trim().toUpperCase();
        switch (s) {
            case "STANDARD":
                return new BigDecimal("0.03");
            case "PREMIUM":
                return new BigDecimal("0.05");
            case "VIP":
                return new BigDecimal("0.08");
            default:
                return BigDecimal.ZERO;
        }
    }

    public String getServiceChargeRatePercent() {
        BigDecimal rate = getServiceChargeRate();
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            return "";
        }
        return rate.multiply(new BigDecimal("100"))
                .setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public BigDecimal getServiceChargeComputed() {
        BigDecimal rate = getServiceChargeRate();
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return money(getFoodSubtotal().multiply(rate));
    }

    public BigDecimal getServiceChargePerGuest() {
        if (!isHasGuestCount()) {
            return BigDecimal.ZERO;
        }
        return money(getServiceChargeComputed()
                .divide(new BigDecimal(booking.getGuestCount()), 2, RoundingMode.HALF_UP));
    }

    // ---- Other charges shown in summary
    public BigDecimal getOtherCharges() {
        if (booking == null || booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }

        // If service type selected => show computed service charge
        if (isHasServiceType() && getServiceChargeRate().compareTo(BigDecimal.ZERO) > 0) {
            return getServiceChargeComputed();
        }

        // Otherwise fallback = total - food subtotal
        BigDecimal total = booking.getTotalAmount();
        BigDecimal other = total.subtract(getFoodSubtotal());
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return money(other);
    }

    public boolean isHasOtherCharges() {
        return getOtherCharges().compareTo(BigDecimal.ZERO) > 0;
    }

    // ===================== HELPERS =====================
    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private BigDecimal money(BigDecimal v) {
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    // Extra: avoid NPE for repeat
    public Collection<BookingCombos> getBookingCombos() {
        if (booking == null || booking.getBookingCombosCollection() == null) {
            return Collections.emptyList();
        }
        return booking.getBookingCombosCollection();
    }

    public Collection<BookingMenuItems> getBookingMenuItems() {
        if (booking == null || booking.getBookingMenuItemsCollection() == null) {
            return Collections.emptyList();
        }
        return booking.getBookingMenuItemsCollection();
    }
}
