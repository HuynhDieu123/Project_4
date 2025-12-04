package com.mypack.bean;

import com.mypack.entity.FavoriteRestaurants;
import com.mypack.entity.FavoriteRestaurantsPK;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.FavoriteRestaurantsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

@Named("restaurantFavoritesBean")
@ViewScoped
public class RestaurantFavoritesBean implements Serializable {

    @EJB
    private FavoriteRestaurantsFacadeLocal favoriteRestaurantsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private Users currentUser;
    private Long selectedRestaurantId;   // nhận từ inputHidden

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        Map<String, Object> sessionMap = ctx.getExternalContext().getSessionMap();

        Object u = sessionMap.get("loggedInUser");
        if (u == null) u = sessionMap.get("currentUser");
        if (u == null) u = sessionMap.get("user");

        if (u instanceof Users) {
            currentUser = (Users) u;
        }
    }

    public Long getSelectedRestaurantId() {
        return selectedRestaurantId;
    }

    public void setSelectedRestaurantId(Long selectedRestaurantId) {
        this.selectedRestaurantId = selectedRestaurantId;
    }

    public Users getCurrentUser() {
        return currentUser;
    }

    /**
     * Được gọi khi click trái tim:
     * - Nếu chưa login: chuyển tới Customer/login.xhtml
     * - Nếu đã có favorite rồi: chỉ báo info, ở lại trang
     * - Nếu thêm mới thành công: báo thành công + redirect sang favorites.xhtml
     */
    public String addFavoriteById() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (selectedRestaurantId == null) {
            return null;
        }

        // CHƯA ĐĂNG NHẬP -> ĐI THẲNG TỚI login.xhtml
        if (currentUser == null) {
            return "/login";
        }

        try {
            long customerId = currentUser.getUserId();
            long restaurantId = selectedRestaurantId;

            FavoriteRestaurantsPK pk = new FavoriteRestaurantsPK(customerId, restaurantId);
            FavoriteRestaurants existed = favoriteRestaurantsFacade.find(pk);

            if (existed != null) {
                // Đã tồn tại rồi -> chỉ báo, không redirect
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "This restaurant is already on my favorites list.",
                        null));
                return null;
            }

            Restaurants restaurant = restaurantsFacade.find(restaurantId);
            if (restaurant == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Restaurant not found.",
                        null));
                return null;
            }

            // Tạo bản ghi mới
            FavoriteRestaurants fav = new FavoriteRestaurants(pk, new Date());
            fav.setUsers(currentUser);
            fav.setRestaurants(restaurant);

            favoriteRestaurantsFacade.create(fav);

            // Thông báo thành công + giữ message sau redirect
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Added to favorites successfully.",
                    null));
            ctx.getExternalContext().getFlash().setKeepMessages(true);

            // Redirect sang trang favorites.xhtml
            return "/Customer/favorites";

        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "There was an error saving the favorite restaurant.",
                    ex.getMessage()));
        }

        return null;    // lỗi thì ở lại trang
    }
}
