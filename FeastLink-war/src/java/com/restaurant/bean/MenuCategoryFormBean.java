package com.restaurant.bean;

import com.mypack.entity.MenuCategories;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.Map;

@Named("menuCategoryFormBean")
@ViewScoped
public class MenuCategoryFormBean implements Serializable {

    @EJB
    private MenuCategoriesFacadeLocal menuCategoriesFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private MenuCategories category;
    private boolean editMode;

    public MenuCategoryFormBean() {
    }

    // ===== Helper: lấy restaurant theo user login =====
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;

        Map<String, Object> session = ctx.getExternalContext().getSessionMap();
        Users currentUser = (Users) session.get("currentUser");
        if (currentUser == null || currentUser.getEmail() == null) {
            return null;
        }
        String email = currentUser.getEmail();

        // Tạm: map Users.Email == Restaurants.Email
        for (Restaurants r : restaurantsFacade.findAll()) {
            if (r.getEmail() != null &&
                r.getEmail().equalsIgnoreCase(email)) {
                return r;
            }
        }
        return null;
    }

    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String idStr = params.get("categoryId");

        if (idStr != null && !idStr.isBlank()) {
            // EDIT MODE
            Long id = Long.valueOf(idStr);
            category = menuCategoriesFacade.find(id);
            editMode = true;
        } else {
            // NEW MODE
            editMode = false;
            category = new MenuCategories();
            category.setIsActive(true);

            // gán restaurant theo user đăng nhập
            Restaurants r = resolveCurrentRestaurant();
            category.setRestaurantId(r);
        }
    }

    public String save() {
        if (editMode) {
            menuCategoriesFacade.edit(category);
        } else {
            menuCategoriesFacade.create(category);
        }
        return "/Restaurant/menu-packages?faces-redirect=true&tab=categories";
    }

    public String cancel() {
        return "/Restaurant/menu-packages?faces-redirect=true&tab=categories";
    }

    // ===== getters / setters =====
    public MenuCategories getCategory() {
        return category;
    }

    public void setCategory(MenuCategories category) {
        this.category = category;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }
}
