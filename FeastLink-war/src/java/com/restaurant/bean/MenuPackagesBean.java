package com.restaurant.bean;

import com.mypack.entity.MenuCategories;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuCombosFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("menuPackagesBean")
@ViewScoped
public class MenuPackagesBean implements Serializable {

    private String activeTab; // "items", "packages", "categories"

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private MenuCategoriesFacadeLocal menuCategoriesFacade;

    @EJB
    private MenuCombosFacadeLocal menuCombosFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private List<MenuItems> items;
    private List<MenuCategories> categories;
    private List<MenuCombos> packages;   // danh sách combo thực tế

    // ================== HELPER: LẤY NHÀ HÀNG HIỆN TẠI ==================
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
        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null &&
                r.getEmail().equalsIgnoreCase(email)) {
                return r;
            }
        }
        return null;
    }

    // ================== INIT ==================
    @PostConstruct
    public void init() {
        // Lấy tab từ query (?tab=items/packages/categories)
        FacesContext fc = FacesContext.getCurrentInstance();
        if (fc != null && fc.getExternalContext() != null) {
            Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
            String tabParam = params.get("tab");
            if (tabParam != null && !tabParam.isBlank()) {
                activeTab = tabParam;
            } else {
                activeTab = "items";
            }
        } else {
            activeTab = "items";
        }

        refreshItems();
        refreshCategories();
        refreshPackages();
    }

    // ===== LOAD ITEMS (theo nhà hàng hiện tại, bỏ IsDeleted = true) =====
    private void refreshItems() {
        items = new ArrayList<>();

        Restaurants currentRestaurant = resolveCurrentRestaurant();
        if (currentRestaurant == null) {
            return;   // không tìm được restaurant thì cho list rỗng
        }

        List<MenuItems> all = menuItemsFacade.findAll();
        if (all != null) {
            Long currentRestaurantId = currentRestaurant.getRestaurantId();
            for (MenuItems m : all) {
                if (m == null) continue;

                // lọc theo restaurant
                if (m.getRestaurantId() == null ||
                    m.getRestaurantId().getRestaurantId() == null ||
                    !m.getRestaurantId().getRestaurantId().equals(currentRestaurantId)) {
                    continue;
                }

                Boolean deleted = m.getIsDeleted();
                if (deleted == null || !deleted) {
                    items.add(m);
                }
            }
        }
    }

    // ===== LOAD CATEGORIES (theo nhà hàng hiện tại, IsActive = true) =====
    private void refreshCategories() {
        categories = new ArrayList<>();

        Restaurants currentRestaurant = resolveCurrentRestaurant();
        if (currentRestaurant == null) {
            return;
        }

        List<MenuCategories> all = menuCategoriesFacade.findAll();
        if (all != null) {
            Long currentRestaurantId = currentRestaurant.getRestaurantId();
            for (MenuCategories c : all) {
                if (c == null) continue;

                if (c.getRestaurantId() == null ||
                    c.getRestaurantId().getRestaurantId() == null ||
                    !c.getRestaurantId().getRestaurantId().equals(currentRestaurantId)) {
                    continue;
                }

                Boolean active = c.getIsActive();
                if (active == null || active) {
                    categories.add(c);
                }
            }
        }
    }

    // ===== LOAD PACKAGES/COMBOS (theo nhà hàng hiện tại, IsDeleted = false) =====
    private void refreshPackages() {
        packages = new ArrayList<>();

        Restaurants currentRestaurant = resolveCurrentRestaurant();
        if (currentRestaurant == null) {
            return;
        }

        List<MenuCombos> all = menuCombosFacade.findAll();
        if (all != null) {
            Long currentRestaurantId = currentRestaurant.getRestaurantId();
            for (MenuCombos combo : all) {
                if (combo == null) continue;

                if (combo.getRestaurantId() == null ||
                    combo.getRestaurantId().getRestaurantId() == null ||
                    !combo.getRestaurantId().getRestaurantId().equals(currentRestaurantId)) {
                    continue;
                }

                Boolean deleted = combo.getIsDeleted();
                if (deleted == null || !deleted) {
                    packages.add(combo);
                }
            }
        }
    }

    // ===== TAB ACTIONS =====
    public String getActiveTab() {
        return activeTab;
    }

    public void showItems() {
        activeTab = "items";
    }

    public void showPackages() {
        activeTab = "packages";
    }

    public void showCategories() {
        activeTab = "categories";
    }

    // ===== DELETE ITEM (soft delete) =====
    public String deleteItem(Long itemId) {
        if (itemId != null) {
            MenuItems item = menuItemsFacade.find(itemId);
            if (item != null) {
                item.setIsDeleted(true);
                menuItemsFacade.edit(item);
            }
        }
        refreshItems();
        return null;
    }

    // ===== DELETE CATEGORY (IsActive = false) =====
    public String deleteCategory(Long categoryId) {
        if (categoryId != null) {
            MenuCategories cat = menuCategoriesFacade.find(categoryId);
            if (cat != null) {
                cat.setIsActive(false);
                menuCategoriesFacade.edit(cat);
            }
        }
        refreshCategories();
        return null;
    }

    // ===== DELETE PACKAGE / COMBO (soft delete) =====
    public String deletePackage(Long comboId) {
        if (comboId != null) {
            MenuCombos combo = menuCombosFacade.find(comboId);
            if (combo != null) {
                combo.setIsDeleted(true);
                menuCombosFacade.edit(combo);
            }
        }
        refreshPackages();
        return null;
    }

    // ===== GETTERS =====
    public List<MenuItems> getItems() {
        return items;
    }

    public List<MenuCategories> getCategories() {
        return categories;
    }

    public List<MenuCombos> getPackages() {
        return packages;
    }
}
