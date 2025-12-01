package com.customer.bean;

import com.mypack.entity.FavoriteRestaurants;
import com.mypack.entity.FavoriteRestaurantsPK;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.FavoriteRestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Named("customerFavoritesBean")
@ViewScoped
public class CustomerFavoritesBean implements Serializable {

    @EJB
    private FavoriteRestaurantsFacadeLocal favoriteRestaurantsFacade;

    private Users currentUser;
    private List<Restaurants> favoriteRestaurants = new ArrayList<>();

    // ALL | RECENT | TOP_RATED
    private String activeFilter = "ALL";

    // ================== INIT ==================

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();

        Object userObj = sessionMap.get("loggedInUser");
        if (userObj == null) {
            userObj = sessionMap.get("currentUser");
        }
        if (userObj == null) {
            userObj = sessionMap.get("user");
        }

        if (userObj instanceof Users) {
            currentUser = (Users) userObj;
        }

        if (currentUser == null) {
            favoriteRestaurants = Collections.emptyList();
            return;
        }

        long currentUserId = currentUser.getUserId(); // Long -> long (auto-unbox)

        List<FavoriteRestaurants> allFavorites = favoriteRestaurantsFacade.findAll();
        List<Restaurants> result = new ArrayList<>();

        if (allFavorites != null) {
            for (FavoriteRestaurants fav : allFavorites) {
                FavoriteRestaurantsPK pk = fav.getFavoriteRestaurantsPK();
                if (pk != null
                        && pk.getCustomerId() == currentUserId
                        && fav.getRestaurants() != null) {

                    result.add(fav.getRestaurants());
                }
            }
        }

        this.favoriteRestaurants = result;
    }

    // ================== GETTERS ==================

    public Users getCurrentUser() {
        return currentUser;
    }

    public List<Restaurants> getFavoriteRestaurants() {
        return favoriteRestaurants;
    }

    public String getActiveFilter() {
        return activeFilter;
    }

    /**
     * Danh sách hiển thị sau khi áp dụng filter (ALL / RECENT / TOP_RATED)
     */
    public List<Restaurants> getDisplayedFavorites() {
        if (favoriteRestaurants == null) {
            return Collections.emptyList();
        }

        List<Restaurants> list = new ArrayList<>(favoriteRestaurants);

        switch (activeFilter) {
            case "RECENT":
                // Mới tạo gần đây (dựa vào Restaurants.createdAt)
                list.sort((a, b) -> {
                    Date da = a.getCreatedAt();
                    Date db = b.getCreatedAt();
                    if (da == null && db == null) return 0;
                    if (da == null) return 1;
                    if (db == null) return -1;
                    return db.compareTo(da); // desc
                });
                break;

            case "TOP_RATED":
                // Đánh giá cao (rating trung bình giảm dần)
                list.sort(Comparator.comparingDouble(this::calculateAverageRating).reversed());
                break;

            case "ALL":
            default:
                // Sắp xếp theo tên cho dễ xem
                list.sort(Comparator.comparing(
                        r -> r.getName() != null ? r.getName().toLowerCase() : "",
                        Comparator.naturalOrder()));
                break;
        }

        return list;
    }

    // ================== ACTION FILTER ==================

    public String filterAll() {
        this.activeFilter = "ALL";
        return null;   // ở lại trang hiện tại
    }

    public String filterRecent() {
        this.activeFilter = "RECENT";
        return null;
    }

    public String filterTopRated() {
        this.activeFilter = "TOP_RATED";
        return null;
    }

    // ================== REMOVE FAVORITE ==================

    /**
     * Bấm trái tim để xóa 1 nhà hàng khỏi danh sách yêu thích.
     */
    public String removeFavorite(Restaurants restaurant) {
        if (restaurant == null || currentUser == null) {
            return null;
        }

        try {
            // FavoriteRestaurantsPK(long customerId, long restaurantId)
            FavoriteRestaurantsPK pk = new FavoriteRestaurantsPK(
                    currentUser.getUserId(),            // Long -> long (CustomerId)
                    restaurant.getRestaurantId()        // Long -> long (RestaurantId)
            );

            FavoriteRestaurants fav = favoriteRestaurantsFacade.find(pk);
            if (fav != null) {
                favoriteRestaurantsFacade.remove(fav);  // xóa trong DB
            }

            // Xóa khỏi list trên UI
            favoriteRestaurants.remove(restaurant);

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    // ================== UTILS ==================

    // Avatar chữ cái (GP, RB, SC...)
    public String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "FL";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toUpperCase(parts[0].charAt(0)));
        if (parts.length > 1) {
            sb.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
        }
        return sb.toString();
    }

    // Tính rating trung bình 1 nhà hàng (chỉ review đã duyệt & chưa xóa)
    private double calculateAverageRating(Restaurants restaurant) {
        if (restaurant == null) return 0d;

        Collection<RestaurantReviews> reviews = restaurant.getRestaurantReviewsCollection();
        if (reviews == null || reviews.isEmpty()) return 0d;

        int total = 0;
        int count = 0;

        for (RestaurantReviews review : reviews) {
            if (review == null) continue;
            if (review.getIsDeleted()) continue;    // boolean isDeleted
            if (!review.getIsApproved()) continue;  // boolean isApproved

            total += review.getRating();
            count++;
        }

        return count == 0 ? 0d : (double) total / count;
    }

    // Hiển thị rating dạng "4.5" hoặc "-" nếu chưa có
    public String averageRatingFormatted(Restaurants restaurant) {
        double avg = calculateAverageRating(restaurant);
        if (avg <= 0d) {
            return "-";
        }
        return String.format(java.util.Locale.US, "%.1f", avg);
    }

    // Đếm số review đã duyệt / chưa xóa
    public int approvedReviewCount(Restaurants restaurant) {
        if (restaurant == null) return 0;

        Collection<RestaurantReviews> reviews = restaurant.getRestaurantReviewsCollection();
        if (reviews == null) return 0;

        int count = 0;
        for (RestaurantReviews review : reviews) {
            if (review == null) continue;
            if (review.getIsDeleted()) continue;
            if (!review.getIsApproved()) continue;
            count++;
        }
        return count;
    }
}
