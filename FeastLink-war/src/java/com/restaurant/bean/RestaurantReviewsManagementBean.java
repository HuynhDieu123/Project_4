package com.restaurant.bean;

import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.RestaurantReviewsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Named("restaurantReviewsManagementBean")
@ViewScoped
public class RestaurantReviewsManagementBean implements Serializable {

    @EJB
    private RestaurantReviewsFacadeLocal reviewsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private Users currentUser;
    private Restaurants currentRestaurant;

    // ===== Stats (follow date range) =====
    private double avgRating = 0;
    private long totalReviews = 0;

    private int count5, count4, count3, count2, count1;
    private double pct5, pct4, pct3, pct2, pct1;

    // ===== Filters =====
    private String sortKey = "recent";   // recent / oldest / highest / lowest
    private Integer ratingFilter = null; // null = all
    private String keyword = "";

    // Date filter: from/to (yyyy-MM-dd)
    private String dateFrom; // inclusive
    private String dateTo;   // inclusive

    // ===== Pagination =====
    private List<RestaurantReviews> reviews = new ArrayList<>();
    private int pageSize = 10;

    private int currentPage = 1;
    private int totalPages = 1;

    private long totalCount = 0;

    private final List<Integer> starList = Arrays.asList(1, 2, 3, 4, 5);
    private final List<Integer> ratingDesc = Arrays.asList(5, 4, 3, 2, 1);

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        ExternalContext ec = ctx.getExternalContext();
        Object obj = ec.getSessionMap().get("currentUser");
        if (obj instanceof Users) {
            currentUser = (Users) obj;
        }

        // Block non-manager
        if (currentUser == null || currentUser.getRole() == null
                || !"MANAGER".equalsIgnoreCase(currentUser.getRole())) {
            redirect("/login.xhtml");
            return;
        }

        // Resolve restaurant by manager email (same approach as RestaurantDashboardBean)
        currentRestaurant = resolveCurrentRestaurantByEmail(currentUser);
        if (currentRestaurant == null) {
            addMsg(FacesMessage.SEVERITY_ERROR,
                    "No restaurant found",
                    "Manager account is not linked to any restaurant (by email).");
            return;
        }

        // Default: show last 7 days (nice for manager)
        presetLast7Days();

        reloadAll();
    }

    private Restaurants resolveCurrentRestaurantByEmail(Users u) {
        if (u == null || u.getEmail() == null) {
            return null;
        }
        String emailLogin = u.getEmail();

        List<Restaurants> all = restaurantsFacade.findAll();
        if (all == null) {
            return null;
        }

        for (Restaurants r : all) {
            if (r.getEmail() != null && r.getEmail().equalsIgnoreCase(emailLogin)) {
                return r;
            }
        }
        return null;
    }

    // =========================
    // LOADERS
    // =========================
    public void reloadAll() {
        loadStats();
        loadReviews(true);
    }

    private void loadStats() {
        if (currentRestaurant == null || currentRestaurant.getRestaurantId() == null) {
            avgRating = 0;
            totalReviews = 0;
            count5 = count4 = count3 = count2 = count1 = 0;
            pct5 = pct4 = pct3 = pct2 = pct1 = 0;
            return;
        }

        Long rid = currentRestaurant.getRestaurantId();
        Date[] range = normalizeRange(dateFrom, dateTo);
        Date fromDt = range[0];
        Date toDt = range[1];

        // total reviews in date range (ignore keyword/rating for stats)
        totalReviews = reviewsFacade.countForRestaurant(rid, null, null, null, fromDt, toDt);

        long weightedSum
                = (long) count1 * 1
                + (long) count2 * 2
                + (long) count3 * 3
                + (long) count4 * 4
                + (long) count5 * 5;

        avgRating = (totalReviews > 0) ? (weightedSum * 1.0 / totalReviews) : 0.0;

        count5 = count4 = count3 = count2 = count1 = 0;
        List<Object[]> rows = reviewsFacade.ratingBreakdownForRestaurant(rid, fromDt, toDt);
        if (rows != null) {
            for (Object[] r : rows) {
                if (r == null || r.length < 2) {
                    continue;
                }
                int rating = ((Number) r[0]).intValue();
                int cnt = ((Number) r[1]).intValue();
                switch (rating) {
                    case 5:
                        count5 = cnt;
                        break;
                    case 4:
                        count4 = cnt;
                        break;
                    case 3:
                        count3 = cnt;
                        break;
                    case 2:
                        count2 = cnt;
                        break;
                    case 1:
                        count1 = cnt;
                        break;
                    default:
                        break;
                }
            }
        }

        pct5 = percent(count5, totalReviews);
        pct4 = percent(count4, totalReviews);
        pct3 = percent(count3, totalReviews);
        pct2 = percent(count2, totalReviews);
        pct1 = percent(count1, totalReviews);
    }

    private void loadReviews(boolean reset) {
        if (currentRestaurant == null || currentRestaurant.getRestaurantId() == null) {
            return;
        }

        if (reset) {
            currentPage = 1;
        }

        Date[] range = normalizeRange(dateFrom, dateTo);
        Date fromDt = range[0];
        Date toDt = range[1];
        Long rid = currentRestaurant.getRestaurantId();

        totalCount = reviewsFacade.countForRestaurant(rid, null, ratingFilter, keyword, fromDt, toDt);

        totalPages = (int) Math.ceil(totalCount / (double) pageSize);
        if (totalPages <= 0) {
            totalPages = 1;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int offset = (currentPage - 1) * pageSize;

        List<RestaurantReviews> page = reviewsFacade.findForRestaurant(
                rid, null, ratingFilter, keyword,
                fromDt, toDt,
                offset, pageSize, sortKey
        );

        reviews = (page == null) ? new ArrayList<>() : page;
    }

    // =========================
    // ACTIONS
    // =========================
    public void applyFilter() {
        loadStats();      // stats follow date
        loadReviews(true);
    }

    public void resetFilter() {
        sortKey = "recent";
        ratingFilter = null;
        keyword = "";
        dateFrom = null;
        dateTo = null;

        loadStats();
        loadReviews(true);
    }

    public void loadMore() {
        loadReviews(false);
    }

    // Quick presets
    public void presetToday() {
        LocalDate d = LocalDate.now();
        dateFrom = d.toString();
        dateTo = d.toString();
        applyFilter();
    }

    public void presetLast7Days() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        dateFrom = from.toString();
        dateTo = today.toString();
        applyFilter();
    }

    public void presetThisMonth() {
        LocalDate today = LocalDate.now();
        LocalDate from = today.withDayOfMonth(1);
        dateFrom = from.toString();
        dateTo = today.toString();
        applyFilter();
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadReviews(false);
        }
    }

    public void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadReviews(false);
        }
    }

    public void goPage() {
        // currentPage đã được set từ UI (f:setPropertyActionListener)
        loadReviews(false);
    }

    public List<Integer> getPageNumbers() {
        int window = 5; // hiển thị tối đa 5 nút trang
        int half = window / 2;

        int start = Math.max(1, currentPage - half);
        int end = Math.min(totalPages, start + window - 1);

        // chỉnh lại start nếu end chạm trần
        start = Math.max(1, end - window + 1);

        List<Integer> nums = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            nums.add(i);
        }
        return nums;
    }

