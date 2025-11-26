package com.customer.bean;

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
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Handle final booking confirmation from booking.xhtml (wizard).
 */
@Named("customerBookingBean")
@RequestScoped
public class CustomerBookingBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    // Fields bound from booking.xhtml (hidden inputs)
    private Long restaurantId;
    private String eventDateStr;   // yyyy-MM-dd
    private int guestCount;
    private String locationType;   // AT_RESTAURANT / AT_HOME
    private String outsideAddress;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    // ===== Getters / setters =====
    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getEventDateStr() {
        return eventDateStr;
    }

    public void setEventDateStr(String eventDateStr) {
        this.eventDateStr = eventDateStr;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getOutsideAddress() {
        return outsideAddress;
    }

    public void setOutsideAddress(String outsideAddress) {
        this.outsideAddress = outsideAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getRemainingAmount() {
        return remainingAmount;
    }

    public void setRemainingAmount(BigDecimal remainingAmount) {
        this.remainingAmount = remainingAmount;
    }

    // ===== Main action: save booking =====
    public String confirmBooking() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 1. Require login
        Users currentUser = (Users) ctx.getExternalContext()
                .getSessionMap()
                .get("currentUser");

        if (currentUser == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please sign in",
                    "You need to log in before making a booking."
            ));
            // quay về login (không faces-redirect)
            return "login";
        }

        try {
            // 2. Đọc request params làm fallback
            Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

            // restaurantId: ưu tiên field bind -> hidden -> query string
            if (restaurantId == null) {
                String rIdHidden = params.get("hf-restaurant-id");
                String rIdQuery = params.get("restaurantId");

                String raw = (rIdHidden != null && !rIdHidden.isBlank()) ? rIdHidden : rIdQuery;
                if (raw != null && !raw.isBlank()) {
                    try {
                        restaurantId = Long.parseLong(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // eventDateStr: ưu tiên field bind -> hidden -> ?date=
            if (eventDateStr == null || eventDateStr.isBlank()) {
                String dHidden = params.get("hf-event-date");
                String dQuery = params.get("date");
                if (dHidden != null && !dHidden.isBlank()) {
                    eventDateStr = dHidden;
                } else if (dQuery != null && !dQuery.isBlank()) {
                    eventDateStr = dQuery;
                }
            }

            // guest count default
            if (guestCount <= 0) {
                guestCount = 200; // demo: 20 bàn * 10 khách
            }

            if (locationType == null || locationType.isBlank()) {
                locationType = "AT_RESTAURANT";
            }

            // 3. Load restaurant
            Restaurants restaurant = null;
            if (restaurantId != null) {
                restaurant = restaurantsFacade.find(restaurantId);
            }

            // Fallback demo: lấy nhà hàng đầu tiên nếu vẫn null
            if (restaurant == null) {
                List<Restaurants> all = restaurantsFacade.findAll();
                if (all != null && !all.isEmpty()) {
                    restaurant = all.get(0);
                }
            }

            if (restaurant == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cannot find restaurant",
                        "There is no available venue to attach to this booking."
                ));
                return null;
            }

            // 4. Event date
            Date eventDate;
            if (eventDateStr != null && !eventDateStr.isBlank()) {
                try {
                    LocalDate ld = LocalDate.parse(eventDateStr);
                    eventDate = java.sql.Date.valueOf(ld);
                } catch (Exception ex) {
                    LocalDate ld = LocalDate.now().plusDays(7);
                    eventDate = java.sql.Date.valueOf(ld);
                }
            } else {
                LocalDate ld = LocalDate.now().plusDays(7);
                eventDate = java.sql.Date.valueOf(ld);
            }

            // 5. Build entity Bookings
            Bookings booking = new Bookings();
            booking.setBookingCode(generateBookingCode());
            booking.setCustomerId(currentUser);
            booking.setRestaurantId(restaurant);
            booking.setEventDate(eventDate);
            booking.setGuestCount(guestCount);
            booking.setLocationType(locationType);

            booking.setOutsideAddress(
                    (outsideAddress != null && !outsideAddress.isBlank())
                    ? outsideAddress
                    : null
            );

            if (totalAmount != null) {
                booking.setTotalAmount(totalAmount);
            }
            if (depositAmount != null) {
                booking.setDepositAmount(depositAmount);
            }
            if (remainingAmount != null) {
                booking.setRemainingAmount(remainingAmount);
            }

            booking.setBookingStatus("PENDING");
            booking.setPaymentStatus("UNPAID");
            booking.setCreatedAt(new Date());

            // 6. Lưu DB
            bookingsFacade.create(booking);

            // 7. Thông báo (ở lại hoặc tự điều hướng)
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Booking request sent",
                    "Your booking has been created successfully. Our team will contact you for confirmation."
            ));

            // Về trang index customer
            return "/Customer/index";

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
