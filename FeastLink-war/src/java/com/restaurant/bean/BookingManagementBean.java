package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Users;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;

import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.math.RoundingMode;
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
import com.mypack.entity.Restaurants;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Named("bookingManagementBean")
@ViewScoped
public class BookingManagementBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    // dùng để tìm nhà hàng của manager
    @EJB
    private RestaurantManagersFacadeLocal restaurantManagersFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    // id nhà hàng hiện tại (của manager đang login)
    private Integer currentRestaurantId;

    // tất cả booking của nhà hàng hiện tại
    private List<Bookings> bookings = new ArrayList<>();

    // booking đang được chọn xem chi tiết
    private Bookings selectedBooking;

    // filter / sort
    private String statusFilter = "All";      // All, PENDING, CONFIRMED, COMPLETED, CANCELLED
    private String sortBy = "newest";         // newest, oldest, event_date, highest_value
    private String searchQuery = "";          // text search
    private String timeFilter = "All";        // All, Today, This week, This month

    // paging
    private int rowsPerPage = 10;
    private int currentPage = 1;

    // header
    private Date lastUpdated;

    // lý do từ chối (optional, nhập trong prompt JS)
    private String rejectReason;

    // ===== Edit booking (manager) =====
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
    private String editMinDate;     // yyyy-MM-dd
    private Integer editGuestMin;
    private Integer editGuestMax;

    @PostConstruct
    public void init() {
        // 1. xác định nhà hàng hiện tại từ user login
        resolveCurrentRestaurant();
        // 2. load booking của nhà hàng đó
        loadBookings();
    }

    /**
     * Lấy Users đang login từ session ("currentUser") rồi tìm
     * RestaurantManagers để lấy restaurantId
     */
    private void resolveCurrentRestaurant() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx == null) {
                return;
            }

            Object obj = ctx.getExternalContext()
                    .getSessionMap()
                    .get("currentUser"); // LoginBean set key này

            if (!(obj instanceof Users)) {
                return;
            }

            Users user = (Users) obj;

            // tìm manager theo userId
            List<RestaurantManagers> managers = restaurantManagersFacade.findAll();
            if (managers == null) {
                return;
            }

            for (RestaurantManagers rm : managers) {
                if (rm == null || rm.getUserId() == null) {
                    continue;
                }
                if (rm.getUserId().getUserId() == null) {
                    continue;
                }

                if (rm.getUserId().getUserId().equals(user.getUserId())
                        && rm.getRestaurantId() != null
                        && rm.getRestaurantId().getRestaurantId() != null) {

                    Object ridObj = rm.getRestaurantId().getRestaurantId();
                    if (ridObj instanceof Number) {
                        currentRestaurantId = ((Number) ridObj).intValue();
                    } else {
                        try {
                            currentRestaurantId = Integer.valueOf(ridObj.toString());
                        } catch (NumberFormatException ignore) {
                            currentRestaurantId = null;
                        }
                    }
                    break;
                }
            }

            System.out.println("BookingManagementBean - currentRestaurantId = " + currentRestaurantId);

        } catch (Exception ex) {
            currentRestaurantId = null;
        }
    }

    public void loadBookings() {
        try {
            bookings = bookingsFacade.findAll();
            if (bookings == null) {
                bookings = new ArrayList<>();
            }

            // =============== LỌC THEO NHÀ HÀNG HIỆN TẠI ===============
            // Nếu đã xác định được nhà hàng của manager -> chỉ giữ booking của nhà hàng đó.
            // Nếu chưa xác định được (currentRestaurantId == null) thì GIỮ NGUYÊN,
            // để còn thấy tất cả booking (không bị clear như trước).
            if (currentRestaurantId != null) {
                final String ridStr = String.valueOf(currentRestaurantId);

                bookings.removeIf(b -> {
                    if (b == null || b.getRestaurantId() == null
                            || b.getRestaurantId().getRestaurantId() == null) {
                        // booking không gắn nhà hàng -> bỏ
                        return true;
                    }
                    String bidStr = String.valueOf(b.getRestaurantId().getRestaurantId());
                    // khác id nhà hàng hiện tại -> bỏ
                    return !bidStr.equals(ridStr);
                });
            }
            bookings.removeIf(b -> b != null
                    && b.getBookingStatus() != null
                    && "DRAFT".equalsIgnoreCase(b.getBookingStatus()));

        } catch (Exception ex) {
            bookings = new ArrayList<>();
        }
    }

    private int getMinDaysInAdvance(Bookings b) {
        Restaurants r = (b != null) ? b.getRestaurantId() : null;
        Integer v = (r != null) ? r.getMinDaysInAdvance() : null;
        return (v != null && v >= 0) ? v : 0;
    }

    private int resolveGuestMin(Restaurants r) {
        Integer minDb = (r != null) ? r.getMinGuestCount() : null;
        return (minDb != null && minDb > 0) ? minDb : 1;
    }

    private int resolveGuestMax(Restaurants r, int guestMin) {
        // Nếu manager bean đã có capacitySettingsFacade giống profile thì dùng luôn
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

    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }

        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }

        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // helper: kiểm tra booking thuộc nhà hàng hiện tại
    // helper: kiểm tra booking thuộc nhà hàng hiện tại
    private boolean belongsToCurrentRestaurant(Bookings b) {
        if (b == null || b.getRestaurantId() == null
                || b.getRestaurantId().getRestaurantId() == null) {
            return false;
        }

        // Nếu chưa xác định được nhà hàng hiện tại -> cho qua hết
        // (để các counter vẫn đếm bình thường)
        if (currentRestaurantId == null) {
            return true;
        }

        String ridStr = String.valueOf(currentRestaurantId);
        String bidStr = String.valueOf(b.getRestaurantId().getRestaurantId());
        return bidStr.equals(ridStr);
    }


    /* ================= FILTER + SORT ================= */
    private List<Bookings> getFilteredList() {
        List<Bookings> result = new ArrayList<>(bookings);

        // 1. Lọc theo status
        if (statusFilter != null && !"All".equalsIgnoreCase(statusFilter)) {
            final String sf = statusFilter.toUpperCase();
            result.removeIf(b -> {
                String st = (b.getBookingStatus() != null)
                        ? b.getBookingStatus().toUpperCase()
                        : "";
                return !st.equals(sf);
            });
        }

        // 2. Lọc theo search text
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            final String q = searchQuery.trim().toLowerCase();
            result.removeIf(b -> !matchesSearch(b, q));
        }

        // 3. Lọc theo timeFilter (All, Today, This week, This month) dựa vào EventDate
        if (timeFilter != null && !"All".equalsIgnoreCase(timeFilter)) {
            final Date today = truncateTime(new Date());

            result.removeIf(b -> {
                Date eventDate = truncateTime(b.getEventDate());
                if (eventDate == null) {
                    return true;
                }

                if ("Today".equalsIgnoreCase(timeFilter)) {
                    return !eventDate.equals(today);
                } else if ("This week".equalsIgnoreCase(timeFilter)) {
                    Date weekFromNow = addDays(today, 7);
                    return eventDate.before(today) || !eventDate.before(weekFromNow);
                } else if ("This month".equalsIgnoreCase(timeFilter)) {
                    return eventDate.getMonth() != today.getMonth()
                            || eventDate.getYear() != today.getYear();
                }

                return false;
            });
        }

        // 4. Sort
        if ("oldest".equalsIgnoreCase(sortBy)) {
            Collections.sort(result, Comparator.comparing(this::getSortCreatedDateSafe));
        } else if ("event_date".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparing(
                    Bookings::getEventDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        } else if ("highest_value".equalsIgnoreCase(sortBy)) {
            result.sort((a, b2) -> {
                BigDecimal ta = (a.getTotalAmount() != null) ? a.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal tb = (b2.getTotalAmount() != null) ? b2.getTotalAmount() : BigDecimal.ZERO;
                return tb.compareTo(ta);
            });
        } else {
            // newest (default)
            result.sort((a, b2) -> getSortCreatedDateSafe(b2)
                    .compareTo(getSortCreatedDateSafe(a)));
        }

        return result;
    }

    private boolean matchesSearch(Bookings b, String q) {
        if (b == null || q == null || q.isEmpty()) {
            return false;
        }

        if (containsIgnoreCase(b.getBookingCode(), q)) {
            return true;
        }

        try {
            if (containsIgnoreCase(b.getContactFullName(), q)) {
                return true;
            }
            if (containsIgnoreCase(b.getContactPhone(), q)) {
                return true;
            }
            if (containsIgnoreCase(b.getContactEmail(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getCustomerId() != null) {
                if (containsIgnoreCase(b.getCustomerId().getFullName(), q)) {
                    return true;
                }
                if (containsIgnoreCase(b.getCustomerId().getPhone(), q)) {
                    return true;
                }
                if (containsIgnoreCase(b.getCustomerId().getEmail(), q)) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getRestaurantId() != null
                    && containsIgnoreCase(b.getRestaurantId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getEventTypeId() != null
                    && containsIgnoreCase(b.getEventTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getServiceTypeId() != null
                    && containsIgnoreCase(b.getServiceTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (containsIgnoreCase(b.getOutsideAddress(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        return false;
    }

    private boolean containsIgnoreCase(String value, String q) {
        return value != null && q != null && value.toLowerCase().contains(q);
    }

    private Date getSortCreatedDateSafe(Bookings b) {
        try {
            if (b.getCreatedAt() != null) {
                return b.getCreatedAt();
            }
        } catch (Exception ignore) {
        }

        long idAsTime = (b.getBookingId() != null) ? b.getBookingId().longValue() : 0L;
        return new Date(idAsTime);
    }

    /* ================= PAGINATION ================= */
    public List<Bookings> getPagedBookings() {
        List<Bookings> filtered = getFilteredList();
        if (filtered.isEmpty()) {
            return filtered;
        }

        int from = (currentPage - 1) * rowsPerPage;
        if (from < 0) {
            from = 0;
        }
        if (from >= filtered.size()) {
            currentPage = 1;
            from = 0;
        }
        int to = Math.min(from + rowsPerPage, filtered.size());
        return filtered.subList(from, to);
    }

    public int getTotalFilteredBookings() {
        return getFilteredList().size();
    }

    public int getTotalPages() {
        int size = getTotalFilteredBookings();
        if (size == 0) {
            return 1;
        }
        return (int) Math.ceil((double) size / (double) rowsPerPage);
    }

    /* ================= STATS ================= */
    public int getUpcomingCount() {
        Date now = truncateTime(new Date());
        int count = 0;
        for (Bookings b : bookings) {
            if (b.getBookingStatus() != null
                    && "CONFIRMED".equalsIgnoreCase(b.getBookingStatus())
                    && b.getEventDate() != null
                    && truncateTime(b.getEventDate()).after(now)
                    && belongsToCurrentRestaurant(b)) {
                count++;
            }
        }
        return count;
    }

    public int getPendingCount() {
        return countByStatus("PENDING");
    }

    public int getCompletedCount() {
        return countByStatus("COMPLETED");
    }

    public int getCancelledCount() {
        return countByStatus("CANCELLED");
    }

    private int countByStatus(String status) {
        int c = 0;
        for (Bookings b : bookings) {
            if (b.getBookingStatus() != null
                    && status.equalsIgnoreCase(b.getBookingStatus())
                    && belongsToCurrentRestaurant(b)) {
                c++;
            }
        }
        return c;
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bookings b : getFilteredList()) {
            if (b == null) {
                continue;
            }

            String bookingStatus = (b.getBookingStatus() != null)
                    ? b.getBookingStatus().trim().toUpperCase()
                    : "";
            if (!"COMPLETED".equals(bookingStatus)) {
                continue;
            }

            String paymentStatus = (b.getPaymentStatus() != null)
                    ? b.getPaymentStatus().trim().toUpperCase()
                    : "";
            if (!"PAID_IN_FULL".equals(paymentStatus)) {
                continue;
            }

            BigDecimal total = b.getTotalAmount();
            if (total == null) {
                continue;
            }

            sum = sum.add(total);
        }
        return sum;
    }

    /* ================= EDIT (MANAGER) ================= */
    public boolean canEditBooking(Bookings b) {
        if (b == null) {
            return false;
        }

        // phải thuộc nhà hàng đang quản lý
        if (!belongsToCurrentRestaurant(b)) {
            return false;
        }

        String st = (b.getBookingStatus() != null) ? b.getBookingStatus().trim().toUpperCase() : "";
        String pay = (b.getPaymentStatus() != null) ? b.getPaymentStatus().trim().toUpperCase() : "";

        // chỉ cho sửa khi PENDING/CONFIRMED + UNPAID (an toàn nhất)
        if (!("PENDING".equals(st) || "CONFIRMED".equals(st))) {
            return false;
        }
        if (!"UNPAID".equals(pay)) {
            return false;
        }

        return true;
    }

    /**
     * Load booking vào form edit (AJAX)
     */
    public void prepareEdit(Bookings booking) {
        if (booking == null || booking.getBookingId() == null) {
            return;
        }

        try {
            Bookings fresh = bookingsFacade.find(booking.getBookingId());
            if (fresh == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not found", "Booking not found."));
                return;
            }

            if (!canEditBooking(fresh)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Not allowed",
                                "This booking cannot be edited at its current status."));
                return;
            }

            this.editBookingId = fresh.getBookingId();
            this.editEventDate = fresh.getEventDate();
            this.editStartTime = fresh.getStartTime();
            this.editEndTime = fresh.getEndTime();
            this.editGuestCount = fresh.getGuestCount();
            this.editLocationType = fresh.getLocationType();
            this.editOutsideAddress = fresh.getOutsideAddress();
            this.editNote = fresh.getNote();
            this.editContactFullName = fresh.getContactFullName();
            this.editContactEmail = fresh.getContactEmail();
            this.editContactPhone = fresh.getContactPhone();
            this.editSaveSuccess = false;

            // ===== constraints for edit UI (manager) =====
            Restaurants r = fresh.getRestaurantId();

// 1) min date = today + minDaysInAdvance
            int daysAdvance = getMinDaysInAdvance(fresh);
            LocalDate minAllowed = LocalDate.now().plusDays(daysAdvance);
            editMinDate = minAllowed.format(DateTimeFormatter.ISO_LOCAL_DATE);

// 2) guest range
            int gMin = resolveGuestMin(r);
            int gMax = resolveGuestMax(r, gMin);
            editGuestMin = gMin;
            editGuestMax = gMax;

        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Load failed", ex.getMessage()));
        }
    }

    /**
     * Save edit booking (AJAX)
     */
    public void saveEdit() {
        editSaveSuccess = false;

        if (editBookingId == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No booking", "No booking selected to edit."));
            return;
        }

        try {
            Bookings b = bookingsFacade.find(editBookingId);
            if (b == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Not found", "Booking not found."));
                return;
            }

            if (!canEditBooking(b)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_WARN, "Not allowed",
                                "This booking cannot be edited at its current status."));
                return;
            }

            // ===== validate basic =====
            if (editEventDate == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Event date is required."));
                return;
            }
            LocalDate eventDay = toLocalDate(editEventDate);
            LocalDate today = LocalDate.now();

// chặn quá khứ
            if (eventDay != null && eventDay.isBefore(today)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                "Event date cannot be in the past."));
                return;
            }

// chặn theo minDaysInAdvance của nhà hàng
            int daysAdvance = getMinDaysInAdvance(b);
            LocalDate minAllowed = today.plusDays(daysAdvance);
            if (eventDay != null && eventDay.isBefore(minAllowed)) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                "This venue requires booking at least " + daysAdvance + " days in advance."));
                return;
            }

            Restaurants r = b.getRestaurantId();
            int gMin = resolveGuestMin(r);
            int gMax = resolveGuestMax(r, gMin);

            if (editGuestCount == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                "Guest count is required."));
                return;
            }
            if (editGuestCount < gMin) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                "Guest count must be at least " + gMin + "."));
                return;
            }
            if (editGuestCount > gMax) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                "Guest count cannot exceed " + gMax + "."));
                return;
            }

            // validate time range (optional)
            if (editStartTime != null && editEndTime != null) {
                if (!editEndTime.after(editStartTime)) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                    "End time must be after start time."));
                    return;
                }
            }

            String lt = (editLocationType != null) ? editLocationType.trim().toUpperCase() : "";
            if (lt.isEmpty()) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation", "Location type is required."));
                return;
            }

            // outside address required if OUTSIDE
            if ("OUTSIDE".equals(lt)) {
                String oa = (editOutsideAddress != null) ? editOutsideAddress.trim() : "";
                if (oa.isEmpty()) {
                    FacesContext.getCurrentInstance().addMessage(null,
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation",
                                    "Outside address is required when location is OUTSIDE."));
                    return;
                }
                b.setOutsideAddress(oa);
            } else {
                // in-restaurant: clear outsideAddress
                b.setOutsideAddress(null);
            }

            // ===== apply changes =====
            int oldGuests = b.getGuestCount();
            BigDecimal oldTotal = b.getTotalAmount();
            BigDecimal oldDeposit = b.getDepositAmount();

