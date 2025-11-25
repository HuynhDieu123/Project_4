package com.restaurant.bean;

import com.mypack.entity.MenuItems;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;

@Named("menuItemFormBean")
@ViewScoped
public class MenuItemFormBean implements Serializable {

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;   // <--- THÊM

    private MenuItems newItem;

    public MenuItemFormBean() {
    }

    @PostConstruct
    public void init() {
        newItem = new MenuItems();
        newItem.setIsVegetarian(false);
        newItem.setStatus("ACTIVE");
        newItem.setIsDeleted(false);

        // TẠM THỜI: gán nhà hàng cố định để test (ví dụ RestaurantId = 1)
        Restaurants r = restaurantsFacade.find(1L); // nhớ sửa ID cho đúng DB của bạn
        newItem.setRestaurantId(r);

        // SAU NÀY: lấy từ account đang đăng nhập thay vì hardcode
        // Restaurants currentRestaurant = restaurantSessionBean.getCurrentRestaurant();
        // newItem.setRestaurantId(currentRestaurant);
    }

    public MenuItems getNewItem() {
        return newItem;
    }

    public void setNewItem(MenuItems newItem) {
        this.newItem = newItem;
    }

    public String save() {
        menuItemsFacade.create(newItem);
        return "/Restaurant/menu-packages?faces-redirect=true";
    }

    public String cancel() {
        return "/Restaurant/menu-packages?faces-redirect=true";
    }
}
