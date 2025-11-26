package com.restaurant.bean;

import com.mypack.entity.ComboItems;
import com.mypack.entity.ComboItemsPK;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
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
import java.util.List;
import java.util.Map;

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

    // các món đơn để chọn
    private List<MenuItems> availableItems;

    // danh sách id món được chọn trong combo
    private List<Long> selectedItemIds;

    @PostConstruct
    public void init() {
        // load tất cả món đang ACTIVE, chưa bị xóa
        loadAvailableItems();

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        String comboIdStr = params.get("comboId");

        if (comboIdStr != null && !comboIdStr.isBlank()) {
            // EDIT MODE
            Long comboId = Long.valueOf(comboIdStr);
            combo = menuCombosFacade.find(comboId);
            editMode = true;

            // load danh sách món thuộc combo này
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
    }

    private void loadAvailableItems() {
        availableItems = new ArrayList<>();
        List<MenuItems> all = menuItemsFacade.findAll();
        if (all != null) {
            for (MenuItems m : all) {
                Boolean deleted = m.getIsDeleted();
                if ((deleted == null || !deleted)
                        && "ACTIVE".equalsIgnoreCase(m.getStatus())) {
                    availableItems.add(m);
                }
            }
        }
    }

    private void loadSelectedItems(Long comboId) {
        selectedItemIds = new ArrayList<>();
        List<ComboItems> all = comboItemsFacade.findAll();
        if (all != null) {
            for (ComboItems ci : all) {
                ComboItemsPK pk = ci.getComboItemsPK();
                if (pk != null && pk.getComboId() == comboId) {
                    selectedItemIds.add(pk.getMenuItemId());
                }
            }
        }
    }

    public String save() {
        // Lưu combo chính
        if (editMode) {
            menuCombosFacade.edit(combo);
        } else {
            menuCombosFacade.create(combo);
        }

        Long comboId = combo.getComboId();

        // Xóa toàn bộ ComboItems cũ (nếu đang edit)
        List<ComboItems> all = comboItemsFacade.findAll();
        if (all != null) {
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

    // ===== GET / SET =====
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
}
