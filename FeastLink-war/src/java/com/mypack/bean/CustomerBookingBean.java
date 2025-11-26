package com.mypack.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import jakarta.faces.application.FacesMessage;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Map;

@Named("customerBookingBean")
@RequestScoped
public class CustomerBookingBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    // ===== dữ liệu lấy từ form/JS =====
    private Long restaurantId;
    private String eventDateStr;      // yyyy-MM-dd
    private int guestCount;
    private String locationType;      // AT_RESTAURANT / OUTSIDE
    private String outsideAddress;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    // ===== GET/SET =====
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }

    public String getEventDateStr() { return eventDateStr; }
    public void setEventDateStr(String eventDateStr) { this.eventDateStr = eventDateStr; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getOutsideAddress() { return outsideAddress; }
    public void setOutsideAddress(String outsideAddress) { this.outsideAddress = outsideAddress; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    // ===== ACTION: lưu booking =====
    public String confirmBooking() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Lấy user đang đăng nhập từ session (loginBean đã set "currentUser")
        Users currentUser = (Users) ctx.getExternalContext()
                .getSessionMap()
                .get("currentUser");

        if (currentUser == null) {
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please sign in",
                    "You need to log in before making a booking."
            ));
            return "login?faces-redirect=true";
        }

        // Nếu hidden field không có restaurantId / date, lấy thêm từ query string (?restaurantId=...&date=...)
        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
        if (restaurantId == null) {
            String rId = params.get("restaurantId");
            if (rId != null && !rId.isBlank()) {
                try { restaurantId = Long.parseLong(rId); } catch (NumberFormatException ignored) {}
            }
        }
        if (eventDateStr == null || eventDateStr.isBlank()) {
            eventDateStr = params.get("date");    // có thể null
        }

        try {
            Bookings booking = new Bookings();

            // Mã booking đơn giản
            booking.setBookingCode(generateBookingCode());

            // Event date
            Date eventDate;
            if (eventDateStr != null && !eventDateStr.isBlank()) {
                try {
                    LocalDate ld = LocalDate.parse(eventDateStr);
                    eventDate = java.sql.Date.valueOf(ld);
                } catch (DateTimeParseException ex) {
                    eventDate = new Date();
                }
            } else {
                eventDate = new Date();
            }
            booking.setEventDate(eventDate);

            // Các thông tin chính
            booking.setGuestCount(guestCount > 0 ? guestCount : 100);
            booking.setLocationType(
                    (locationType != null && !locationType.isBlank())
                            ? locationType
                            : "AT_RESTAURANT"
            );
            booking.setOutsideAddress(outsideAddress);

            booking.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
            booking.setDepositAmount(depositAmount != null ? depositAmount : BigDecimal.ZERO);
            booking.setRemainingAmount(remainingAmount != null ? remainingAmount : BigDecimal.ZERO);

            booking.setBookingStatus("PENDING");
            booking.setPaymentStatus("PENDING");
            booking.setCreatedAt(new Date());

            // Quan hệ: Restaurant + Customer
            if (restaurantId != null) {
                Restaurants r = restaurantsFacade.find(restaurantId);
                booking.setRestaurantId(r);
            }
            booking.setCustomerId(currentUser);

            // Lưu DB
            bookingsFacade.create(booking);

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Booking created",
                    "Your booking request has been sent. We will contact you soon."
            ));

            // Ở lại trang hiện tại, chỉ show message
            return null;
        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "Could not create booking: " + ex.getMessage()
            ));
            return null;
        }
    }

    private String generateBookingCode() {
        return "BK" + System.currentTimeMillis();
    }
}
