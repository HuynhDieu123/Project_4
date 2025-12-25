package com.mypack.admin;

import com.mypack.entity.FavoriteRestaurants;
import com.mypack.entity.FavoriteRestaurantsPK;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.FavoriteRestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Named("adminFavoritesStatsBean")
@ViewScoped
public class AdminFavoritesStatsBean implements Serializable {

    @EJB
    private FavoriteRestaurantsFacadeLocal favoriteRestaurantsFacade;

    // ====== DATA ======
    private List<RestaurantStat> computedStats = new ArrayList<>();
    private int totalFavoriteRecords;        // tổng số dòng FavoriteRestaurants
    private int uniqueCustomerCount;         // số customer khác nhau đã thả tim
    private int uniqueRestaurantCount;       // số restaurant được thả tim

    // ====== FILTERS / UI STATE ======
    private String keyword = "";             // search theo tên / id
    private String cityKeyword = "";         // search theo city
    private int minFavorites = 0;            // lọc count >=
    private String sortBy = "FAV_DESC";      // FAV_DESC | FAV_ASC | RATING_DESC | NAME_ASC | NEWEST
    private int top = 50;                    // giới hạn top N
    private Date lastRecomputedAt;

    @PostConstruct
    public void init() {
        recompute();
    }

    // ================== ACTIONS ==================
    public String recompute() {
        List<FavoriteRestaurants> all = favoriteRestaurantsFacade.findAll();
        if (all == null) all = Collections.emptyList();

        this.totalFavoriteRecords = all.size();
        Set<Long> uniqCustomers = new HashSet<>();

        // restaurantId -> builder
        Map<Long, StatBuilder> map = new HashMap<>();

        for (FavoriteRestaurants fav : all) {
            if (fav == null) continue;

            FavoriteRestaurantsPK pk = fav.getFavoriteRestaurantsPK();
            Restaurants r = fav.getRestaurants();
            if (pk == null || r == null) continue;

            long restaurantId;
            long customerId;
            try {
                // PK
                customerId = pk.getCustomerId();
                // Nếu PK có getRestaurantId thì ưu tiên; nếu không thì lấy từ Restaurants
                // (đa số PK của bảng trung gian đều có restaurantId)
                try {
                    restaurantId = pk.getRestaurantId();
                } catch (Exception ignore) {
                    restaurantId = r.getRestaurantId();
                }
            } catch (Exception e) {
                continue;
            }

            uniqCustomers.add(customerId);

            StatBuilder b = map.computeIfAbsent(restaurantId, k -> new StatBuilder(r));
            b.favCount++;
            b.uniqueCustomers.add(customerId);

            // giữ reference nhà hàng “đầy đủ” nhất
            if (b.restaurant == null && r != null) b.restaurant = r;
        }

        List<RestaurantStat> stats = new ArrayList<>();
        for (StatBuilder b : map.values()) {
            Restaurants restaurant = b.restaurant;
            double avgRating = calculateAverageRating(restaurant);
            int approvedReviews = approvedReviewCount(restaurant);

            RestaurantStat row = new RestaurantStat();
            row.setRestaurant(restaurant);
            row.setFavoriteCount(b.favCount);
            row.setUniqueCustomers(b.uniqueCustomers.size());
            row.setAverageRating(avgRating);
            row.setApprovedReviewCount(approvedReviews);
            row.setCreatedAt(restaurant != null ? restaurant.getCreatedAt() : null);

            stats.add(row);
        }

        this.uniqueCustomerCount = uniqCustomers.size();
        this.uniqueRestaurantCount = stats.size();
        this.computedStats = stats;
        this.lastRecomputedAt = new Date();
        return null;
    }

    public String clearFilters() {
        this.keyword = "";
        this.cityKeyword = "";
        this.minFavorites = 0;
        this.sortBy = "FAV_DESC";
        this.top = 50;
        return null;
    }

    public String applyFilters() {
        // chỉ cần submit form để JSF cập nhật state -> getter displayedStats sẽ áp dụng
        return null;
    }

