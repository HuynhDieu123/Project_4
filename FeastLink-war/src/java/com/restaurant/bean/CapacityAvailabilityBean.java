package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    // --- default limits (bên phải) ---
    private int defaultMaxGuestsPerDay;
    private int defaultMaxBookingsPerDay;

    // form chặn theo range
    private String blockStartDate; // yyyy-MM-dd
    private String blockEndDate;
    private String blockHall;

    // --- calendar state ---
    private YearMonth currentMonth;
    private List<CalendarDay> calendarDays;

    // trạng thái từng ngày
    private Map<LocalDate, StatusType> dayStatusMap = new HashMap<>();

    // --- chọn 1 ngày để chỉnh trạng thái ---
    private LocalDate selectedDate;
    private String selectedStatus; // map với enum StatusType

    @PostConstruct
    public void init() {
        defaultMaxGuestsPerDay = 150;
        defaultMaxBookingsPerDay = 10;

        currentMonth = YearMonth.now();

        // DEMO một vài ngày trong tháng hiện tại
        YearMonth demoMonth = currentMonth;
        setStatus(demoMonth.atDay(3), StatusType.AVAILABLE);
        setStatus(demoMonth.atDay(5), StatusType.NEAR_FULL);
        setStatus(demoMonth.atDay(7), StatusType.FULL);

        buildCalendar();
    }

    // ========= CALENDAR LOGIC =========
    private void setStatus(LocalDate date, StatusType status) {
        dayStatusMap.put(date, status);
    }

    private void buildCalendar() {
        calendarDays = new ArrayList<>();

        LocalDate firstOfMonth = currentMonth.atDay(1);
        // Monday=1..Sunday=7  -> Sunday=0
        int shift = firstOfMonth.getDayOfWeek().getValue() % 7;
        LocalDate start = firstOfMonth.minusDays(shift);

        for (int i = 0; i < 42; i++) { // 6 hàng x 7 cột
            LocalDate date = start.plusDays(i);
            boolean inMonth = date.getMonthValue() == currentMonth.getMonthValue();
            StatusType status = dayStatusMap.getOrDefault(date, StatusType.NONE);
            boolean isSelected = (selectedDate != null && selectedDate.equals(date));
            calendarDays.add(new CalendarDay(date, inMonth, status, isSelected));
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

    // ========== CLICK 1 NGÀY TRÊN LỊCH ==========
    public void selectDay(String dateIso) {
        try {
            if (dateIso == null || dateIso.isBlank()) {
                selectedDate = null;
                selectedStatus = null;
            } else {
                selectedDate = LocalDate.parse(dateIso); // yyyy-MM-dd
                StatusType st = dayStatusMap.getOrDefault(selectedDate, StatusType.NONE);
                selectedStatus = st.name(); // bind cho dropdown
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        buildCalendar(); // để highlight ngày được chọn
    }

    // áp dụng trạng thái từ dropdown xuống ngày đã chọn
    public void applyStatusToSelectedDay() {
        // log cho chắc là hàm có chạy và nhận được value mới
        System.out.println("Apply status: date=" + selectedDate + ", status=" + selectedStatus);

        if (selectedDate == null || selectedStatus == null || selectedStatus.isBlank()) {
            return;
        }
        StatusType st;
        try {
            st = StatusType.valueOf(selectedStatus);
        } catch (IllegalArgumentException ex) {
            st = StatusType.NONE;
        }

        if (st == StatusType.NONE) {
            dayStatusMap.remove(selectedDate); // xóa chấm
        } else {
            dayStatusMap.put(selectedDate, st);
        }
        buildCalendar();
    }

    public String getSelectedDateLabel() {
        if (selectedDate == null) {
            return "Chưa chọn ngày";
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return selectedDate.format(fmt);
    }

    // ========= ACTIONS BÊN PHẢI =========
    public void saveLimits() {
        System.out.println("Saving default limits: guests=" + defaultMaxGuestsPerDay
                + ", bookings=" + defaultMaxBookingsPerDay);
    }

    public void blockDates() {
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

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public String getSelectedStatus() {
        return selectedStatus;
    }

    public void setSelectedStatus(String selectedStatus) {
        this.selectedStatus = selectedStatus;
    }

    // ========= INNER TYPES =========
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

        public String getDateIso() {
            return date.toString(); // yyyy-MM-dd
        }

        public boolean isSelected() {
            return selected;
        }

        // nếu cần dùng trong EL: #{day.selectedBorderCss}
        public String getSelectedBorderCss() {
            return selected ? "bg-gray-100 rounded-full" : "";
        }
    }
}
