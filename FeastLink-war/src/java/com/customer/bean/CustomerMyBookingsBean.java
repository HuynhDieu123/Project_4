package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.ServiceTypes;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Named("customerMyBookingsBean")
@RequestScoped
public class CustomerMyBookingsBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private List<Bookings> myBookings = new ArrayList<>();

    private final Locale displayLocale = Locale.US;
    private final SimpleDateFormat shortDateFmt =
            new SimpleDateFormat("EEE, dd MMM yyyy", displayLocale);
    private final SimpleDateFormat longDateFmt  =
            new SimpleDateFormat("EEEE, dd MMMM yyyy", displayLocale);
    private final SimpleDateFormat timeFmt      =
            new SimpleDateFormat("HH:mm", displayLocale);

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Object userObj = ctx.getExternalContext()
                .getSessionMap()
                .get("currentUser");

        if (userObj instanceof Users) {
            Users currentUser = (Users) userObj;
            List<Bookings> all = bookingsFacade.findAll();
            if (all != null) {
                for (Bookings b : all) {
                    if (b.getCustomerId() != null
                            && b.getCustomerId().getUserId()
                            .equals(currentUser.getUserId())) {
                        myBookings.add(b);
                    }
                }
            }
        }
    }

    public List<Bookings> getMyBookings() {
        return myBookings;
    }

    // ========== STATS ==========

    public int getUpcomingCount() {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Bookings b : myBookings) {
            String status = safe(b.getBookingStatus());
            LocalDate event = toLocalDate(b.getEventDate());
            boolean isFutureOrToday = (event == null || !event.isBefore(today));
            if (isFutureOrToday &&
                    ("PENDING".equalsIgnoreCase(status)
                            || "CONFIRMED".equalsIgnoreCase(status))) {
                count++;
            }
        }
        return count;
    }

    public int getCompletedCount() {
        int count = 0;
        for (Bookings b : myBookings) {
            if ("COMPLETED".equalsIgnoreCase(safe(b.getBookingStatus()))) {
                count++;
            }
        }
        return count;
    }

    public BigDecimal getTotalSpent() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bookings b : myBookings) {
            if ("COMPLETED".equalsIgnoreCase(safe(b.getBookingStatus()))
                    && b.getTotalAmount() != null) {
                sum = sum.add(b.getTotalAmount());
            }
        }
        return sum;
    }

    public String getFormattedTotalSpent() {
        BigDecimal total = getTotalSpent();
        NumberFormat nf = NumberFormat.getCurrencyInstance(displayLocale);
        return nf.format(total != null ? total : BigDecimal.ZERO);
    }

    // ========== FORMAT HỖ TRỢ ==========

    private LocalDate toLocalDate(Date date) {
        if (date == null) return null;
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public String formatDateShort(Date date) {
        return (date != null) ? shortDateFmt.format(date) : "";
    }

    public String formatDateLong(Date date) {
        return (date != null) ? longDateFmt.format(date) : "";
    }

    public String formatTime(Date date) {
        return (date != null) ? timeFmt.format(date) : "";
    }

    public String formatMoney(BigDecimal value) {
        if (value == null) return "$0.00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(displayLocale);
        return nf.format(value);
    }

    // ========== HELPER DÙNG TRONG EL ==========

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    // Cho phép hủy: đang Pending/Confirmed và còn cách ngày tiệc >= 3 ngày
    public boolean canCancel(Bookings b) {
        if (b == null) return false;
        String status = safe(b.getBookingStatus()).toUpperCase();
        if (!("PENDING".equals(status) || "CONFIRMED".equals(status))) {
            return false;
        }
        return canCancelByDate(b);
    }

    private boolean canCancelByDate(Bookings b) {
        Date date = b.getEventDate();
        if (date == null) return true;  // demo: nếu thiếu ngày thì cho hủy
        LocalDate event = toLocalDate(date);
        LocalDate today = LocalDate.now();
        // phải còn ít nhất 3 ngày
        return today.isBefore(event.minusDays(3));
    }

    public boolean isRemainingPositive(Bookings b) {
        if (b == null || b.getRemainingAmount() == null) return false;
        return b.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean needsPayment(Bookings b) {
        if (b == null) return false;
        String status = safe(b.getBookingStatus()).toUpperCase();
        String payment = safe(b.getPaymentStatus()).toUpperCase();
        return "CONFIRMED".equals(status) && !"PAID".equals(payment);
    }

    public String statusMessage(Bookings b) {
        if (b == null) return "";
        String status = safe(b.getBookingStatus()).toUpperCase();

        if ("CONFIRMED".equals(status)) {
            return "Free cancellation up to 3 days before the event.";
        }
        if ("PENDING".equals(status)) {
            return "We'll confirm your request within 24–48 hours.";
        }
        if ("COMPLETED".equals(status)) {
            return "You can rate this venue and download the invoice.";
        }
        if ("CANCELLED".equals(status)) {
            String reason = safe(b.getCancelReason());
            return reason.isEmpty()
                    ? "This booking was cancelled."
                    : "Cancelled: " + reason;
        }
        return "";
    }

    public String locationDisplay(Bookings b) {
        if (b == null) return "";
        String locType = safe(b.getLocationType());
        if ("AT_RESTAURANT".equalsIgnoreCase(locType)) {
            return "At restaurant";
        }
        String outside = safe(b.getOutsideAddress());
        return outside.isEmpty() ? locType : outside;
    }

    public String eventTypeDisplay(Bookings b) {
        if (b == null) return "";
        EventTypes et = b.getEventTypeId();
        return (et != null) ? safe(et.getName()) : "";
    }

    public String serviceTypeDisplay(Bookings b) {
        if (b == null) return "";
        ServiceTypes st = b.getServiceTypeId();
        return (st != null) ? safe(st.getName()) : "";
    }

    // ========== ACTION: VIEW DETAILS ==========

    public String viewDetails(Long bookingId) {
        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Missing booking id.");
            return null;
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().getSessionMap()
                .put("selectedBookingId", bookingId);
        // sang trang chi tiết
        return "/Customer/booking-details";
    }

    // ========== ACTION: CANCEL BOOKING ==========

    public String cancelBooking(Long bookingId) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Missing booking id.");
            return null;
        }

        Bookings booking = bookingsFacade.find(bookingId);
        if (booking == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Booking not found.");
            return null;
        }

        // Check quyền: chỉ được hủy booking của chính mình
        Object userObj = ctx.getExternalContext()
                .getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "You need to log in again.");
            return "/login";
        }

        Users currentUser = (Users) userObj;
        if (booking.getCustomerId() == null
                || !booking.getCustomerId().getUserId()
                .equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "You cannot cancel this booking.");
            return null;
        }

        // Check rule hủy
        if (!canCancel(booking)) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Cannot cancel",
                    "This booking can no longer be cancelled online.");
            return null;
        }

        // Cập nhật trạng thái
        booking.setBookingStatus("CANCELLED");
        booking.setCancelReason("Cancelled by customer via My Bookings page.");
        booking.setCancelTime(new Date());
        booking.setUpdatedAt(new Date());

        bookingsFacade.edit(booking);

        addMessage(FacesMessage.SEVERITY_INFO,
                "Booking cancelled",
                "Your booking has been cancelled. Refund policy will follow the venue's rules.");

        // load lại trang danh sách
        return "/Customer/my-bookings";
    }

    private void addMessage(FacesMessage.Severity severity,
                            String summary,
                            String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(severity, summary, detail));
    }
}