// apply changes
            b.setEventDate(editEventDate);
            b.setStartTime(editStartTime);
            b.setEndTime(editEndTime);
            b.setGuestCount(editGuestCount);
            b.setLocationType(lt);

            b.setNote(trimToNull(editNote));
            b.setContactFullName(trimToNull(editContactFullName));
            b.setContactEmail(trimToNull(editContactEmail));
            b.setContactPhone(trimToNull(editContactPhone));
            recalculateAmountsAfterGuestChange(b, oldGuests, editGuestCount, oldTotal, oldDeposit);
            try {
                b.setUpdatedAt(new Date());
            } catch (Exception ignore) {
            }

            bookingsFacade.edit(b);
            loadBookings();
            editSaveSuccess = true;

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Booking updated successfully."));
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Save failed", ex.getMessage()));
        }
    }

    private void recalculateAmountsAfterGuestChange(Bookings b,
            int oldGuests,
            int newGuests,
            BigDecimal oldTotal,
            BigDecimal oldDeposit) {

        if (b == null) {
            return;
        }
        if (newGuests <= 0) {
            return;
        }

        BigDecimal total = BigDecimal.ZERO;
        boolean hasLines = false;

        // ====== 1) Update COMBO (thường là theo bàn) ======
        // Rule mặc định: 10 khách / 1 combo (giống data mẫu: 180 khách -> 18 combo)
        final int GUESTS_PER_TABLE = 10;
        int comboQty = (newGuests + GUESTS_PER_TABLE - 1) / GUESTS_PER_TABLE; // ceil

        if (b.getBookingCombosCollection() != null && !b.getBookingCombosCollection().isEmpty()) {
            for (BookingCombos bc : b.getBookingCombosCollection()) {
                if (bc == null) {
                    continue;
                }

                BigDecimal unit = (bc.getUnitPrice() != null) ? bc.getUnitPrice() : BigDecimal.ZERO;
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(comboQty)).setScale(2, RoundingMode.HALF_UP);

                bc.setQuantity(comboQty);
                bc.setTotalPrice(lineTotal);

                total = total.add(lineTotal);
            }
            hasLines = true;
        }

        // ====== 2) Update MENU ITEMS (thường tính theo người) ======
        if (b.getBookingMenuItemsCollection() != null && !b.getBookingMenuItemsCollection().isEmpty()) {
            for (BookingMenuItems mi : b.getBookingMenuItemsCollection()) {
                if (mi == null) {
                    continue;
                }

                BigDecimal unit = (mi.getUnitPrice() != null) ? mi.getUnitPrice() : BigDecimal.ZERO;

                // mặc định: mỗi món tính theo người => quantity = số khách
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(newGuests)).setScale(2, RoundingMode.HALF_UP);

                mi.setQuantity(newGuests);
                mi.setTotalPrice(lineTotal);

                total = total.add(lineTotal);
            }
            hasLines = true;
        }

        // ====== 3) Fallback nếu booking không có combo/menu lines ======
        if (!hasLines) {
            if (oldTotal != null && oldGuests > 0) {
                total = oldTotal
                        .multiply(BigDecimal.valueOf(newGuests))
                        .divide(BigDecimal.valueOf(oldGuests), 2, RoundingMode.HALF_UP);
            } else {
                total = oldTotal; // có gì giữ nguyên
            }
        }

        // set total
        b.setTotalAmount(total);

        // ====== 4) Recalc deposit/remaining theo tỷ lệ cũ (an toàn nhất) ======
        BigDecimal depositPercent;

        if (oldDeposit != null && oldTotal != null && oldTotal.compareTo(BigDecimal.ZERO) > 0) {
            // giữ nguyên % cọc theo booking cũ
            depositPercent = oldDeposit.divide(oldTotal, 4, RoundingMode.HALF_UP);
        } else {
            // fallback % cọc mặc định (bro muốn bao nhiêu chỉnh ở đây)
            depositPercent = new BigDecimal("0.30"); // 30%
        }

        BigDecimal deposit = total.multiply(depositPercent).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remaining = total.subtract(deposit).setScale(2, RoundingMode.HALF_UP);

        b.setDepositAmount(deposit);
        b.setRemainingAmount(remaining);
    }

    public void clearEdit() {
        editSaveSuccess = false;
        editBookingId = null;
        editEventDate = null;
        editStartTime = null;
        editEndTime = null;
        editGuestCount = null;
        editLocationType = null;
        editOutsideAddress = null;
        editNote = null;
        editContactFullName = null;
        editContactEmail = null;
        editContactPhone = null;
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


    /* ================= ACTIONS ================= */
    public String updateStatus(Bookings booking, String newStatus) {
        if (booking == null || newStatus == null) {
            return null;
        }

        try {
            Date now = new Date();

            booking.setBookingStatus(newStatus);
            try {
                booking.setUpdatedAt(now);
            } catch (Exception ignore) {
            }

            if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                String reason = (rejectReason != null) ? rejectReason.trim() : "";
                if (!reason.isEmpty()) {
                    try {
                        booking.setRejectReason(reason);
                    } catch (Exception ignore) {
                    }
                    try {
                        booking.setCancelReason(reason);
                    } catch (Exception ignore) {
                    }
                }
                try {
                    booking.setCancelTime(now);
                } catch (Exception ignore) {
                }
            }

            bookingsFacade.edit(booking);
            loadBookings();
            rejectReason = null;

            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "Booking status updated to " + newStatus
                    )
            );
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Update failed",
                            ex.getMessage()
                    )
            );
        }
        return null;
    }

    public String updatePaymentStatus(Bookings booking, String newStatus) {
        if (booking == null || newStatus == null) {
            return null;
        }

        try {
            Date now = new Date();

            booking.setPaymentStatus(newStatus);
            try {
                booking.setUpdatedAt(now);
            } catch (Exception ignore) {
            }

            bookingsFacade.edit(booking);
            loadBookings();

            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "Payment status updated to " + newStatus
                    )
            );
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Update payment failed",
                            ex.getMessage()
                    )
            );
        }
        return null;
    }

    // mở / đóng panel chi tiết
    public void selectBooking(Bookings booking) {
        this.selectedBooking = booking;
    }

    public void closeDetail() {
        this.selectedBooking = null;
    }

    /* ================= Menu & Package helper cho selectedBooking ================= */
    // Có package không?
    public boolean getHasSelectedPackage() {
        if (selectedBooking == null
                || selectedBooking.getBookingCombosCollection() == null) {
            return false;
        }
        return !selectedBooking.getBookingCombosCollection().isEmpty();
    }

    // Có custom menu không?
    public boolean getHasSelectedMenuItems() {
        if (selectedBooking == null
                || selectedBooking.getBookingMenuItemsCollection() == null) {
            return false;
        }
        return !selectedBooking.getBookingMenuItemsCollection().isEmpty();
    }

    // Tên package (lấy combo đầu tiên)
    public String getSelectedBookingPackageName() {
        if (!getHasSelectedPackage()) {
            return null;
        }

        BookingCombos bc = selectedBooking.getBookingCombosCollection()
                .iterator().next();

        // NOTE: nếu trong BookingCombos quan hệ tới MenuCombos tên khác "menuCombos"
        // thì đổi lại cho đúng, ví dụ: bc.getCombo().getName()
        if (bc.getMenuCombos() != null && bc.getMenuCombos().getName() != null) {
            return bc.getMenuCombos().getName();
        }
        return null;
    }

    // Tổng tiền package của booking này
    public BigDecimal getSelectedBookingPackageSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (!getHasSelectedPackage()) {
            return total;
        }

        for (BookingCombos bc : selectedBooking.getBookingCombosCollection()) {
            if (bc.getTotalPrice() != null) {
                total = total.add(bc.getTotalPrice());
            }
        }
        return total;
    }

    // Tổng tiền custom menu
    public BigDecimal getSelectedBookingMenuSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (!getHasSelectedMenuItems()) {
            return total;
        }

        for (BookingMenuItems bmi : selectedBooking.getBookingMenuItemsCollection()) {
            if (bmi.getTotalPrice() != null) {
                total = total.add(bmi.getTotalPrice());
            }
        }
        return total;
    }

    // Số món trong custom menu
    public int getSelectedBookingMenuItemCount() {
        if (!getHasSelectedMenuItems()) {
            return 0;
        }
        return selectedBooking.getBookingMenuItemsCollection().size();
    }


    /* ================= Date helper ================= */
    private Date truncateTime(Date d) {
        if (d == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        Date res = new Date(d.getYear(), d.getMonth(), d.getDate());
        return res;
    }

    private Date addDays(Date d, int days) {
        if (d == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        Date res = new Date(d.getYear(), d.getMonth(), d.getDate() + days);
        return res;
    }

    /* ================= GET / SET ================= */
    public List<Bookings> getBookings() {
        return bookings;
    }

    public void setBookings(List<Bookings> bookings) {
        this.bookings = bookings;
    }

    public Bookings getSelectedBooking() {
        return selectedBooking;
    }

    public void setSelectedBooking(Bookings selectedBooking) {
        this.selectedBooking = selectedBooking;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
        this.currentPage = 1;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
        this.currentPage = 1;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        this.currentPage = 1;
    }

    public String getTimeFilter() {
        return timeFilter;
    }

    public void setTimeFilter(String timeFilter) {
        this.timeFilter = timeFilter;
        this.currentPage = 1;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
        this.currentPage = 1;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getCurrentRestaurantId() {
        return currentRestaurantId;
    }

    public void setCurrentRestaurantId(Integer currentRestaurantId) {
        this.currentRestaurantId = currentRestaurantId;
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

    public String getEditMinDate() {
        return editMinDate;
    }

    public void setEditMinDate(String editMinDate) {
        this.editMinDate = editMinDate;
    }

    public Integer getEditGuestMin() {
        return editGuestMin;
    }

    public void setEditGuestMin(Integer editGuestMin) {
        this.editGuestMin = editGuestMin;
    }

    public Integer getEditGuestMax() {
        return editGuestMax;
    }

    public void setEditGuestMax(Integer editGuestMax) {
        this.editGuestMax = editGuestMax;
    }

}
