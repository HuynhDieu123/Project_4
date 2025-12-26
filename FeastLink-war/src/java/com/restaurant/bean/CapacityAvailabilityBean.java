package com.restaurant.bean;

import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.entity.RestaurantDayCapacity;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    @EJB
    private RestaurantDayCapacityFacadeLocal dayCapacityFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private Restaurants currentRestaurant;
    private RestaurantCapacitySettings currentSettings;

    private int defaultMaxGuestsPerDay;
    private int defaultMaxBookingsPerDay;

    private String blockStartDate;
    private String blockEndDate;
    private String blockHall;

    private YearMonth currentMonth;
    private List<CalendarDay> calendarDays;

    private Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();
    private Map<LocalDate, int[]> bookingAggMap = new HashMap<>();

    private LocalDate selectedDate;
    private String selectedStatus;
    private Integer inputGuestCount;
    private Integer inputBookingCount;
    private String selectedSlot = "ALLDAY";

    // ======================================================
    // INIT (FAIL-SAFE)
    // ======================================================
    @PostConstruct
    public void init() {
        // ✅ Always init basic state first (so UI never breaks)
        if (currentMonth == null) {
            currentMonth = YearMonth.now();
        }
        if (selectedStatus == null) {
            selectedStatus = "--";
        }
        if (inputGuestCount == null) {
            inputGuestCount = 0;
        }
        if (inputBookingCount == null) {
            inputBookingCount = 0;
        }
        if (selectedSlot == null || selectedSlot.isBlank()) {
            selectedSlot = "ALLDAY";
        }

        try {
            currentRestaurant = resolveCurrentRestaurant();

            // ✅ If restaurant context missing -> still show an empty calendar instead of disappearing
            if (currentRestaurant == null) {
                dayStatusMap.clear();
                bookingAggMap.clear();
                buildCalendar(); // show blank calendar grid (no dots)
                return;
            }

            loadSettingsFromDb();
            loadCalendarFromDb();

        } catch (Exception e) {
            e.printStackTrace();

            // ✅ Even if something crashes, keep UI visible
            try {
                dayStatusMap.clear();
                bookingAggMap.clear();
                buildCalendar();
            } catch (Exception ignore) {
            }
        }
    }

    // ✅ Auto-heal if calendarDays is null/empty after reload
    private void ensureCalendarReady() {
        if (currentMonth == null) {
            currentMonth = YearMonth.now();
        }

        if (calendarDays == null || calendarDays.isEmpty()) {
            // try to rebuild properly
            try {
                if (currentRestaurant == null) {
                    currentRestaurant = resolveCurrentRestaurant();
                }
                if (currentRestaurant != null) {
                    loadSettingsFromDb();
                    loadCalendarFromDb();
                } else {
                    dayStatusMap.clear();
                    bookingAggMap.clear();
                    buildCalendar();
                }
            } catch (Exception e) {
                e.printStackTrace();
                dayStatusMap.clear();
                bookingAggMap.clear();
                buildCalendar();
            }
        }
    }

    // ======================================================
    // RESOLVE CURRENT RESTAURANT
    // ======================================================
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return null;
        }

        Map<String, Object> session = ctx.getExternalContext().getSessionMap();
        Users currentUser = (Users) session.get("currentUser");
        if (currentUser == null || currentUser.getEmail() == null) {
            return null;
        }

        String email = currentUser.getEmail();

        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null && r.getEmail().equalsIgnoreCase(email)) {
                return r;
            }
        }
        return null;
    }

    // ======================================================
    // SETTINGS
    // ======================================================
    private void loadSettingsFromDb() {
        if (currentRestaurant == null) {
            // fallback defaults
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
            return;
        }

        currentSettings = capacitySettingsFacade.findByRestaurant(currentRestaurant);
        if (currentSettings != null) {
            defaultMaxGuestsPerDay = Optional.ofNullable(currentSettings.getMaxGuestsPerSlot()).orElse(150);
            defaultMaxBookingsPerDay = Optional.ofNullable(currentSettings.getMaxBookingsPerDay()).orElse(10);
        } else {
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
        }
    }

    // ======================================================
    // LOAD BOOKING AGG FOR GRID (42 CELLS)
    // ======================================================
    private void loadBookingAggForCalendarGrid() {
        bookingAggMap.clear();
        if (currentRestaurant == null || currentRestaurant.getRestaurantId() == null) {
            return;
        }

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate gridStart = firstOfMonth.minusDays(shift);
        LocalDate gridEndExclusive = gridStart.plusDays(42);

        Date from = java.sql.Date.valueOf(gridStart);
        Date to = java.sql.Date.valueOf(gridEndExclusive);

        List<Object[]> rows = bookingsFacade.aggregateForCalendar(
                currentRestaurant.getRestaurantId(),
                from,
                to
        );

        if (rows == null) {
            return;
        }

        for (Object[] r : rows) {
            // ✅ robust date conversion (Date/Timestamp/LocalDate)
            LocalDate day = toLocalDate(r[0]);
            if (day == null) {
                continue;
            }

            int totalGuests = numberToInt(r[1]);
            int bookingCount = numberToInt(r[2]);

            bookingAggMap.put(day, new int[]{totalGuests, bookingCount});
        }
    }

    // ======================================================
    // LOAD CALENDAR DATA
    // ======================================================
    private void loadCalendarFromDb() {
        if (currentRestaurant == null) {
            dayStatusMap.clear();
            bookingAggMap.clear();
            buildCalendar();
            return;
        }

        dayStatusMap.clear();

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        // 1) Manual/Blocked from RestaurantDayCapacity (month range)
        List<RestaurantDayCapacity> list = dayCapacityFacade.findByRestaurantAndDateRange(
                currentRestaurant,
                java.sql.Date.valueOf(first),
                java.sql.Date.valueOf(last)
        );

        if (list != null) {
            for (RestaurantDayCapacity d : list) {
                LocalDate date = toLocalDate(d.getEventDate());
                if (date == null) {
                    continue;
                }

                StatusType st = computeStatusForRow(d);

                StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
                StatusType merged = maxStatus(existing, st);

                if (merged != StatusType.NONE) {
                    dayStatusMap.put(date, merged);
                }
            }
        }

        // 2) AUTO overlay from Bookings (do not override BLOCKED)
        loadBookingAggForCalendarGrid();

        for (Map.Entry<LocalDate, int[]> e : bookingAggMap.entrySet()) {
            LocalDate date = e.getKey();
            int guests = e.getValue()[0];
            int bookings = e.getValue()[1];

            if (guests == 0 && bookings == 0) {
                continue;
            }

            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            if (existing == StatusType.BLOCKED) {
                continue;
            }

            StatusType st = computeStatus(guests, bookings, defaultMaxGuestsPerDay, defaultMaxBookingsPerDay);
            dayStatusMap.put(date, maxStatus(existing, st));
        }

        buildCalendar();
    }

    // ======================================================
    // MIN DAYS IN ADVANCE (unchanged)
    // ======================================================
    private int getMinDaysInAdvanceSafe() {
        if (currentRestaurant == null) {
            return 0;
        }
        Integer v = currentRestaurant.getMinDaysInAdvance();
        return (v != null && v > 0) ? v : 0;
    }

    private boolean isTooSoonByMinAdvance(LocalDate date) {
        if (date == null) {
            return false;
        }

        int min = getMinDaysInAdvanceSafe();
        if (min <= 0) {
            return false;
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate earliestAllowed = today.plusDays(min);

        return !date.isBefore(today) && date.isBefore(earliestAllowed);
    }

    private StatusType applyMinAdvanceOverlay(LocalDate date, StatusType base) {
        if (!isTooSoonByMinAdvance(date)) {
            return base;
        }
        if (base == StatusType.BLOCKED || base == StatusType.FULL) {
            return base;
        }
        return StatusType.TOO_SOON;
    }

    // ======================================================
    // BUILD CALENDAR (ALWAYS CREATE LIST)
    // ======================================================
    private void buildCalendar() {
        if (currentMonth == null) {
            currentMonth = YearMonth.now();
        }
        if (dayStatusMap == null) {
            dayStatusMap = new HashMap<>();
        }

        calendarDays = new ArrayList<>(42);

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);

            boolean inMonth = YearMonth.from(date).equals(currentMonth);
            boolean isSelected = (selectedDate != null && selectedDate.equals(date));

            StatusType baseStatus = dayStatusMap.getOrDefault(date, StatusType.NONE);
            StatusType effectiveStatus = applyMinAdvanceOverlay(date, baseStatus);
            boolean tooSoon = isTooSoonByMinAdvance(date);

            calendarDays.add(new CalendarDay(date, inMonth, effectiveStatus, isSelected, tooSoon));
        }
    }

    // ======================================================
    // NAVIGATION
    // ======================================================
    public void nextMonth() {
        currentMonth = (currentMonth == null) ? YearMonth.now().plusMonths(1) : currentMonth.plusMonths(1);
        loadCalendarFromDb();
    }

    public void previousMonth() {
        currentMonth = (currentMonth == null) ? YearMonth.now().minusMonths(1) : currentMonth.minusMonths(1);
        loadCalendarFromDb();
    }

    public void selectDay(String dateIso) {
        if (dateIso == null || dateIso.isBlank()) {
            return;
        }

        LocalDate d = LocalDate.parse(dateIso);

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

    public void onSlotChange() {
        loadSelectedDayData();
    }

    // ======================================================
    // SELECTED DAY DATA
    // ======================================================
    private void loadSelectedDayData() {
        if (currentRestaurant == null || selectedDate == null) {
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = "--";
            return;
        }

        RestaurantDayCapacity row = findCapacityRow(selectedDate, selectedSlot);

        if (row != null) {
            Integer cg = row.getCurrentGuestCount();
            Integer cb = row.getCurrentBookingCount();
            inputGuestCount = (cg != null) ? cg : 0;
            inputBookingCount = (cb != null) ? cb : 0;

            StatusType st = computeStatusForRow(row);
            selectedStatus = translateStatus(st);
            return;
        }

        // AUTO from bookings
        int[] agg = bookingAggMap.getOrDefault(selectedDate, new int[]{0, 0});
        inputGuestCount = agg[0];
        inputBookingCount = agg[1];

        StatusType st = computeStatus(inputGuestCount, inputBookingCount,
                defaultMaxGuestsPerDay, defaultMaxBookingsPerDay);
        selectedStatus = translateStatus(st);
    }

    private RestaurantDayCapacity findCapacityRow(LocalDate date, String slotCode) {
        if (currentRestaurant == null || date == null) {
            return null;
        }

        java.sql.Date sqlDate = java.sql.Date.valueOf(date);
        List<RestaurantDayCapacity> list = dayCapacityFacade.findByRestaurantAndDateRange(
                currentRestaurant, sqlDate, sqlDate
        );

        if (list == null || list.isEmpty()) {
            return null;
        }

        RestaurantDayCapacity allDayFallback = null;

        for (RestaurantDayCapacity d : list) {
            String rowSlot = d.getSlotCode();

            if (rowSlot == null || rowSlot.isBlank()) {
                if ("ALLDAY".equalsIgnoreCase(slotCode)) {
                    return d;
                }
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

    // ======================================================
    // SAVE LIMITS (unchanged, but safe reload)
    // ======================================================
    public void saveLimits() {
        try {
            if (currentRestaurant == null) {
                buildCalendar();
                return;
            }

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

            loadCalendarFromDb();

        } catch (Exception e) {
            e.printStackTrace();
            buildCalendar();
        }
    }

    // ======================================================
    // APPLY MANUAL STATUS (unchanged)
    // ======================================================
    public void applyStatusToSelectedDay() {
        FacesContext ctx = FacesContext.getCurrentInstance();

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

        if (inputGuestCount == null) {
            inputGuestCount = 0;
        }
        if (inputBookingCount == null) {
            inputBookingCount = 0;
        }

        // ✅ CASE 1: user set 0/0 => coi như RESET (clear override)
        if (inputGuestCount == 0 && inputBookingCount == 0) {

            RestaurantDayCapacity row = findCapacityRow(selectedDate, selectedSlot);

            if (row != null) {
                Integer mg = row.getMaxGuests();
                Integer mb = row.getMaxBookings();
                boolean isBlockedRow = (mg != null && mb != null && mg == 0 && mb == 0);

                // Nếu row là BLOCKED thì không reset ở đây (dùng tab Block/Unblock)
                if (isBlockedRow) {
                    ctx.addMessage("dayStatusForm",
                            new FacesMessage(FacesMessage.SEVERITY_WARN,
                                    "Ngày này đang bị Block. Hãy dùng tab 'Block dates' để Unblock.", null));
                    return;
                }

                // ✅ Xóa override thủ công
                dayCapacityFacade.remove(row);
            }

            // ✅ Reload lại tất cả để status không bị “kẹt”
            loadCalendarFromDb();
            loadSelectedDayData(); // lấy lại từ bookingAggMap hoặc 0/0

            ctx.addMessage("dayStatusForm",
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Đã reset day status cho ngày " + getSelectedDateLabel()
                            + " (" + selectedSlot + ")", null));
            return;
        }

        // ✅ CASE 2: validate vượt limit
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
        if (hasError) {
            return;
        }

        // ✅ CASE 3: lưu override thủ công (upsert)
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

        // ✅ QUAN TRỌNG: reload lại thay vì maxStatus (để có thể “hạ” trạng thái)
        loadCalendarFromDb();
        loadSelectedDayData();

        ctx.addMessage("dayStatusForm",
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        "Đã lưu sức chứa cho ngày " + getSelectedDateLabel()
                        + " (" + selectedSlot + ")", null));
    }

    // ======================================================
    // BLOCK / UNBLOCK (unchanged, but safe)
    // ======================================================
    public void blockDates() {
        try {
            if (currentRestaurant == null) {
                buildCalendar();
                return;
            }
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
            buildCalendar();
        }
    }

    public void unblockDates() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (currentRestaurant == null) {
                buildCalendar();
                return;
            }
            if (blockStartDate == null || blockStartDate.isBlank()
                    || blockEndDate == null || blockEndDate.isBlank()) {
                return;
            }

            LocalDate start = LocalDate.parse(blockStartDate);
            LocalDate end = LocalDate.parse(blockEndDate);

            if (end.isBefore(start)) {
                LocalDate tmp = start;
                start = end;
                end = tmp;
            }

            LocalDate d = start;
            while (!d.isAfter(end)) {
                RestaurantDayCapacity row = findCapacityRow(d, "ALLDAY");
                if (row != null) {
                    Integer mg = row.getMaxGuests();
                    Integer mb = row.getMaxBookings();
                    boolean isBlockedRow = (mg != null && mb != null && mg == 0 && mb == 0);
                    if (isBlockedRow) {
                        dayCapacityFacade.remove(row);
                    }
                }
                d = d.plusDays(1);
            }

            loadCalendarFromDb();

            if (selectedDate != null && !selectedDate.isBefore(start) && !selectedDate.isAfter(end)) {
                loadSelectedDayData();
                buildCalendar();
            }

            ctx.addMessage("blockForm",
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Unblocked dates successfully.", null));

        } catch (Exception e) {
            e.printStackTrace();
            if (ctx != null) {
                ctx.addMessage("blockForm",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Unblock failed.", e.getMessage()));
            }
            buildCalendar();
        }
    }

    // ======================================================
    // UTIL (ROBUST DATE CONVERSION)
    // ======================================================
    private LocalDate toLocalDate(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof LocalDate) {
            return (LocalDate) obj;
        }
        if (obj instanceof java.sql.Date) {
            return ((java.sql.Date) obj).toLocalDate();
        }
        if (obj instanceof Timestamp) {
            return ((Timestamp) obj).toLocalDateTime().toLocalDate();
        }
        if (obj instanceof Date) {
            Date d = (Date) obj;
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private int numberToInt(Object o) {
        if (o == null) {
            return 0;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        return 0;
    }

    private StatusType computeStatusForRow(RestaurantDayCapacity d) {
        Integer maxGuests = d.getMaxGuests();
        Integer maxBookings = d.getMaxBookings();

        if (maxGuests != null && maxBookings != null && maxGuests == 0 && maxBookings == 0) {
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

        if (guestCount < 0) {
            guestCount = 0;
        }
        if (bookingCount < 0) {
            bookingCount = 0;
        }

        if (guestCount == 0 && bookingCount == 0) {
            return StatusType.AVAILABLE;
        }

        double guestRatio = (maxGuests != null && maxGuests > 0) ? (double) guestCount / maxGuests : 0.0;
        double bookingRatio = (maxBookings != null && maxBookings > 0) ? (double) bookingCount / maxBookings : 0.0;
        double ratio = Math.max(guestRatio, bookingRatio);

        if (ratio >= 1.0) {
            return StatusType.FULL;
        }
        if (ratio >= 0.5) {
            return StatusType.NEAR_FULL;
        }
        return StatusType.AVAILABLE;
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
        if (a == null) {
            a = StatusType.NONE;
        }
        if (b == null) {
            b = StatusType.NONE;
        }
        return (weight(b) > weight(a)) ? b : a;
    }

    private int weight(StatusType st) {
        switch (st) {
            case NONE:
                return 0;
            case AVAILABLE:
                return 1;
            case NEAR_FULL:
                return 2;
            case FULL:
                return 3;
            case TOO_SOON:
                return 3;
            case BLOCKED:
                return 4;
            default:
                return 0;
        }
    }

    public String getSelectedDateLabel() {
        if (selectedDate == null) {
            return "Date not yet selected";
        }
        return selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ======================================================
    // GETTERS / SETTERS
    // ======================================================
    public int getDefaultMaxGuestsPerDay() {
        return defaultMaxGuestsPerDay;
    }

    public void setDefaultMaxGuestsPerDay(int v) {
        this.defaultMaxGuestsPerDay = v;
    }

    public int getDefaultMaxBookingsPerDay() {
        return defaultMaxBookingsPerDay;
    }

    public void setDefaultMaxBookingsPerDay(int v) {
        this.defaultMaxBookingsPerDay = v;
    }

    public String getBlockStartDate() {
        return blockStartDate;
    }

    public void setBlockStartDate(String v) {
        this.blockStartDate = v;
    }

    public String getBlockEndDate() {
        return blockEndDate;
    }

    public void setBlockEndDate(String v) {
        this.blockEndDate = v;
    }

    public String getBlockHall() {
        return blockHall;
    }

    public void setBlockHall(String v) {
        this.blockHall = v;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String v) {
        this.selectedStatus = v;
    }

    public Integer getInputGuestCount() {
        return inputGuestCount;
    }

    public void setInputGuestCount(Integer v) {
        this.inputGuestCount = v;
    }

    public Integer getInputBookingCount() {
        return inputBookingCount;
    }

    public void setInputBookingCount(Integer v) {
        this.inputBookingCount = v;
    }

    public List<CalendarDay> getCalendarDays() {
        ensureCalendarReady();
        return calendarDays;
    }

    public String getCurrentMonthLabel() {
        ensureCalendarReady();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return currentMonth.format(fmt);
    }

    public String getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(String v) {
        this.selectedSlot = v;
    }

    // ======================================================
    // ENUM + DTO
    // ======================================================
    public enum StatusType {
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED, TOO_SOON
    }

    public static class CalendarDay implements Serializable {

        private static final long serialVersionUID = 1L;

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
                case AVAILABLE:
                    return "bg-success-green";
                case NEAR_FULL:
                    return "bg-champagne-gold";
                case FULL:
                    return "bg-error-red";
                case BLOCKED:
                    return "bg-gray-400";
                case TOO_SOON:
                    return "bg-error-red";
                default:
                    return "";
            }
        }

        public String getDateIso() {
            return date.toString();
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
