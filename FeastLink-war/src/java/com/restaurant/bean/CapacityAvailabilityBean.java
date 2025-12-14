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

    // selected day + input
    private LocalDate selectedDate;
    private String selectedStatus;
    private Integer inputGuestCount;
    private Integer inputBookingCount;

    // slot được chọn (ALLDAY / LUNCH / EVENING)
    private String selectedSlot = "ALLDAY";

    // Helper: lấy nhà hàng theo user login
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;

        Map<String, Object> session = ctx.getExternalContext().getSessionMap();
        Users currentUser = (Users) session.get("currentUser");
        if (currentUser == null || currentUser.getEmail() == null) {
            return null;
        }
        String email = currentUser.getEmail();

        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null &&
                r.getEmail().equalsIgnoreCase(email)) {
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

    // ----------------------------------------------------
    // LOAD SETTINGS + CALENDAR FROM DB
    // ----------------------------------------------------
    private void loadSettingsFromDb() {
        currentSettings = capacitySettingsFacade.findByRestaurant(currentRestaurant);

        if (currentSettings != null) {
            defaultMaxGuestsPerDay = Optional.ofNullable(currentSettings.getMaxGuestsPerSlot())
                                             .orElse(150);
            defaultMaxBookingsPerDay = Optional.ofNullable(currentSettings.getMaxBookingsPerDay())
                                               .orElse(10);
        } else {
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
        }
    }

    private void loadCalendarFromDb() {
        dayStatusMap.clear();

        LocalDate first = currentMonth.atDay(1);
        LocalDate last  = currentMonth.atEndOfMonth();

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

        buildCalendar();
    }

    private void buildCalendar() {
        calendarDays = new ArrayList<>();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);
            boolean inMonth = date.getMonthValue() == currentMonth.getMonthValue();
            StatusType status = dayStatusMap.getOrDefault(date, StatusType.NONE);
            boolean isSelected = (selectedDate != null && selectedDate.equals(date));

            calendarDays.add(new CalendarDay(date, inMonth, status, isSelected));
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

        selectedDate = LocalDate.parse(dateIso); // yyyy-MM-dd
        // Khi chọn ngày -> load lại dữ liệu cho slot hiện tại
        loadSelectedDayData();
        buildCalendar();
    }

    // Gọi khi đổi slot trên UI
    public void onSlotChange() {
        loadSelectedDayData();
    }

    // Load dữ liệu currentGuests/currentBookings + status cho (selectedDate, selectedSlot)
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
        } else {
            // chưa có record -> xem như 0/0, status Available
            inputGuestCount = 0;
            inputBookingCount = 0;
            StatusType st = computeStatus(0, 0,
                    defaultMaxGuestsPerDay,
                    defaultMaxBookingsPerDay);
            selectedStatus = translateStatus(st);
        }
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

        if (list == null || list.isEmpty()) {
            return null;
        }

        RestaurantDayCapacity allDayFallback = null;

        for (RestaurantDayCapacity d : list) {
            String rowSlot = d.getSlotCode();

            if (rowSlot == null || rowSlot.isBlank()) {
                // xem như ALLDAY cũ
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

    public void applyStatusToSelectedDay() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        System.out.println(">>> applyStatusToSelectedDay, date=" + selectedDate + ", slot=" + selectedSlot);

        if (currentRestaurant == null) {
            ctx.addMessage("dayStatusForm",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Không tìm thấy nhà hàng hiện tại.", null));
            return;
        }

        // Nếu chưa chọn ngày mà bấm Save
        if (selectedDate == null) {
            ctx.addMessage("dayStatusForm",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Please select a date on the calendar first.", null));
            return;
        }

        if (inputGuestCount == null)  inputGuestCount = 0;
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

        // Lấy record theo (ngày + slot)
        RestaurantDayCapacity row = findCapacityRow(selectedDate, selectedSlot);

        if (row == null) {
            row = new RestaurantDayCapacity();
            row.setRestaurantId(currentRestaurant);
            row.setEventDate(java.sql.Date.valueOf(selectedDate));
            row.setSlotCode(selectedSlot);
        } else {
            row.setSlotCode(selectedSlot); // đảm bảo slot đúng
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

        // Cập nhật trạng thái đang chọn
        selectedStatus = translateStatus(st);

        // ✅ Cập nhật heatmap ngay cho ngày đang chọn
        StatusType existing = dayStatusMap.getOrDefault(selectedDate, StatusType.NONE);
        dayStatusMap.put(selectedDate, maxStatus(existing, st));
        buildCalendar();

        // Thông báo thành công
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
            LocalDate end   = LocalDate.parse(blockEndDate);

            while (!end.isBefore(start)) {
                // Block cả ngày -> dùng slot ALLDAY
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
        Integer maxGuests   = d.getMaxGuests();
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
            default:
                return "--";
        }
    }

    // so sánh độ "nặng" của status để lên màu trên calendar
    private StatusType maxStatus(StatusType a, StatusType b) {
        if (a == null) a = StatusType.NONE;
        if (b == null) b = StatusType.NONE;

        int wa = weight(a);
        int wb = weight(b);
        return (wb > wa) ? b : a;
    }

    private int weight(StatusType st) {
        switch (st) {
            case NONE:       return 0;
            case AVAILABLE:  return 1;
            case NEAR_FULL:  return 2;
            case FULL:       return 3;
            case BLOCKED:    return 4;
            default:         return 0;
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
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED
    }

    public static class CalendarDay implements Serializable {
        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final StatusType status;
        private final boolean selected;

        public CalendarDay(LocalDate date, boolean inCurrentMonth,
                           StatusType status, boolean selected) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.status = status;
            this.selected = selected;
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
                default:
                    return "";
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
    }
}
