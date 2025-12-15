package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.entity.EventTypes;
import com.mypack.entity.ServiceTypes;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingCombosPK;
import com.mypack.entity.BookingMenuItems;
import com.mypack.entity.BookingMenuItemsPK;
import com.mypack.entity.MenuCombos;          // nếu đã có MenuCombosFacade
import com.mypack.entity.MenuItems;
import com.mypack.entity.Vouchers;
import com.mypack.entity.UserVouchers;

import com.mypack.sessionbean.BookingCombosFacadeLocal;
import com.mypack.sessionbean.BookingMenuItemsFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.MenuCombosFacadeLocal;  // nếu project đã có

import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import com.mypack.sessionbean.ServiceTypesFacadeLocal;

import com.mypack.sessionbean.VouchersFacadeLocal;
import com.mypack.sessionbean.UserVouchersFacadeLocal;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// moi them pay
import com.mypack.vnpay.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.faces.context.ExternalContext;

import com.mypack.entity.Payments;
import com.mypack.sessionbean.PaymentsFacadeLocal;

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

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private BookingCombosFacadeLocal bookingCombosFacade;

    @EJB
    private BookingMenuItemsFacadeLocal bookingMenuItemsFacade;

    // moi them
    @EJB
    private VnPayService vnPayService;

    @EJB
    private MenuCombosFacadeLocal menuCombosFacade; // nếu có

    @EJB
    private PaymentsFacadeLocal paymentsFacade;

    // ===== Voucher =====
    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private UserVouchersFacadeLocal userVouchersFacade;

    private List<EventTypes> allEventTypes;

    // danh sách event type cho dropdown
    private List<EventTypes> eventTypes = new ArrayList<>();

    // id event type được chọn trên form
    private Integer selectedEventTypeId;
    private String selectedEventTypeName;
    private EventTypes selectedEventType;

    // ====== Fields bound từ booking.xhtml (hidden inputs / form) ======
    private Long restaurantId;

    // ===== Custom menu (selected dishes) =====
    private String selectedMenuItemIds;        // raw: "1,2,3"
    private List<MenuItems> selectedMenuItems = new ArrayList<>();

    // ===== Selected package / combo (MenuCombos) từ restaurant detail =====
    private Long selectedComboId;

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

    private List<EventTypes> availableEventTypes;

    private String paymentMethod; // VNPAY / CASH
    private String paymentType;   // DEPOSIT / FULL
    private BigDecimal payAmount; // số tiền sẽ thanh toán

    // (giữ nguyên field này nếu bạn đang dùng chỗ khác, nhưng trong redirect VNPay sẽ dùng payAmount)
    private BigDecimal amount;

    // ===== Voucher apply (server-side) =====
    private String voucherCode;                 // input code (sync from JS -> hidden)
    private BigDecimal totalBeforeDiscount;     // total before voucher (sync from JS -> hidden)
    private BigDecimal voucherDiscount = BigDecimal.ZERO;
    private Long appliedVoucherId;
    private Long appliedUserVoucherId;
    private String appliedVoucherName;

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

        // ❌ KHÔNG ĐƯỢC fallback lấy nhà hàng đầu tiên nữa
        if (restaurant == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid access",
                    "No restaurant was selected for this booking. Please go back and choose a venue again."
            ));
            return;
        }

        restaurantName = safe(restaurant.getName());
        restaurantAddress = safe(restaurant.getAddress());

        // ---- Selected combo / package (optional) ----
        String comboParam = params.get("comboId");
        if (comboParam == null || comboParam.isBlank()) {
            // nếu bên restaurant detail dùng tên khác, ví dụ packageId thì vẫn bắt được
            comboParam = params.get("packageId");
        }
        if (comboParam != null && !comboParam.isBlank()) {
            try {
                selectedComboId = Long.parseLong(comboParam.trim());
            } catch (NumberFormatException ignored) {
                selectedComboId = null;
            }
        }

        /* ===== Load selected menu items from query param ===== */
        String menuItemsParam = params.get("menuItems");
        if (menuItemsParam != null && !menuItemsParam.trim().isEmpty()) {
            selectedMenuItemIds = menuItemsParam;
            selectedMenuItems = new ArrayList<>();

            String[] parts = menuItemsParam.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                try {
                    Long id = Long.valueOf(trimmed);
                    MenuItems mi = menuItemsFacade.find(id);
                    if (mi != null) {
                        // Optional: chỉ nhận món đúng nhà hàng này
                        if (restaurant != null
                                && mi.getRestaurantId() != null
                                && !mi.getRestaurantId().getRestaurantId()
                                        .equals(restaurant.getRestaurantId())) {
                            continue;
                        }

                        selectedMenuItems.add(mi);
                    }
                } catch (NumberFormatException ex) {
                    // ignore invalid id
                }
            }
        } else {
            selectedMenuItemIds = null;
            selectedMenuItems = new ArrayList<>();
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

        eventTypes = eventTypesFacade.findAll();  // nếu bạn đặt tên khác thì giữ nguyên tên cũ
        if (eventTypes != null) {
            for (EventTypes et : eventTypes) {
                if (et.getEventTypeId() != null) {
                    eventTypeNameMap.put(et.getEventTypeId(), et.getName());
                }
            }
        }

        // nếu muốn giá trị mặc định:
        selectedEventTypeId = null;
        selectedEventTypeName = null;
    }

    public void onEventTypeChange() {
        if (selectedEventTypeId == null) {
            selectedEventTypeName = null;
        } else {
            selectedEventTypeName = eventTypeNameMap.get(selectedEventTypeId);
        }
    }

    public String getSelectedEventTypeName() {
        return selectedEventTypeName;
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

    public List<EventTypes> getAvailableEventTypes() {
        return availableEventTypes;
    }

    public List<EventTypes> getAllEventTypes() {
        return allEventTypes;
    }

    public List<EventTypes> getEventTypes() {
        return eventTypes;
    }

    public Integer getSelectedEventTypeId() {
        return selectedEventTypeId;
    }

    public void setSelectedEventTypeId(Integer selectedEventTypeId) {
        this.selectedEventTypeId = selectedEventTypeId;
    }

    public void setSelectedEventType(EventTypes selectedEventType) {
        this.selectedEventType = selectedEventType;
    }

    // Alias cho view cũ nếu dùng eventTypeId
    public Integer getEventTypeId() {
        return selectedEventTypeId;
    }

    public void setEventTypeId(Integer eventTypeId) {
        this.selectedEventTypeId = eventTypeId;
    }

    // Alias cho tên hiển thị nếu view dùng eventTypeName
    public String getEventTypeName() {
        return getSelectedEventTypeName();
    }

    public String getSelectedMenuItemIds() {
        return selectedMenuItemIds;
    }

    public void setSelectedMenuItemIds(String selectedMenuItemIds) {
        this.selectedMenuItemIds = selectedMenuItemIds;
    }

    public List<MenuItems> getSelectedMenuItems() {
        return selectedMenuItems;
    }

    private Map<Integer, String> eventTypeNameMap = new HashMap<>();

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

            // ===== Combo / package (selectedComboId) =====
            if (selectedComboId == null) {
                String cHidden = params.get("hf-combo-id");
                String cQuery1 = params.get("comboId");
                String cQuery2 = params.get("packageId");
                String rawCombo = notBlank(cHidden) ? cHidden
                        : (notBlank(cQuery1) ? cQuery1 : cQuery2);
                if (notBlank(rawCombo)) {
                    try {
                        selectedComboId = Long.valueOf(rawCombo.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // ===== Custom menu ids (selectedMenuItemIds) =====
            if (!notBlank(selectedMenuItemIds)) {
                String mHidden = params.get("hf-menu-items");
                String mQuery = params.get("menuItems");
                if (notBlank(mHidden)) {
                    selectedMenuItemIds = mHidden;
                } else if (notBlank(mQuery)) {
                    selectedMenuItemIds = mQuery;
                }
            }

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

            // ================== ✅ (CHÈN 1) ĐỌC PAYMENT METHOD/TYPE/AMOUNT ==================
            String pmHidden = params.get("hf-payment-method"); // VNPAY / CASH
            if (notBlank(pmHidden)) paymentMethod = pmHidden.trim();

            String ptHidden = params.get("hf-payment-type");   // DEPOSIT / FULL
            if (notBlank(ptHidden)) paymentType = ptHidden.trim();

            if (payAmount == null) {
                String paHidden = params.get("hf-pay-amount");
                payAmount = parseBigDecimalSafe(paHidden);
            }

            // default fallback
            if (!notBlank(paymentMethod)) paymentMethod = "VNPAY";
            if (!notBlank(paymentType)) paymentType = "DEPOSIT";

            // ✅ đảm bảo payAmount luôn có (KHÔNG NULL)
            if (payAmount == null) {
                payAmount = ("FULL".equalsIgnoreCase(paymentType) && totalAmount != null)
                        ? totalAmount
                        : (depositAmount != null ? depositAmount : totalAmount);
            }
            if (payAmount == null) {
                payAmount = BigDecimal.ZERO;
            }
            // ================== ✅ (HẾT CHÈN 1) ==================

            // ================== ✅ VOUCHER (SERVER-SIDE APPLY) ==================
            // NOTE: booking.xhtml cần sync voucherCode + totalBeforeDiscount vào hidden (nếu có).
            // Nếu không sync thì bean sẽ bỏ qua voucher.
            if (totalBeforeDiscount == null) {
                totalBeforeDiscount = parseBigDecimalSafe(params.get("hf-total-before-discount"));
            }
            if (!notBlank(voucherCode)) {
                // ưu tiên đọc param nếu JS gửi (không ảnh hưởng nếu không có)
                String vcHidden = params.get("hf-voucher-code");
                String vcAny = notBlank(vcHidden) ? vcHidden : params.get("voucherCode");
                if (notBlank(vcAny)) voucherCode = vcAny;
            }

            // Nếu user có nhập code (được sync) thì apply vào total/deposit/payAmount
            if (notBlank(voucherCode)) {
                applyVoucherOnServer(currentUser, params);

                // nếu voucher invalid => có message ERROR, chặn tạo booking để user sửa
                if (appliedVoucherId == null) {
                    return null;
                }
            }
            // ================== ✅ END VOUCHER ==================

            // ===== Contact information từ form =====
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

            // ❌ KHÔNG fallback lấy nhà hàng đầu tiên nữa
            if (restaurant == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cannot find restaurant",
                        "We couldn't detect which venue you're booking. Please go back and choose the restaurant again."
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

            // ✅ sửa hợp lý: VNPAY là pending, CASH là unpaid
            booking.setPaymentStatus("VNPAY".equalsIgnoreCase(paymentMethod) ? "PENDING" : "UNPAID");

            booking.setCreatedAt(new Date());

            // 5. Lưu DB
            bookingsFacade.create(booking);

            // ✅ consume voucher (mark used) if applied
            consumeAppliedVoucher(booking);

            // ================== ✅ (CHÈN 2) TẠO PAYMENT PENDING ==================
            Payments p = new Payments();
            p.setBookingId(booking);
            p.setPaymentMethod(paymentMethod);   // VNPAY / CASH
            p.setPaymentType(paymentType);       // DEPOSIT / FULL
            p.setPaymentGateway("VNPAY".equalsIgnoreCase(paymentMethod) ? "VNPAY" : "MANUAL");
            p.setAmount(payAmount);
            p.setStatus("PENDING");

            String txnRef = "PAY" + System.currentTimeMillis();
            p.setTransactionCode(txnRef);

            paymentsFacade.create(p);

            // 5.1. Lưu package/combo nếu có
            saveSelectedCombo(booking);

            // 5.2. Lưu custom menu (các món lẻ) nếu có
            saveSelectedMenuItems(booking);

            // ================== ✅ VNPAY redirect (NGAY TẠI ĐÂY) ==================
            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {

                ExternalContext ec = ctx.getExternalContext();
                HttpServletRequest req = (HttpServletRequest) ec.getRequest();

                String orderInfo = "FeastLink booking #" + booking.getBookingId()
                        + " - " + (paymentType == null ? "PAY" : paymentType);

                // ✅ QUAN TRỌNG: build URL bằng payAmount (không dùng amount bị null)
                String redirectUrl = vnPayService.buildRedirectUrl(req, txnRef, payAmount, orderInfo);

                ec.redirect(redirectUrl);
                ctx.responseComplete();
                return null; // IMPORTANT
            }
            // ================== ✅ END VNPAY ==================

            // 6. Message + điều hướng (chỉ chạy cho CASH / các method khác)
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Booking request sent",
                    "Your booking has been created successfully. Our team will contact you for confirmation."
            ));

            return "/Customer/index?faces-redirect=true";

        } catch (Exception ex) {
            ex.printStackTrace();
            FacesContext ctx2 = FacesContext.getCurrentInstance();
            if (ctx2 != null) {
                ctx2.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Error",
                        "Could not create booking: " + ex.getMessage()
                ));
            }
            return null;
        }
    }

    // ========== Lưu package/combo vào BookingCombos ==========
    private void saveSelectedCombo(Bookings booking) {
        if (selectedComboId == null || booking == null || booking.getBookingId() == null) {
            return;
        }
        if (bookingCombosFacade == null) {
            return;
        }

        try {
            // Lấy thông tin combo để có giá
            MenuCombos combo = null;
            BigDecimal unitPrice = BigDecimal.ZERO;

            if (menuCombosFacade != null) {
                combo = menuCombosFacade.find(selectedComboId);
            }

            if (combo != null && combo.getPriceTotal() != null) {
                unitPrice = combo.getPriceTotal();
            }

            int quantity = 1; // 1 package cho cả event
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

            // Tạo PK (BookingId + ComboId)
            BookingCombosPK pk = new BookingCombosPK(booking.getBookingId(), selectedComboId);

            BookingCombos bc = new BookingCombos(pk);
            bc.setBookings(booking);
            if (combo != null) {
                bc.setMenuCombos(combo);
            }

            bc.setUnitPrice(unitPrice);
            bc.setQuantity(quantity);
            bc.setTotalPrice(totalPrice);

            bookingCombosFacade.create(bc);
        } catch (Exception ex) {
            // Không cho lỗi combo làm fail booking
            ex.printStackTrace();
        }
    }

    // ========== Lưu các món lẻ vào BookingMenuItems ==========
    private void saveSelectedMenuItems(Bookings booking) {
        if (booking == null || booking.getBookingId() == null) {
            return;
        }
        if (bookingMenuItemsFacade == null || menuItemsFacade == null) {
            return;
        }
        // lấy source id món: từ chuỗi selectedMenuItemIds
        if (!notBlank(selectedMenuItemIds)) {
            return;
        }

        int quantityPerDish = guestCount > 0 ? guestCount : 1;

        String[] parts = selectedMenuItemIds.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            try {
                Long itemId = Long.valueOf(trimmed);
                MenuItems mi = menuItemsFacade.find(itemId);
                if (mi == null) {
                    continue;
                }

                BigDecimal unitPrice = mi.getPricePerPerson() != null
                        ? mi.getPricePerPerson()
                        : BigDecimal.ZERO;

                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantityPerDish));

                BookingMenuItemsPK pk = new BookingMenuItemsPK(
                        booking.getBookingId(),
                        itemId
                );

                BookingMenuItems bmi = new BookingMenuItems(pk);
                bmi.setBookings(booking);
                bmi.setMenuItems(mi);
                bmi.setUnitPrice(unitPrice);
                bmi.setQuantity(quantityPerDish);
                bmi.setTotalPrice(totalPrice);

                bookingMenuItemsFacade.create(bmi);
            } catch (NumberFormatException ex) {
                // bỏ qua id không hợp lệ
            }
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

    // ================== VOUCHER (SERVER-SIDE) ==================
    private void applyVoucherOnServer(Users currentUser, Map<String, String> params) {
        // reset (giữ cho an toàn)
        voucherDiscount = BigDecimal.ZERO;
        appliedVoucherId = null;
        appliedUserVoucherId = null;
        appliedVoucherName = null;

        if (params == null) return;

        // đọc code từ hidden / param (nếu JS sync)
        String code = firstNotBlank(
                params.get("hf-voucher-code"),
                params.get("voucherCode"),
                params.get("voucher-code"),
                params.get("voucher_input"),
                params.get("voucher-input")
        );
        if (!notBlank(code)) {
            voucherCode = null;
            totalBeforeDiscount = null;
            return;
        }

        voucherCode = code.trim().toUpperCase();

        // đọc total trước giảm (nếu có)
        if (totalBeforeDiscount == null) {
            totalBeforeDiscount = parseBigDecimalSafe(params.get("hf-total-before-discount"));
        }
        BigDecimal baseTotal = (totalBeforeDiscount != null && totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? totalBeforeDiscount
                : totalAmount;

        if (baseTotal == null || baseTotal.compareTo(BigDecimal.ZERO) <= 0) {
            // chưa có tổng tiền thì bỏ qua
            return;
        }

        if (currentUser == null || currentUser.getUserId() == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Please sign in", "Login is required to use vouchers."));
            return;
        }

        Vouchers v = findVoucherByCode(voucherCode);
        if (v == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid voucher", "This voucher code does not exist."));
            return;
        }

        if (!isVoucherValidNow(v, new Date())) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Voucher unavailable", "This voucher is expired or inactive."));
            return;
        }

        // user phải sở hữu voucher trong UserVouchers (đã redeem)
        UserVouchers uv = findUsableUserVoucher(currentUser, v);
        if (uv == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Voucher not in wallet", "You haven't redeemed this voucher yet."));
            return;
        }

        // Min order
        BigDecimal minOrder = safeBD(v.getMinOrderAmount());
        if (minOrder.compareTo(BigDecimal.ZERO) > 0 && baseTotal.compareTo(minOrder) < 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Voucher conditions not met",
                            "Order must be at least " + minOrder + " USD to use this voucher."));
            return;
        }

        BigDecimal discount = calcDiscountAmount(v, baseTotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Voucher not applicable", "Discount amount is invalid."));
            return;
        }

        // apply
        voucherDiscount = discount;
        appliedVoucherId = v.getVoucherId();
        appliedUserVoucherId = uv.getUserVoucherId();
        appliedVoucherName = v.getName();

        // update totals
        totalAmount = baseTotal.subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) totalAmount = BigDecimal.ZERO;

        // Recalc deposit/remaining/pay based on paymentType
        recalcPaymentAmounts();
    }

    private String firstNotBlank(String... arr) {
        if (arr == null) return null;
        for (String s : arr) {
            if (notBlank(s)) return s;
        }
        return null;
    }

    private Vouchers findVoucherByCode(String code) {
        if (!notBlank(code) || vouchersFacade == null) return null;
        String c = code.trim().toUpperCase();
        List<Vouchers> all = vouchersFacade.findAll();
        if (all == null) return null;
        for (Vouchers v : all) {
            if (v == null || v.getCode() == null) continue;
            if (v.getCode().trim().toUpperCase().equals(c)) return v;
        }
        return null;
    }

    private boolean isVoucherValidNow(Vouchers v, Date now) {
        if (v == null) return false;

        String st = v.getStatus();
        if (st != null && !"ACTIVE".equalsIgnoreCase(st.trim())) return false;

        Date start = v.getStartAt();
        Date end = v.getEndAt();
        if (start != null && now.before(start)) return false;
        if (end != null && now.after(end)) return false;

        return true;
    }

    private UserVouchers findUsableUserVoucher(Users user, Vouchers voucher) {
        if (userVouchersFacade == null || user == null || voucher == null) return null;

        List<UserVouchers> all = userVouchersFacade.findAll();
        if (all == null) return null;

        Long uid = user.getUserId();
        Long vid = voucher.getVoucherId();

        for (UserVouchers uv : all) {
            if (uv == null || uv.getUserId() == null || uv.getVoucherId() == null) continue;
            if (uv.getUserId().getUserId() == null || uv.getVoucherId().getVoucherId() == null) continue;

            if (!uv.getUserId().getUserId().equals(uid)) continue;
            if (!uv.getVoucherId().getVoucherId().equals(vid)) continue;

            String st = uv.getStatus();
            if (st != null && !"ACTIVE".equalsIgnoreCase(st.trim()) && !"NEW".equalsIgnoreCase(st.trim())) continue;

            int qty = uv.getQuantity();
            int used = uv.getUsedQuantity();
            if (qty < 0) qty = 0;
            if (used < 0) used = 0;

            if (used < qty) return uv;
        }
        return null;
    }

    private BigDecimal calcDiscountAmount(Vouchers v, BigDecimal base) {
        if (v == null || base == null) return BigDecimal.ZERO;

        String type = (v.getDiscountType() == null) ? "" : v.getDiscountType().trim().toUpperCase();
        BigDecimal val = safeBD(v.getDiscountValue());
        BigDecimal max = safeBD(v.getMaxDiscount());

        BigDecimal discount = BigDecimal.ZERO;

        if ("PERCENT".equals(type)) {
            // base * val/100
            discount = base.multiply(val).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else if ("AMOUNT".equals(type)) {
            discount = val;
        }

        if (max.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(max) > 0) {
            discount = max;
        }

        if (discount.compareTo(base) > 0) discount = base;
        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;

        return discount;
    }

    private BigDecimal safeBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void recalcPaymentAmounts() {
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        // fallback 30%
        BigDecimal depositPercent = new BigDecimal("30");
        if (depositPercent.compareTo(BigDecimal.ZERO) <= 0) depositPercent = new BigDecimal("30");

        if ("FULL".equalsIgnoreCase(paymentType)) {
            depositAmount = totalAmount;
            remainingAmount = BigDecimal.ZERO;
            payAmount = totalAmount;
        } else {
            // DEPOSIT
            depositAmount = totalAmount.multiply(depositPercent)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            if (depositAmount.compareTo(totalAmount) > 0) depositAmount = totalAmount;
            if (depositAmount.compareTo(BigDecimal.ZERO) < 0) depositAmount = BigDecimal.ZERO;

            remainingAmount = totalAmount.subtract(depositAmount);
            if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) remainingAmount = BigDecimal.ZERO;

            payAmount = depositAmount;
        }
    }

    private void consumeAppliedVoucher(Bookings booking) {
        if (booking == null || appliedUserVoucherId == null || userVouchersFacade == null) return;

        try {
            UserVouchers uv = userVouchersFacade.find(appliedUserVoucherId);
            if (uv == null) return;

            int used = uv.getUsedQuantity();
            int qty = uv.getQuantity();
            if (used < 0) used = 0;
            if (qty < 0) qty = 0;

            if (used >= qty) {
                // đã dùng hết
                uv.setStatus("USED");
                userVouchersFacade.edit(uv);
                return;
            }

            uv.setUsedQuantity(used + 1);
            uv.setUsedAt(new Date());
            uv.setUsedBookingId(booking);

            if (uv.getUsedQuantity() >= qty) {
                uv.setStatus("USED");
            } else {
                if (uv.getStatus() == null || uv.getStatus().trim().isEmpty()) {
                    uv.setStatus("ACTIVE");
                }
            }

            userVouchersFacade.edit(uv);

        } catch (Exception ex) {
            // không cho fail booking
            ex.printStackTrace();
        }
    }

    private EventTypes resolveEventType() {
        // 1. ƯU TIÊN id chọn từ dropdown
        if (selectedEventTypeId != null && eventTypesFacade != null) {
            return eventTypesFacade.find(selectedEventTypeId);
        }

        // 2. Fallback cũ: dựa theo text eventTypeKey (nếu còn dùng hidden field)
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

    private void loadEventTypes() {
        try {
            availableEventTypes = eventTypesFacade.findAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            availableEventTypes = new ArrayList<>();
        }
    }

    // ===== Voucher getters/setters =====
    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public BigDecimal getTotalBeforeDiscount() {
        return totalBeforeDiscount;
    }

    public void setTotalBeforeDiscount(BigDecimal totalBeforeDiscount) {
        this.totalBeforeDiscount = totalBeforeDiscount;
    }

    public BigDecimal getVoucherDiscount() {
        return voucherDiscount;
    }

    public void setVoucherDiscount(BigDecimal voucherDiscount) {
        this.voucherDiscount = (voucherDiscount == null) ? BigDecimal.ZERO : voucherDiscount;
    }

    public Long getAppliedVoucherId() {
        return appliedVoucherId;
    }

    public void setAppliedVoucherId(Long appliedVoucherId) {
        this.appliedVoucherId = appliedVoucherId;
    }

    public Long getAppliedUserVoucherId() {
        return appliedUserVoucherId;
    }

    public void setAppliedUserVoucherId(Long appliedUserVoucherId) {
        this.appliedUserVoucherId = appliedUserVoucherId;
    }

    public String getAppliedVoucherName() {
        return appliedVoucherName;
    }

    public void setAppliedVoucherName(String appliedVoucherName) {
        this.appliedVoucherName = appliedVoucherName;
    }

    public Long getSelectedComboId() {
        return selectedComboId;
    }

    public void setSelectedComboId(Long selectedComboId) {
        this.selectedComboId = selectedComboId;
    }

}
