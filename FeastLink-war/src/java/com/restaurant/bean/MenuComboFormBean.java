package com.restaurant.bean;

import com.mypack.entity.ComboItems;
import com.mypack.entity.ComboItemsPK;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
import com.mypack.entity.MenuCategories;
import com.mypack.entity.Cuisines;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.ComboItemsFacadeLocal;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named("menuComboFormBean")
@ViewScoped
public class MenuComboFormBean implements Serializable {

    @EJB
    private MenuCombosFacadeLocal menuCombosFacade;

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private ComboItemsFacadeLocal comboItemsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private MenuCombos combo;
    private boolean editMode;

    // tất cả món đơn của nhà hàng (chưa bị xóa)
    private List<MenuItems> availableItems;

    // danh sách id món thuộc combo (dùng khi lưu)
    private List<Long> selectedItemIds;

    // ====== UI FILTERS ======
    private String searchKeyword;       // ô search
    private String filterCuisineId;     // lọc theo cuisine
    private String filterCategoryId;    // lọc theo category
    private String filterVegetarian = "ALL"; // ALL / VEG / NON_VEG

    // map dùng cho checkbox: itemId -> checked?
    private Map<Long, Boolean> itemSelection = new HashMap<>();

    @PostConstruct
    public void init() {
        // load tất cả món chưa bị xóa
        loadAvailableItems();

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String comboIdStr = params.get("comboId");

        if (comboIdStr != null && !comboIdStr.isBlank()) {
            // EDIT MODE
            Long comboId = Long.valueOf(comboIdStr);
            combo = menuCombosFacade.find(comboId);
            editMode = true;

            // load các item đã thuộc combo
            loadSelectedItems(comboId);
        } else {
            // NEW MODE
            editMode = false;
            combo = new MenuCombos();
            combo.setStatus("ACTIVE");
            combo.setIsDeleted(false);

            // TODO: sau này lấy restaurant từ account đăng nhập
            Restaurants r = restaurantsFacade.find(1L);
            combo.setRestaurantId(r);

            selectedItemIds = new ArrayList<>();
        }

        // sync selectedItemIds -> itemSelection (để khi edit checkbox tự tick)
        if (selectedItemIds != null) {
            for (Long id : selectedItemIds) {
                itemSelection.put(id, Boolean.TRUE);
            }
        }
    }

    /**
     * Lấy toàn bộ món ăn chưa bị xóa để hiển thị trong "Included dishes".
     */
    private void loadAvailableItems() {
        availableItems = new ArrayList<>();
        List<MenuItems> all = menuItemsFacade.findAll();
        if (all != null) {
            for (MenuItems m : all) {
                Boolean deleted = m.getIsDeleted();
                if (deleted == null || !deleted) {
                    availableItems.add(m);
                }
            }
        }
    }

    /**
     * Load danh sách item đã thuộc về combo (khi edit).
     */
    private void loadSelectedItems(Long comboId) {
        selectedItemIds = new ArrayList<>();
        List<ComboItems> all = comboItemsFacade.findAll();
        if (all != null && comboId != null) {
            for (ComboItems ci : all) {
                ComboItemsPK pk = ci.getComboItemsPK();
                // pk.getComboId() là primitive long, so sánh bằng ==
                if (pk != null && pk.getComboId() == comboId) {
                    selectedItemIds.add(pk.getMenuItemId());
                }
            }
        }
    }

    // ================== FILTERED LIST ==================

    /**
     * Danh sách món sau khi áp dụng search + filters.
     * Được dùng trong <ui:repeat value="#{menuComboFormBean.filteredItems}">
     */
    public List<MenuItems> getFilteredItems() {
        List<MenuItems> result = new ArrayList<>();
        if (availableItems == null) {
            return result;
        }

        String kw = (searchKeyword == null) ? "" : searchKeyword.trim().toLowerCase();

        for (MenuItems mi : availableItems) {
            if (mi == null) continue;

            // search theo name/description
            if (!kw.isEmpty()) {
                String name = mi.getName() != null ? mi.getName().toLowerCase() : "";
                String desc = mi.getDescription() != null ? mi.getDescription().toLowerCase() : "";
                if (!name.contains(kw) && !desc.contains(kw)) {
                    continue;
                }
            }

            // filter cuisine
            if (filterCuisineId != null && !filterCuisineId.isBlank()) {
                if (mi.getCuisineId() == null
                        || mi.getCuisineId().getCuisineId() == null
                        || !String.valueOf(mi.getCuisineId().getCuisineId()).equals(filterCuisineId)) {
                    continue;
                }
            }

            // filter category
            if (filterCategoryId != null && !filterCategoryId.isBlank()) {
                if (mi.getCategoryId() == null
                        || mi.getCategoryId().getCategoryId() == null
                        || !String.valueOf(mi.getCategoryId().getCategoryId()).equals(filterCategoryId)) {
                    continue;
                }
            }

            // filter vegetarian / non-veg
            Boolean veg = mi.getIsVegetarian();
            if ("VEG".equals(filterVegetarian)) {
                if (veg == null || !veg) continue;
            } else if ("NON_VEG".equals(filterVegetarian)) {
                if (veg != null && veg) continue;
            }

            result.add(mi);
        }

        return result;
    }

