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
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
import com.mypack.entity.Vouchers;
import com.mypack.entity.UserVouchers;

import com.mypack.sessionbean.BookingCombosFacadeLocal;
import com.mypack.sessionbean.BookingMenuItemsFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.MenuCombosFacadeLocal;

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
import jakarta.faces.context.ExternalContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// payment
import com.mypack.vnpay.VnPayService;
import jakarta.servlet.http.HttpServletRequest;

import com.mypack.entity.Payments;
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.sessionbean.PaymentsFacadeLocal;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;

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

    @EJB
    private VnPayService vnPayService;

    @EJB
    private MenuCombosFacadeLocal menuCombosFacade;

    @EJB
    private PaymentsFacadeLocal paymentsFacade;

    // ===== Voucher =====
    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private UserVouchersFacadeLocal userVouchersFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    private int guestMin;
    private int guestMax;

    private List<EventTypes> allEventTypes;

    private List<EventTypes> eventTypes = new ArrayList<>();
    private Integer selectedEventTypeId;
    private String selectedEventTypeName;
    private EventTypes selectedEventType;

    // ====== Fields bound from booking.xhtml ======
    private Long restaurantId;

    private String selectedMenuItemIds;
    private List<MenuItems> selectedMenuItems = new ArrayList<>();

    private Long selectedComboId;

    private String eventDateStr;
    private int guestCount;
    private String locationType;
    private String outsideAddress;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    private Restaurants restaurant;
    private String restaurantName;
    private String restaurantAddress;

    private String eventTypeKey;
    private String serviceLevel;

    private String contactFullName;
    private String contactEmail;
    private String contactPhone;
    private List<EventTypes> availableEventTypes;

    private String paymentMethod; // VNPAY / CASH
    private String paymentType;   // DEPOSIT / FULL
    private BigDecimal payAmount; // amount to pay now

    private BigDecimal amount; // legacy

    // ===== Voucher apply (server-side) =====
    private String voucherCode;
    private BigDecimal totalBeforeDiscount;
    private BigDecimal voucherDiscount = BigDecimal.ZERO;
    private Long appliedVoucherId;
    private Long appliedUserVoucherId;
    private String appliedVoucherName;

    private Map<Integer, String> eventTypeNameMap = new HashMap<>();

    // ===== Time selection =====
    private String startTimeStr; // "HH:mm"
    private String endTimeStr;   // "HH:mm"
    private List<String> startTimeOptions = new ArrayList<>();
    private List<String> endTimeOptions = new ArrayList<>();

    private static final int SLOT_MINUTES = 30;
    private static final int MIN_DURATION_MINUTES = 60;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

        // Prefill contact
        Users currentUser = (Users) ctx.getExternalContext().getSessionMap().get("currentUser");
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

        // restaurantId from param/session
        if (restaurantId == null) {
            String rIdParam = params.get("restaurantId");
            if (rIdParam == null || rIdParam.isBlank()) {
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

        if (restaurantId != null) {
            restaurant = restaurantsFacade.find(restaurantId);
        }

        // keep booking page quiet, and try to recover restaurantId from session across postbacks.
        ExternalContext ec = ctx.getExternalContext();
        Map<String, Object> session = ec.getSessionMap();

        if (restaurantId != null) {
            session.put("selectedRestaurantId", restaurantId);
            session.put("bookingRestaurantId", restaurantId);
        }

        if (restaurantId == null) {
            Object sid = session.get("selectedRestaurantId");
            Object bid = session.get("bookingRestaurantId");
            Object pick = (sid != null) ? sid : bid;
            if (pick instanceof Long) {
                restaurantId = (Long) pick;
            } else if (pick instanceof Integer) {
                restaurantId = ((Integer) pick).longValue();
            }

            if (restaurantId != null) {
                restaurant = restaurantsFacade.find(restaurantId);
            }
        }

        if (restaurant == null) {
            return;
        }

        restaurantName = safe(restaurant.getName());
        restaurantAddress = safe(restaurant.getAddress());
        loadGuestBounds();

        // =======================
// combo/package (SAVE/RESTORE SESSION)
// =======================
        String skCombo = sessionKey("comboId");
        String skMenu = sessionKey("menuItems");

        String comboParam = firstNotBlank(params.get("comboId"), params.get("packageId"));
        if (notBlank(comboParam)) {
            try {
                selectedComboId = Long.parseLong(comboParam.trim());
                session.put(skCombo, selectedComboId); // ✅ SAVE
            } catch (NumberFormatException ignored) {
            }
        } else {
            // ✅ RESTORE nếu URL không có comboId
            if (selectedComboId == null) {
                selectedComboId = toLong(session.get(skCombo));
            }
        }

// =======================
// menu items (SAVE/RESTORE SESSION)
// =======================
        String menuItemsParam = params.get("menuItems");

        if (notBlank(menuItemsParam)) {
            selectedMenuItemIds = menuItemsParam.trim();
            session.put(skMenu, selectedMenuItemIds); // ✅ SAVE
        } else {
            // ✅ RESTORE nếu URL không có menuItems
            if (!notBlank(selectedMenuItemIds)) {
                Object saved = session.get(skMenu);
                if (saved != null) {
                    selectedMenuItemIds = String.valueOf(saved);
                }
            }
        }

// rebuild selectedMenuItems từ selectedMenuItemIds (dù lấy từ param hay session)
        selectedMenuItems = new ArrayList<>();
        if (notBlank(selectedMenuItemIds)) {
            String[] parts = selectedMenuItemIds.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                try {
                    Long id = Long.valueOf(trimmed);
                    MenuItems mi = menuItemsFacade.find(id);
                    if (mi != null) {
                        if (restaurant != null && mi.getRestaurantId() != null
                                && !mi.getRestaurantId().getRestaurantId().equals(restaurant.getRestaurantId())) {
                            continue;
                        }
                        selectedMenuItems.add(mi);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // date (FIX: eventDate first)
        if (eventDateStr == null || eventDateStr.isBlank()) {
            String dParam = firstNotBlank(params.get("eventDate"), params.get("date"));
            if (dParam != null && !dParam.isBlank()) {
                eventDateStr = dParam;
            }
        }

        // guests
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
            guestCount = 200;
        }
        clampGuestCount();

        // location type default
        if (locationType == null || locationType.isBlank()) {
            locationType = "AT_RESTAURANT";
        }

        // eventTypes map
        eventTypes = eventTypesFacade.findAll();
        if (eventTypes != null) {
            for (EventTypes et : eventTypes) {
                if (et.getEventTypeId() != null) {
                    eventTypeNameMap.put(et.getEventTypeId(), et.getName());
                }
            }
        }

        selectedEventTypeId = null;
        selectedEventTypeName = null;

        // ===== build time slots based on restaurant open/close =====
        initTimeSlotsIfNeeded();
    }

    public void onEventTypeChange() {
        if (selectedEventTypeId == null) {
            selectedEventTypeName = null;
        } else {
            selectedEventTypeName = eventTypeNameMap.get(selectedEventTypeId);
        }
    }

    // =========================
    // TIME SLOT LOGIC (NEW)
    // =========================
    private void initTimeSlotsIfNeeded() {
        if (restaurant == null) {
            return;
        }

        LocalTime open = extractLocalTime(restaurant.getOpenTime(), LocalTime.of(8, 0));
        LocalTime close = extractLocalTime(restaurant.getCloseTime(), LocalTime.of(22, 0));

        if (!close.isAfter(open)) {
            open = LocalTime.of(8, 0);
            close = LocalTime.of(22, 0);
        }

        LocalTime latestStart = close.minusMinutes(MIN_DURATION_MINUTES);
        if (latestStart.isBefore(open)) {
            startTimeOptions = new ArrayList<>();
            endTimeOptions = new ArrayList<>();
            startTimeStr = null;
            endTimeStr = null;
            return;
        }

        startTimeOptions = buildSlots(open, latestStart, SLOT_MINUTES);

        if (!notBlank(startTimeStr) || !startTimeOptions.contains(startTimeStr)) {
            startTimeStr = startTimeOptions.isEmpty() ? null : startTimeOptions.get(0);
        }

        rebuildEndTimes(open, close);
    }

    public void onStartTimeChange() {
        if (restaurant == null) {
            return;
        }

        LocalTime open = extractLocalTime(restaurant.getOpenTime(), LocalTime.of(8, 0));
        LocalTime close = extractLocalTime(restaurant.getCloseTime(), LocalTime.of(22, 0));
        if (!close.isAfter(open)) {
            open = LocalTime.of(8, 0);
            close = LocalTime.of(22, 0);
        }

        rebuildEndTimes(open, close);
    }

    private void rebuildEndTimes(LocalTime open, LocalTime close) {
        if (!notBlank(startTimeStr)) {
            return;
        }

        LocalTime start = parseHHmm(startTimeStr);
        if (start == null) {
            return;
        }

        LocalTime minEnd = start.plusMinutes(MIN_DURATION_MINUTES);
        if (minEnd.isBefore(open)) {
            minEnd = open;
        }

        if (minEnd.isAfter(close)) {
            endTimeOptions = new ArrayList<>();
            endTimeStr = null;
            return;
        }

        endTimeOptions = buildSlots(minEnd, close, SLOT_MINUTES);

        if (!notBlank(endTimeStr) || !endTimeOptions.contains(endTimeStr)) {
            endTimeStr = endTimeOptions.isEmpty() ? null : endTimeOptions.get(0);
        }
    }

    private List<String> buildSlots(LocalTime from, LocalTime to, int stepMinutes) {
        List<String> out = new ArrayList<>();
        LocalTime t = from;
        while (!t.isAfter(to)) {
            out.add(t.format(HHMM));
            t = t.plusMinutes(stepMinutes);
        }
        return out;
    }

    private LocalTime parseHHmm(String s) {
        try {
            return LocalTime.parse(s.trim(), HHMM);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalTime extractLocalTime(Object timeObj, LocalTime fallback) {
        if (timeObj == null) {
            return fallback;
        }

        if (timeObj instanceof Date) {
            Calendar cal = Calendar.getInstance();
            cal.setTime((Date) timeObj);
            return LocalTime.of(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
        }

        if (timeObj instanceof java.sql.Time) {
            return ((java.sql.Time) timeObj).toLocalTime();
        }

        if (timeObj instanceof LocalTime) {
            return (LocalTime) timeObj;
        }

        return fallback;
    }

    private String sessionKey(String suffix) {
        return "booking_" + suffix + "_" + (restaurantId != null ? restaurantId : "0");
    }

    private Long toLong(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Long) {
            return (Long) o;
        }
        if (o instanceof Integer) {
            return ((Integer) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public String confirmBooking() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        Users currentUser = (Users) ctx.getExternalContext().getSessionMap().get("currentUser");
        if (currentUser == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please sign in",
                    "You need to log in before making a booking."
            ));
            return "login";
        }

        try {
            Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

            if (selectedComboId == null) {
                String cHidden = params.get("hf-combo-id");
                String cQuery1 = params.get("comboId");
                String cQuery2 = params.get("packageId");
                String rawCombo = notBlank(cHidden) ? cHidden : (notBlank(cQuery1) ? cQuery1 : cQuery2);
                if (notBlank(rawCombo)) {
                    try {
                        selectedComboId = Long.valueOf(rawCombo.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (!notBlank(selectedMenuItemIds)) {
                String mHidden = params.get("hf-menu-items");
                String mQuery = params.get("menuItems");
                if (notBlank(mHidden)) {
                    selectedMenuItemIds = mHidden;
                } else if (notBlank(mQuery)) {
                    selectedMenuItemIds = mQuery;
                }
            }

            ExternalContext ec = ctx.getExternalContext();
            Map<String, Object> session = ec.getSessionMap();
            if (restaurantId != null) {
                if (selectedComboId != null) {
                    session.put(sessionKey("comboId"), selectedComboId);
                }
                if (notBlank(selectedMenuItemIds)) {
                    session.put(sessionKey("menuItems"), selectedMenuItemIds);
                }
            }

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

            // FIX: read eventDate first (then date)
            if (!notBlank(eventDateStr)) {
                String dHidden = params.get("hf-event-date");
                String dQuery = firstNotBlank(params.get("eventDate"), params.get("date"));
                if (notBlank(dHidden)) {
                    eventDateStr = dHidden;
                } else if (notBlank(dQuery)) {
                    eventDateStr = dQuery;
                }
            }

            // read start/end time
            if (!notBlank(startTimeStr)) {
                startTimeStr = firstNotBlank(params.get("hfStartTime"), params.get("hf-start-time"));
                if (!notBlank(startTimeStr)) {
                    startTimeStr = params.get("startTime");
                }
            }
            if (!notBlank(endTimeStr)) {
                endTimeStr = firstNotBlank(params.get("hfEndTime"), params.get("hf-end-time"));
                if (!notBlank(endTimeStr)) {
                    endTimeStr = params.get("endTime");
                }
            }

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
            loadGuestBounds();

            if (guestCount < guestMin || guestCount > guestMax) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid guests",
                                "Guest count must be between " + guestMin + " and " + guestMax + "."));
                return null;
            }

            if (!notBlank(locationType)) {
                String locHidden = params.get("hf-location-type");
                if (notBlank(locHidden)) {
                    locationType = locHidden;
                } else {
                    locationType = "AT_RESTAURANT";
                }
            }

            if (!notBlank(outsideAddress)) {
                String addrHidden = params.get("hf-outside-address");
                if (notBlank(addrHidden)) {
                    outsideAddress = addrHidden;
                }
            }

            if (totalAmount == null) {
                totalAmount = parseBigDecimalSafe(params.get("hf-total-amount"));
            }
            if (!notBlank(eventTypeKey)) {
                eventTypeKey = params.get("hf-event-type");
            }
            if (!notBlank(serviceLevel)) {
                serviceLevel = params.get("hf-service-level");
            }
            if (depositAmount == null) {
                depositAmount = parseBigDecimalSafe(params.get("hf-deposit-amount"));
            }
            if (remainingAmount == null) {
                remainingAmount = parseBigDecimalSafe(params.get("hf-remaining-amount"));
            }

            // payment
            String pmHidden = params.get("hf-payment-method");
            if (notBlank(pmHidden)) {
                paymentMethod = pmHidden.trim();
            }
            String ptHidden = params.get("hf-payment-type");
            if (notBlank(ptHidden)) {
                paymentType = ptHidden.trim();
            }
            if (payAmount == null) {
                payAmount = parseBigDecimalSafe(params.get("hf-pay-amount"));
            }

            if (!notBlank(paymentMethod)) {
                paymentMethod = "VNPAY";
            }
            if (!notBlank(paymentType)) {
                paymentType = "DEPOSIT";
            }

            if (payAmount == null) {
                payAmount = ("FULL".equalsIgnoreCase(paymentType) && totalAmount != null)
                        ? totalAmount
                        : (depositAmount != null ? depositAmount : totalAmount);
            }
            if (payAmount == null) {
                payAmount = BigDecimal.ZERO;
            }

            // ===== Voucher (server-side) =====
            if (totalBeforeDiscount == null) {
                totalBeforeDiscount = parseBigDecimalSafe(params.get("hf-total-before-discount"));
            }
            if (!notBlank(voucherCode)) {
                String vcHidden = params.get("hf-voucher-code");
                if (notBlank(vcHidden)) {
                    voucherCode = vcHidden;
                }
            }
            if (notBlank(voucherCode)) {
                applyVoucherOnServerSilent(currentUser, params);
            }

            // contact fields
            if (!notBlank(contactFullName)) {
                String cName = firstNotBlank(params.get("contact-fullname"), params.get("contactFullName"));
                if (notBlank(cName)) {
                    contactFullName = cName.trim();
                }
            }
            if (!notBlank(contactEmail)) {
                String cEmail = firstNotBlank(params.get("contact-email"), params.get("contactEmail"));
                if (notBlank(cEmail)) {
                    contactEmail = cEmail.trim();
                }
            }
            if (!notBlank(contactPhone)) {
                String cPhone = firstNotBlank(params.get("contact-phone"), params.get("contactPhone"));
                if (notBlank(cPhone)) {
                    contactPhone = cPhone.trim();
                }
            }

            if (restaurant == null && restaurantId != null) {
                restaurant = restaurantsFacade.find(restaurantId);
            }
            if (restaurant == null) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please choose restaurant again."));
                return null;
            }

            // Event date
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

            // ===== NEW: parse + validate start/end time within open/close =====
            LocalTime startLt = parseHHmm(startTimeStr);
            LocalTime endLt = parseHHmm(endTimeStr);

            if (startLt == null || endLt == null || !endLt.isAfter(startLt)) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Invalid time", "Please select a valid start/end time."));
                return null;
            }

            long minutes = Duration.between(startLt, endLt).toMinutes();
            if (minutes < MIN_DURATION_MINUTES) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Too short", "Minimum duration is " + MIN_DURATION_MINUTES + " minutes."));
                return null;
            }

            LocalTime openLt = extractLocalTime(restaurant.getOpenTime(), LocalTime.of(8, 0));
            LocalTime closeLt = extractLocalTime(restaurant.getCloseTime(), LocalTime.of(22, 0));
            if (!closeLt.isAfter(openLt)) {
                openLt = LocalTime.of(8, 0);
                closeLt = LocalTime.of(22, 0);
            }

            if (startLt.isBefore(openLt) || endLt.isAfter(closeLt)) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Outside opening hours",
                        "Please choose a time within " + openLt.format(HHMM) + " - " + closeLt.format(HHMM)));
                return null;
            }

            ZoneId zone = ZoneId.systemDefault();
            LocalDateTime startLdt = LocalDateTime.of(eventLocalDate, startLt);
            LocalDateTime endLdt = LocalDateTime.of(eventLocalDate, endLt);
            Date startTime = Date.from(startLdt.atZone(zone).toInstant());
            Date endTime = Date.from(endLdt.atZone(zone).toInstant());

            EventTypes et = resolveEventType();
            ServiceTypes st = resolveServiceType();

            String specialRequests = params.get("special-requests");

            Bookings booking = new Bookings();
            booking.setBookingCode(generateBookingCode());
            booking.setCustomerId(currentUser);
            booking.setRestaurantId(restaurant);
            booking.setEventDate(eventDate);
            booking.setGuestCount(guestCount);
            booking.setLocationType(locationType);

            // SAVE selected times
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);

            if (notBlank(specialRequests)) {
                booking.setNote(specialRequests.trim());
            }
            if (et != null) {
                booking.setEventTypeId(et);
            }
            if (st != null) {
                booking.setServiceTypeId(st);
            }

            booking.setOutsideAddress(notBlank(outsideAddress) ? outsideAddress : null);

            if (totalAmount != null) {
                booking.setTotalAmount(totalAmount);
            }
            if (depositAmount != null) {
                booking.setDepositAmount(depositAmount);
            }
            if (remainingAmount != null) {
                booking.setRemainingAmount(remainingAmount);
            }

            booking.setContactFullName(notBlank(contactFullName) ? contactFullName.trim() : null);
            booking.setContactEmail(notBlank(contactEmail) ? contactEmail.trim() : null);
            booking.setContactPhone(notBlank(contactPhone) ? contactPhone.trim() : null);

            booking.setBookingStatus("PENDING");
            booking.setPaymentStatus("VNPAY".equalsIgnoreCase(paymentMethod) ? "PENDING" : "UNPAID");
            booking.setCreatedAt(new Date());

            bookingsFacade.create(booking);

            // consume voucher if applied
            consumeAppliedVoucher(booking);

            // payment record
            Payments p = new Payments();
            p.setBookingId(booking);
            p.setPaymentMethod(paymentMethod);
            p.setPaymentType(paymentType);
            p.setPaymentGateway("VNPAY".equalsIgnoreCase(paymentMethod) ? "VNPAY" : "MANUAL");
            p.setAmount(payAmount);
            p.setStatus("PENDING");

            String txnRef = "PAY" + System.currentTimeMillis();
            p.setTransactionCode(txnRef);

            paymentsFacade.create(p);

            saveSelectedCombo(booking);
            saveSelectedMenuItems(booking);

            if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
                ExternalContext ec2 = ctx.getExternalContext();
                HttpServletRequest req = (HttpServletRequest) ec2.getRequest();
                String orderInfo = "FeastLink booking #" + booking.getBookingId() + " - " + (paymentType == null ? "PAY" : paymentType);
                String redirectUrl = vnPayService.buildRedirectUrl(req, txnRef, payAmount, orderInfo);
                ec2.redirect(redirectUrl);
                ctx.responseComplete();
                return null;
            }

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Booking request sent", "Your booking has been created successfully."));
            return "/Customer/index?faces-redirect=true";

        } catch (Exception ex) {
            ex.printStackTrace();
            FacesContext ctx2 = FacesContext.getCurrentInstance();
            if (ctx2 != null) {
                ctx2.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not create booking: " + ex.getMessage()));
            }
            return null;
        }
    }

    public String saveQuotation() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        Users currentUser = (Users) ctx.getExternalContext().getSessionMap().get("currentUser");
        if (currentUser == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please sign in",
                    "You need to log in before making a booking."
            ));
            return "login";
        }

        try {
            Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();

            if (selectedComboId == null) {
                String cHidden = params.get("hf-combo-id");
                String cQuery1 = params.get("comboId");
                String cQuery2 = params.get("packageId");
                String rawCombo = notBlank(cHidden) ? cHidden : (notBlank(cQuery1) ? cQuery1 : cQuery2);
                if (notBlank(rawCombo)) {
                    try {
                        selectedComboId = Long.valueOf(rawCombo.trim());
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            if (!notBlank(selectedMenuItemIds)) {
                String mHidden = params.get("hf-menu-items");
                String mQuery = params.get("menuItems");
                if (notBlank(mHidden)) {
                    selectedMenuItemIds = mHidden;
                } else if (notBlank(mQuery)) {
                    selectedMenuItemIds = mQuery;
                }
            }

            ExternalContext ec = ctx.getExternalContext();
            Map<String, Object> session = ec.getSessionMap();
            if (restaurantId != null) {
                if (selectedComboId != null) {
                    session.put(sessionKey("comboId"), selectedComboId);
                }
                if (notBlank(selectedMenuItemIds)) {
                    session.put(sessionKey("menuItems"), selectedMenuItemIds);
                }
            }

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

            // FIX: read eventDate first (then date)
            if (!notBlank(eventDateStr)) {
                String dHidden = params.get("hf-event-date");
                String dQuery = firstNotBlank(params.get("eventDate"), params.get("date"));
                if (notBlank(dHidden)) {
                    eventDateStr = dHidden;
                } else if (notBlank(dQuery)) {
                    eventDateStr = dQuery;
                }
            }

            // read start/end time
            if (!notBlank(startTimeStr)) {
                startTimeStr = firstNotBlank(params.get("hfStartTime"), params.get("hf-start-time"));
                if (!notBlank(startTimeStr)) {
                    startTimeStr = params.get("startTime");
                }
            }
            if (!notBlank(endTimeStr)) {
                endTimeStr = firstNotBlank(params.get("hfEndTime"), params.get("hf-end-time"));
                if (!notBlank(endTimeStr)) {
                    endTimeStr = params.get("endTime");
                }
            }

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
            loadGuestBounds();

            if (guestCount < guestMin || guestCount > guestMax) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Invalid guests",
                                "Guest count must be between " + guestMin + " and " + guestMax + "."));
                return null;
            }

            if (!notBlank(locationType)) {
                String locHidden = params.get("hf-location-type");
                if (notBlank(locHidden)) {
                    locationType = locHidden;
                } else {
                    locationType = "AT_RESTAURANT";
                }
            }

            if (!notBlank(outsideAddress)) {
                String addrHidden = params.get("hf-outside-address");
                if (notBlank(addrHidden)) {
                    outsideAddress = addrHidden;
                }
            }

            if (totalAmount == null) {
                totalAmount = parseBigDecimalSafe(params.get("hf-total-amount"));
            }
            if (!notBlank(eventTypeKey)) {
                eventTypeKey = params.get("hf-event-type");
            }
            if (!notBlank(serviceLevel)) {
                serviceLevel = params.get("hf-service-level");
            }
            if (depositAmount == null) {
                depositAmount = parseBigDecimalSafe(params.get("hf-deposit-amount"));
            }
            if (remainingAmount == null) {
                remainingAmount = parseBigDecimalSafe(params.get("hf-remaining-amount"));
            }

            // payment
            String pmHidden = params.get("hf-payment-method");
            if (notBlank(pmHidden)) {
                paymentMethod = pmHidden.trim();
            }
            String ptHidden = params.get("hf-payment-type");
            if (notBlank(ptHidden)) {
                paymentType = ptHidden.trim();
            }
            if (payAmount == null) {
                payAmount = parseBigDecimalSafe(params.get("hf-pay-amount"));
            }

            if (!notBlank(paymentMethod)) {
                paymentMethod = "VNPAY";
            }
            if (!notBlank(paymentType)) {
                paymentType = "DEPOSIT";
            }

            if (payAmount == null) {
                payAmount = ("FULL".equalsIgnoreCase(paymentType) && totalAmount != null)
                        ? totalAmount
                        : (depositAmount != null ? depositAmount : totalAmount);
            }
            if (payAmount == null) {
                payAmount = BigDecimal.ZERO;
            }

            // ===== Voucher (server-side) =====
            if (totalBeforeDiscount == null) {
                totalBeforeDiscount = parseBigDecimalSafe(params.get("hf-total-before-discount"));
            }
            if (!notBlank(voucherCode)) {
                String vcHidden = params.get("hf-voucher-code");
                if (notBlank(vcHidden)) {
                    voucherCode = vcHidden;
                }
            }
            if (notBlank(voucherCode)) {
                applyVoucherOnServerSilent(currentUser, params);
            }

            // contact fields
            if (!notBlank(contactFullName)) {
                String cName = firstNotBlank(params.get("contact-fullname"), params.get("contactFullName"));
                if (notBlank(cName)) {
                    contactFullName = cName.trim();
                }
            }
            if (!notBlank(contactEmail)) {
                String cEmail = firstNotBlank(params.get("contact-email"), params.get("contactEmail"));
                if (notBlank(cEmail)) {
                    contactEmail = cEmail.trim();
                }
            }
            if (!notBlank(contactPhone)) {
                String cPhone = firstNotBlank(params.get("contact-phone"), params.get("contactPhone"));
                if (notBlank(cPhone)) {
                    contactPhone = cPhone.trim();
                }
            }

            if (restaurant == null && restaurantId != null) {
                restaurant = restaurantsFacade.find(restaurantId);
            }
            if (restaurant == null) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please choose restaurant again."));
                return null;
            }

            // Event date
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

            // ===== NEW: parse + validate start/end time within open/close =====
            LocalTime startLt = parseHHmm(startTimeStr);
            LocalTime endLt = parseHHmm(endTimeStr);

            if (startLt == null || endLt == null || !endLt.isAfter(startLt)) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Invalid time", "Please select a valid start/end time."));
                return null;
            }

            long minutes = Duration.between(startLt, endLt).toMinutes();
            if (minutes < MIN_DURATION_MINUTES) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Too short", "Minimum duration is " + MIN_DURATION_MINUTES + " minutes."));
                return null;
            }

            LocalTime openLt = extractLocalTime(restaurant.getOpenTime(), LocalTime.of(8, 0));
            LocalTime closeLt = extractLocalTime(restaurant.getCloseTime(), LocalTime.of(22, 0));
            if (!closeLt.isAfter(openLt)) {
                openLt = LocalTime.of(8, 0);
                closeLt = LocalTime.of(22, 0);
            }

            if (startLt.isBefore(openLt) || endLt.isAfter(closeLt)) {
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Outside opening hours",
                        "Please choose a time within " + openLt.format(HHMM) + " - " + closeLt.format(HHMM)));
                return null;
            }

            ZoneId zone = ZoneId.systemDefault();
            LocalDateTime startLdt = LocalDateTime.of(eventLocalDate, startLt);
            LocalDateTime endLdt = LocalDateTime.of(eventLocalDate, endLt);
            Date startTime = Date.from(startLdt.atZone(zone).toInstant());
            Date endTime = Date.from(endLdt.atZone(zone).toInstant());

            EventTypes et = resolveEventType();
            ServiceTypes st = resolveServiceType();

            String specialRequests = params.get("special-requests");

            Bookings booking = new Bookings();
            booking.setBookingCode(generateBookingCode());
            booking.setCustomerId(currentUser);
            booking.setRestaurantId(restaurant);
            booking.setEventDate(eventDate);
            booking.setGuestCount(guestCount);
            booking.setLocationType(locationType);

            // SAVE selected times
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);

            if (notBlank(specialRequests)) {
                booking.setNote(specialRequests.trim());
            }
            if (et != null) {
                booking.setEventTypeId(et);
            }
            if (st != null) {
                booking.setServiceTypeId(st);
            }

            booking.setOutsideAddress(notBlank(outsideAddress) ? outsideAddress : null);

            if (totalAmount != null) {
                booking.setTotalAmount(totalAmount);
            }
            if (depositAmount != null) {
                booking.setDepositAmount(depositAmount);
            }
            if (remainingAmount != null) {
                booking.setRemainingAmount(remainingAmount);
            }

            booking.setContactFullName(notBlank(contactFullName) ? contactFullName.trim() : null);
            booking.setContactEmail(notBlank(contactEmail) ? contactEmail.trim() : null);
            booking.setContactPhone(notBlank(contactPhone) ? contactPhone.trim() : null);

            booking.setBookingStatus("DRAFT");
            booking.setPaymentStatus("UNPAID");
            booking.setCreatedAt(new Date());

            bookingsFacade.create(booking);

            saveSelectedCombo(booking);
            saveSelectedMenuItems(booking);

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Quotation saved",
                    "Your quotation has been saved successfully."
            ));
            return "/Customer/my-bookings.xhtml?faces-redirect=true";

        } catch (Exception ex) {
            ex.printStackTrace();
            FacesContext ctx2 = FacesContext.getCurrentInstance();
            if (ctx2 != null) {
                ctx2.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "Could not create booking: " + ex.getMessage()));
            }
            return null;
        }
    }

    // ========== Voucher silent apply (NO FacesMessage, NO block) ==========
    private void applyVoucherOnServerSilent(Users currentUser, Map<String, String> params) {
        voucherDiscount = BigDecimal.ZERO;
        appliedVoucherId = null;
        appliedUserVoucherId = null;
        appliedVoucherName = null;

        String code = firstNotBlank(params.get("hf-voucher-code"), params.get("voucherCode"));
        if (!notBlank(code)) {
            voucherCode = null;
            totalBeforeDiscount = null;
            return;
        }

        voucherCode = code.trim().toUpperCase();

        if (totalBeforeDiscount == null) {
            totalBeforeDiscount = parseBigDecimalSafe(params.get("hf-total-before-discount"));
        }

        BigDecimal baseTotal = (totalBeforeDiscount != null && totalBeforeDiscount.compareTo(BigDecimal.ZERO) > 0)
                ? totalBeforeDiscount
                : totalAmount;

        if (baseTotal == null || baseTotal.compareTo(BigDecimal.ZERO) <= 0) {
            clearVoucherOnly();
            return;
        }

        if (currentUser == null || currentUser.getUserId() == null) {
            clearVoucherOnly();
            return;
        }

        Vouchers v = findVoucherByCode(voucherCode);
        if (v == null || !isVoucherValidNow(v, new Date())) {
            clearVoucherOnly();
            return;
        }

        UserVouchers uv = findUsableUserVoucher(currentUser, v);
        if (uv == null) {
            clearVoucherOnly();
            return;
        }

        BigDecimal minOrder = safeBD(v.getMinOrderAmount());
        if (minOrder.compareTo(BigDecimal.ZERO) > 0 && baseTotal.compareTo(minOrder) < 0) {
            clearVoucherOnly();
            return;
        }

        BigDecimal discount = calcDiscountAmount(v, baseTotal);
        if (discount.compareTo(BigDecimal.ZERO) <= 0) {
            clearVoucherOnly();
            return;
        }

        voucherDiscount = discount;
        appliedVoucherId = v.getVoucherId();
        appliedUserVoucherId = uv.getUserVoucherId();
        appliedVoucherName = v.getName();

        totalAmount = baseTotal.subtract(discount);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        recalcPaymentAmounts();
    }

    private void clearVoucherOnly() {
        appliedVoucherId = null;
        appliedUserVoucherId = null;
        appliedVoucherName = null;
        voucherDiscount = BigDecimal.ZERO;
        voucherCode = null;
    }

    // ========== Save combo/package ==========
    private void saveSelectedCombo(Bookings booking) {
        if (selectedComboId == null || booking == null || booking.getBookingId() == null) {
            return;
        }

        try {
            MenuCombos combo = null;
            BigDecimal unitPrice = BigDecimal.ZERO;

            if (menuCombosFacade != null) {
                combo = menuCombosFacade.find(selectedComboId);
            }
            if (combo != null && combo.getPriceTotal() != null) {
                unitPrice = combo.getPriceTotal();
            }

            int quantity = 1;
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

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
            ex.printStackTrace();
        }
    }

    // ========== Save menu items ==========
    private void saveSelectedMenuItems(Bookings booking) {
        if (booking == null || booking.getBookingId() == null) {
            return;
        }
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

                BigDecimal unitPrice = mi.getPricePerPerson() != null ? mi.getPricePerPerson() : BigDecimal.ZERO;
                BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantityPerDish));

                BookingMenuItemsPK pk = new BookingMenuItemsPK(booking.getBookingId(), itemId);
                BookingMenuItems bmi = new BookingMenuItems(pk);
                bmi.setBookings(booking);
                bmi.setMenuItems(mi);
                bmi.setUnitPrice(unitPrice);
                bmi.setQuantity(quantityPerDish);
                bmi.setTotalPrice(totalPrice);

                bookingMenuItemsFacade.create(bmi);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private String generateBookingCode() {
        return "BK" + System.currentTimeMillis();
    }

    // ========== Voucher helpers ==========
    private Vouchers findVoucherByCode(String code) {
        if (!notBlank(code) || vouchersFacade == null) {
            return null;
        }
        String c = code.trim().toUpperCase();
        List<Vouchers> all = vouchersFacade.findAll();
        if (all == null) {
            return null;
        }
        for (Vouchers v : all) {
            if (v == null || v.getCode() == null) {
                continue;
            }
            if (v.getCode().trim().toUpperCase().equals(c)) {
                return v;
            }
        }
        return null;
    }

    private boolean isVoucherValidNow(Vouchers v, Date now) {
        if (v == null) {
            return false;
        }
        String st = v.getStatus();
        if (st != null && !"ACTIVE".equalsIgnoreCase(st.trim())) {
            return false;
        }
        Date start = v.getStartAt();
        Date end = v.getEndAt();
        if (start != null && now.before(start)) {
            return false;
        }
        if (end != null && now.after(end)) {
            return false;
        }
        return true;
    }

    private UserVouchers findUsableUserVoucher(Users user, Vouchers voucher) {
        if (userVouchersFacade == null || user == null || voucher == null) {
            return null;
        }

        List<UserVouchers> all = userVouchersFacade.findAll();
        if (all == null) {
            return null;
        }

        Long uid = user.getUserId();
        Long vid = voucher.getVoucherId();

        for (UserVouchers uv : all) {
            if (uv == null || uv.getUserId() == null || uv.getVoucherId() == null) {
                continue;
            }
            if (uv.getUserId().getUserId() == null || uv.getVoucherId().getVoucherId() == null) {
                continue;
            }

            if (!uv.getUserId().getUserId().equals(uid)) {
                continue;
            }
            if (!uv.getVoucherId().getVoucherId().equals(vid)) {
                continue;
            }

            String st = uv.getStatus();
            if (st != null && !"ACTIVE".equalsIgnoreCase(st.trim()) && !"NEW".equalsIgnoreCase(st.trim())) {
                continue;
            }

            int qty = uv.getQuantity();
            int used = uv.getUsedQuantity();
            if (qty < 0) {
                qty = 0;
            }
            if (used < 0) {
                used = 0;
            }

            if (used < qty) {
                return uv;
            }
        }
        return null;
    }

    private BigDecimal calcDiscountAmount(Vouchers v, BigDecimal base) {
        if (v == null || base == null) {
            return BigDecimal.ZERO;
        }

        String type = (v.getDiscountType() == null) ? "" : v.getDiscountType().trim().toUpperCase();
        BigDecimal val = safeBD(v.getDiscountValue());
        BigDecimal max = safeBD(v.getMaxDiscount());

        BigDecimal discount = BigDecimal.ZERO;
        if ("PERCENT".equals(type)) {
            discount = base.multiply(val).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else if ("AMOUNT".equals(type)) {
            discount = val;
        }

        if (max.compareTo(BigDecimal.ZERO) > 0 && discount.compareTo(max) > 0) {
            discount = max;
        }
        if (discount.compareTo(base) > 0) {
            discount = base;
        }
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        return discount;
    }

    private BigDecimal safeBD(Object val) {
        if (val == null) {
            return BigDecimal.ZERO;
        }
        if (val instanceof BigDecimal) {
            return (BigDecimal) val;
        }
        if (val instanceof Number) {
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private void recalcPaymentAmounts() {
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }

        BigDecimal depositPercent = new BigDecimal("30");

        if ("FULL".equalsIgnoreCase(paymentType)) {
            depositAmount = totalAmount;
            remainingAmount = BigDecimal.ZERO;
            payAmount = totalAmount;
        } else {
            depositAmount = totalAmount.multiply(depositPercent)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

            if (depositAmount.compareTo(totalAmount) > 0) {
                depositAmount = totalAmount;
            }
            if (depositAmount.compareTo(BigDecimal.ZERO) < 0) {
                depositAmount = BigDecimal.ZERO;
            }

            remainingAmount = totalAmount.subtract(depositAmount);
            if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
                remainingAmount = BigDecimal.ZERO;
            }

            payAmount = depositAmount;
        }
    }

    private void consumeAppliedVoucher(Bookings booking) {
        if (booking == null || appliedUserVoucherId == null || userVouchersFacade == null) {
            return;
        }

        try {
            UserVouchers uv = userVouchersFacade.find(appliedUserVoucherId);
            if (uv == null) {
                return;
            }

            int used = uv.getUsedQuantity();
            int qty = uv.getQuantity();
            if (used < 0) {
                used = 0;
            }
            if (qty < 0) {
                qty = 0;
            }

            if (used >= qty) {
                uv.setStatus("USED");
                userVouchersFacade.edit(uv);
                return;
            }

            uv.setUsedQuantity(used + 1);
            uv.setUsedAt(new Date());
            uv.setUsedBookingId(booking);

            if (uv.getUsedQuantity() >= qty) {
                uv.setStatus("USED");
            } else if (uv.getStatus() == null || uv.getStatus().trim().isEmpty()) {
                uv.setStatus("ACTIVE");
            }

            userVouchersFacade.edit(uv);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private EventTypes resolveEventType() {
        if (selectedEventTypeId != null && eventTypesFacade != null) {
            return eventTypesFacade.find(selectedEventTypeId);
        }
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

    private void loadGuestBounds() {
        // MIN từ Restaurants.MinGuestCount
        Integer minDb = (restaurant != null) ? restaurant.getMinGuestCount() : null;
        guestMin = (minDb != null && minDb > 0) ? minDb : 1;

        // MAX từ RestaurantCapacitySettings.MaxGuestsPerSlot
        Integer maxDb = null;
        try {
            if (capacitySettingsFacade != null && restaurant != null) {
                RestaurantCapacitySettings s = capacitySettingsFacade.findByRestaurant(restaurant);
                if (s != null) {
                    maxDb = s.getMaxGuestsPerSlot();
                }
            }
        } catch (Exception ignore) {
        }

        // fallback nếu chưa có settings
        guestMax = (maxDb != null && maxDb > 0) ? maxDb : (guestMin * 3);

        // đảm bảo max >= min
        if (guestMax < guestMin) {
            guestMax = guestMin;
        }
    }

    private void clampGuestCount() {
        if (guestCount < guestMin) {
            guestCount = guestMin;
        }
        if (guestCount > guestMax) {
            guestCount = guestMax;
        }
    }

    // ===== Helpers =====
    private boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String firstNotBlank(String a, String b) {
        if (notBlank(a)) {
            return a;
        }
        if (notBlank(b)) {
            return b;
        }
        return null;
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

    public String getEventDateDisplay() {
        if (eventDateStr == null || eventDateStr.isBlank()) {
            return "";
        }
        try {
            LocalDate d = LocalDate.parse(eventDateStr.trim()); // yyyy-MM-dd
            return d.format(DATE_DISPLAY);
        } catch (Exception e) {
            return eventDateStr; // fallback nếu lỗi format
        }
    }

    public String getEventTimeDisplay() {
        String s = (startTimeStr != null && !startTimeStr.isBlank()) ? startTimeStr : "18:00";
        String e = (endTimeStr != null && !endTimeStr.isBlank()) ? endTimeStr : "22:00";
        return "Dinner (" + s + "–" + e + ")";
    }

    public String getEventDateTimeDisplay() {
        String date = getEventDateDisplay();
        if (date.isBlank()) {
            return "";
        }
        String s = (startTimeStr != null && !startTimeStr.isBlank()) ? startTimeStr : "18:00";
        String e = (endTimeStr != null && !endTimeStr.isBlank()) ? endTimeStr : "22:00";
        return date + " – Dinner (" + s + "–" + e + ")";
    }

    // ===== Getters / Setters =====
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
        clampGuestCount(); // ép về [guestMin..guestMax]
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

    public String getSelectedEventTypeName() {
        return selectedEventTypeName;
    }

    public Integer getEventTypeId() {
        return selectedEventTypeId;
    }

    public void setEventTypeId(Integer eventTypeId) {
        this.selectedEventTypeId = eventTypeId;
    }

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

    public Long getSelectedComboId() {
        return selectedComboId;
    }

    public void setSelectedComboId(Long selectedComboId) {
        this.selectedComboId = selectedComboId;
    }

    // voucher getters/setters
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
        this.voucherDiscount = voucherDiscount;
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

    // time getters/setters
    public String getStartTimeStr() {
        return startTimeStr;
    }

    public void setStartTimeStr(String startTimeStr) {
        this.startTimeStr = startTimeStr;
    }

    public String getEndTimeStr() {
        return endTimeStr;
    }

    public void setEndTimeStr(String endTimeStr) {
        this.endTimeStr = endTimeStr;
    }

    public List<String> getStartTimeOptions() {
        return startTimeOptions;
    }

    public List<String> getEndTimeOptions() {
        return endTimeOptions;
    }

    public int getGuestMin() {
        return guestMin;
    }

    public int getGuestMax() {
        return guestMax;
    }

}
