package com.restaurant.bean;

import com.mypack.entity.Cuisines;
import com.mypack.entity.MenuCategories;
import com.mypack.entity.MenuItems;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.CuisinesFacadeLocal;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Named("menuItemFormBean")
@ViewScoped
public class MenuItemFormBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private CuisinesFacadeLocal cuisinesFacade;

    @EJB
    private MenuCategoriesFacadeLocal menuCategoriesFacade;

    private MenuItems newItem;
    private boolean editMode;

    // Cuisines.CuisineId = INT  -> Integer
    private Integer cuisineId;
    // MenuCategories.CategoryId = BIGINT -> Long
    private Long categoryId;

    // list cho dropdown
    private List<Cuisines> cuisines;
    private List<MenuCategories> categories;

    public MenuItemFormBean() {
    }

    @PostConstruct
    public void init() {
        // load list cho dropdown
        cuisines   = cuisinesFacade.findAll();
        categories = menuCategoriesFacade.findAll();

        // đọc itemId từ query string (?itemId=...)
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        String idStr = params.get("itemId");

        if (idStr != null && !idStr.isBlank()) {
            // ---- EDIT MODE ----
            Long id = Long.valueOf(idStr);
            newItem = menuItemsFacade.find(id);
            editMode = true;

            // set sẵn value cho dropdown cuisine & category
            if (newItem != null) {
                if (newItem.getCuisineId() != null) {
                    cuisineId = newItem.getCuisineId().getCuisineId();    // Integer
                }
                if (newItem.getCategoryId() != null) {
                    categoryId = newItem.getCategoryId().getCategoryId(); // Long
                }
            }
        } else {
            // ---- NEW MODE ----
            editMode = false;
            newItem = new MenuItems();
            newItem.setIsVegetarian(false);
            newItem.setStatus("ACTIVE");
            newItem.setIsDeleted(false);

            // TODO: sau này lấy từ tài khoản đăng nhập
            Restaurants r = restaurantsFacade.find(1L);
            newItem.setRestaurantId(r);
        }
    }

    public String save() {
        // gán Cuisine từ id
        if (cuisineId != null) {
            Cuisines c = cuisinesFacade.find(cuisineId);
            newItem.setCuisineId(c);
        } else {
            newItem.setCuisineId(null);
        }

        // gán Category từ id
        if (categoryId != null) {
            MenuCategories cat = menuCategoriesFacade.find(categoryId);
            newItem.setCategoryId(cat);
        } else {
            newItem.setCategoryId(null);
        }

        if (editMode) {
            menuItemsFacade.edit(newItem);
        } else {
            menuItemsFacade.create(newItem);
        }

        return "/Restaurant/menu-packages?faces-redirect=true";
    }

    public String cancel() {
        return "/Restaurant/menu-packages?faces-redirect=true";
    }

    // ========== GET / SET ==========

    public MenuItems getNewItem() {
        return newItem;
    }

    public void setNewItem(MenuItems newItem) {
        this.newItem = newItem;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public Integer getCuisineId() {
        return cuisineId;
    }

    public void setCuisineId(Integer cuisineId) {
        this.cuisineId = cuisineId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Cuisines> getCuisines() {
        return cuisines;
    }

    public void setCuisines(List<Cuisines> cuisines) {
        this.cuisines = cuisines;
    }

    public List<MenuCategories> getCategories() {
        return categories;
    }

    public void setCategories(List<MenuCategories> categories) {
        this.categories = categories;
    }
}
