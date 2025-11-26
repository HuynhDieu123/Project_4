package com.restaurant.bean;

import com.mypack.entity.MenuCategories;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuCombosFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
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

    private List<MenuItems> items;
    private List<MenuCategories> categories;
    private List<MenuCombos> packages;   // danh sách combo thực tế

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

    // ===== LOAD ITEMS (bỏ IsDeleted = true) =====
    private void refreshItems() {
        List<MenuItems> all = menuItemsFacade.findAll();
        items = new ArrayList<>();
        if (all != null) {
            for (MenuItems m : all) {
                Boolean deleted = m.getIsDeleted();
                if (deleted == null || !deleted) {
                    items.add(m);
                }
            }
        }
    }

    // ===== LOAD CATEGORIES (IsActive = true) =====
    private void refreshCategories() {
        List<MenuCategories> all = menuCategoriesFacade.findAll();
        categories = new ArrayList<>();
        if (all != null) {
            for (MenuCategories c : all) {
                Boolean active = c.getIsActive();
                if (active == null || active) {
                    categories.add(c);
                }
            }
        }
    }

    // ===== LOAD PACKAGES/COMBOS (IsDeleted = false) =====
    private void refreshPackages() {
        List<MenuCombos> all = menuCombosFacade.findAll();
        packages = new ArrayList<>();
        if (all != null) {
            for (MenuCombos combo : all) {
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
