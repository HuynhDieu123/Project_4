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
                // Không có nhà hàng tương ứng user -> không làm gì
                return;
            }

            currentMonth = YearMonth.now();
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = "--";

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

        for (RestaurantDayCapacity d : list) {
            LocalDate date = toLocalDate(d.getEventDate());
            StatusType st = computeStatusForRow(d);

            if (st != StatusType.NONE) {
                dayStatusMap.put(date, st);
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

        RestaurantDayCapacity row = dayCapacityFacade.findByRestaurantAndDate(
                currentRestaurant,
                java.sql.Date.valueOf(selectedDate)
        );

        if (row != null) {
            inputGuestCount = row.getCurrentGuestCount();
            inputBookingCount = row.getCurrentBookingCount();
            StatusType st = computeStatusForRow(row);
            selectedStatus = translateStatus(st);
        } else {
            inputGuestCount = 0;
            inputBookingCount = 0;
            selectedStatus = "--";
        }

        buildCalendar();
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
        if (currentRestaurant == null || selectedDate == null) {
            return;
        }

        if (inputGuestCount == null)  inputGuestCount = 0;
        if (inputBookingCount == null) inputBookingCount = 0;

        FacesContext ctx = FacesContext.getCurrentInstance();
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

        RestaurantDayCapacity row = dayCapacityFacade.findByRestaurantAndDate(
                currentRestaurant,
                java.sql.Date.valueOf(selectedDate)
        );

        if (row == null) {
            row = new RestaurantDayCapacity();
            row.setRestaurantId(currentRestaurant);
            row.setEventDate(java.sql.Date.valueOf(selectedDate));
            row.setSlotCode("ALLDAY");
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

        dayStatusMap.put(selectedDate, st);
        selectedStatus = translateStatus(st);

        buildCalendar();
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
                RestaurantDayCapacity d = dayCapacityFacade.findByRestaurantAndDate(
                        currentRestaurant,
                        java.sql.Date.valueOf(start)
                );

                if (d == null) {
                    d = new RestaurantDayCapacity();
                    d.setRestaurantId(currentRestaurant);
                    d.setEventDate(java.sql.Date.valueOf(start));
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

        return computeStatus(
                d.getCurrentGuestCount(),
                d.getCurrentBookingCount(),
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
