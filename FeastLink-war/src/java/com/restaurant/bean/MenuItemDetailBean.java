package com.restaurant.bean;

import com.mypack.entity.MenuItems;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import java.io.Serializable;
import java.util.Map;

@Named("menuItemDetailBean")
@ViewScoped
public class MenuItemDetailBean implements Serializable {

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    private MenuItems item;

    public MenuItemDetailBean() {
    }

    @PostConstruct
    public void init() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String idParam = params.get("itemId");

        if (idParam != null && !idParam.isEmpty()) {
            try {
                Long id = Long.valueOf(idParam);
                item = menuItemsFacade.find(id);
            } catch (NumberFormatException ex) {
                // TODO: log nếu cần
            }
        }
    }

    public MenuItems getItem() {
        return item;
    }

    public boolean isFound() {
        return item != null;
    }
}
