package com.mypack.customer.bean;

import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.entity.RestaurantDayCapacity;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
import com.mypack.sessionbean.RestaurantDayCapacityFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("publicAvailabilityBean")
@ViewScoped
public class RestaurantPublicAvailabilityBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

    @EJB
    private RestaurantDayCapacityFacadeLocal dayCapacityFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    // ===== viewParam =====
    private Integer restaurantId;

    // ===== data =====
    private Restaurants restaurant;
    private RestaurantCapacitySettings settings;

    private int defaultMaxGuestsPerDay = 150;
    private int defaultMaxBookingsPerDay = 10;

    private YearMonth currentMonth;
    private List<CalendarDay> calendarDays;

    // date -> status (NONE/NEAR_FULL/FULL/BLOCKED/TOO_SOON)
    private final Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();

    // booking aggregates from DB
    private final Map<LocalDate, int[]> bookingAggMap = new HashMap<>();

    // manual overrides from RestaurantDayCapacity (non-block)
    private final Map<LocalDate, ManualOverride> manualOverrideMap = new HashMap<>();

    // if any "0/0" row exists for date => blocked
    private final Set<LocalDate> blockedDates = new HashSet<>();

    // ===== selection =====
    private String selectedDateIso;            // bound by hidden input
    private String selectedStatusLabel = "--";
    private boolean canProceed = false;

    private boolean loaded = false;

    @PostConstruct
    public void init() {
        if (currentMonth == null) currentMonth = YearMonth.now();
    }

    // called by <f:viewAction>
    public void load() {
        if (loaded) {
            // still ensure calendar exists after ajax navigations
            if (calendarDays == null || calendarDays.isEmpty()) {
                reloadCalendar();
            }
            return;
        }
        loaded = true;

        if (currentMonth == null) currentMonth = YearMonth.now();

        restaurant = resolveRestaurant();
        loadSettings();

        reloadCalendar();

        // if user already selected a date (ajax), re-evaluate
        if (selectedDateIso != null && !selectedDateIso.isBlank()) {
            selectSelectedDate();
        }
    }

    private Restaurants resolveRestaurant() {
        if (restaurantId == null) return null;
        try {
            return restaurantsFacade.find(restaurantId);
        } catch (Exception ignore) {
            // fallback safe
            try {
                List<Restaurants> all = restaurantsFacade.findAll();
                if (all != null) {
                    for (Restaurants r : all) {
                        if (r != null && r.getRestaurantId() != null
                                && r.getRestaurantId().toString().equals(String.valueOf(restaurantId))) {
                            return r;
                        }
                    }
                }
            } catch (Exception ignored2) {
            }
            return null;
        }
    }

    private void loadSettings() {
        if (restaurant == null) {
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
            return;
        }

        settings = capacitySettingsFacade.findByRestaurant(restaurant);
        if (settings != null) {
            Integer mg = settings.getMaxGuestsPerSlot();     // bạn đang dùng field này như maxGuests/day
            Integer mb = settings.getMaxBookingsPerDay();

            defaultMaxGuestsPerDay = (mg != null && mg > 0) ? mg : 150;
            defaultMaxBookingsPerDay = (mb != null && mb > 0) ? mb : 10;
        } else {
            defaultMaxGuestsPerDay = 150;
            defaultMaxBookingsPerDay = 10;
        }
    }

    private void reloadCalendar() {
        dayStatusMap.clear();
        bookingAggMap.clear();
        manualOverrideMap.clear();
        blockedDates.clear();

        if (restaurant == null) {
            buildCalendar();
            return;
        }

        // load manual capacity rows for this month (BLOCKED + manual override)
        loadDayCapacityRowsForMonth();

        // load booking aggregates (guests/bookings) for 42-cell grid range (smooth UX)
        loadBookingAggForCalendarGrid();

        // merge into dayStatusMap
        mergeStatus();

        buildCalendar();
    }

    private void loadDayCapacityRowsForMonth() {
        LocalDate first = currentMonth.atDay(1);
        LocalDate last = currentMonth.atEndOfMonth();

        List<RestaurantDayCapacity> rows = dayCapacityFacade.findByRestaurantAndDateRange(
                restaurant,
                java.sql.Date.valueOf(first),
                java.sql.Date.valueOf(last)
        );

        if (rows == null) return;

        for (RestaurantDayCapacity d : rows) {
            LocalDate date = toLocalDate(d.getEventDate());
            if (date == null) continue;

            Integer mg = d.getMaxGuests();
            Integer mb = d.getMaxBookings();

            boolean isBlocked = (mg != null && mb != null && mg == 0 && mb == 0);
            if (isBlocked) {
                blockedDates.add(date);
                dayStatusMap.put(date, StatusType.BLOCKED);
                continue;
            }

            // only treat ALLDAY as the public availability signal
            String slot = d.getSlotCode();
            if (slot != null && !slot.isBlank() && !"ALLDAY".equalsIgnoreCase(slot)) {
                continue;
            }

            int curGuests = safeInt(d.getCurrentGuestCount());
            int curBookings = safeInt(d.getCurrentBookingCount());

            int maxGuests = (mg != null && mg > 0) ? mg : defaultMaxGuestsPerDay;
            int maxBookings = (mb != null && mb > 0) ? mb : defaultMaxBookingsPerDay;

            manualOverrideMap.put(date, new ManualOverride(curGuests, curBookings, maxGuests, maxBookings));
        }
    }

    private void loadBookingAggForCalendarGrid() {
        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7; // Sun=0
        LocalDate gridStart = firstOfMonth.minusDays(shift);
        LocalDate gridEndExclusive = gridStart.plusDays(42);

        Date from = java.sql.Date.valueOf(gridStart);
        Date to = java.sql.Date.valueOf(gridEndExclusive);

        List<Object[]> rows = bookingsFacade.aggregateForCalendar(
                restaurant.getRestaurantId(),
                from,
                to
        );

        if (rows == null) return;

        for (Object[] r : rows) {
            LocalDate day = toLocalDate(r[0]);
            if (day == null) continue;

            int totalGuests = numberToInt(r[1]);
            int bookingCount = numberToInt(r[2]);

            bookingAggMap.put(day, new int[]{totalGuests, bookingCount});
        }
    }

    private void mergeStatus() {
        // for every day that appears in either map, compute effective status
        Set<LocalDate> dates = new HashSet<>();
        dates.addAll(bookingAggMap.keySet());
        dates.addAll(manualOverrideMap.keySet());
        dates.addAll(blockedDates);

        for (LocalDate date : dates) {
            if (blockedDates.contains(date)) {
                dayStatusMap.put(date, StatusType.BLOCKED);
                continue;
            }

            int[] agg = bookingAggMap.getOrDefault(date, new int[]{0, 0});
            int aggGuests = agg[0];
            int aggBookings = agg[1];

            ManualOverride ov = manualOverrideMap.get(date);

            int effGuests = aggGuests;
            int effBookings = aggBookings;
            int maxGuests = defaultMaxGuestsPerDay;
            int maxBookings = defaultMaxBookingsPerDay;

            if (ov != null) {
                effGuests = Math.max(effGuests, ov.currentGuests);
                effBookings = Math.max(effBookings, ov.currentBookings);
                maxGuests = (ov.maxGuests > 0) ? ov.maxGuests : maxGuests;
                maxBookings = (ov.maxBookings > 0) ? ov.maxBookings : maxBookings;
            }

            // nếu hoàn toàn trống thì để NONE (không chấm)
            if (effGuests == 0 && effBookings == 0 && ov == null) {
                // do nothing (NONE)
                continue;
            }

            StatusType st = computeStatus(effGuests, effBookings, maxGuests, maxBookings);
            dayStatusMap.put(date, st);
        }
    }

    // ===== MinDaysInAdvance overlay =====
    private int getMinDaysInAdvanceSafe() {
        if (restaurant == null) return 0;
        Integer v = restaurant.getMinDaysInAdvance();
        return (v != null && v > 0) ? v : 0;
    }

    private boolean isTooSoon(LocalDate date) {
        if (date == null) return false;

        int min = getMinDaysInAdvanceSafe();
        if (min <= 0) return false;

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        LocalDate earliestAllowed = today.plusDays(min);

        // chỉ áp dụng cho ngày tương lai
        return !date.isBefore(today) && date.isBefore(earliestAllowed);
    }

    private StatusType applyMinAdvanceOverlay(LocalDate date, StatusType base) {
        if (!isTooSoon(date)) return base;
        if (base == StatusType.BLOCKED || base == StatusType.FULL) return base;
        return StatusType.TOO_SOON;
    }

    // ===== build calendar (42 cells) =====
    private void buildCalendar() {
        calendarDays = new ArrayList<>(42);

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate start = firstOfMonth.minusDays(shift);

        LocalDate selected = null;
        try {
            if (selectedDateIso != null && !selectedDateIso.isBlank()) {
                selected = LocalDate.parse(selectedDateIso);
            }
        } catch (Exception ignore) {
        }

        for (int i = 0; i < 42; i++) {
            LocalDate date = start.plusDays(i);

            boolean inMonth = YearMonth.from(date).equals(currentMonth);
            boolean isSelected = (selected != null && selected.equals(date));

            StatusType base = dayStatusMap.getOrDefault(date, StatusType.NONE);
            StatusType effective = applyMinAdvanceOverlay(date, base);

            boolean disabled = (!inMonth) ? true : (effective == StatusType.BLOCKED || effective == StatusType.FULL || effective == StatusType.TOO_SOON);

            calendarDays.add(new CalendarDay(date, inMonth, effective, disabled, isSelected));
        }
    }

    // ===== actions =====
    public void nextMonth() {
        currentMonth = (currentMonth == null) ? YearMonth.now().plusMonths(1) : currentMonth.plusMonths(1);
        reloadCalendar();
        // reset selection if it’s outside the new month (optional)
        // selectedDateIso = null;
        // selectSelectedDate();
    }

    public void previousMonth() {
        currentMonth = (currentMonth == null) ? YearMonth.now().minusMonths(1) : currentMonth.minusMonths(1);
        reloadCalendar();
    }

    // called by hidden ajax button
    public void selectSelectedDate() {
        canProceed = false;
        selectedStatusLabel = "--";

        if (selectedDateIso == null || selectedDateIso.isBlank()) return;

        LocalDate date;
        try {
            date = LocalDate.parse(selectedDateIso);
        } catch (Exception e) {
            return;
        }

        // compute effective status for selected date
        StatusType base = dayStatusMap.getOrDefault(date, StatusType.NONE);
        StatusType effective = applyMinAdvanceOverlay(date, base);

        selectedStatusLabel = translateStatus(effective);

        // can proceed only if NOT blocked/full/tooSoon and date not past
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        boolean past = date.isBefore(today);

        canProceed = !past
                && (effective == StatusType.NONE || effective == StatusType.AVAILABLE || effective == StatusType.NEAR_FULL);
    }

    // ===== status logic =====
    private StatusType computeStatus(int guestCount, int bookingCount, int maxGuests, int maxBookings) {
        if (maxGuests <= 0) maxGuests = defaultMaxGuestsPerDay;
        if (maxBookings <= 0) maxBookings = defaultMaxBookingsPerDay;

        if (guestCount < 0) guestCount = 0;
        if (bookingCount < 0) bookingCount = 0;

        double guestRatio = (maxGuests > 0) ? (double) guestCount / maxGuests : 0.0;
        double bookingRatio = (maxBookings > 0) ? (double) bookingCount / maxBookings : 0.0;
        double ratio = Math.max(guestRatio, bookingRatio);

        if (ratio >= 1.0) return StatusType.FULL;
        if (ratio >= 0.5) return StatusType.NEAR_FULL;
        return StatusType.AVAILABLE;
    }

    private String translateStatus(StatusType st) {
        switch (st) {
            case NONE:
            case AVAILABLE:
                return "Available";
            case NEAR_FULL:
                return "Limited (Almost full)";
            case FULL:
                return "Fully booked";
            case BLOCKED:
                return "Blocked by restaurant";
            case TOO_SOON:
                return "Not enough days in advance";
            default:
                return "--";
        }
    }

    // ===== util =====
    private LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;

        if (obj instanceof LocalDate) return (LocalDate) obj;
        if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate();
        if (obj instanceof Timestamp) return ((Timestamp) obj).toLocalDateTime().toLocalDate();
        if (obj instanceof Date) {
            Date d = (Date) obj;
            return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }

    private int numberToInt(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        return 0;
    }

    private int safeInt(Integer v) {
        return (v != null) ? v : 0;
    }

    // ===== getters/setters =====
    public Integer getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Integer restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getCurrentMonthLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return currentMonth.format(fmt);
    }

    public List<CalendarDay> getCalendarDays() {
        if (calendarDays == null || calendarDays.isEmpty()) {
            reloadCalendar();
        }
        return calendarDays;
    }

    public String getSelectedDateIso() {
        return selectedDateIso;
    }

    public void setSelectedDateIso(String selectedDateIso) {
        this.selectedDateIso = selectedDateIso;
    }

    public String getSelectedStatusLabel() {
        return selectedStatusLabel;
    }

    public boolean isCanProceed() {
        return canProceed;
    }

    // ===== enum + dto =====
    public enum StatusType {
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED, TOO_SOON
    }

    private static class ManualOverride implements Serializable {
        int currentGuests;
        int currentBookings;
        int maxGuests;
        int maxBookings;

        ManualOverride(int cg, int cb, int mg, int mb) {
            this.currentGuests = cg;
            this.currentBookings = cb;
            this.maxGuests = mg;
            this.maxBookings = mb;
        }
    }

    public static class CalendarDay implements Serializable {
        private static final long serialVersionUID = 1L;

        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final StatusType status;
        private final boolean disabled;
        private final boolean selected;

        public CalendarDay(LocalDate date, boolean inCurrentMonth, StatusType status, boolean disabled, boolean selected) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.status = status;
            this.disabled = disabled;
            this.selected = selected;
        }

        public String getDayNumber() {
            return String.valueOf(date.getDayOfMonth());
        }

        public String getDateIso() {
            return date.toString();
        }

        public boolean isInCurrentMonth() {
            return inCurrentMonth;
        }

        public boolean isDisabled() {
            return disabled;
        }

        public boolean isShowDot() {
            // chỉ hiện dot khi có trạng thái đáng chú ý
            return status == StatusType.NEAR_FULL || status == StatusType.FULL || status == StatusType.BLOCKED || status == StatusType.TOO_SOON;
        }

        public String getDotCss() {
            switch (status) {
                case NEAR_FULL:
                    return "bg-[#EAB308]";
                case FULL:
                    return "bg-[#EF4444]";
                case BLOCKED:
                    return "bg-gray-400";
                case TOO_SOON:
                    return "bg-[#EF4444]";
                default:
                    return "bg-transparent";
            }
        }

        public String getCellCss() {
            String base = "relative w-12 h-12 rounded-xl border flex items-center justify-center transition-all ";

            if (!inCurrentMonth) {
                return base + "bg-transparent border-transparent cursor-default";
            }

            if (selected) {
                // selected style
                return base + "bg-[#0B1120] border-[#0B1120] shadow-lg";
            }

            if (disabled) {
                if (status == StatusType.BLOCKED) {
                    return base + "bg-gray-100 border-gray-200 cursor-not-allowed opacity-80";
                }
                if (status == StatusType.FULL) {
                    return base + "bg-[#FEF2F2] border-[#FCA5A5] cursor-not-allowed";
                }
                if (status == StatusType.TOO_SOON) {
                    return base + "bg-[#FEF2F2] border-[#FECACA] cursor-not-allowed opacity-85";
                }
                return base + "bg-gray-50 border-gray-200 cursor-not-allowed";
            }

            // enabled cells
            if (status == StatusType.NEAR_FULL) {
                return base + "bg-[#FFFBEB] border-[#FACC6B] hover:shadow-sm cursor-pointer";
            }

            return base + "bg-white border-[#E5E7EB] hover:border-[#D4AF37] hover:bg-[#D4AF37]/5 cursor-pointer";
        }

        public String getTextCss() {
            if (!inCurrentMonth) return "text-gray-300 text-sm font-semibold";
            if (selected) return "text-white text-sm font-semibold";
            if (disabled) {
                if (status == StatusType.FULL || status == StatusType.TOO_SOON) return "text-[#DC2626] text-sm font-semibold";
                if (status == StatusType.BLOCKED) return "text-gray-500 text-sm font-semibold";
                return "text-gray-400 text-sm font-semibold";
            }
            return "text-[#111827] text-sm font-semibold";
        }
    }
}
