/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSF/JSFManagedBean.java to edit this template
 */
package com.mypack.admin;

import com.mypack.entity.ServiceTypes;
import com.mypack.sessionbean.ServiceTypesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import java.io.Serializable;
import java.util.List;

/**
 *
 * @author Laptop
 */
@Named("adminServiceTypeBean")
@ViewScoped
public class AdminServiceTypeBean implements Serializable {

    @EJB
    private ServiceTypesFacadeLocal serviceTypesFacade;

    private List<ServiceTypes> items;
    private ServiceTypes selected;      // for edit
    private String keyword;             // for search
    private String newName;             // for add

    @PostConstruct
    public void init() {
        loadData();
    }

    public void loadData() {
        items = serviceTypesFacade.findByKeyword(keyword);
    }

    public void search() {
        loadData();
    }

    public void create() {
        String name = (newName != null) ? newName.trim() : "";

        if (name.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Missing name",
                    "Please enter a service type name.");
            return;
        }

        if (serviceTypesFacade.existsByName(name)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Duplicate name",
                    "This service type already exists. Try another name ✨");
            return;
        }

        ServiceTypes st = new ServiceTypes();
        st.setName(name);
        serviceTypesFacade.create(st);
        newName = "";
        loadData();

        addMessage(FacesMessage.SEVERITY_INFO,
                "Created",
                "New service type has been created successfully 🎉");
    }

    public void update(ServiceTypes st) {
        String name = (st.getName() != null) ? st.getName().trim() : "";

        if (name.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Name is required",
                    "Service type name cannot be empty.");
            // reload để tránh lưu rỗng
            loadData();
            return;
        }

        if (serviceTypesFacade.existsByNameExceptId(name, st.getServiceTypeId())) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Duplicate name",
                    "Another service type already uses this name. Please choose a different one 🌟");
            // reload để đưa tên cũ về
            loadData();
            return;
        }

        st.setName(name);
        serviceTypesFacade.edit(st);

        addMessage(FacesMessage.SEVERITY_INFO,
                "Updated",
                "Service type has been updated successfully ✅");
    }

    public void delete(ServiceTypes st) {
        try {
            serviceTypesFacade.remove(st);
            loadData();
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Deleted successfully", null));
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Cannot delete because this service type is used", null));
        }
    }
private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
    FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(severity, summary, detail));
}

    // getters & setters
    public List<ServiceTypes> getItems() {
        return items;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    public ServiceTypes getSelected() {
        return selected;
    }

    public void setSelected(ServiceTypes selected) {
        this.selected = selected;
    }
}
