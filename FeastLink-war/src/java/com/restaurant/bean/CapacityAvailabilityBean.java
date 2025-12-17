package com.restaurant.bean;

import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.entity.RestaurantDayCapacity;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
import com.mypack.sessionbean.RestaurantDayCapacityFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    // ========= EJB =========
    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    @EJB
    private RestaurantDayCapacityFacadeLocal dayCapacityFacade;

    @EJB
    private com.mypack.sessionbean.BookingsFacadeLocal bookingsFacade;

    // ========= FIELDS =========
    private Restaurants currentRestaurant;
    private RestaurantCapacitySettings currentSettings;

    // limit mặc định
    private int defaultMaxGuestsPerDay;
    private int defaultMaxBookingsPerDay;

    // form block range
    private String blockStartDate;   // yyyy-MM-dd
    private String blockEndDate;     // yyyy-MM-dd
    private String blockHall;        // chưa dùng

    // calendar
    private YearMonth currentMonth;
    private List<CalendarDay> calendarDays;
    private Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();

    // ✅ NEW: auto usage từ Bookings theo ngày
    private Map<LocalDate, Usage> bookingUsageByDate = new HashMap<>();

    private static class Usage implements Serializable {
        int guests;
        int bookings;

        Usage(int guests, int bookings) {
            this.guests = guests;
            this.bookings = bookings;
        }
    }

    // selected day + input
    private LocalDate selectedDate;
    private String selectedStatus;
    private Integer inputGuestCount;
    private Integer inputBookingCount;

    // slot được chọn (ALLDAY / LUNCH / EVENING)
    // ⚠️ Bookings hiện chưa có slot -> tạm thời slot nào cũng tính theo tổng ngày
    private String selectedSlot = "ALLDAY";

    // Helper: lấy nhà hàng theo user login
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;

        Map<String, Object> session = ctx.getExternalContext().getSessionMap();
        Users currentUser = (Users) session.get("currentUser");
        if (currentUser == null || currentUser.getEmail() == null) return null;

        String email = currentUser.getEmail();

        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null && r.getEmail().equalsIgnoreCase(email)) {
                return r;
            }
        }
        return null;
    }

    // ========= INIT =========
    @PostConstruct
    public void init() {
        try {
            currentRestaurant = resolveCurrentRestaurant();
            if (currentRestaurant == null) return;

            currentMonth = YearMonth.now();
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = "--";
            selectedSlot = "ALLDAY";

            loadSettingsFromDb();
            loadCalendarFromDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    // LOAD SETTINGS + CALENDAR FROM DB
    // ----------------------------------------------------
    private void loadSettingsFromDb() {
        currentSettings = capacitySettingsFacade.findByRestaurant(currentRestaurant);

        if (currentSettings != null) {
            defaultMaxGuestsPerDay = Optional.ofNullable(currentSettings.getMaxGuestsPerSlot()).orElse(150);
            defaultMaxBookingsPerDay = Optional.ofNullable(currentSettings.getMaxBookingsPerDay()).orElse(10);
        } else {
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
        }
    }

    // ✅ NEW: Status KHÔNG tính vào capacity (bạn chỉnh nếu muốn)
    // Ví dụ nếu bạn KHÔNG muốn tính PENDING thì thêm "PENDING" vào đây.
    private List<String> getExcludedBookingStatuses() {
        return Arrays.asList("CANCELLED", "REJECTED");
    }

    // ✅ NEW: load tổng khách + tổng booking từ Bookings theo tháng
    private void loadBookingUsageForMonth(LocalDate first, LocalDate last) {
        bookingUsageByDate.clear();

        List<Object[]> rows = bookingsFacade.sumUsageByRestaurantAndDateRange(
                currentRestaurant,
                java.sql.Date.valueOf(first),
                java.sql.Date.valueOf(last),
                getExcludedBookingStatuses()
        );

        if (rows == null) return;

        for (Object[] r : rows) {
            // r[0] = eventDate (Date)
            // r[1] = SUM(guestCount) (Number)
            // r[2] = COUNT(bookings) (Number)
            Date d = (Date) r[0];
            Number sumGuests = (Number) r[1];
            Number cntBookings = (Number) r[2];

            LocalDate date = toLocalDate(d);
            int g = (sumGuests == null) ? 0 : sumGuests.intValue();
            int b = (cntBookings == null) ? 0 : cntBookings.intValue();

            bookingUsageByDate.put(date, new Usage(g, b));
        }
    }

    // ✅ NEW: lấy usage 1 ngày (nếu không nằm trong map thì query 1 ngày)
    private Usage getBookingUsage(LocalDate date) {
        if (currentRestaurant == null || date == null) return new Usage(0, 0);

        Usage u = bookingUsageByDate.get(date);
        if (u != null) return u;

        List<Object[]> rows = bookingsFacade.sumUsageByRestaurantAndDateRange(
                currentRestaurant,
                java.sql.Date.valueOf(date),
                java.sql.Date.valueOf(date),
                getExcludedBookingStatuses()
        );

        if (rows == null || rows.isEmpty()) return new Usage(0, 0);

        Object[] r = rows.get(0);
        Number sumGuests = (Number) r[1];
        Number cntBookings = (Number) r[2];

        return new Usage(sumGuests == null ? 0 : sumGuests.intValue(),
                         cntBookings == null ? 0 : cntBookings.intValue());
    }

    private void loadCalendarFromDb() {
        dayStatusMap.clear();

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        // 1) Load từ RestaurantDayCapacity (manual + blocked)
        List<RestaurantDayCapacity> list =
                dayCapacityFacade.findByRestaurantAndDateRange(
                        currentRestaurant,
                        java.sql.Date.valueOf(first),
                        java.sql.Date.valueOf(last)
                );

        // Nếu một ngày có nhiều slot -> lấy status "nặng" nhất
        for (RestaurantDayCapacity d : list) {
            LocalDate date = toLocalDate(d.getEventDate());
            StatusType st = computeStatusForRow(d);

            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            StatusType merged = maxStatus(existing, st);

            if (merged != StatusType.NONE) {
                dayStatusMap.put(date, merged);
            }
        }

        // 2) ✅ Load auto từ Bookings theo tháng
        loadBookingUsageForMonth(first, last);

        // 3) ✅ Merge status từ Bookings (ưu tiên maxStatus; BLOCKED vẫn giữ)
        for (Map.Entry<LocalDate, Usage> e : bookingUsageByDate.entrySet()) {
            LocalDate date = e.getKey();
            Usage u = e.getValue();
            if (u == null) continue;

            // không có booking thì khỏi vẽ dot (tránh xanh hết lịch)
            if (u.guests == 0 && u.bookings == 0) continue;

            StatusType bookingStatus = computeStatus(
                    u.guests,
                    u.bookings,
                    defaultMaxGuestsPerDay,
                    defaultMaxBookingsPerDay
            );

            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            dayStatusMap.put(date, maxStatus(existing, bookingStatus));
        }

        buildCalendar();
    }

    // ===== NEW: MinDaysInAdvance =====
    private int getMinDaysInAdvanceSafe() {
        if (currentRestaurant == null) return 0;
        Integer v = currentRestaurant.getMinDaysInAdvance(); // column MinDaysInAdvance
        return (v != null && v > 0) ? v : 0;
    }

    // Theo ví dụ: hôm nay 15, min=3 -> 16,17,18 bị chặn; 19 mới được đặt
    private boolean isTooSoonByMinAdvance(LocalDate date) {
        if (date == null) return false;

        int min = getMinDaysInAdvanceSafe();
        if (min <= 0) return false;

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(min);

        return date.isAfter(today) && !date.isAfter(cutoff);
    }

    private StatusType applyMinAdvanceOverlay(LocalDate date, StatusType base) {
        if (!isTooSoonByMinAdvance(date)) return base;

        // Nếu đã blocked/full sẵn thì giữ nguyên
        if (base == StatusType.BLOCKED || base == StatusType.FULL) return base;

        return StatusType.TOO_SOON;
    }

    private void buildCalendar() {
        calendarDays = new ArrayList<>();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);
            boolean inMonth = date.getMonthValue() == currentMonth.getMonthValue();
            boolean isSelected = (selectedDate != null && selectedDate.equals(date));

            StatusType baseStatus = dayStatusMap.getOrDefault(date, StatusType.NONE);
            StatusType effectiveStatus = applyMinAdvanceOverlay(date, baseStatus);
            boolean tooSoon = isTooSoonByMinAdvance(date);

            calendarDays.add(new CalendarDay(date, inMonth, effectiveStatus, isSelected, tooSoon));
        }
    }

    // ----------------------------------------------------
    // ACTIONS (calendar)
    // ----------------------------------------------------
    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        loadCalendarFromDb();
    }

    public void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        loadCalendarFromDb();
    }

    public void selectDay(String dateIso) {
        if (dateIso == null || dateIso.isBlank()) return;

        LocalDate d = LocalDate.parse(dateIso);

        // ✅ chặn click các ngày chưa đủ MinDaysInAdvance
        if (isTooSoonByMinAdvance(d)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN,
                            "Ngày này chưa đủ số ngày đặt trước (MinDaysInAdvance).", null));
            return;
        }

        selectedDate = d;
        loadSelectedDayData();
        buildCalendar();
    }

    // Gọi khi đổi slot trên UI
    public void onSlotChange() {
        loadSelectedDayData(); // slot chưa có trong Bookings -> vẫn tính theo tổng ngày
    }

    // ✅ UPDATED: Load dữ liệu theo Bookings (auto)
    private void loadSelectedDayData() {
        if (currentRestaurant == null || selectedDate == null) {
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = "--";
            return;
        }

        // 1) Nếu ngày bị block (ALLDAY) thì ưu tiên blocked
        RestaurantDayCapacity blockRow = findCapacityRow(selectedDate, "ALLDAY");
        if (blockRow != null && computeStatusForRow(blockRow) == StatusType.BLOCKED) {
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = translateStatus(StatusType.BLOCKED);
            return;
        }

        // 2) ✅ Auto lấy từ Bookings theo ngày
        Usage u = getBookingUsage(selectedDate);
        inputGuestCount = (u != null) ? u.guests : 0;
        inputBookingCount = (u != null) ? u.bookings : 0;

        StatusType st = computeStatus(
                inputGuestCount,
                inputBookingCount,
                defaultMaxGuestsPerDay,
                defaultMaxBookingsPerDay
        );
        selectedStatus = translateStatus(st);
    }

    /**
     * Tìm record capacity cho 1 ngày + slot.
     * Dùng findByRestaurantAndDateRange(start=end=date) để không phải sửa Facade.
     */
    private RestaurantDayCapacity findCapacityRow(LocalDate date, String slotCode) {
        if (currentRestaurant == null || date == null) return null;

        java.sql.Date sqlDate = java.sql.Date.valueOf(date);
        List<RestaurantDayCapacity> list =
                dayCapacityFacade.findByRestaurantAndDateRange(
                        currentRestaurant,
                        sqlDate,
                        sqlDate);

        if (list == null || list.isEmpty()) return null;

        RestaurantDayCapacity allDayFallback = null;

        for (RestaurantDayCapacity d : list) {
            String rowSlot = d.getSlotCode();

            if (rowSlot == null || rowSlot.isBlank()) {
                if ("ALLDAY".equalsIgnoreCase(slotCode)) return d;
                allDayFallback = d;
            } else if (rowSlot.equalsIgnoreCase(slotCode)) {
                return d;
            }
        }

        if ("ALLDAY".equalsIgnoreCase(slotCode) && allDayFallback != null) {
            return allDayFallback;
        }

        return null;
    }

    // ----------------------------------------------------
    // ACTIONS (right panel)
    // ----------------------------------------------------
    public void saveLimits() {
        try {
            if (currentSettings == null) {
                currentSettings = new RestaurantCapacitySettings();
                currentSettings.setRestaurantId(currentRestaurant);
                currentSettings.setCreatedAt(new Date());
            }

            currentSettings.setMaxGuestsPerSlot(defaultMaxGuestsPerDay);
            currentSettings.setMaxBookingsPerDay(defaultMaxBookingsPerDay);
            currentSettings.setDefaultSlotDurationMin(120);

            if (currentSettings.getCapacityId() == null) {
                capacitySettingsFacade.create(currentSettings);
            } else {
                capacitySettingsFacade.edit(currentSettings);
            }

            System.out.println("✅ Limits saved to DB");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ⚠️ NOTE: hàm này vẫn giữ để bạn dùng manual override nếu muốn
    // Nhưng vì status giờ auto theo Bookings, nên manual currentGuestCount/currentBookingCount có thể không còn đúng.
    public void applyStatusToSelectedDay() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        System.out.println(">>> applyStatusToSelectedDay, date=" + selectedDate + ", slot=" + selectedSlot);

        if (currentRestaurant == null) {
            ctx.addMessage("dayStatusForm",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Không tìm thấy nhà hàng hiện tại.", null));
            return;
        }

        if (selectedDate == null) {
            ctx.addMessage("dayStatusForm",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Please select a date on the calendar first.", null));
            return;
        }

        if (inputGuestCount == null) inputGuestCount = 0;
        if (inputBookingCount == null) inputBookingCount = 0;

        boolean hasError = false;

        if (defaultMaxGuestsPerDay > 0 && inputGuestCount > defaultMaxGuestsPerDay) {
            ctx.addMessage("dayStatusForm:guestCount",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Số khách không được lớn hơn " + defaultMaxGuestsPerDay, null));
            hasError = true;
        }
        if (defaultMaxBookingsPerDay > 0 && inputBookingCount > defaultMaxBookingsPerDay) {
            ctx.addMessage("dayStatusForm:bookingCount",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Số booking không được lớn hơn " + defaultMaxBookingsPerDay, null));
            hasError = true;
        }

        if (hasError) return;

        RestaurantDayCapacity row = findCapacityRow(selectedDate, selectedSlot);

        if (row == null) {
            row = new RestaurantDayCapacity();
            row.setRestaurantId(currentRestaurant);
            row.setEventDate(java.sql.Date.valueOf(selectedDate));
            row.setSlotCode(selectedSlot);
        } else {
            row.setSlotCode(selectedSlot);
        }

        row.setMaxGuests(defaultMaxGuestsPerDay);
        row.setMaxBookings(defaultMaxBookingsPerDay);
        row.setCurrentGuestCount(inputGuestCount);
        row.setCurrentBookingCount(inputBookingCount);

        StatusType st = computeStatus(
                inputGuestCount,
                inputBookingCount,
                defaultMaxGuestsPerDay,
                defaultMaxBookingsPerDay
        );

        row.setIsFull(st == StatusType.FULL);

        if (row.getDayCapacityId() == null) {
            dayCapacityFacade.create(row);
        } else {
            dayCapacityFacade.edit(row);
        }

        selectedStatus = translateStatus(st);

        // Update heatmap ngay (tạm thời)
        StatusType existing = dayStatusMap.getOrDefault(selectedDate, StatusType.NONE);
        dayStatusMap.put(selectedDate, maxStatus(existing, st));
        buildCalendar();

        ctx.addMessage("dayStatusForm",
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Đã lưu sức chứa cho ngày " + getSelectedDateLabel()
                                + " (" + selectedSlot + ")", null));
    }

    public void blockDates() {
        try {
            if (currentRestaurant == null) return;
            if (blockStartDate == null || blockStartDate.isBlank()
                    || blockEndDate == null || blockEndDate.isBlank()) {
                return;
            }

            LocalDate start = LocalDate.parse(blockStartDate);
            LocalDate end = LocalDate.parse(blockEndDate);

            while (!end.isBefore(start)) {
                RestaurantDayCapacity d = findCapacityRow(start, "ALLDAY");

                if (d == null) {
                    d = new RestaurantDayCapacity();
                    d.setRestaurantId(currentRestaurant);
                    d.setEventDate(java.sql.Date.valueOf(start));
                    d.setSlotCode("ALLDAY");
                } else {
                    d.setSlotCode("ALLDAY");
                }

                d.setMaxGuests(0);
                d.setMaxBookings(0);
                d.setCurrentGuestCount(0);
                d.setCurrentBookingCount(0);
                d.setIsFull(false);

                if (d.getDayCapacityId() == null) {
                    dayCapacityFacade.create(d);
                } else {
                    dayCapacityFacade.edit(d);
                }

                dayStatusMap.put(start, StatusType.BLOCKED);

                if (selectedDate != null && selectedDate.equals(start)) {
                    inputGuestCount = 0;
                    inputBookingCount = 0;
                    selectedStatus = translateStatus(StatusType.BLOCKED);
                }

                start = start.plusDays(1);
            }

            buildCalendar();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------
    // UTIL
    // ----------------------------------------------------
    private LocalDate toLocalDate(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    private StatusType computeStatusForRow(RestaurantDayCapacity d) {
        Integer maxGuests = d.getMaxGuests();
        Integer maxBookings = d.getMaxBookings();

        if (maxGuests != null && maxBookings != null
                && maxGuests == 0 && maxBookings == 0) {
            return StatusType.BLOCKED;
        }

        Integer cg = d.getCurrentGuestCount();
        Integer cb = d.getCurrentBookingCount();

        return computeStatus(
                cg != null ? cg : 0,
                cb != null ? cb : 0,
                maxGuests,
                maxBookings
        );
    }

    private StatusType computeStatus(int guestCount, int bookingCount,
                                     Integer maxGuests, Integer maxBookings) {

        if (maxGuests == null || maxGuests <= 0) {
            maxGuests = defaultMaxGuestsPerDay;
        }
        if (maxBookings == null || maxBookings <= 0) {
            maxBookings = defaultMaxBookingsPerDay;
        }

        if ((maxGuests == null || maxGuests <= 0) &&
                (maxBookings == null || maxBookings <= 0)) {
            return StatusType.NONE;
        }

        if (guestCount < 0) guestCount = 0;
        if (bookingCount < 0) bookingCount = 0;

        if (guestCount == 0 && bookingCount == 0) {
            return StatusType.AVAILABLE;
        }

        double guestRatio = 0.0;
        if (maxGuests != null && maxGuests > 0) {
            guestRatio = (double) guestCount / maxGuests;
        }

        double bookingRatio = 0.0;
        if (maxBookings != null && maxBookings > 0) {
            bookingRatio = (double) bookingCount / maxBookings;
        }

        double ratio = Math.max(guestRatio, bookingRatio);

        if (ratio >= 1.0) {
            return StatusType.FULL;
        } else if (ratio >= 0.5) {
            return StatusType.NEAR_FULL;
        } else {
            return StatusType.AVAILABLE;
        }
    }

    private String translateStatus(StatusType st) {
        switch (st) {
            case AVAILABLE:
                return "Available (0% - <50%)";
            case NEAR_FULL:
                return "Gần đầy (≥50%)";
            case FULL:
                return "Đã đầy (100%)";
            case BLOCKED:
                return "Bị chặn";
            case TOO_SOON:
                return "Chưa đủ ngày đặt trước";
            default:
                return "--";
        }
    }

    private StatusType maxStatus(StatusType a, StatusType b) {
        if (a == null) a = StatusType.NONE;
        if (b == null) b = StatusType.NONE;

        int wa = weight(a);
        int wb = weight(b);
        return (wb > wa) ? b : a;
    }

    private int weight(StatusType st) {
        switch (st) {
            case NONE: return 0;
            case AVAILABLE: return 1;
            case NEAR_FULL: return 2;
            case FULL: return 3;
            case TOO_SOON: return 3;
            case BLOCKED: return 4;
            default: return 0;
        }
    }

    public String getSelectedDateLabel() {
        if (selectedDate == null) return "Chưa chọn ngày";
        return selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ----------------------------------------------------
    // GET / SET
    // ----------------------------------------------------
    public int getDefaultMaxGuestsPerDay() {
        return defaultMaxGuestsPerDay;
    }

    public void setDefaultMaxGuestsPerDay(int defaultMaxGuestsPerDay) {
        this.defaultMaxGuestsPerDay = defaultMaxGuestsPerDay;
    }

    public int getDefaultMaxBookingsPerDay() {
        return defaultMaxBookingsPerDay;
    }

    public void setDefaultMaxBookingsPerDay(int defaultMaxBookingsPerDay) {
        this.defaultMaxBookingsPerDay = defaultMaxBookingsPerDay;
    }

    public String getBlockStartDate() {
        return blockStartDate;
    }

    public void setBlockStartDate(String blockStartDate) {
        this.blockStartDate = blockStartDate;
    }

    public String getBlockEndDate() {
        return blockEndDate;
    }

    public void setBlockEndDate(String blockEndDate) {
        this.blockEndDate = blockEndDate;
    }

    public String getBlockHall() {
        return blockHall;
    }

    public void setBlockHall(String blockHall) {
        this.blockHall = blockHall;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    public Integer getInputGuestCount() {
        return inputGuestCount;
    }

    public void setInputGuestCount(Integer inputGuestCount) {
        this.inputGuestCount = inputGuestCount;
    }

    public Integer getInputBookingCount() {
        return inputBookingCount;
    }

    public void setInputBookingCount(Integer inputBookingCount) {
        this.inputBookingCount = inputBookingCount;
    }

    public List<CalendarDay> getCalendarDays() {
        return calendarDays;
    }

    public String getCurrentMonthLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return currentMonth.format(fmt);
    }

    public String getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(String selectedSlot) {
        this.selectedSlot = selectedSlot;
    }

    // ----------------------------------------------------
    // INNER TYPES
    // ----------------------------------------------------
    public enum StatusType {
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED, TOO_SOON
    }

    public static class CalendarDay implements Serializable {
        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final StatusType status;
        private final boolean selected;
        private final boolean tooSoon;

        public CalendarDay(LocalDate date, boolean inCurrentMonth,
                           StatusType status, boolean selected, boolean tooSoon) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.status = status;
            this.selected = selected;
            this.tooSoon = tooSoon;
        }

        public String getDayNumber() {
            return String.valueOf(date.getDayOfMonth());
        }

        public boolean isShowDot() {
            return status != StatusType.NONE;
        }

        public String getDotCss() {
            switch (status) {
                case AVAILABLE: return "bg-success-green";
                case NEAR_FULL: return "bg-champagne-gold";
                case FULL: return "bg-error-red";
                case BLOCKED: return "bg-gray-400";
                case TOO_SOON: return "bg-error-red";
                default: return "";
            }
        }

        public String getDateIso() {
            return date.toString(); // yyyy-MM-dd
        }

        public boolean isInCurrentMonth() {
            return inCurrentMonth;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean isTooSoon() {
            return tooSoon;
        }
    }
}
