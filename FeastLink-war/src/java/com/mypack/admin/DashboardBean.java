package com.mypack.admin;

import com.mypack.entity.Bookings;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DF_DDMM = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter DF_MMYYYY = DateTimeFormatter.ofPattern("MM/yyyy");

    // Java 8/11 compatible: không dùng Set.of
    private static final Set<String> REVENUE_BOOKING_STATUSES =
            new HashSet<>(Arrays.asList("CONFIRMED", "PAID", "COMPLETED"));

    private static final String PAYMENT_PAID = "PAID";

    @EJB private UsersFacadeLocal usersFacade;
    @EJB private RestaurantsFacadeLocal restaurantsFacade;
    @EJB private BookingsFacadeLocal bookingsFacade;

    // ====== FILTER (DAY / MONTH / YEAR / RANGE) ======
    private String periodMode = "MONTH"; // DAY | MONTH | YEAR | RANGE

    private Date selectedDay;
    private int selectedMonth;
    private int selectedYear;
    private Date rangeFrom;
    private Date rangeTo;

    private LocalDate appliedFrom;
    private LocalDate appliedTo;
    private String appliedLabel = "";

    // ====== TOP METRICS ======
    private long totalUsers;
    private long totalRestaurants;
    private long totalBookings;

    private long confirmedCount;
    private long pendingCount;
    private long cancelledCount;

    private long pendingApprovals; // restaurants pending approval

    private int cancelRate; // %
    private BigDecimal monthlyRevenue = BigDecimal.ZERO;
    private String formattedMonthlyRevenue = "0";

    // ====== CHART JSON ======
    private String revenueByMonthJson = "{\"labels\":[],\"values\":[]}";
    private String bookingsByStatusJson = "{\"labels\":[],\"values\":[]}";
    private String cancelRateByMonthJson = "{\"labels\":[],\"values\":[]}";

    // ====== TABLE ======
    private List<Bookings> recentBookings = new ArrayList<>();

    @PostConstruct
    public void init() {
        LocalDate now = LocalDate.now(ZONE);

        this.selectedDay = Date.from(now.atStartOfDay(ZONE).toInstant());
        this.selectedMonth = now.getMonthValue();
        this.selectedYear = now.getYear();

        // RANGE mặc định 30 ngày gần nhất
        LocalDate from = now.minusDays(29);
        this.rangeFrom = Date.from(from.atStartOfDay(ZONE).toInstant());
        this.rangeTo = Date.from(now.atStartOfDay(ZONE).toInstant());

        applyFilter();
    }

    /** Gọi khi đổi mode để set default hợp lý + auto apply */
    public void onModeChanged() {
        LocalDate now = LocalDate.now(ZONE);

        String mode = safeUpper(periodMode);
        switch (mode) {
            case "DAY":
                if (selectedDay == null) {
                    selectedDay = Date.from(now.atStartOfDay(ZONE).toInstant());
                }
                break;

            case "MONTH":
                if (selectedYear <= 0) selectedYear = now.getYear();
                if (selectedMonth <= 0 || selectedMonth > 12) selectedMonth = now.getMonthValue();
                break;

            case "YEAR":
                if (selectedYear <= 0) selectedYear = now.getYear();
                break;

            case "RANGE":
                if (rangeFrom == null || rangeTo == null) {
                    LocalDate f = now.minusDays(29);
                    rangeFrom = Date.from(f.atStartOfDay(ZONE).toInstant());
                    rangeTo = Date.from(now.atStartOfDay(ZONE).toInstant());
                }
                break;

            default:
                periodMode = "MONTH";
                selectedMonth = now.getMonthValue();
                selectedYear = now.getYear();
                break;
        }

        applyFilter();
    }

    /** Apply filter + reload dashboard data (đã sửa switch -> thành switch thường) */
    public void applyFilter() {
        LocalDate from;
        LocalDate to;

        try {
            String mode = safeUpper(periodMode);

            switch (mode) {
                case "DAY": {
                    LocalDate d = toLocalDate(selectedDay);
                    from = d;
                    to = d;
                    appliedLabel = "1 ngày: " + d;
                    break;
                }

                case "MONTH": {
                    YearMonth ym = YearMonth.of(selectedYear, selectedMonth);
                    from = ym.atDay(1);
                    to = ym.atEndOfMonth();
                    appliedLabel = "1 tháng: " + ym.getMonthValue() + "/" + ym.getYear();
                    break;
                }

                case "YEAR": {
                    from = LocalDate.of(selectedYear, 1, 1);
                    to = LocalDate.of(selectedYear, 12, 31);
                    appliedLabel = "1 năm: " + selectedYear;
                    break;
                }

                case "RANGE": {
                    if (rangeFrom == null || rangeTo == null) {
                        FacesContext.getCurrentInstance().addMessage(null,
                                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                        "Thiếu ngày", "Vui lòng chọn Từ ngày và Đến ngày."));
                        return;
                    }

                    LocalDate rf = toLocalDate(rangeFrom);
                    LocalDate rt = toLocalDate(rangeTo);

                    if (rf.isAfter(rt)) {
                        LocalDate tmp = rf; rf = rt; rt = tmp;
                    }

                    from = rf;
                    to = rt;
                    appliedLabel = "Khoảng: " + rf + " → " + rt;
                    break;
                }

                default: {
                    periodMode = "MONTH";
                    YearMonth ym = YearMonth.now(ZONE);
                    from = ym.atDay(1);
                    to = ym.atEndOfMonth();
                    appliedLabel = "1 tháng: " + ym.getMonthValue() + "/" + ym.getYear();
                    break;
                }
            }

        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Filter không hợp lệ", "Vui lòng chọn lại thời gian."));
            return;
        }

        appliedFrom = from;
        appliedTo = to;

        loadDashboard(from, to);
    }

    private void loadDashboard(LocalDate from, LocalDate to) {
        List<Users> users = safeList(usersFacade != null ? usersFacade.findAll() : null);
        List<Restaurants> restaurants = safeList(restaurantsFacade != null ? restaurantsFacade.findAll() : null);
        List<Bookings> bookingsAll = safeList(bookingsFacade != null ? bookingsFacade.findAll() : null);

        totalUsers = users.stream()
                .filter(u -> isEqIgnoreCase(getStr(u, "getRole"), "CUSTOMER") || getStr(u, "getRole") == null)
                .filter(u -> getStr(u, "getStatus") == null || isEqIgnoreCase(getStr(u, "getStatus"), "ACTIVE"))
                .count();

        totalRestaurants = restaurants.stream()
                .filter(r -> getStr(r, "getStatus") == null || isEqIgnoreCase(getStr(r, "getStatus"), "ACTIVE"))
                .count();

        pendingApprovals = restaurants.stream()
                .filter(r -> isEqIgnoreCase(getStr(r, "getStatus"), "PENDING_APPROVAL")
                        || isEqIgnoreCase(getStr(r, "getStatus"), "PENDING"))
                .count();

        List<Bookings> bookings = bookingsAll.stream()
                .filter(b -> isInRange(getBookingDate(b), from, to))
                .collect(Collectors.toList());

        totalBookings = bookings.size();

        confirmedCount = bookings.stream().filter(b -> isEqIgnoreCase(getBookingStatus(b), "CONFIRMED")).count();
        pendingCount   = bookings.stream().filter(b -> isEqIgnoreCase(getBookingStatus(b), "PENDING")).count();
        cancelledCount = bookings.stream().filter(b -> isCancelled(getBookingStatus(b))).count();

        cancelRate = (totalBookings == 0) ? 0 : (int) Math.round((cancelledCount * 100.0) / totalBookings);

        monthlyRevenue = bookings.stream()
                .filter(this::isRevenueBooking)
                .map(this::getBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        formattedMonthlyRevenue = formatMoney(monthlyRevenue);

        recentBookings = bookings.stream()
                .sorted((a, b) -> compareBookingDesc(a, b))
                .limit(10)
                .collect(Collectors.toList());

        revenueByMonthJson = buildRevenueSeriesJson(bookings, from, to);
        bookingsByStatusJson = buildStatusJson(bookings);
        cancelRateByMonthJson = buildCancelRateSeriesJson(bookings, from, to);
    }

    // ================== FIX: Lấy tên nhà hàng để render trong bảng ==================
    public String restaurantName(Bookings b) {
        if (b == null) return "-";

        // thường là ManyToOne Restaurants -> getRestaurantId()
        Object r = getObj(b, "getRestaurantId", null);
        if (r == null) r = getObj(b, "getRestaurant", null);

        if (r == null) return "-";

        // nếu là entity Restaurants
        String name = getStr(r, "getName");
        if (name == null || name.trim().isEmpty()) name = getStr(r, "getRestaurantName");

        if (name != null && !name.trim().isEmpty()) return name.trim();

        // fallback cuối: tránh in cả package dài
        // nếu vẫn muốn, bạn có thể return r.toString()
        return "-";
    }

    // ================= GETTERS used by XHTML =================
    public long getTotalUsers() { return totalUsers; }
    public long getTotalRestaurants() { return totalRestaurants; }
    public long getTotalBookings() { return totalBookings; }

    public long getPendingApprovals() { return pendingApprovals; }

    public long getConfirmedCount() { return confirmedCount; }
    public long getPendingCount() { return pendingCount; }
    public long getCancelledCount() { return cancelledCount; }

    public int getCancelRate() { return cancelRate; }
    public String getFormattedMonthlyRevenue() { return formattedMonthlyRevenue; }

    public String getRevenueByMonthJson() { return revenueByMonthJson; }
    public String getBookingsByStatusJson() { return bookingsByStatusJson; }
    public String getCancelRateByMonthJson() { return cancelRateByMonthJson; }

    public List<Bookings> getRecentBookings() { return recentBookings; }

    // ===== Filter getters/setters =====
    public String getPeriodMode() { return periodMode; }
    public void setPeriodMode(String periodMode) { this.periodMode = periodMode; }

    public Date getSelectedDay() { return selectedDay; }
    public void setSelectedDay(Date selectedDay) { this.selectedDay = selectedDay; }

    public int getSelectedMonth() { return selectedMonth; }
    public void setSelectedMonth(int selectedMonth) { this.selectedMonth = selectedMonth; }

    public int getSelectedYear() { return selectedYear; }
    public void setSelectedYear(int selectedYear) { this.selectedYear = selectedYear; }

    public Date getRangeFrom() { return rangeFrom; }
    public void setRangeFrom(Date rangeFrom) { this.rangeFrom = rangeFrom; }

    public Date getRangeTo() { return rangeTo; }
    public void setRangeTo(Date rangeTo) { this.rangeTo = rangeTo; }

    public LocalDate getAppliedFrom() { return appliedFrom; }
    public LocalDate getAppliedTo() { return appliedTo; }
    public String getAppliedLabel() { return appliedLabel; }

    public List<SelectItem> getMonthOptions() {
        List<SelectItem> items = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) items.add(new SelectItem(m, "Tháng " + m));
        return items;
    }

    public List<SelectItem> getYearOptions() {
        int nowYear = LocalDate.now(ZONE).getYear();
        int start = nowYear - 5;
        int end = nowYear + 1;
        List<SelectItem> items = new ArrayList<>();
        for (int y = end; y >= start; y--) items.add(new SelectItem(y, String.valueOf(y)));
        return items;
    }

    // ==================== CHART BUILDERS ====================

    private String buildRevenueSeriesJson(List<Bookings> bookings, LocalDate from, LocalDate to) {
        String gran = resolveGranularity(from, to);

        if ("HOUR".equals(gran)) {
            BigDecimal[] sum = new BigDecimal[24];
            Arrays.setAll(sum, i -> BigDecimal.ZERO);
            for (Bookings b : bookings) {
                if (!isRevenueBooking(b)) continue;
                Date d = getBookingDate(b);
                if (d == null) continue;
                int hour = ZonedDateTime.ofInstant(d.toInstant(), ZONE).getHour();
                sum[hour] = sum[hour].add(getBookingAmount(b));
            }
            String[] labels = new String[24];
            for (int i = 0; i < 24; i++) labels[i] = String.format("%02dh", i);
            return toJson(labels, sum);
        }

        if ("DAY".equals(gran)) {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            if (days < 1) days = 1;

            BigDecimal[] sum = new BigDecimal[(int) days];
            Arrays.setAll(sum, i -> BigDecimal.ZERO);

            for (Bookings b : bookings) {
                if (!isRevenueBooking(b)) continue;
                Date d = getBookingDate(b);
                if (d == null) continue;
                LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
                if (ld.isBefore(from) || ld.isAfter(to)) continue;

                int idx = (int) ChronoUnit.DAYS.between(from, ld);
                if (idx >= 0 && idx < sum.length) sum[idx] = sum[idx].add(getBookingAmount(b));
            }

            String[] labels = new String[(int) days];
            boolean sameMonth = from.getYear() == to.getYear()
                    && from.getMonthValue() == to.getMonthValue()
                    && days <= 31;

            for (int i = 0; i < days; i++) {
                LocalDate d = from.plusDays(i);
                labels[i] = sameMonth ? String.valueOf(d.getDayOfMonth()) : d.format(DF_DDMM);
            }
            return toJson(labels, sum);
        }

        // MONTH granularity
        if ("YEAR".equals(safeUpper(periodMode)) && from.getYear() == to.getYear()
                && from.getMonthValue() == 1 && to.getMonthValue() == 12) {

            BigDecimal[] sum = new BigDecimal[12];
            Arrays.setAll(sum, i -> BigDecimal.ZERO);

            for (Bookings b : bookings) {
                if (!isRevenueBooking(b)) continue;
                Date d = getBookingDate(b);
                if (d == null) continue;
                LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
                if (ld.getYear() != from.getYear()) continue;

                int idx = ld.getMonthValue() - 1;
                sum[idx] = sum[idx].add(getBookingAmount(b));
            }

            String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            return toJson(labels, sum);
        }

        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        int months = (int) (start.until(end, ChronoUnit.MONTHS) + 1);
        if (months < 1) months = 1;

        BigDecimal[] sum = new BigDecimal[months];
        Arrays.setAll(sum, i -> BigDecimal.ZERO);

        for (Bookings b : bookings) {
            if (!isRevenueBooking(b)) continue;
            Date d = getBookingDate(b);
            if (d == null) continue;
            LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
            if (ld.isBefore(from) || ld.isAfter(to)) continue;

            YearMonth ym = YearMonth.from(ld);
            int idx = (int) (start.until(ym, ChronoUnit.MONTHS));
            if (idx >= 0 && idx < months) sum[idx] = sum[idx].add(getBookingAmount(b));
        }

        String[] labels = new String[months];
        for (int i = 0; i < months; i++) labels[i] = start.plusMonths(i).format(DF_MMYYYY);

        return toJson(labels, sum);
    }

    private String buildCancelRateSeriesJson(List<Bookings> bookings, LocalDate from, LocalDate to) {
        String gran = resolveGranularity(from, to);

        if ("HOUR".equals(gran)) {
            long[] total = new long[24];
            long[] cancel = new long[24];

            for (Bookings b : bookings) {
                Date d = getBookingDate(b);
                if (d == null) continue;
                int hour = ZonedDateTime.ofInstant(d.toInstant(), ZONE).getHour();
                total[hour]++;
                if (isCancelled(getBookingStatus(b))) cancel[hour]++;
            }

            double[] rate = new double[24];
            for (int i = 0; i < 24; i++) rate[i] = total[i] == 0 ? 0 : (cancel[i] * 100.0 / total[i]);

            String[] labels = new String[24];
            for (int i = 0; i < 24; i++) labels[i] = String.format("%02dh", i);

            return toJson(labels, rate);
        }

        if ("DAY".equals(gran)) {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            if (days < 1) days = 1;

            long[] total = new long[(int) days];
            long[] cancel = new long[(int) days];

            for (Bookings b : bookings) {
                Date d = getBookingDate(b);
                if (d == null) continue;
                LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
                if (ld.isBefore(from) || ld.isAfter(to)) continue;

                int idx = (int) ChronoUnit.DAYS.between(from, ld);
                total[idx]++;
                if (isCancelled(getBookingStatus(b))) cancel[idx]++;
            }

            double[] rate = new double[(int) days];
            for (int i = 0; i < days; i++) rate[i] = total[i] == 0 ? 0 : (cancel[i] * 100.0 / total[i]);

            String[] labels = new String[(int) days];
            boolean sameMonth = from.getYear() == to.getYear()
                    && from.getMonthValue() == to.getMonthValue()
                    && days <= 31;

            for (int i = 0; i < days; i++) {
                LocalDate d = from.plusDays(i);
                labels[i] = sameMonth ? String.valueOf(d.getDayOfMonth()) : d.format(DF_DDMM);
            }

            return toJson(labels, rate);
        }

        if ("YEAR".equals(safeUpper(periodMode)) && from.getYear() == to.getYear()
                && from.getMonthValue() == 1 && to.getMonthValue() == 12) {

            long[] total = new long[12];
            long[] cancel = new long[12];

            for (Bookings b : bookings) {
                Date d = getBookingDate(b);
                if (d == null) continue;
                LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
                if (ld.getYear() != from.getYear()) continue;

                int idx = ld.getMonthValue() - 1;
                total[idx]++;
                if (isCancelled(getBookingStatus(b))) cancel[idx]++;
            }

            double[] rate = new double[12];
            for (int i = 0; i < 12; i++) rate[i] = total[i] == 0 ? 0 : (cancel[i] * 100.0 / total[i]);

            String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
            return toJson(labels, rate);
        }

        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        int months = (int) (start.until(end, ChronoUnit.MONTHS) + 1);
        if (months < 1) months = 1;

        long[] total = new long[months];
        long[] cancel = new long[months];

        for (Bookings b : bookings) {
            Date d = getBookingDate(b);
            if (d == null) continue;
            LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
            if (ld.isBefore(from) || ld.isAfter(to)) continue;

            YearMonth ym = YearMonth.from(ld);
            int idx = (int) (start.until(ym, ChronoUnit.MONTHS));
            if (idx >= 0 && idx < months) {
                total[idx]++;
                if (isCancelled(getBookingStatus(b))) cancel[idx]++;
            }
        }

        double[] rate = new double[months];
        for (int i = 0; i < months; i++) rate[i] = total[i] == 0 ? 0 : (cancel[i] * 100.0 / total[i]);

        String[] labels = new String[months];
        for (int i = 0; i < months; i++) labels[i] = start.plusMonths(i).format(DF_MMYYYY);

        return toJson(labels, rate);
    }

    private String buildStatusJson(List<Bookings> bookings) {
        // Java 8/11 compatible: không dùng List.of
        List<String> baseOrder = new ArrayList<>(Arrays.asList(
                "PENDING", "CONFIRMED", "PAID", "COMPLETED", "CANCELLED", "REJECTED"
        ));

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String s : baseOrder) counts.put(s, 0L);

        for (Bookings b : bookings) {
            String st = safeUpper(getBookingStatus(b));
            if (st.isEmpty()) continue;
            if (!counts.containsKey(st)) counts.put(st, 0L);
            counts.put(st, counts.get(st) + 1);
        }

        String[] labels = counts.keySet().toArray(new String[0]);
        long[] values = new long[counts.size()];
        int i = 0;
        for (Long v : counts.values()) values[i++] = (v == null ? 0 : v);

        return toJson(labels, values);
    }

    private String resolveGranularity(LocalDate from, LocalDate to) {
        String mode = safeUpper(periodMode);
        if ("DAY".equals(mode)) return "HOUR";
        if ("MONTH".equals(mode)) return "DAY";
        if ("YEAR".equals(mode)) return "MONTH";

        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days <= 31 ? "DAY" : "MONTH";
    }

    // ================= Helpers =================
    private static <T> List<T> safeList(List<T> list) { return list == null ? new ArrayList<>() : list; }

    private static boolean isEqIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static String safeUpper(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isCancelled(String status) {
        String s = safeUpper(status);
        return "CANCELLED".equals(s) || "CANCELED".equals(s);
    }

    private static boolean isInRange(Date d, LocalDate from, LocalDate to) {
        if (d == null || from == null || to == null) return false;
        LocalDate ld = d.toInstant().atZone(ZONE).toLocalDate();
        return (!ld.isBefore(from)) && (!ld.isAfter(to));
    }

    private LocalDate toLocalDate(Date d) {
        if (d == null) throw new IllegalArgumentException("Date is null");
        return d.toInstant().atZone(ZONE).toLocalDate();
    }

    private boolean isRevenueBooking(Bookings b) {
        String pay = getStr(b, "getPaymentStatus");
        if (isEqIgnoreCase(pay, PAYMENT_PAID)) return true;

        String st = safeUpper(getBookingStatus(b));
        return REVENUE_BOOKING_STATUSES.contains(st);
    }

    private String getBookingStatus(Bookings b) {
        String s = getStr(b, "getBookingStatus");
        if (s == null) s = getStr(b, "getStatus");
        return s;
    }

    private BigDecimal getBookingAmount(Bookings b) {
        Object val =
                getObj(b, "getTotalAmount",
                getObj(b, "getTotalPrice",
                getObj(b, "getFinalAmount",
                getObj(b, "getTotalCost",
                getObj(b, "getTotalPayment", null)))));

        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private Date getBookingDate(Bookings b) {
        Object d =
                getObj(b, "getCreatedAt",
                getObj(b, "getEventDate",
                getObj(b, "getUpdatedAt", null)));

        return (d instanceof Date) ? (Date) d : null;
    }

    private String formatMoney(BigDecimal money) {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return nf.format(money);
        } catch (Exception e) {
            return money == null ? "0" : money.toPlainString();
        }
    }

    private int compareBookingDesc(Bookings a, Bookings b) {
        Date da = getBookingDate(a);
        Date db = getBookingDate(b);
        if (da != null && db != null) return db.compareTo(da);

        Long ia = getLong(a, "getBookingId");
        Long ib = getLong(b, "getBookingId");
        if (ia != null && ib != null) return Long.compare(ib, ia);
        return 0;
    }

    // ================= JSON builders =================
    private static String toJson(String[] labels, BigDecimal[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"labels\":[");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(labels[i]).append("\"");
        }
        sb.append("],\"values\":[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            BigDecimal v = values[i] == null ? BigDecimal.ZERO : values[i];
            sb.append(v.toPlainString());
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String toJson(String[] labels, double[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"labels\":[");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(labels[i]).append("\"");
        }
        sb.append("],\"values\":[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format(Locale.ROOT, "%.2f", values[i]));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String toJson(String[] labels, long[] values) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"labels\":[");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(labels[i]).append("\"");
        }
        sb.append("],\"values\":[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(values[i]);
        }
        sb.append("]}");
        return sb.toString();
    }

    // ================= Reflection Helpers =================
    private static String getStr(Object obj, String method) {
        Object o = getObj(obj, method, null);
        return (o == null) ? null : String.valueOf(o);
    }

    private static Long getLong(Object obj, String method) {
        Object o = getObj(obj, method, null);
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return null; }
    }

    private static Object getObj(Object obj, String method, Object fallback) {
        try {
            Method m = obj.getClass().getMethod(method);
            return m.invoke(obj);
        } catch (Exception e) {
            return fallback;
        }
    }
}
