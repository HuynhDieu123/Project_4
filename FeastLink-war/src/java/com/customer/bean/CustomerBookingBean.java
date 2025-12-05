package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.entity.EventTypes;
import com.mypack.entity.ServiceTypes;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import com.mypack.sessionbean.ServiceTypesFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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

    @EJB
    private EventTypesFacadeLocal eventTypesFacade;

    @EJB
    private ServiceTypesFacadeLocal serviceTypesFacade;

    // ====== Fields bound từ booking.xhtml (hidden inputs / form) ======
    private Long restaurantId;
    private String eventDateStr;   // yyyy-MM-dd (URL / hidden field)
    private int guestCount;
    private String locationType;   // AT_RESTAURANT / OUTSIDE
    private String outsideAddress;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    // ====== Thông tin hiển thị trên booking.xhtml ======
    private Restaurants restaurant;        // venue đã load
    private String restaurantName;
    private String restaurantAddress;

    // Loại tiệc & gói service lấy từ UI (hidden input)
    private String eventTypeKey;  // label loại tiệc từ UI (vd: "Wedding", "Birthday")
    private String serviceLevel;  // standard / premium / vip / exclusive

    // Contact information (step 2)
    private String contactFullName;
    private String contactEmail;
    private String contactPhone;

    // ====== INIT: load data từ param & DB ======
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

        // Lấy current user để prefill contact
        Users currentUser = (Users) ctx.getExternalContext()
                .getSessionMap()
                .get("currentUser");

        if (currentUser != null) {
            if (!notBlank(contactFullName)) {
                contactFullName = safe(currentUser.getFullName());
            }
            if (!notBlank(contactEmail)) {
                contactEmail = safe(currentUser.getEmail());
            }
            if (!notBlank(contactPhone)) {
                contactPhone = safe(currentUser.getPhone());
            }
        }

        // ---- RestaurantId ----
        if (restaurantId == null) {
            String rIdParam = params.get("restaurantId");
            if (rIdParam == null || rIdParam.isBlank()) {
                // fallback: có thể lưu trong session
                Object obj = ctx.getExternalContext().getSessionMap().get("selectedRestaurantId");
                if (obj instanceof Long) {
                    restaurantId = (Long) obj;
                } else if (obj instanceof Integer) {
                    restaurantId = ((Integer) obj).longValue();
                }
            } else {
                try {
                    restaurantId = Long.parseLong(rIdParam);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // ---- Load restaurant từ DB ----
        if (restaurantId != null) {
            restaurant = restaurantsFacade.find(restaurantId);
        }

        // Fallback demo: nếu vẫn null, lấy nhà hàng đầu tiên
        if (restaurant == null) {
            List<Restaurants> all = restaurantsFacade.findAll();
            if (all != null && !all.isEmpty()) {
                restaurant = all.get(0);
                restaurantId = restaurant.getRestaurantId();
            }
        }

        if (restaurant != null) {
            restaurantName = safe(restaurant.getName());
            // tuỳ entity: Address / FullAddress / ...
            restaurantAddress = safe(restaurant.getAddress());
        }

        // ---- Event date ----
        if (eventDateStr == null || eventDateStr.isBlank()) {
            String dParam = params.get("date");
            if (dParam != null && !dParam.isBlank()) {
                eventDateStr = dParam;
            }
        }

        // ---- Guest count ----
        if (guestCount <= 0) {
            String guestsParam = params.get("guests");
            if (guestsParam != null && !guestsParam.isBlank()) {
                try {
                    guestCount = Integer.parseInt(guestsParam);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (guestCount <= 0) {
            // default demo: 20 bàn * 10 khách
            guestCount = 200;
        }

        // ---- Location type ----
        if (locationType == null || locationType.isBlank()) {
            locationType = "AT_RESTAURANT";
        }
    }

    // ====== Getters / setters cho JSF ======
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

    // ====== Getters dùng cho booking.xhtml (hiển thị) ======
    public String getRestaurantName() {
        return restaurantName;
    }

    public String getRestaurantAddress() {
        return restaurantAddress;
    }

    public Restaurants getRestaurant() {
        return restaurant;
    }

    public String getEventTypeKey() {
        return eventTypeKey;
    }

    public void setEventTypeKey(String eventTypeKey) {
        this.eventTypeKey = eventTypeKey;
    }

    public String getServiceLevel() {
        return serviceLevel;
    }

    public void setServiceLevel(String serviceLevel) {
        this.serviceLevel = serviceLevel;
    }

    // Contact info
    public String getContactFullName() {
        return contactFullName;
    }

    public void setContactFullName(String contactFullName) {
        this.contactFullName = contactFullName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    // ====== Main action: save booking ======
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
                    "You need to log in before making a booking. After signing in, you can view your booking history, manage or cancel reservations, and track payments easily."
            ));
            return "login"; // hoặc "/Customer/login?faces-redirect=true"
        }

        try {
            Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

            // ===== Fallback đọc thêm từ request params khi field chưa bind =====
            // restaurantId
            if (restaurantId == null) {
                String rHidden = params.get("hf-restaurant-id");
                String rQuery = params.get("restaurantId");
                String raw = (notBlank(rHidden)) ? rHidden : rQuery;
                if (notBlank(raw)) {
                    try {
                        restaurantId = Long.parseLong(raw);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // eventDateStr
            if (!notBlank(eventDateStr)) {
                String dHidden = params.get("hf-event-date");
                String dQuery = params.get("date");
                if (notBlank(dHidden)) {
                    eventDateStr = dHidden;
                } else if (notBlank(dQuery)) {
                    eventDateStr = dQuery;
                }
            }

            // guestCount
            if (guestCount <= 0) {
                String gHidden = params.get("hf-guest-count");
                if (notBlank(gHidden)) {
                    try {
                        guestCount = Integer.parseInt(gHidden);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (guestCount <= 0) {
                guestCount = 200;
            }

            // locationType
            if (!notBlank(locationType)) {
                String locHidden = params.get("hf-location-type");
                if (notBlank(locHidden)) {
                    locationType = locHidden;
                } else {
                    locationType = "AT_RESTAURANT";
                }
            }

            // outsideAddress
            if (!notBlank(outsideAddress)) {
                String addrHidden = params.get("hf-outside-address");
                if (notBlank(addrHidden)) {
                    outsideAddress = addrHidden;
                }
            }

            // total / deposit / remaining
            if (totalAmount == null) {
                String tHidden = params.get("hf-total-amount");
                totalAmount = parseBigDecimalSafe(tHidden);
            }

            // eventTypeKey
            if (!notBlank(eventTypeKey)) {
                String eHidden = params.get("hf-event-type");
                if (notBlank(eHidden)) {
                    eventTypeKey = eHidden;
                }
            }

            // serviceLevel
            if (!notBlank(serviceLevel)) {
                String sHidden = params.get("hf-service-level");
                if (notBlank(sHidden)) {
                    serviceLevel = sHidden;
                }
            }

            if (depositAmount == null) {
                String dHidden = params.get("hf-deposit-amount");
                depositAmount = parseBigDecimalSafe(dHidden);
            }
            if (remainingAmount == null) {
                String rHidden = params.get("hf-remaining-amount");
                remainingAmount = parseBigDecimalSafe(rHidden);
            }

            // ===== Contact information từ form =====
            // (giả sử 3 input trong booking.xhtml có name: contact-fullname, contact-email, contact-phone)
            if (!notBlank(contactFullName)) {
                String cName = params.get("contact-fullname");
                if (!notBlank(cName)) {
                    cName = params.get("contactFullName");
                }
                if (notBlank(cName)) {
                    contactFullName = cName.trim();
                }
            }

            if (!notBlank(contactEmail)) {
                String cEmail = params.get("contact-email");
                if (!notBlank(cEmail)) {
                    cEmail = params.get("contactEmail");
                }
                if (notBlank(cEmail)) {
                    contactEmail = cEmail.trim();
                }
            }

            if (!notBlank(contactPhone)) {
                String cPhone = params.get("contact-phone");
                if (!notBlank(cPhone)) {
                    cPhone = params.get("contactPhone");
                }
                if (notBlank(cPhone)) {
                    contactPhone = cPhone.trim();
                }
            }

            // fallback cuối: nếu vẫn rỗng thì lấy từ profile
            if (currentUser != null) {
                if (!notBlank(contactFullName)) {
                    contactFullName = safe(currentUser.getFullName());
                }
                if (!notBlank(contactEmail)) {
                    contactEmail = safe(currentUser.getEmail());
                }
                if (!notBlank(contactPhone)) {
                    contactPhone = safe(currentUser.getPhone());
                }
            }

            // 2. Load restaurant (nếu chưa có)
            if (restaurant == null && restaurantId != null) {
                restaurant = restaurantsFacade.find(restaurantId);
            }

            // Fallback demo: lấy nhà hàng đầu tiên nếu vẫn null
            if (restaurant == null) {
                List<Restaurants> all = restaurantsFacade.findAll();
                if (all != null && !all.isEmpty()) {
                    restaurant = all.get(0);
                    restaurantId = restaurant.getRestaurantId();
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

            // 3. Event date + default time 18:00–22:00
            LocalDate eventLocalDate;
            if (notBlank(eventDateStr)) {
                try {
                    eventLocalDate = LocalDate.parse(eventDateStr);
                } catch (Exception ex) {
                    eventLocalDate = LocalDate.now().plusDays(7);
                }
            } else {
                eventLocalDate = LocalDate.now().plusDays(7);
            }

            Date eventDate = java.sql.Date.valueOf(eventLocalDate);

            ZoneId zone = ZoneId.systemDefault();
            LocalDateTime startLdt = LocalDateTime.of(eventLocalDate, LocalTime.of(18, 0));
            LocalDateTime endLdt = LocalDateTime.of(eventLocalDate, LocalTime.of(22, 0));
            Date startTime = Date.from(startLdt.atZone(zone).toInstant());
            Date endTime = Date.from(endLdt.atZone(zone).toInstant());

            // 3.1 Resolve EventTypes & ServiceTypes từ DB
            EventTypes selectedEventType = resolveEventType();
            ServiceTypes selectedServiceType = resolveServiceType();

            // special requests từ step 2
            String specialRequests = params.get("special-requests");

            // 4. Build entity Bookings
            Bookings booking = new Bookings();
            booking.setBookingCode(generateBookingCode());
            booking.setCustomerId(currentUser);
            booking.setRestaurantId(restaurant);
            booking.setEventDate(eventDate);
            booking.setGuestCount(guestCount);
            booking.setLocationType(locationType);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);

            // lưu special requests vào Note
            if (notBlank(specialRequests)) {
                booking.setNote(specialRequests.trim());
            }

            // Gán loại tiệc & gói dịch vụ nếu tìm được
            if (selectedEventType != null) {
                booking.setEventTypeId(selectedEventType);
            }
            if (selectedServiceType != null) {
                booking.setServiceTypeId(selectedServiceType);
            }

            booking.setOutsideAddress(
                    notBlank(outsideAddress) ? outsideAddress : null
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

            // *** GÁN CONTACT INFO VÀO BOOKING ***
            booking.setContactFullName(notBlank(contactFullName) ? contactFullName.trim() : null);
            booking.setContactEmail(notBlank(contactEmail) ? contactEmail.trim() : null);
            booking.setContactPhone(notBlank(contactPhone) ? contactPhone.trim() : null);

            booking.setBookingStatus("PENDING");
            booking.setPaymentStatus("UNPAID");
            booking.setCreatedAt(new Date());

            // 5. Lưu DB
            bookingsFacade.create(booking);

            // 6. Message + điều hướng
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Booking request sent",
                    "Your booking has been created successfully. Our team will contact you for confirmation."
            ));

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

    // ====== Helpers ======
    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private BigDecimal parseBigDecimalSafe(String raw) {
        if (!notBlank(raw)) {
            return null;
        }
        try {
            String normalized = raw.replaceAll("[^0-9.]", "");
            if (normalized.isEmpty()) {
                return null;
            }
            return new BigDecimal(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private EventTypes resolveEventType() {
        if (!notBlank(eventTypeKey) || eventTypesFacade == null) {
            return null;
        }
        String key = eventTypeKey.trim().toLowerCase();

        for (EventTypes et : eventTypesFacade.findAll()) {
            if (et.getName() == null) {
                continue;
            }
            String name = et.getName().trim().toLowerCase();
            if (name.equals(key) || name.contains(key) || key.contains(name)) {
                return et;
            }
        }
        return null;
    }

    private ServiceTypes resolveServiceType() {
        if (!notBlank(serviceLevel) || serviceTypesFacade == null) {
            return null;
        }
        String key = serviceLevel.trim().toLowerCase();

        for (ServiceTypes st : serviceTypesFacade.findAll()) {
            if (st.getName() == null) {
                continue;
            }
            String name = st.getName().trim().toLowerCase();
            if (name.equals(key) || name.contains(key) || key.contains(name)) {
                return st;
            }
        }
        return null;
    }
}