// getters/setters
    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalCount() {
        return totalCount;
    }

    // =========================
    // HELPERS
    // =========================
    public String initials(String name) {
        String s = (name == null) ? "" : name.trim();
        if (s.isEmpty()) {
            return "?";
        }
        String[] parts = s.split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase();
        String last = (parts.length >= 2) ? parts[parts.length - 1].substring(0, 1).toUpperCase() : "";
        return first + last;
    }

    private double percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (part * 100.0) / total;
    }

    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(sev, sum, detail));
        }
    }

    private void redirect(String to) {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx == null) {
                return;
            }
            String cp = ctx.getExternalContext().getRequestContextPath();
            ctx.getExternalContext().redirect(cp + "/faces" + to);
            ctx.responseComplete();
        } catch (IOException ignored) {
        }
    }

    // Normalize dateFrom/dateTo:
    // - accept null
    // - swap if from > to
    // - inclusive end-of-day for to
    private Date[] normalizeRange(String from, String to) {
        Date fromDt = parseDateStart(from);
        Date toDt = parseDateEnd(to);

        if (fromDt != null && toDt != null && fromDt.after(toDt)) {
            // swap
            Date tmp = fromDt;
            fromDt = parseDateStart(to);
            toDt = parseDateEnd(from);
        }
        return new Date[]{fromDt, toDt};
    }

    private Date parseDateStart(String yyyyMMdd) {
        try {
            if (yyyyMMdd == null || yyyyMMdd.trim().isEmpty()) {
                return null;
            }
            LocalDate d = LocalDate.parse(yyyyMMdd.trim());
            return Timestamp.valueOf(d.atStartOfDay());
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseDateEnd(String yyyyMMdd) {
        try {
            if (yyyyMMdd == null || yyyyMMdd.trim().isEmpty()) {
                return null;
            }
            LocalDate d = LocalDate.parse(yyyyMMdd.trim());
            return Timestamp.valueOf(d.atTime(23, 59, 59));
        } catch (Exception e) {
            return null;
        }
    }

    // =========================
    // GETTERS / SETTERS
    // =========================
    public Restaurants getCurrentRestaurant() {
        return currentRestaurant;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public long getTotalReviews() {
        return totalReviews;
    }

    public int getCount5() {
        return count5;
    }

    public int getCount4() {
        return count4;
    }

    public int getCount3() {
        return count3;
    }

    public int getCount2() {
        return count2;
    }

    public int getCount1() {
        return count1;
    }

    public double getPct5() {
        return pct5;
    }

    public double getPct4() {
        return pct4;
    }

    public double getPct3() {
        return pct3;
    }

    public double getPct2() {
        return pct2;
    }

    public double getPct1() {
        return pct1;
    }

    public String getSortKey() {
        return sortKey;
    }

    public void setSortKey(String sortKey) {
        this.sortKey = sortKey;
    }

    public Integer getRatingFilter() {
        return ratingFilter;
    }

    public void setRatingFilter(Integer ratingFilter) {
        this.ratingFilter = ratingFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public List<RestaurantReviews> getReviews() {
        return reviews;
    }


    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<Integer> getStarList() {
        return starList;
    }

    public List<Integer> getRatingDesc() {
        return ratingDesc;
    }
}
