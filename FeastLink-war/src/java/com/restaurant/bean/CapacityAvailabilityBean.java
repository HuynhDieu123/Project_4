package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    // --- default limits (bên phải) ---
    private int defaultMaxGuestsPerDay;
    private int defaultMaxBookingsPerDay;

    private String blockStartDate; // yyyy-MM-dd từ input type="date"
    private String blockEndDate;
    private String blockHall;

    // --- calendar state ---
    private YearMonth currentMonth;
    private List<CalendarDay> calendarDays;

    // trạng thái từng ngày (sau này có thể load từ DB)
    private Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();

    @PostConstruct
    public void init() {
        defaultMaxGuestsPerDay = 150;
        defaultMaxBookingsPerDay = 10;

        // tháng hiện tại
        currentMonth = YearMonth.now();

        // DEMO: set vài trạng thái giống Figma (có thể bỏ nếu muốn)
        YearMonth dec = YearMonth.of(2023, 12);
        setStatus(dec.atDay(3), StatusType.AVAILABLE);
        setStatus(dec.atDay(4), StatusType.NEAR_FULL);
        setStatus(dec.atDay(7), StatusType.FULL);
        setStatus(dec.atDay(9), StatusType.BLOCKED);
        setStatus(dec.atDay(10), StatusType.AVAILABLE);
        setStatus(dec.atDay(11), StatusType.NEAR_FULL);
        setStatus(dec.atDay(14), StatusType.FULL);
        setStatus(dec.atDay(16), StatusType.BLOCKED);
        setStatus(dec.atDay(17), StatusType.AVAILABLE);
        setStatus(dec.atDay(18), StatusType.NEAR_FULL);
        setStatus(dec.atDay(21), StatusType.FULL);
        setStatus(dec.atDay(23), StatusType.BLOCKED);
        setStatus(dec.atDay(24), StatusType.AVAILABLE);
        setStatus(dec.atDay(25), StatusType.NEAR_FULL);
        setStatus(dec.atDay(28), StatusType.FULL);
        setStatus(dec.atDay(30), StatusType.BLOCKED);
        setStatus(dec.atDay(31), StatusType.AVAILABLE);

        buildCalendar();
    }

    // ========= CALENDAR LOGIC =========

    private void setStatus(LocalDate date, StatusType status) {
        dayStatusMap.put(date, status);
    }

    private void buildCalendar() {
        calendarDays = new ArrayList<>();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        // JS time: Monday=1..Sunday=7 ⇒ ta chuyển về 0..6 với Sunday=0
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) { // 6 hàng x 7 cột
            LocalDate date = start.plusDays(i);
            boolean inMonth = date.getMonthValue() == currentMonth.getMonthValue();
            StatusType status = dayStatusMap.getOrDefault(date, StatusType.NONE);
            calendarDays.add(new CalendarDay(date, inMonth, status));
        }
    }

    public void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    public void previousMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    public String getCurrentMonthLabel() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
        return currentMonth.format(fmt);
    }

    public List<CalendarDay> getCalendarDays() {
        return calendarDays;
    }

    // ========= ACTIONS BÊN PHẢI =========

    public void saveLimits() {
        // TODO: sau này lưu xuống bảng RestaurantCapacitySettings
        System.out.println("Saving default limits: guests=" + defaultMaxGuestsPerDay
                + ", bookings=" + defaultMaxBookingsPerDay);
    }

    public void blockDates() {
        // parse yyyy-MM-dd từ input type="date"
        try {
            if (blockStartDate != null && !blockStartDate.isBlank()
                    && blockEndDate != null && !blockEndDate.isBlank()) {

                LocalDate start = LocalDate.parse(blockStartDate);
                LocalDate end = LocalDate.parse(blockEndDate);

                if (!end.isBefore(start)) {
                    LocalDate d = start;
                    while (!d.isAfter(end)) {
                        dayStatusMap.put(d, StatusType.BLOCKED);
                        d = d.plusDays(1);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // rebuild calendar để thấy dot xám
        buildCalendar();
    }

    // ========= GET / SET =========

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

    // ========= INNER TYPES =========

    public enum StatusType {
        NONE, AVAILABLE, NEAR_FULL, FULL, BLOCKED
    }

    public static class CalendarDay implements Serializable {
        private final LocalDate date;
        private final boolean inCurrentMonth;
        private final StatusType status;

        public CalendarDay(LocalDate date, boolean inCurrentMonth, StatusType status) {
            this.date = date;
            this.inCurrentMonth = inCurrentMonth;
            this.status = status;
        }

        public boolean isInCurrentMonth() {
            return inCurrentMonth;
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

        public StatusType getStatus() {
            return status;
        }

        public LocalDate getDate() {
            return date;
        }
    }
}