    /**
     * Danh sách cuisine duy nhất, build từ availableItems
     */
    public List<Cuisines> getCuisineFilters() {
        List<Cuisines> list = new ArrayList<>();
        Set<Object> seen = new HashSet<>();

        if (availableItems != null) {
            for (MenuItems mi : availableItems) {
                if (mi != null && mi.getCuisineId() != null) {
                    Cuisines c = mi.getCuisineId();
                    Object id = c.getCuisineId();
                    if (id != null && !seen.contains(id)) {
                        seen.add(id);
                        list.add(c);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Danh sách category duy nhất, build từ availableItems
     */
    public List<MenuCategories> getCategoryFilters() {
        List<MenuCategories> list = new ArrayList<>();
        Set<Object> seen = new HashSet<>();

        if (availableItems != null) {
            for (MenuItems mi : availableItems) {
                if (mi != null && mi.getCategoryId() != null) {
                    MenuCategories cat = mi.getCategoryId();
                    Object id = cat.getCategoryId();
                    if (id != null && !seen.contains(id)) {
                        seen.add(id);
                        list.add(cat);
                    }
                }
            }
        }
        return list;
    }

    // ================== SAVE / CANCEL ==================

    public String save() {
        // gom checkbox -> selectedItemIds
        selectedItemIds = new ArrayList<>();
        if (itemSelection != null) {
            for (Map.Entry<Long, Boolean> e : itemSelection.entrySet()) {
                if (Boolean.TRUE.equals(e.getValue())) {
                    selectedItemIds.add(e.getKey());
                }
            }
        }

        // Lưu combo chính
        if (editMode) {
            menuCombosFacade.edit(combo);
        } else {
            menuCombosFacade.create(combo);
        }

        Long comboId = combo.getComboId();

        // Xóa toàn bộ ComboItems cũ (nếu đang edit)
        List<ComboItems> all = comboItemsFacade.findAll();
        if (all != null && comboId != null) {
            for (ComboItems ci : all) {
                ComboItemsPK pk = ci.getComboItemsPK();
                if (pk != null && pk.getComboId() == comboId) {
                    comboItemsFacade.remove(ci);
                }
            }
        }

        // Tạo lại ComboItems dựa trên selectedItemIds, quantity = 1
        if (selectedItemIds != null) {
            for (Long menuItemId : selectedItemIds) {
                if (menuItemId == null) continue;
                ComboItems ci = new ComboItems();
                ci.setComboItemsPK(new ComboItemsPK(comboId, menuItemId));
                ci.setQuantity(1);
                comboItemsFacade.create(ci);
            }
        }

        return "/Restaurant/menu-packages?faces-redirect=true&tab=packages";
    }

    public String cancel() {
        return "/Restaurant/menu-packages?faces-redirect=true&tab=packages";
    }

    // ================== GET / SET ==================

    public MenuCombos getCombo() {
        return combo;
    }

    public void setCombo(MenuCombos combo) {
        this.combo = combo;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public List<MenuItems> getAvailableItems() {
        return availableItems;
    }

    public void setAvailableItems(List<MenuItems> availableItems) {
        this.availableItems = availableItems;
    }

    public List<Long> getSelectedItemIds() {
        return selectedItemIds;
    }

    public void setSelectedItemIds(List<Long> selectedItemIds) {
        this.selectedItemIds = selectedItemIds;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(String searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public String getFilterCuisineId() {
        return filterCuisineId;
    }

    public void setFilterCuisineId(String filterCuisineId) {
        this.filterCuisineId = filterCuisineId;
    }

    public String getFilterCategoryId() {
        return filterCategoryId;
    }

    public void setFilterCategoryId(String filterCategoryId) {
        this.filterCategoryId = filterCategoryId;
    }

    public String getFilterVegetarian() {
        return filterVegetarian;
    }

    public void setFilterVegetarian(String filterVegetarian) {
        this.filterVegetarian = filterVegetarian;
    }

    public Map<Long, Boolean> getItemSelection() {
        return itemSelection;
    }

    public void setItemSelection(Map<Long, Boolean> itemSelection) {
        this.itemSelection = itemSelection;
    }
}
