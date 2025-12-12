package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Users;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;
import java.math.BigDecimal;
import java.util.Collection;

import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final SimpleDateFormat fullDateFmt =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", locale);
    private final SimpleDateFormat timeFmt =
            new SimpleDateFormat("HH:mm", locale);
    private final SimpleDateFormat dateTimeFmt =
            new SimpleDateFormat("dd MMM yyyy HH:mm", locale);
    private final SimpleDateFormat shortDateFmt =
            new SimpleDateFormat("EEE, dd MMM yyyy", locale);

    private final NumberFormat currencyFmt =
            NumberFormat.getCurrencyInstance(locale);
        // ====== Payment breakdown subtotals ======
    private BigDecimal packageSubtotal = BigDecimal.ZERO;
    private BigDecimal menuSubtotal = BigDecimal.ZERO;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        Map<String, String> params =
                ctx.getExternalContext().getRequestParameterMap();

        // Ưu tiên ?bookingId= trên URL
        String idParam = params.get("bookingId");
        if (idParam != null && !idParam.isBlank()) {
            try {
                bookingId = Long.parseLong(idParam);
            } catch (NumberFormatException ignored) {
            }
        }

        // Nếu chưa có thì lấy từ session "selectedBookingId"
        if (bookingId == null) {
            Object obj = ctx.getExternalContext()
                    .getSessionMap().get("selectedBookingId");
            if (obj instanceof Long) {
                bookingId = (Long) obj;
            } else if (obj instanceof Integer) {
                bookingId = ((Integer) obj).longValue();
            } else if (obj instanceof String) {
                try {
                    bookingId = Long.parseLong((String) obj);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (bookingId != null) {
            booking = bookingsFacade.find(bookingId);
        }
    }

    public Bookings getBooking() {
        return booking;
    }

    // ========= FORMATTER =========

    public String formatDate() {
        if (booking == null || booking.getEventDate() == null) return "";
        return fullDateFmt.format(booking.getEventDate());
    }

    public String formatTimeRange() {
        if (booking == null) return "";
        String start = (booking.getStartTime() != null)
                ? timeFmt.format(booking.getStartTime())
                : "";
        String end = (booking.getEndTime() != null)
                ? timeFmt.format(booking.getEndTime())
                : "";
        if (!start.isEmpty() && !end.isEmpty()) {
            return start + " – " + end;
        }
        return start + end;
    }

    public String formatDateTime(Date d) {
        if (d == null) return "";
        return dateTimeFmt.format(d);
    }

    public String formatMoney(BigDecimal value) {
        if (value == null) return "$0.00";
        return currencyFmt.format(value);
    }

    // ========= LABELS =========

    public String getBookingStatusLabel() {
        if (booking == null || booking.getBookingStatus() == null) return "";
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
        if (booking == null || booking.getPaymentStatus() == null) return "";
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

    public String getLocationDisplay() {
        if (booking == null) return "";
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

        // eventDate hiện là java.sql.Date -> không dùng toInstant() trực tiếp
        Date eventDate = booking.getEventDate();
        LocalDate event;

        if (eventDate instanceof java.sql.Date) {
            event = ((java.sql.Date) eventDate).toLocalDate();
        } else {
            event = eventDate.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }

        LocalDate deadline = event.minusDays(3);

        Date deadlineDate = Date.from(
                deadline.atStartOfDay(ZoneId.systemDefault()).toInstant()
        );

        String deadlineStr = shortDateFmt.format(deadlineDate);
        return "Free cancellation is available until " + deadlineStr
                + ". After this date, your deposit may become non-refundable "
                + "based on the venue's policy.";
    }

    public boolean hasCustomerNote() {
        return booking != null && booking.getNote() != null
                && !booking.getNote().trim().isEmpty();
    }

    public boolean hasCancelInfo() {
        return booking != null && booking.getCancelReason() != null
                && !booking.getCancelReason().trim().isEmpty();
    }

        // ========= CONTACT INFO =========

    private Users getCustomer() {
        return booking != null ? booking.getCustomerId() : null;
    }

    public String getContactFullName() {
        if (booking == null) {
            return "";
        }

        // ƯU TIÊN: tên liên hệ mà user nhập khi booking
        String name = safe(booking.getContactFullName());
        if (!name.isEmpty()) {
            return name;
        }

        // Fallback: tên trong account
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
        // Chưa có cột Company riêng -> để trống
        return "";
    }

    public String getCompanyTaxId() {
        // Chưa có cột Tax ID -> để trống
        return "";
    }

    public String getBillingAddress() {
        Users u = getCustomer();
        return u != null ? safe(u.getAddress()) : "";
    }


    // ========= SPECIAL REQUESTS & PAYMENT TYPE =========

    public String getSpecialRequests() {
        if (booking == null) return "";
        String note = safe(booking.getNote());
        if (note.isEmpty()) {
            return "No additional notes were provided for this booking.";
        }
        return note;
    }

    public String getPaymentTypeLabel() {
        if (booking == null) return "";
        BigDecimal remaining = booking.getRemainingAmount();
        if (remaining != null && remaining.compareTo(BigDecimal.ZERO) > 0) {
            return "Pay deposit only (30%)";
        }
        return "Pay full amount now";
    }
    
        // =========================================
    //  PRICE BREAKDOWN: PACKAGE / MENU / OTHER
    // =========================================

    private void calculatePriceBreakdown() {
        packageSubtotal = BigDecimal.ZERO;
        menuSubtotal = BigDecimal.ZERO;

        if (booking == null) {
            return;
        }

        // Tính subtotal cho package từ BookingCombos.TotalPrice
        Collection<BookingCombos> comboColl = booking.getBookingCombosCollection();
        if (comboColl != null) {
            for (BookingCombos bc : comboColl) {
                if (bc != null && bc.getTotalPrice() != null) {
                    packageSubtotal = packageSubtotal.add(bc.getTotalPrice());
                }
            }
        }

        // Tính subtotal cho custom menu từ BookingMenuItems.TotalPrice
        Collection<BookingMenuItems> menuColl = booking.getBookingMenuItemsCollection();
        if (menuColl != null) {
            for (BookingMenuItems bmi : menuColl) {
                if (bmi != null && bmi.getTotalPrice() != null) {
                    menuSubtotal = menuSubtotal.add(bmi.getTotalPrice());
                }
            }
        }
    }

    public BigDecimal getPackageSubtotal() {
        return packageSubtotal != null ? packageSubtotal : BigDecimal.ZERO;
    }

    public BigDecimal getMenuSubtotal() {
        return menuSubtotal != null ? menuSubtotal : BigDecimal.ZERO;
    }

    public BigDecimal getOtherCharges() {
        if (booking == null || booking.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = booking.getTotalAmount();
        BigDecimal pkg = getPackageSubtotal();
        BigDecimal menu = getMenuSubtotal();

        BigDecimal other = total.subtract(pkg.add(menu));
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return other;
    }

    public boolean isHasPackage() {
        return getPackageSubtotal().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isHasMenuItems() {
        return getMenuSubtotal().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isHasOtherCharges() {
        return getOtherCharges().compareTo(BigDecimal.ZERO) > 0;
    }


    // ========= HELPERS =========

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}
