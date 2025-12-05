package com.restaurant.bean;

import com.mypack.entity.ComboItems;
import com.mypack.entity.ComboItemsPK;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.MenuItems;
import com.mypack.sessionbean.ComboItemsFacadeLocal;
import com.mypack.sessionbean.MenuCombosFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Named("menuComboDetailBean")
@RequestScoped
public class MenuComboDetailBean implements Serializable {

    @EJB
    private MenuCombosFacadeLocal menuCombosFacade;

    @EJB
    private ComboItemsFacadeLocal comboItemsFacade;

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    private MenuCombos combo;
    private boolean found;

    // Danh sách món thuộc combo dùng để hiển thị
    private List<MenuItems> comboItems;

    @PostConstruct
    public void init() {
        try {
            Map<String, String> params = FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .getRequestParameterMap();

            String comboIdStr = params.get("comboId");
            if (comboIdStr == null || comboIdStr.isBlank()) {
                comboIdStr = params.get("combolId"); // fallback lỗi chính tả cũ
            }

            if (comboIdStr == null || comboIdStr.isBlank()) {
                found = false;
                return;
            }

            Long comboId = Long.valueOf(comboIdStr);

            // Lấy thông tin combo
            combo = menuCombosFacade.find(comboId);
            found = (combo != null);

            System.out.println(">>> Detail comboId = " + comboId + ", found = " + found);

            // Nếu tìm thấy combo thì load luôn danh sách món
            if (found) {
                loadComboItems(comboId);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            found = false;
        }
    }

    /**
     * Lấy danh sách món thuộc combo từ bảng ComboItems.
     */
    private void loadComboItems(Long comboId) {
        comboItems = new ArrayList<>();

        if (comboId == null) return;

        List<ComboItems> all = comboItemsFacade.findAll();
        if (all == null) return;

        for (ComboItems ci : all) {
            if (ci == null || ci.getComboItemsPK() == null) continue;

            ComboItemsPK pk = ci.getComboItemsPK();
            // pk.getComboId() là primitive long, comboId là Long
            if (pk.getComboId() == comboId) {
                Long menuItemId = pk.getMenuItemId();
                MenuItems mi = menuItemsFacade.find(menuItemId);
                if (mi != null) {
                    comboItems.add(mi);
                }
            }
        }
    }

    // ==== GETTERS ====

    public MenuCombos getCombo() {
        return combo;
    }

    public boolean isFound() {
        return found;
    }

    public List<MenuItems> getComboItems() {
        return comboItems;
    }
}
