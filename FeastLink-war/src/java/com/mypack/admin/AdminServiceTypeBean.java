package com.mypack.admin;

import com.mypack.entity.ServiceTypes;
import com.mypack.sessionbean.ServiceTypesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("adminServiceTypeBean")
@ViewScoped
public class AdminServiceTypeBean implements Serializable {

    @EJB
    private ServiceTypesFacadeLocal serviceTypesFacade;

    private String keyword;
    private String newName;
    private List<ServiceTypes> items;

    @PostConstruct
    public void init() {
        items = new ArrayList<>();
        search(); // load list first time
    }

    // ========= ACTIONS called by XHTML =========

    public void search() {
        String kw = normalize(keyword);
        if (kw == null) {
            items = serviceTypesFacade.findAllOrderByName();
        } else {
            items = serviceTypesFacade.searchByName(kw);
        }
    }

    public void create() {
        String name = normalize(newName);
        if (name == null) {
            addMsg(FacesMessage.SEVERITY_WARN, "Name is required.");
            return;
        }

        if (serviceTypesFacade.existsByName(name)) {
            addMsg(FacesMessage.SEVERITY_WARN, "This service type name already exists.");
            return;
        }

        try {
            ServiceTypes st = new ServiceTypes();
            st.setName(name);
            serviceTypesFacade.create(st);

            newName = null;
            addMsg(FacesMessage.SEVERITY_INFO, "Created successfully.");
            search();
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Create failed: " + rootMsg(e));
        }
    }

    public void update(ServiceTypes st) {
        if (st == null || st.getServiceTypeId() == null) return;

        String name = normalize(st.getName());
        if (name == null) {
            addMsg(FacesMessage.SEVERITY_WARN, "Name is required.");
            search(); // reload to revert invalid edits
            return;
        }

        if (serviceTypesFacade.existsByNameExceptId(name, st.getServiceTypeId())) {
            addMsg(FacesMessage.SEVERITY_WARN, "This name already exists (duplicate).");
            search();
            return;
        }

        try {
            st.setName(name);
            serviceTypesFacade.edit(st); // merge
            addMsg(FacesMessage.SEVERITY_INFO, "Saved.");
            search();
        } catch (Exception e) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Save failed: " + rootMsg(e));
            search();
        }
    }

    public void delete(ServiceTypes st) {
        if (st == null || st.getServiceTypeId() == null) return;

        try {
            ServiceTypes managed = serviceTypesFacade.find(st.getServiceTypeId());
            if (managed == null) {
                addMsg(FacesMessage.SEVERITY_WARN, "Not found.");
                search();
                return;
            }

            serviceTypesFacade.remove(managed);
            addMsg(FacesMessage.SEVERITY_INFO, "Deleted.");
            search();
        } catch (Exception e) {
            // Usually FK constraint violation if it is used somewhere
            addMsg(FacesMessage.SEVERITY_ERROR,
                    "Cannot delete. This service type may be used by restaurants/bookings.");
        }
    }

    // ========= HELPERS =========

    private String normalize(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void addMsg(FacesMessage.Severity severity, String msg) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, msg, null));
    }

    private String rootMsg(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return (cur.getMessage() != null) ? cur.getMessage() : cur.toString();
    }

    // ========= GETTERS/SETTERS for XHTML =========

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getNewName() { return newName; }
    public void setNewName(String newName) { this.newName = newName; }

    public List<ServiceTypes> getItems() { return items; }
    public void setItems(List<ServiceTypes> items) { this.items = items; }
}
