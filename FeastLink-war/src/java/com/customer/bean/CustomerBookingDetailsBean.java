package com.customer.bean;

import com.mypack.entity.Bookings;
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

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        Map<String, String> params =
                ctx.getExternalContext().getRequestParameterMap();

        String idParam = params.get("bookingId");
        if (idParam != null && !idParam.isBlank()) {
            try {
                bookingId = Long.parseLong(idParam);
            } catch (NumberFormatException ignored) {
            }
        }

        if (bookingId == null) {
            Object obj = ctx.getExternalContext()
                    .getSessionMap().get("selectedBookingId");
            if (obj instanceof Long) {
                bookingId = (Long) obj;
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
        NumberFormat nf = NumberFormat.getCurrencyInstance(locale);
        return nf.format(value);
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
        LocalDate event = booking.getEventDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
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

    private String safe(String s) {
        return (s == null) ? "" : s;
    }
}
