package com.customer.bean;

import com.mypack.entity.RestaurantDayCapacity;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.RestaurantDayCapacityFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("publicAvailabilityBean")
@ViewScoped
public class RestaurantPublicAvailabilityBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantDayCapacityFacadeLocal dayCapacityFacade;

    private Long restaurantId;
    private Restaurants restaurant;

    private YearMonth currentMonth = YearMonth.now();
    private final List<CalendarDay> calendarDays = new ArrayList<>();
    private final Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();

    private LocalDate selectedDate;
    private String selectedStatusLabel = "--";
    private boolean canProceed = false;

    private boolean loaded = false;

    public void load() {
        if (loaded) return;
        loaded = true;

        if (restaurantId == null) return;

        restaurant = restaurantsFacade.find(restaurantId);
        if (restaurant == null) return;

        loadCalendarFromDb();
    }

    private void ensureRestaurantLoaded() {
        if (restaurant == null && restaurantId != null) {
            restaurant = restaurantsFacade.find(restaurantId);
        }
    }

    public void nextMonth() {
        ensureRestaurantLoaded();
        currentMonth = currentMonth.plusMonths(1);

        // reset selection
        selectedDate = null;
        selectedStatusLabel = "--";
        canProceed = false;

        loadCalendarFromDb();
    }

    public void previousMonth() {
        ensureRestaurantLoaded();
        currentMonth = currentMonth.minusMonths(1);

        // reset selection
        selectedDate = null;
        selectedStatusLabel = "--";
        canProceed = false;

        loadCalendarFromDb();
    }

    public void selectDay(String dateIso) {
        ensureRestaurantLoaded();
        LocalDate d = LocalDate.parse(dateIso);

        boolean inMonth = d.getMonth().equals(currentMonth.getMonth())
                && d.getYear() == currentMonth.getYear();

        StatusType st = resolveStatus(d, inMonth);

        // lưu selection để hiển thị status (kể cả FULL/BLOCKED)
        selectedDate = d;
        selectedStatusLabel = translateStatus(st);
        canProceed = (st == StatusType.AVAILABLE || st == StatusType.NEAR_FULL);

        buildCalendar();
    }

    private void loadCalendarFromDb() {
        if (restaurant == null) return;

        dayStatusMap.clear();

        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        List<RestaurantDayCapacity> list = dayCapacityFacade.findByRestaurantAndDateRange(
                restaurant,
                java.sql.Date.valueOf(first),
                java.sql.Date.valueOf(last)
        );

        for (RestaurantDayCapacity d : list) {
            LocalDate date = toLocalDate(d.getEventDate());
            if (date == null) continue;

            StatusType st = computeStatusForRow(d);
            StatusType existing = dayStatusMap.getOrDefault(date, StatusType.NONE);
            dayStatusMap.put(date, maxStatus(existing, st));
        }

        buildCalendar();
    }

    private LocalDate toLocalDate(Object raw) {
        if (raw == null) return null;

        if (raw instanceof java.sql.Date) {
            return ((java.sql.Date) raw).toLocalDate();
        }
        if (raw instanceof java.util.Date) {
            return ((java.util.Date) raw).toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
        }
        if (raw instanceof LocalDate) {
            return (LocalDate) raw;
        }
        return null;
    }

    // ✅ default no DB row => AVAILABLE, + check PAST / TOO_SOON
    private StatusType resolveStatus(LocalDate date, boolean inMonth) {
        if (!inMonth) return StatusType.NONE;

        LocalDate today = LocalDate.now();

        if (date.isBefore(today)) return StatusType.PAST;

        int minDays = 0;
        if (restaurant != null && restaurant.getMinDaysInAdvance() != null) {
            minDays = restaurant.getMinDaysInAdvance();
        }
        if (date.isBefore(today.plusDays(minDays))) return StatusType.TOO_SOON;

        return dayStatusMap.getOrDefault(date, StatusType.AVAILABLE);
    }

    private void buildCalendar() {
        calendarDays.clear();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);
            boolean inMonth = date.getMonth().equals(currentMonth.getMonth())
                    && date.getYear() == currentMonth.getYear();

            StatusType st = resolveStatus(date, inMonth);
            boolean isSelected = (selectedDate != null && selectedDate.equals(date));

            calendarDays.add(new CalendarDay(date, inMonth, st, isSelected));
        }
    }

    private StatusType computeStatusForRow(RestaurantDayCapacity d) {
        Number maxGuestsN = d.getMaxGuests();
        Number maxBookingsN = d.getMaxBookings();

        int maxGuests = (maxGuestsN == null) ? 0 : maxGuestsN.intValue();
        int maxBookings = (maxBookingsN == null) ? 0 : maxBookingsN.intValue();

        // Blocked if explicitly set both and both are 0
        if (maxGuestsN != null && maxBookingsN != null && maxGuests == 0 && maxBookings == 0) {
            return StatusType.BLOCKED;
        }

        Number cgN = d.getCurrentGuestCount();
        int cg = (cgN == null) ? 0 : cgN.intValue();

        Number cbN = d.getCurrentBookingCount();
        int cb = (cbN == null) ? 0 : cbN.intValue();

        if (cg == 0 && cb == 0) {
            return StatusType.AVAILABLE;
        }

        double guestRatio = (maxGuests > 0) ? (double) cg / maxGuests : 0d;
        double bookRatio = (maxBookings > 0) ? (double) cb / maxBookings : 0d;

        double ratio = Math.max(guestRatio, bookRatio);
        if (ratio >= 1.0) return StatusType.FULL;
        if (ratio >= 0.5) return StatusType.NEAR_FULL;
        return StatusType.AVAILABLE;
    }

    private StatusType maxStatus(StatusType a, StatusType b) {
        return (weight(b) > weight(a)) ? b : a;
    }

    private int weight(StatusType st) {
        if (st == null) return 0;
        switch (st) {
            case NONE:      return 0;
            case AVAILABLE: return 1;
            case NEAR_FULL: return 2;
            case FULL:      return 3;
            case BLOCKED:   return 4;
            default:        return 0;
        }
    }

    private String translateStatus(StatusType st) {
        if (st == null) return "--";
        switch (st) {
            case AVAILABLE: return "Available";
            case NEAR_FULL: return "Limited";
            case FULL:      return "Fully booked";
            case BLOCKED:   return "Blocked";
            case TOO_SOON:  return "Not enough days in advance";
            case PAST:      return "Past date";
            default:        return "--";
        }
    }

    public String getCurrentMonthLabel() {
        return currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
    }

    public String getSelectedDateIso() {
        return (selectedDate == null) ? null : selectedDate.toString();
    }

    public boolean isCanProceed() {
        return canProceed;
    }

    // ===== getters/setters =====
    public Long getRestaurantId() { return restaurantId; }
    public void setRestaurantId(Long restaurantId) { this.restaurantId = restaurantId; }
    public List<CalendarDay> getCalendarDays() { return calendarDays; }
    public String getSelectedStatusLabel() { return selectedStatusLabel; }

    public enum StatusType {
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED, TOO_SOON, PAST
    }

    public static class CalendarDay implements Serializable {
        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final StatusType status;
        private final boolean selected;

        public CalendarDay(LocalDate date, boolean inCurrentMonth, StatusType status, boolean selected) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.status = status;
            this.selected = selected;
        }

        public String getDayNumber() { return String.valueOf(date.getDayOfMonth()); }
        public String getDateIso() { return date.toString(); }
        public boolean isInCurrentMonth() { return inCurrentMonth; }
        public boolean isSelected() { return selected; }

        // ✅ disable only for: out-month / past / too soon
        public boolean isDisabled() {
            if (!inCurrentMonth) return true;
            return status == StatusType.PAST || status == StatusType.TOO_SOON;
        }

        // ✅ legend của bạn: dot chỉ cho Limited
        public boolean isShowDot() {
            return inCurrentMonth && status == StatusType.NEAR_FULL;
        }

        public String getDotCss() {
            switch (status) {
                case NEAR_FULL: return "bg-amber-500";
                default:        return "";
            }
        }

        public String getCellCss() {
            String base = "relative w-10 h-10 sm:w-11 sm:h-11 rounded-xl flex items-center justify-center "
                    + "border transition-all duration-200 ";

            if (!inCurrentMonth) {
                return base + "border-transparent opacity-40 cursor-not-allowed";
            }

            String state;
            switch (status) {
                case AVAILABLE:
                    state = "bg-emerald-50 border-emerald-200 hover:border-emerald-400";
                    break;
                case NEAR_FULL:
                    state = "bg-amber-50 border-amber-200 hover:border-amber-400";
                    break;
                case FULL:
                    state = "bg-red-50 border-red-200 opacity-70";
                    break;
                case BLOCKED:
                    state = "bg-gray-100 border-gray-200 opacity-70";
                    break;
                case TOO_SOON:
                    state = "bg-rose-50 border-rose-200 opacity-70 cursor-not-allowed";
                    break;
                case PAST:
                    state = "bg-gray-50 border-gray-200 opacity-50 cursor-not-allowed";
                    break;
                default:
                    state = "bg-white border-gray-200 hover:border-yellow-400 hover:bg-yellow-50/40";
                    break;
            }

            if (selected) {
                state += " ring-2 ring-yellow-400 ring-offset-2";
            }
            return base + state;
        }

        public String getTextCss() {
            if (!inCurrentMonth) return "text-gray-400 text-sm font-medium";

            if (status == StatusType.PAST) return "text-gray-400 text-sm font-medium";
            if (status == StatusType.TOO_SOON) return "text-rose-600 text-sm font-semibold";
            if (status == StatusType.FULL || status == StatusType.BLOCKED) return "text-gray-600 text-sm font-semibold";

            if (selected) return "text-gray-900 text-sm font-extrabold";
            return "text-gray-800 text-sm font-semibold";
        }
    }
}
