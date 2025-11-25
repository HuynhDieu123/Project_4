package com.restaurant.bean;

import jakarta.annotation.PostConstruct;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("capacityAvailabilityBean")
@ViewScoped
public class CapacityAvailabilityBean implements Serializable {

    private int defaultMaxGuestsPerDay;
    private int defaultMaxBookingsPerDay;

    private String blockStartDate;
    private String blockEndDate;
    private String blockHall;

    private List<DayStatus> days;

    @PostConstruct
    public void init() {
        defaultMaxGuestsPerDay = 150;
        defaultMaxBookingsPerDay = 10;

        days = new ArrayList<>();
        days.add(new DayStatus("Dec 3",  "Sun", "Available", "text-green-600"));
        days.add(new DayStatus("Dec 4",  "Mon", "Almost full", "text-yellow-600"));
        days.add(new DayStatus("Dec 7",  "Thu", "Full", "text-red-600"));
        days.add(new DayStatus("Dec 10", "Sun", "Available", "text-green-600"));
        days.add(new DayStatus("Dec 15", "Fri", "Full", "text-red-600"));
        days.add(new DayStatus("Dec 24", "Sun", "Available", "text-green-600"));
    }

    // Actions
    public void saveLimits() {
        System.out.println("Saving default limits: guests=" + defaultMaxGuestsPerDay
                + ", bookings=" + defaultMaxBookingsPerDay);
    }

    public void blockDates() {
        System.out.println("Blocking dates from " + blockStartDate + " to " + blockEndDate
                + " for hall: " + blockHall);
    }

    // Getters / Setters

    public int getDefaultMaxGuestsPerDay() { return defaultMaxGuestsPerDay; }
    public void setDefaultMaxGuestsPerDay(int v) { defaultMaxGuestsPerDay = v; }

    public int getDefaultMaxBookingsPerDay() { return defaultMaxBookingsPerDay; }
    public void setDefaultMaxBookingsPerDay(int v) { defaultMaxBookingsPerDay = v; }

    public String getBlockStartDate() { return blockStartDate; }
    public void setBlockStartDate(String blockStartDate) { this.blockStartDate = blockStartDate; }

    public String getBlockEndDate() { return blockEndDate; }
    public void setBlockEndDate(String blockEndDate) { this.blockEndDate = blockEndDate; }

    public String getBlockHall() { return blockHall; }
    public void setBlockHall(String blockHall) { this.blockHall = blockHall; }

    public List<DayStatus> getDays() { return days; }

    // Inner class
    public static class DayStatus implements Serializable {
        private String dateLabel;
        private String dayOfWeek;
        private String status;
        private String statusClass;

        public DayStatus(String dateLabel, String dayOfWeek,
                         String status, String statusClass) {
            this.dateLabel = dateLabel;
            this.dayOfWeek = dayOfWeek;
            this.status = status;
            this.statusClass = statusClass;
        }

        public String getDateLabel() { return dateLabel; }
        public String getDayOfWeek() { return dayOfWeek; }
        public String getStatus() { return status; }
        public String getStatusClass() { return statusClass; }
    }
}
