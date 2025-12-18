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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    @EJB
    private RestaurantDayCapacityFacadeLocal dayCapacityFacade;

    // ✅ NEW
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

    // ✅ NEW: date -> [totalGuests, bookingCount]
    private Map<LocalDate, int[]> bookingAggMap = new HashMap<>();

    private LocalDate selectedDate;
    private String selectedStatus;
    private Integer inputGuestCount;
    private Integer inputBookingCount;
    private String selectedSlot = "ALLDAY";

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

    @PostConstruct
    public void init() {
        try {
            currentRestaurant = resolveCurrentRestaurant();
            if (currentRestaurant == null) {
                return;
            }

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

    // ✅ NEW: load booking agg cho đúng “grid 42 ô”
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

        System.out.println("[Capacity] rid=" + currentRestaurant.getRestaurantId()
                + " gridStart=" + gridStart + " gridEndEx=" + gridEndExclusive
                + " rows=" + (rows == null ? 0 : rows.size()));

        if (rows == null) {
            return;
        }

        for (Object[] r : rows) {
            // r[0] = eventDate (java.util.Date)
            LocalDate day = toLocalDate((Date) r[0]);
            int totalGuests = numberToInt(r[1]);
            int bookingCount = numberToInt(r[2]);

            bookingAggMap.put(day, new int[]{totalGuests, bookingCount});
        }
    }

    private void loadCalendarFromDb() {
        dayStatusMap.clear();

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        // 1) Manual / Block từ RestaurantDayCapacity (giữ y như cũ)
        List<RestaurantDayCapacity> list
                = dayCapacityFacade.findByRestaurantAndDateRange(
                        currentRestaurant,
                        java.sql.Date.valueOf(first),
                        java.sql.Date.valueOf(last)
                );

        for (RestaurantDayCapacity d : list) {
            LocalDate date = toLocalDate(d.getEventDate());
            StatusType st = computeStatusForRow(d);

            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            StatusType merged = maxStatus(existing, st);

            if (merged != StatusType.NONE) {
                dayStatusMap.put(date, merged);
            }
        }

        // 2) ✅ AUTO từ Bookings: overlay lên dayStatusMap (nhưng không đè BLOCKED)
        loadBookingAggForCalendarGrid();

        for (Map.Entry<LocalDate, int[]> e : bookingAggMap.entrySet()) {
            LocalDate date = e.getKey();
            int guests = e.getValue()[0];
            int bookings = e.getValue()[1];

            // ✅ không có booking thì bỏ qua để khỏi hiện dot cho mọi ngày
            if (guests == 0 && bookings == 0) {
                continue;
            }

            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            if (existing == StatusType.BLOCKED) {
                continue;
            }

            StatusType st = computeStatus(
                    guests,
                    bookings,
                    defaultMaxGuestsPerDay,
                    defaultMaxBookingsPerDay
            );

            dayStatusMap.put(date, maxStatus(existing, st));
        }

        buildCalendar();
    }

    // ===== MinDaysInAdvance giữ nguyên =====
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

    private void buildCalendar() {
        calendarDays = new ArrayList<>();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7;
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

    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        loadCalendarFromDb();
    }

    public void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
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

    // ✅ update: nếu không có manual row thì lấy số liệu từ bookingAggMap
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

        // AUTO from bookings (ALLDAY hoặc slot nào cũng xem như tổng ngày)
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
        List<RestaurantDayCapacity> list
                = dayCapacityFacade.findByRestaurantAndDateRange(
                        currentRestaurant,
                        sqlDate,
                        sqlDate);

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

            // ✅ đổi limit => reload calendar để auto status đổi theo
            loadCalendarFromDb();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ MANUAL giữ nguyên (bạn đang dùng button Save day capacity)
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
            if (currentRestaurant == null) {
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
        }
    }

    // ===== UTIL =====
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

        int wa = weight(a);
        int wb = weight(b);
        return (wb > wa) ? b : a;
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

    // ===== GET/SET (giữ nguyên như bạn) =====
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
        return calendarDays;
    }

    public String getCurrentMonthLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return currentMonth.format(fmt);
    }

    public String getSelectedSlot() {
        return selectedSlot;
    }

    public void setSelectedSlot(String v) {
        this.selectedSlot = v;
    }

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