    // ================== DERIVED LIST ==================
    public List<RestaurantStat> getDisplayedStats() {
        if (computedStats == null || computedStats.isEmpty()) return Collections.emptyList();

        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        String ckw = cityKeyword == null ? "" : cityKeyword.trim().toLowerCase();
        int min = Math.max(0, minFavorites);
        int limit = top <= 0 ? 50 : Math.min(top, 500);

        List<RestaurantStat> list = computedStats.stream()
                .filter(row -> row != null && row.getRestaurant() != null)
                .filter(row -> {
                    if (min > 0 && row.getFavoriteCount() < min) return false;

                    Restaurants r = row.getRestaurant();

                    if (!kw.isEmpty()) {
                        String name = safeLower(r.getName());
                        String id = String.valueOf(r.getRestaurantId());
                        if (!name.contains(kw) && !id.contains(kw)) return false;
                    }

                    if (!ckw.isEmpty()) {
                        String city = "";
                        try {
                            if (r.getAreaId() != null && r.getAreaId().getCityId() != null) {
                                city = safeLower(r.getAreaId().getCityId().getName());
                            }
                        } catch (Exception ignore) {}
                        if (!city.contains(ckw)) return false;
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // sort
        Comparator<RestaurantStat> cmp;
        switch (sortBy == null ? "FAV_DESC" : sortBy) {
            case "FAV_ASC":
                cmp = Comparator.comparingInt(RestaurantStat::getFavoriteCount);
                break;
            case "RATING_DESC":
                cmp = Comparator.comparingDouble(RestaurantStat::getAverageRating).reversed()
                        .thenComparingInt(RestaurantStat::getFavoriteCount).reversed();
                break;
            case "NAME_ASC":
                cmp = Comparator.comparing(a -> safeLower(a.getRestaurant().getName()));
                break;
            case "NEWEST":
                cmp = Comparator.comparing(RestaurantStat::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
                break;
            case "FAV_DESC":
            default:
                cmp = Comparator.comparingInt(RestaurantStat::getFavoriteCount).reversed()
                        .thenComparing(a -> safeLower(a.getRestaurant().getName()));
                break;
        }
        list.sort(cmp);

        if (list.size() > limit) {
            return new ArrayList<>(list.subList(0, limit));
        }
        return list;
    }

    // ================== HELPERS (rating giống customer) ==================
    private double calculateAverageRating(Restaurants restaurant) {
        if (restaurant == null) return 0d;

        Collection<RestaurantReviews> reviews = restaurant.getRestaurantReviewsCollection();
        if (reviews == null || reviews.isEmpty()) return 0d;

        int total = 0;
        int count = 0;

        for (RestaurantReviews review : reviews) {
            if (review == null) continue;
            try {
                if (review.getIsDeleted()) continue;
                if (!review.getIsApproved()) continue;
            } catch (Exception ignore) {
                // nếu entity không có các field này, bỏ qua filter
            }
            total += review.getRating();
            count++;
        }

        return count == 0 ? 0d : (double) total / count;
    }

    public String averageRatingFormatted(RestaurantStat row) {
        if (row == null) return "-";
        double avg = row.getAverageRating();
        if (avg <= 0d) return "-";
        return String.format(java.util.Locale.US, "%.1f", avg);
    }

    private int approvedReviewCount(Restaurants restaurant) {
        if (restaurant == null) return 0;
        Collection<RestaurantReviews> reviews = restaurant.getRestaurantReviewsCollection();
        if (reviews == null || reviews.isEmpty()) return 0;

        int count = 0;
        for (RestaurantReviews review : reviews) {
            if (review == null) continue;
            try {
                if (review.getIsDeleted()) continue;
                if (!review.getIsApproved()) continue;
            } catch (Exception ignore) {}
            count++;
        }
        return count;
    }

    public String initials(String name) {
        if (name == null || name.trim().isEmpty()) return "FL";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        return sb.toString();
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    // ================== GETTERS/SETTERS ==================
    public int getTotalFavoriteRecords() {
        return totalFavoriteRecords;
    }

    public int getUniqueCustomerCount() {
        return uniqueCustomerCount;
    }

    public int getUniqueRestaurantCount() {
        return uniqueRestaurantCount;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCityKeyword() {
        return cityKeyword;
    }

    public void setCityKeyword(String cityKeyword) {
        this.cityKeyword = cityKeyword;
    }

    public int getMinFavorites() {
        return minFavorites;
    }

    public void setMinFavorites(int minFavorites) {
        this.minFavorites = minFavorites;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public Date getLastRecomputedAt() {
        return lastRecomputedAt;
    }

    // ================== INNER MODELS ==================
    private static class StatBuilder {
        private Restaurants restaurant;
        private int favCount = 0;
        private final Set<Long> uniqueCustomers = new HashSet<>();
        private StatBuilder(Restaurants r) { this.restaurant = r; }
    }

    public static class RestaurantStat implements Serializable {
        private Restaurants restaurant;
        private int favoriteCount;
        private int uniqueCustomers;
        private double averageRating;
        private int approvedReviewCount;
        private Date createdAt;

        public Restaurants getRestaurant() { return restaurant; }
        public void setRestaurant(Restaurants restaurant) { this.restaurant = restaurant; }

        public int getFavoriteCount() { return favoriteCount; }
        public void setFavoriteCount(int favoriteCount) { this.favoriteCount = favoriteCount; }

        public int getUniqueCustomers() { return uniqueCustomers; }
        public void setUniqueCustomers(int uniqueCustomers) { this.uniqueCustomers = uniqueCustomers; }

        public double getAverageRating() { return averageRating; }
        public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

        public int getApprovedReviewCount() { return approvedReviewCount; }
        public void setApprovedReviewCount(int approvedReviewCount) { this.approvedReviewCount = approvedReviewCount; }

        public Date getCreatedAt() { return createdAt; }
        public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    }
}

