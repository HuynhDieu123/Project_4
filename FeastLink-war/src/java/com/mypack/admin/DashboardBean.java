package com.mypack.admin;

import com.mypack.entity.Users;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Bookings;
import com.mypack.sessionbean.UsersFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.BookingsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Named("dashboardBean")
@ViewScoped
public class DashboardBean implements Serializable {

    @EJB private UsersFacadeLocal usersFacade;
    @EJB private RestaurantsFacadeLocal restaurantsFacade;
    @EJB private BookingsFacadeLocal bookingsFacade;

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
        loadDashboard();
    }

    private void loadDashboard() {
        // 1) Load data (cách an toàn: findAll rồi tính trong Java)
        List<Users> users = safeList(usersFacade != null ? usersFacade.findAll() : null);
        List<Restaurants> restaurants = safeList(restaurantsFacade != null ? restaurantsFacade.findAll() : null);
        List<Bookings> bookings = safeList(bookingsFacade != null ? bookingsFacade.findAll() : null);

        // 2) totalUsers: “customer active” (bạn có thể nới điều kiện tùy DB)
        totalUsers = users.stream()
                .filter(u -> isEqIgnoreCase(getStr(u, "getRole"), "CUSTOMER") || getStr(u, "getRole") == null)
                .filter(u -> getStr(u, "getStatus") == null || isEqIgnoreCase(getStr(u, "getStatus"), "ACTIVE"))
                .count();

        // 3) totalRestaurants: “live” (nếu status khác thì đổi)
        totalRestaurants = restaurants.stream()
                .filter(r -> getStr(r, "getStatus") == null || isEqIgnoreCase(getStr(r, "getStatus"), "ACTIVE"))
                .count();

        // 4) pendingApprovals (restaurant chờ duyệt)
        pendingApprovals = restaurants.stream()
                .filter(r -> isEqIgnoreCase(getStr(r, "getStatus"), "PENDING_APPROVAL")
                          || isEqIgnoreCase(getStr(r, "getStatus"), "PENDING"))
                .count();

        // 5) Booking counts
        totalBookings = bookings.size();
        confirmedCount = bookings.stream().filter(b -> isEqIgnoreCase(getBookingStatus(b), "CONFIRMED")).count();
        pendingCount   = bookings.stream().filter(b -> isEqIgnoreCase(getBookingStatus(b), "PENDING")).count();
        cancelledCount = bookings.stream().filter(b -> isEqIgnoreCase(getBookingStatus(b), "CANCELLED")).count();

        // 6) Cancel rate %
        cancelRate = (totalBookings == 0) ? 0 : (int) Math.round((cancelledCount * 100.0) / totalBookings);

        // 7) Monthly revenue (this month) - sum amount of CONFIRMED
        LocalDate now = LocalDate.now();
        monthlyRevenue = bookings.stream()
                .filter(b -> isEqIgnoreCase(getBookingStatus(b), "CONFIRMED"))
                .filter(b -> isSameMonth(getBookingDate(b), now))
                .map(this::getBookingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        formattedMonthlyRevenue = formatMoney(monthlyRevenue);

        // 8) Recent bookings (sort desc by createdAt/eventDate/bookingId)
        recentBookings = bookings.stream()
                .sorted((a, b) -> compareBookingDesc(a, b))
                .limit(10)
                .collect(Collectors.toList());

        // 9) Chart JSON
        revenueByMonthJson = buildRevenueByMonthJson(bookings);
        bookingsByStatusJson = buildStatusJson();
        cancelRateByMonthJson = buildCancelRateByMonthJson(bookings);
    }

    // ================= GETTERS used by XHTML =================
    public long getTotalUsers() { return totalUsers; }
    public long getTotalRestaurants() { return totalRestaurants; }
    public long getTotalBookings() { return totalBookings; }

    public String getFormattedMonthlyRevenue() { return formattedMonthlyRevenue; }
    public int getCancelRate() { return cancelRate; }
    public long getPendingApprovals() { return pendingApprovals; }

    public long getConfirmedCount() { return confirmedCount; }
    public long getPendingCount() { return pendingCount; }
    public long getCancelledCount() { return cancelledCount; }

    public String getRevenueByMonthJson() { return revenueByMonthJson; }
    public String getBookingsByStatusJson() { return bookingsByStatusJson; }
    public String getCancelRateByMonthJson() { return cancelRateByMonthJson; }

    public List<Bookings> getRecentBookings() { return recentBookings; }

    // ================= Helpers =================
    private static <T> List<T> safeList(List<T> list) { return list == null ? new ArrayList<>() : list; }

    private static boolean isEqIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    // bookingStatus getter (ưu tiên getBookingStatus(), fallback reflection)
    private String getBookingStatus(Bookings b) {
        String s = getStr(b, "getBookingStatus");
        if (s == null) s = getStr(b, "getStatus");
        return s;
    }

    // Lấy amount an toàn bằng reflection (đỡ bị sai tên cột: totalAmount/totalPrice/finalAmount...)
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

    // Lấy date dùng cho thống kê theo tháng (createdAt -> eventDate -> updatedAt...)
    private Date getBookingDate(Bookings b) {
        Object d =
                getObj(b, "getCreatedAt",
                getObj(b, "getEventDate",
                getObj(b, "getUpdatedAt", null)));

        if (d instanceof Date) return (Date) d;
        return null;
    }

    private static boolean isSameMonth(Date d, LocalDate now) {
        if (d == null) return false;
        LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return ld.getYear() == now.getYear() && ld.getMonthValue() == now.getMonthValue();
    }

    private String formatMoney(BigDecimal money) {
        try {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
            return nf.format(money);
        } catch (Exception e) {
            return money == null ? "0" : money.toPlainString();
        }
    }

    private String buildStatusJson() {
        // labels/values order should match your chart
        return "{\"labels\":[\"CONFIRMED\",\"PENDING\",\"CANCELLED\"],\"values\":["
                + confirmedCount + "," + pendingCount + "," + cancelledCount + "]}";
    }

    private String buildRevenueByMonthJson(List<Bookings> bookings) {
        // This year, months 1..12
        int year = LocalDate.now().getYear();
        BigDecimal[] sum = new BigDecimal[12];
        Arrays.setAll(sum, i -> BigDecimal.ZERO);

        for (Bookings b : bookings) {
            if (!isEqIgnoreCase(getBookingStatus(b), "CONFIRMED")) continue;
            Date d = getBookingDate(b);
            if (d == null) continue;

            LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (ld.getYear() != year) continue;

            int idx = ld.getMonthValue() - 1;
            sum[idx] = sum[idx].add(getBookingAmount(b));
        }

        String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return toJson(labels, sum);
    }

    private String buildCancelRateByMonthJson(List<Bookings> bookings) {
        int year = LocalDate.now().getYear();
        long[] total = new long[12];
        long[] cancel = new long[12];

        for (Bookings b : bookings) {
            Date d = getBookingDate(b);
            if (d == null) continue;
            LocalDate ld = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            if (ld.getYear() != year) continue;

            int idx = ld.getMonthValue() - 1;
            total[idx]++;

            if (isEqIgnoreCase(getBookingStatus(b), "CANCELLED")) cancel[idx]++;
        }

        double[] rate = new double[12];
        for (int i = 0; i < 12; i++) {
            rate[i] = total[i] == 0 ? 0 : (cancel[i] * 100.0 / total[i]);
        }

        String[] labels = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        return toJson(labels, rate);
    }

    private int compareBookingDesc(Bookings a, Bookings b) {
        Date da = getBookingDate(a);
        Date db = getBookingDate(b);
        if (da != null && db != null) return db.compareTo(da);

        // fallback bookingId desc
        Long ia = getLong(a, "getBookingId");
        Long ib = getLong(b, "getBookingId");
        if (ia != null && ib != null) return Long.compare(ib, ia);
        return 0;
    }

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
            sb.append(values[i] == null ? "0" : values[i].toPlainString());
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
            sb.append(String.format(Locale.US, "%.2f", values[i]));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String getStr(Object obj, String method) {
        Object o = getObj(obj, method, null);
        return o == null ? null : String.valueOf(o);
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
