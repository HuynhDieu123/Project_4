package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.ServiceTypes;
import com.mypack.entity.Users;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import com.mypack.entity.Restaurants;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.time.temporal.ChronoUnit;
import jakarta.faces.view.ViewScoped;
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
import java.time.format.DateTimeFormatter;
import com.mypack.sessionbean.BookingCombosFacadeLocal;
import com.mypack.sessionbean.BookingMenuItemsFacadeLocal;

@Named("customerMyBookingsBean")
@ViewScoped
public class CustomerMyBookingsBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private BookingCombosFacadeLocal bookingCombosFacade;

    @EJB
    private BookingMenuItemsFacadeLocal bookingMenuItemsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    private List<Bookings> myBookings = new ArrayList<>();
    private List<Restaurants> bookedRestaurants = new ArrayList<>();

    // ===== EDIT (Customer) =====
    private Long editBookingId;
    private Date editEventDate;
    private Date editStartTime;
    private Date editEndTime;
    private Integer editGuestCount;
    private String editLocationType;
    private String editOutsideAddress;
    private String editNote;
    private String editContactFullName;
    private String editContactEmail;
    private String editContactPhone;
    private boolean editSaveSuccess;
    private Long cancelBookingId;
    private String cancelReason;
    private boolean cancelSuccess;
    private Bookings cancelBooking; // booking đang mở modal cancel
    private String editMinDate;        // yyyy-MM-dd cho input type="date"
    private Integer editGuestMin;      // min guests theo nhà hàng
    private Integer editGuestMax;      // max guests theo capacity settings
    private Long deleteBookingId;
    private boolean deleteSuccess;
    private Bookings deleteBooking; // booking đang mở modal delete

    // ========= PAGINATION =========
    // Số booking mỗi trang (bro muốn 5, 6, 10 tùy chỉnh)
    private int pageSize = 5;

    // Trang hiện tại (bắt đầu từ 1)
    private int currentPage = 1;

    private final Locale displayLocale = Locale.US;
    private final SimpleDateFormat shortDateFmt
            = new SimpleDateFormat("EEE, dd MMM yyyy", displayLocale);
    private final SimpleDateFormat longDateFmt
            = new SimpleDateFormat("EEEE, dd MMMM yyyy", displayLocale);
    private final SimpleDateFormat timeFmt
            = new SimpleDateFormat("HH:mm", displayLocale);
    private static final long serialVersionUID = 1L;

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
                    if (b == null || b.getCustomerId() == null) {
                        continue;
                    }
                    if (b.getCustomerId().getUserId()
                            .equals(currentUser.getUserId())) {
                        myBookings.add(b);
                    }
                }
            }

            // *** Sort: CREATED_AT DESC (booking mới tạo nhất lên đầu) ***
            Collections.sort(myBookings, new Comparator<Bookings>() {
                @Override
                public int compare(Bookings a, Bookings b) {
                    if (a == null && b == null) {
                        return 0;
                    }
                    if (a == null) {
                        return 1;
                    }
                    if (b == null) {
                        return -1;
                    }

                    Date c1 = a.getCreatedAt();
                    Date c2 = b.getCreatedAt();

                    if (c1 != null && c2 != null) {
                        return c2.compareTo(c1); // DESC
                    }
                    if (c1 == null && c2 != null) {
                        return 1;
                    }
                    if (c1 != null && c2 == null) {
                        return -1;
                    }

                    // fallback: bookingId DESC
                    Long id1 = a.getBookingId();
                    Long id2 = b.getBookingId();
                    if (id1 == null && id2 == null) {
                        return 0;
                    }
                    if (id1 == null) {
                        return 1;
                    }
                    if (id2 == null) {
                        return -1;
                    }
                    return id2.compareTo(id1);
                }
            });

            // Build danh sách nhà hàng đã booking (distinct)
            Map<Long, Restaurants> map = new LinkedHashMap<>();
            for (Bookings b : myBookings) {
                if (b == null || b.getRestaurantId() == null) {
                    continue;
                }
                Restaurants r = b.getRestaurantId();
                if (r.getRestaurantId() != null) {
                    map.putIfAbsent(r.getRestaurantId(), r);
                }
            }
            bookedRestaurants = new ArrayList<>(map.values());

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
            if (isFutureOrToday
                    && ("PENDING".equalsIgnoreCase(status)
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
        if (date == null) {
            return null;
        }

        // Nếu là java.sql.Date thì dùng toLocalDate() riêng
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }

        // Còn lại (java.util.Date bình thường) mới dùng toInstant()
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private int getMinDaysInAdvance(Bookings b) {
        Restaurants r = (b != null) ? b.getRestaurantId() : null;
        Integer v = (r != null) ? r.getMinDaysInAdvance() : null; // <-- Restaurants có getter này (profile bean cũng dùng)
        return (v != null && v >= 0) ? v : 0;
    }

    private int resolveGuestMin(Restaurants r) {
        Integer minDb = (r != null) ? r.getMinGuestCount() : null;
        return (minDb != null && minDb > 0) ? minDb : 1;
    }

    private int resolveGuestMax(Restaurants r, int guestMin) {
        Integer maxDb = null;
        try {
            if (capacitySettingsFacade != null && r != null) {
                RestaurantCapacitySettings s = capacitySettingsFacade.findByRestaurant(r);
                if (s != null) {
                    maxDb = s.getMaxGuestsPerSlot();
                }
            }
        } catch (Exception ignore) {
        }

        int guestMax = (maxDb != null && maxDb > 0) ? maxDb : (guestMin * 3);
        if (guestMax < guestMin) {
            guestMax = guestMin;
        }
        return guestMax;
    }

    private int getCancelFullRefundDays(Bookings b) {
        Restaurants r = (b != null) ? b.getRestaurantId() : null;
        Integer v = (r != null) ? r.getCancelFullRefundDays() : null;
        if (v == null || v < 0) {
            return 7; // default giống profile
        }
        return v;
    }

    private int getCancelPartialRefundDays(Bookings b) {
        Restaurants r = (b != null) ? b.getRestaurantId() : null;
        Integer v = (r != null) ? r.getCancelPartialRefundDays() : null;
        if (v == null || v < 0) {
            v = 3; // default giống profile
        }
        int full = getCancelFullRefundDays(b);
        if (v > full) {
            v = full; // clamp cho hợp lệ
        }
        return v;
    }

    public String getCancelPolicyHint() {
        if (cancelBooking == null) {
            return "";
        }
        int full = getCancelFullRefundDays(cancelBooking);
        int partial = getCancelPartialRefundDays(cancelBooking);

        if (partial == full) {
            return "Cancellation is allowed up to " + partial + " days before the event.";
        }
        return "Full refund if cancelled at least " + full
                + " days before the event. Partial refund if cancelled at least "
                + partial + " days before the event.";
    }

    public long daysUntilEvent(Bookings b) {
        if (b == null || b.getEventDate() == null) {
            return 0;
        }
        LocalDate event = toLocalDate(b.getEventDate());
        LocalDate today = LocalDate.now();
        return ChronoUnit.DAYS.between(today, event);
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
        if (value == null) {
            return "$0.00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(displayLocale);
        return nf.format(value);
    }

    // ========== PACKAGE & MENU CHO TỪNG BOOKING ==========
    public boolean hasPackage(Bookings b) {
        return b != null
                && b.getBookingCombosCollection() != null
                && !b.getBookingCombosCollection().isEmpty();
    }

    public boolean hasMenuItems(Bookings b) {
        return b != null
                && b.getBookingMenuItemsCollection() != null
                && !b.getBookingMenuItemsCollection().isEmpty();
    }

    // Tên package (giả sử 1 booking chỉ có 1 combo chính)
    public String getPackageName(Bookings b) {
        if (!hasPackage(b)) {
            return "No package selected";
        }
        BookingCombos bc = b.getBookingCombosCollection().iterator().next();
        if (bc.getMenuCombos() != null && bc.getMenuCombos().getName() != null) {
            return bc.getMenuCombos().getName();
        }
        return "Package";
    }

    // Tóm tắt custom menu: số món
    public String getMenuSummary(Bookings b) {
        if (!hasMenuItems(b)) {
            return "No custom menu selected";
        }
        int dishes = 0;
        for (BookingMenuItems bmi : b.getBookingMenuItemsCollection()) {
            if (bmi.getQuantity() > 0) {
                dishes++;
            }
        }
        return dishes + " dishes";
    }

    // Subtotal package cho booking (từ BookingCombos.TotalPrice)
    public BigDecimal getPackageSubtotal(Bookings b) {
        if (!hasPackage(b)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BookingCombos bc : b.getBookingCombosCollection()) {
            if (bc.getTotalPrice() != null) {
                sum = sum.add(bc.getTotalPrice());
            }
        }
        return sum;
    }

    // Subtotal custom menu (từ BookingMenuItems.TotalPrice)
    public BigDecimal getMenuSubtotal(Bookings b) {
        if (!hasMenuItems(b)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BookingMenuItems bmi : b.getBookingMenuItemsCollection()) {
            if (bmi.getTotalPrice() != null) {
                sum = sum.add(bmi.getTotalPrice());
            }
        }
        return sum;
    }

    // Other charges = Total - Package - Menu (không cho âm)
    public BigDecimal getOtherCharges(Bookings b) {
        if (b == null || b.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = b.getTotalAmount();
        BigDecimal pkg = getPackageSubtotal(b);
        BigDecimal menu = getMenuSubtotal(b);

        BigDecimal other = total.subtract(pkg.add(menu));
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return other;
    }

    // ===== Bridge methods cho EL gọi kiểu method-expression =====
    // dùng cho #{customerMyBookingsBean.packageName(b)}
    public String packageName(Bookings b) {
        return getPackageName(b);
    }

    // dùng cho #{customerMyBookingsBean.menuSummary(b)}
    public String menuSummary(Bookings b) {
        return getMenuSummary(b);
    }

    // dùng cho #{customerMyBookingsBean.packageSubtotal(b)}
    public BigDecimal packageSubtotal(Bookings b) {
        return getPackageSubtotal(b);
    }

    // dùng cho #{customerMyBookingsBean.menuSubtotal(b)}
    public BigDecimal menuSubtotal(Bookings b) {
        return getMenuSubtotal(b);
    }

    // dùng cho #{customerMyBookingsBean.otherCharges(b)}
    public BigDecimal otherCharges(Bookings b) {
        return getOtherCharges(b);
    }

    // ========== HELPER DÙNG TRONG EL ==========
    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    // Cho phép hủy: đang Pending/Confirmed và còn cách ngày tiệc >= 3 ngày
    public boolean canCancel(Bookings b) {
        if (b == null) {
            return false;
        }

        String status = safe(b.getBookingStatus()).toUpperCase();
        if (!("PENDING".equals(status) || "CONFIRMED".equals(status))) {
            return false;
        }

        // Không cho cancel nếu đã PAID full (deposit paid vẫn OK)
        String pay = safe(b.getPaymentStatus()).toUpperCase();
        if ("PAID".equals(pay) || "FULL_PAID".equals(pay) || "PAID_IN_FULL".equals(pay)) {
            return false;
        }

        return canCancelByDate(b);
    }

    private boolean canCancelByDate(Bookings b) {
        Date date = b.getEventDate();
        if (date == null) {
            return true;
        }

        LocalDate event = toLocalDate(date);
        LocalDate today = LocalDate.now();

        int minDays = getCancelPartialRefundDays(b); // ✅ lấy từ policy nhà hàng
        return !today.isAfter(event.minusDays(minDays));
    }

    public boolean isRemainingPositive(Bookings b) {
        if (b == null || b.getRemainingAmount() == null) {
            return false;
        }
        return b.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean needsPayment(Bookings b) {
        if (b == null) {
            return false;
        }
        String status = safe(b.getBookingStatus()).toUpperCase();
        String payment = safe(b.getPaymentStatus()).toUpperCase();
        return "CONFIRMED".equals(status)
                && !("PAID".equals(payment) || "FULL_PAID".equals(payment) || "PAID_IN_FULL".equals(payment));

    }

    public String statusMessage(Bookings b) {
        if (b == null) {
            return "";
        }
        String status = safe(b.getBookingStatus()).toUpperCase();

        if ("CONFIRMED".equals(status)) {
            int days = getCancelPartialRefundDays(b);
            return "Cancellation is allowed up to " + days + " days before the event.";
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
        if (b == null) {
            return "";
        }
        String locType = safe(b.getLocationType());
        if ("AT_RESTAURANT".equalsIgnoreCase(locType)) {
            return "At restaurant";
        }
        String outside = safe(b.getOutsideAddress());
        return outside.isEmpty() ? locType : outside;
    }

    public String eventTypeDisplay(Bookings b) {
        if (b == null) {
            return "";
        }
        EventTypes et = b.getEventTypeId();
        return (et != null) ? safe(et.getName()) : "";
    }

    public String serviceTypeDisplay(Bookings b) {
        if (b == null) {
            return "";
        }
        ServiceTypes st = b.getServiceTypeId();
        return (st != null) ? safe(st.getName()) : "";
    }

    public String bookerDisplay(Bookings b) {
        if (b == null) {
            return "";
        }

        Users u = b.getCustomerId();

        // ƯU TIÊN lấy contact lưu trong booking
        String fullName = safe(b.getContactFullName());
        String phone = safe(b.getContactPhone());
        String email = safe(b.getContactEmail());

        // Nếu booking cũ chưa có contact ⇒ fallback qua account
        if (fullName.isEmpty() && u != null) {
            fullName = safe(u.getFullName());
        }
        if (phone.isEmpty() && u != null) {
            phone = safe(u.getPhone());
        }
        if (email.isEmpty() && u != null) {
            email = safe(u.getEmail());
        }

        StringBuilder sb = new StringBuilder();
        if (!fullName.isEmpty()) {
            sb.append(fullName);
        }
        if (!phone.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(phone);
        }
        if (!email.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(email);
        }

        return sb.toString();
    }

    public boolean canDeleteQuotation(Bookings b) {
        if (b == null) {
            return false;
        }

        String st = safe(b.getBookingStatus()).trim().toUpperCase();
        String pay = safe(b.getPaymentStatus()).trim().toUpperCase();

        // quotation lưu từ "Save this quotation" => DRAFT + UNPAID
        if (!"DRAFT".equals(st)) {
            return false;
        }

        // Không cho xóa nếu đã có trạng thái paid (chặn chắc)
        if ("PAID".equals(pay) || "FULL_PAID".equals(pay) || "PAID_IN_FULL".equals(pay)) {
            return false;
        }

        return true;
    }


    /* ================= EDIT (CUSTOMER) ================= */
    public boolean canEditBooking(Bookings b) {
        if (b == null) {
            return false;
        }

        String st = safe(b.getBookingStatus()).trim().toUpperCase();
        String pay = safe(b.getPaymentStatus()).trim().toUpperCase();

        // chỉ cho edit khi PENDING + UNPAID
        return "PENDING".equals(st) && "UNPAID".equals(pay);
    }

    public void prepareEdit(Long bookingId) {
        editSaveSuccess = false;

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            return;
        }
        Users currentUser = (Users) userObj;

        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Missing booking id.");
            return;
        }

        Bookings b = bookingsFacade.find(bookingId);
        if (b == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            return;
        }

        // ownership check
        if (b.getCustomerId() == null || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot edit this booking.");
            return;
        }

        if (!canEditBooking(b)) {
            addMessage(FacesMessage.SEVERITY_WARN, "Not allowed", "This booking cannot be edited at its current status.");
            return;
        }

        // load into edit fields
        editBookingId = b.getBookingId();
        editEventDate = b.getEventDate();
        editStartTime = b.getStartTime();
        editEndTime = b.getEndTime();
        editGuestCount = b.getGuestCount();
        editLocationType = b.getLocationType();
        editOutsideAddress = b.getOutsideAddress();
        editNote = b.getNote();
        editContactFullName = b.getContactFullName();
        editContactEmail = b.getContactEmail();
        editContactPhone = b.getContactPhone();
        // ===== Compute constraints for UI =====
        int daysAdvance = getMinDaysInAdvance(b);
        LocalDate minAllowed = LocalDate.now().plusDays(daysAdvance);
        editMinDate = minAllowed.format(DateTimeFormatter.ISO_LOCAL_DATE);

        Restaurants r = b.getRestaurantId();
        int gMin = resolveGuestMin(r);
        int gMax = resolveGuestMax(r, gMin);
        editGuestMin = gMin;
        editGuestMax = gMax;

    }

    public void prepareDeleteQuotation(Long bookingId) {
        deleteSuccess = false;
        deleteBookingId = bookingId;
        deleteBooking = null;

        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Missing booking id.");
            return;
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            deleteBookingId = null;
            return;
        }
        Users currentUser = (Users) userObj;

        Bookings b = bookingsFacade.find(bookingId);
        if (b == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            deleteBookingId = null;
            return;
        }

        // ownership check
        if (b.getCustomerId() == null || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot delete this booking.");
            deleteBookingId = null;
            return;
        }

        if (!canDeleteQuotation(b)) {
            addMessage(FacesMessage.SEVERITY_WARN, "Not allowed", "Only draft quotations (unpaid) can be deleted.");
            deleteBookingId = null;
            return;
        }

        deleteBooking = b; // giữ để show info trong modal
    }

    /**
     * Save edit (AJAX). Nếu OK thì JS sẽ reload page
     */
    public void saveEdit() {
        editSaveSuccess = false;

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            return;
        }
        Users currentUser = (Users) userObj;

        if (editBookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No booking selected.");
            return;
        }

        Bookings b = bookingsFacade.find(editBookingId);
        if (b == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            return;
        }

        // ownership check
        if (b.getCustomerId() == null || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot edit this booking.");
            return;
        }

        if (!canEditBooking(b)) {
            addMessage(FacesMessage.SEVERITY_WARN, "Not allowed", "This booking cannot be edited at its current status.");
            return;
        }

        // ===== validate basic =====
        if (editEventDate == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Event date is required.");
            return;
        }

        // ===== validate event date: not in past + respect MinDaysInAdvance =====
        LocalDate showDate = toLocalDate(editEventDate);
        LocalDate today = LocalDate.now();

        if (showDate != null && showDate.isBefore(today)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Event date cannot be in the past.");
            return;
        }

        int daysAdvance = getMinDaysInAdvance(b);
        LocalDate minAllowed = today.plusDays(daysAdvance);
        if (showDate != null && showDate.isBefore(minAllowed)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                    "This venue requires booking at least " + daysAdvance + " days in advance.");
            return;
        }

        Restaurants r = b.getRestaurantId();
        int gMin = resolveGuestMin(r);
        int gMax = resolveGuestMax(r, gMin);

        if (editGuestCount == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Guest count is required.");
            return;
        }
        if (editGuestCount < gMin) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                    "Guest count must be at least " + gMin + ".");
            return;
        }
        if (editGuestCount > gMax) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                    "Guest count cannot exceed " + gMax + ".");
            return;
        }

        if (editStartTime != null && editEndTime != null && !editEndTime.after(editStartTime)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "End time must be after start time.");
            return;
        }

        String lt = (editLocationType != null) ? editLocationType.trim().toUpperCase() : "";
        if (lt.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Location type is required.");
            return;
        }

        if ("OUTSIDE".equals(lt)) {
            String oa = (editOutsideAddress != null) ? editOutsideAddress.trim() : "";
            if (oa.isEmpty()) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Outside address is required when location is OUTSIDE.");
                return;
            }
            b.setOutsideAddress(oa);
        } else {
            b.setOutsideAddress(null);
            lt = "AT_RESTAURANT"; // chuẩn hoá theo DB của bro
        }

        int oldGuests = b.getGuestCount();
        BigDecimal oldTotal = (b.getTotalAmount() != null) ? b.getTotalAmount() : BigDecimal.ZERO;

        // ===== apply changes =====
        b.setEventDate(editEventDate);
        b.setStartTime(editStartTime);
        b.setEndTime(editEndTime);
        b.setGuestCount(editGuestCount);
        b.setLocationType(lt);

        b.setNote(trimToNull(editNote));
        b.setContactFullName(trimToNull(editContactFullName));
        b.setContactEmail(trimToNull(editContactEmail));
        b.setContactPhone(trimToNull(editContactPhone));

        b.setUpdatedAt(new Date());

        // ===== recalc money if guest changed (PENDING+UNPAID) =====
        if (oldGuests != editGuestCount) {
            BigDecimal oldPkg = sumComboTotal(b);
            BigDecimal oldMenu = sumMenuTotal(b);
            BigDecimal oldOther = oldTotal.subtract(oldPkg.add(oldMenu));
            if (oldOther.compareTo(BigDecimal.ZERO) < 0) {
                oldOther = BigDecimal.ZERO;
            }

            recalcTotalsByGuest(b, oldGuests, editGuestCount, oldOther);
        }

        bookingsFacade.edit(b);

        addMessage(FacesMessage.SEVERITY_INFO, "Success", "Booking updated successfully.");
        editSaveSuccess = true;
    }

    public boolean isEditOutside() {
        return editLocationType != null && "OUTSIDE".equalsIgnoreCase(editLocationType);
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

// ===== SUM helpers =====
    private BigDecimal sumComboTotal(Bookings b) {
        BigDecimal sum = BigDecimal.ZERO;
        if (b.getBookingCombosCollection() == null) {
            return sum;
        }
        for (BookingCombos bc : b.getBookingCombosCollection()) {
            if (bc != null && bc.getTotalPrice() != null) {
                sum = sum.add(bc.getTotalPrice());
            }
        }
        return sum;
    }

    private BigDecimal sumMenuTotal(Bookings b) {
        BigDecimal sum = BigDecimal.ZERO;
        if (b.getBookingMenuItemsCollection() == null) {
            return sum;
        }
        for (BookingMenuItems mi : b.getBookingMenuItemsCollection()) {
            if (mi != null && mi.getTotalPrice() != null) {
                sum = sum.add(mi.getTotalPrice());
            }
        }
        return sum;
    }

// ===== Recalc by guest count without needing unitPrice getter =====
    private void recalcTotalsByGuest(Bookings b, int oldGuests, int newGuests, BigDecimal otherCharges) {
        if (b == null || newGuests <= 0) {
            return;
        }

        // combos: theo bàn (default 10 khách/bàn)
        final int GUESTS_PER_TABLE = 10;
        int newTables = (newGuests + GUESTS_PER_TABLE - 1) / GUESTS_PER_TABLE;

        BigDecimal newPkg = BigDecimal.ZERO;
        if (b.getBookingCombosCollection() != null) {
            for (BookingCombos bc : b.getBookingCombosCollection()) {
                if (bc == null) {
                    continue;
                }

                int oldQty = bc.getQuantity();
                if (oldQty <= 0) {
                    oldQty = 1;
                }

                BigDecimal oldLine = (bc.getTotalPrice() != null) ? bc.getTotalPrice() : BigDecimal.ZERO;
                BigDecimal unit = oldLine.divide(BigDecimal.valueOf(oldQty), 2, java.math.RoundingMode.HALF_UP);

                bc.setQuantity(newTables);
                bc.setTotalPrice(unit.multiply(BigDecimal.valueOf(newTables)));

                newPkg = newPkg.add(bc.getTotalPrice());
            }
        }

        // menu items: mặc định theo người (quantity = guests)
        BigDecimal newMenu = BigDecimal.ZERO;
        if (b.getBookingMenuItemsCollection() != null) {
            for (BookingMenuItems mi : b.getBookingMenuItemsCollection()) {
                if (mi == null) {
                    continue;
                }

                int oldQty = mi.getQuantity();
                if (oldQty <= 0) {
                    oldQty = (oldGuests > 0 ? oldGuests : 1);
                }

                BigDecimal oldLine = (mi.getTotalPrice() != null) ? mi.getTotalPrice() : BigDecimal.ZERO;
                BigDecimal unit = oldLine.divide(BigDecimal.valueOf(oldQty), 2, java.math.RoundingMode.HALF_UP);

                mi.setQuantity(newGuests);
                mi.setTotalPrice(unit.multiply(BigDecimal.valueOf(newGuests)));

                newMenu = newMenu.add(mi.getTotalPrice());
            }
        }

        BigDecimal newTotal = newPkg.add(newMenu).add(otherCharges != null ? otherCharges : BigDecimal.ZERO);
        b.setTotalAmount(newTotal);

        // UNPAID => giữ depositAmount như hiện có, remaining = total - depositPaid
        BigDecimal depositPaid = (b.getDepositAmount() != null) ? b.getDepositAmount() : BigDecimal.ZERO;
        b.setRemainingAmount(newTotal.subtract(depositPaid));
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

//    // ========== ACTION: CANCEL BOOKING ==========
//    public String cancelBooking(Long bookingId) {
//        FacesContext ctx = FacesContext.getCurrentInstance();
//
//        if (bookingId == null) {
//            addMessage(FacesMessage.SEVERITY_ERROR,
//                    "Error", "Missing booking id.");
//            return null;
//        }
//
//        Bookings booking = bookingsFacade.find(bookingId);
//        if (booking == null) {
//            addMessage(FacesMessage.SEVERITY_ERROR,
//                    "Error", "Booking not found.");
//            return null;
//        }
//
//        // Check quyền: chỉ được hủy booking của chính mình
//        Object userObj = ctx.getExternalContext()
//                .getSessionMap().get("currentUser");
//        if (!(userObj instanceof Users)) {
//            addMessage(FacesMessage.SEVERITY_ERROR,
//                    "Error", "You need to log in again.");
//            return "/login";
//        }
//
//        Users currentUser = (Users) userObj;
//        if (booking.getCustomerId() == null
//                || !booking.getCustomerId().getUserId()
//                        .equals(currentUser.getUserId())) {
//            addMessage(FacesMessage.SEVERITY_ERROR,
//                    "Error", "You cannot cancel this booking.");
//            return null;
//        }
//
//        // Check rule hủy
//        if (!canCancel(booking)) {
//            addMessage(FacesMessage.SEVERITY_WARN,
//                    "Cannot cancel",
//                    "This booking can no longer be cancelled online.");
//            return null;
//        }
//
//        // Cập nhật trạng thái
//        booking.setBookingStatus("CANCELLED");
//        booking.setCancelReason("Cancelled by customer via My Bookings page.");
//        booking.setCancelTime(new Date());
//        booking.setUpdatedAt(new Date());
//
//        bookingsFacade.edit(booking);
//        
//
//        addMessage(FacesMessage.SEVERITY_INFO,
//                "Booking cancelled",
//                "Your booking has been cancelled. Refund policy will follow the venue's rules.");
//
//        // load lại trang danh sách
//        return "/Customer/my-bookings";
//    }
    public void prepareCancel(Long bookingId) {
        cancelSuccess = false;
        cancelBookingId = bookingId;
        cancelReason = "";

        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Missing booking id.");
            return;
        }

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            return;
        }

        Bookings booking = bookingsFacade.find(bookingId);
        this.cancelBooking = booking;
        if (booking == null) {
            this.cancelBooking = null;
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            return;
        }

        Users currentUser = (Users) userObj;
        if (booking.getCustomerId() == null
                || !booking.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            this.cancelBooking = null;
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot cancel this booking.");
            cancelBookingId = null;
            return;
        }

        if (!canCancel(booking)) {
            this.cancelBooking = null;
            addMessage(FacesMessage.SEVERITY_WARN, "Cannot cancel", "This booking can no longer be cancelled online.");
            cancelBookingId = null;
            return;
        }
    }

    public void confirmCancel() {
        cancelSuccess = false;

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            return;
        }
        Users currentUser = (Users) userObj;

        if (cancelBookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No booking selected.");
            return;
        }

        String reason = (cancelReason != null) ? cancelReason.trim() : "";
        if (reason.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Please enter a cancellation reason.");
            return;
        }

        Bookings booking = bookingsFacade.find(cancelBookingId);
        if (booking == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            return;
        }

        if (booking.getCustomerId() == null
                || !booking.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot cancel this booking.");
            return;
        }

        if (!canCancel(booking)) {
            addMessage(FacesMessage.SEVERITY_WARN, "Cannot cancel", "This booking can no longer be cancelled online.");
            return;
        }

        booking.setBookingStatus("CANCELLED");
        booking.setCancelReason(reason); // <-- dùng reason nhập
        booking.setCancelTime(new Date());
        booking.setUpdatedAt(new Date());
        bookingsFacade.edit(booking);
        this.cancelBooking = null;  // ✅ reset booking đang cancel
        cancelSuccess = true;
        addMessage(FacesMessage.SEVERITY_INFO, "Booking cancelled", "Your booking has been cancelled.");
        cancelBookingId = null;
        cancelReason = "";

    }

    // Dùng cho sort: trả về millis của ngày tạo booking (hoặc eventDate nếu createdAt = null)
    public long bookingCreatedAtMillis(Bookings b) {
        if (b == null) {
            return 0L;
        }

        Date created = b.getCreatedAt();
        if (created != null) {
            return created.getTime();
        }

        // fallback: dùng bookingId cho đúng "mới nhất" (identity tăng dần)
        if (b.getBookingId() != null) {
            return b.getBookingId() * 1000L;
        }

        Date event = b.getEventDate();
        return (event != null) ? event.getTime() : 0L;
    }

    public void confirmDeleteQuotation() {
        deleteSuccess = false;

        FacesContext ctx = FacesContext.getCurrentInstance();
        Object userObj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please login again.");
            return;
        }
        Users currentUser = (Users) userObj;

        if (deleteBookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "No booking selected.");
            return;
        }

        Bookings b = bookingsFacade.find(deleteBookingId);
        if (b == null) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Booking not found.");
            return;
        }

        if (b.getCustomerId() == null || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "You cannot delete this booking.");
            return;
        }

        if (!canDeleteQuotation(b)) {
            addMessage(FacesMessage.SEVERITY_WARN, "Not allowed", "Only draft quotations (unpaid) can be deleted.");
            return;
        }

        try {
            // 1) delete children first (avoid FK conflicts)
            if (b.getBookingMenuItemsCollection() != null) {
                for (BookingMenuItems mi : new ArrayList<>(b.getBookingMenuItemsCollection())) {
                    if (mi != null) {
                        bookingMenuItemsFacade.remove(mi);
                    }
                }
            }

            if (b.getBookingCombosCollection() != null) {
                for (BookingCombos bc : new ArrayList<>(b.getBookingCombosCollection())) {
                    if (bc != null) {
                        bookingCombosFacade.remove(bc);
                    }
                }
            }

            // 2) delete booking
            bookingsFacade.remove(b);

            // 3) update local list to avoid stale UI (optional, JS reload cũng ok)
            if (myBookings != null) {
                myBookings.removeIf(x -> x != null && x.getBookingId() != null && x.getBookingId().equals(deleteBookingId));
            }

            deleteSuccess = true;
            addMessage(FacesMessage.SEVERITY_INFO, "Deleted", "Quotation deleted successfully.");

            deleteBookingId = null;
            deleteBooking = null;

        } catch (Exception ex) {
            deleteSuccess = false;
            addMessage(FacesMessage.SEVERITY_ERROR, "Delete failed",
                    "Cannot delete this quotation right now. Please try again.");
        }
    }

    private void addMessage(FacesMessage.Severity severity,
            String summary,
            String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(severity, summary, detail));
    }
    // ========= PAGINATION HELPERS =========

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (pageSize > 0) {
            this.pageSize = pageSize;
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    // Tổng số booking (dùng hiển thị "X bookings")
    public int getTotalRecords() {
        return (myBookings != null) ? myBookings.size() : 0;
    }

    // Tổng số trang
    public int getTotalPages() {
        int totalRecords = getTotalRecords();
        if (totalRecords == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalRecords / (double) pageSize);
    }

    // Danh sách booking chỉ cho trang hiện tại
    public List<Bookings> getPagedBookings() {
        if (myBookings == null || myBookings.isEmpty()) {
            return new ArrayList<>();
        }

        int totalPages = getTotalPages();

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, myBookings.size());

        return myBookings.subList(fromIndex, toIndex);
    }

    // List số trang: [1, 2, 3, ...] để ui:repeat vẽ button
    public List<Integer> getPageNumbers() {
        List<Integer> pages = new ArrayList<>();
        int totalPages = getTotalPages();
        for (int i = 1; i <= totalPages; i++) {
            pages.add(i);
        }
        return pages;
    }

    // Bấm qua trang kế
    public String nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
        }
        return null; // ở lại trang hiện tại
    }

    // Bấm về trang trước
    public String previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
        return null;
    }

    // Bấm trực tiếp số trang
    public String changePage() {
        // currentPage đã được set bởi f:setPropertyActionListener
        return null;
    }

    public List<Restaurants> getBookedRestaurants() {
        return bookedRestaurants != null ? bookedRestaurants : new ArrayList<>();
    }

    public Long getEditBookingId() {
        return editBookingId;
    }

    public void setEditBookingId(Long editBookingId) {
        this.editBookingId = editBookingId;
    }

    public Date getEditEventDate() {
        return editEventDate;
    }

    public void setEditEventDate(Date editEventDate) {
        this.editEventDate = editEventDate;
    }

    public Date getEditStartTime() {
        return editStartTime;
    }

    public void setEditStartTime(Date editStartTime) {
        this.editStartTime = editStartTime;
    }

    public Date getEditEndTime() {
        return editEndTime;
    }

    public void setEditEndTime(Date editEndTime) {
        this.editEndTime = editEndTime;
    }

    public Integer getEditGuestCount() {
        return editGuestCount;
    }

    public void setEditGuestCount(Integer editGuestCount) {
        this.editGuestCount = editGuestCount;
    }

    public String getEditLocationType() {
        return editLocationType;
    }

    public void setEditLocationType(String editLocationType) {
        this.editLocationType = editLocationType;
    }

    public String getEditOutsideAddress() {
        return editOutsideAddress;
    }

    public void setEditOutsideAddress(String editOutsideAddress) {
        this.editOutsideAddress = editOutsideAddress;
    }

    public String getEditNote() {
        return editNote;
    }

    public void setEditNote(String editNote) {
        this.editNote = editNote;
    }

    public String getEditContactFullName() {
        return editContactFullName;
    }

    public void setEditContactFullName(String editContactFullName) {
        this.editContactFullName = editContactFullName;
    }

    public String getEditContactEmail() {
        return editContactEmail;
    }

    public void setEditContactEmail(String editContactEmail) {
        this.editContactEmail = editContactEmail;
    }

    public String getEditContactPhone() {
        return editContactPhone;
    }

    public void setEditContactPhone(String editContactPhone) {
        this.editContactPhone = editContactPhone;
    }

    public boolean isEditSaveSuccess() {
        return editSaveSuccess;
    }

    public void setEditSaveSuccess(boolean editSaveSuccess) {
        this.editSaveSuccess = editSaveSuccess;
    }

    public Long getCancelBookingId() {
        return cancelBookingId;
    }

    public void setCancelBookingId(Long cancelBookingId) {
        this.cancelBookingId = cancelBookingId;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public boolean isCancelSuccess() {
        return cancelSuccess;
    }

    public String getEditMinDate() {
        return editMinDate;
    }

    public Integer getEditGuestMin() {
        return editGuestMin;
    }

    public Integer getEditGuestMax() {
        return editGuestMax;
    }

    public Long getDeleteBookingId() {
        return deleteBookingId;
    }

    public void setDeleteBookingId(Long deleteBookingId) {
        this.deleteBookingId = deleteBookingId;
    }

    public boolean isDeleteSuccess() {
        return deleteSuccess;
    }

    public void setDeleteSuccess(boolean deleteSuccess) {
        this.deleteSuccess = deleteSuccess;
    }

    public Bookings getDeleteBooking() {
        return deleteBooking;
    }

    public void setDeleteBooking(Bookings deleteBooking) {
        this.deleteBooking = deleteBooking;
    }

}
