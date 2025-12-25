package com.mypack.admin;

import com.mypack.entity.EventTypes;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("adminEventTypeBean")
@ViewScoped
public class AdminEventTypeBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // This must match your XHTML: <h:form id="editForm"> + <h:inputText id="name">
    private static final String NAME_CLIENT_ID = "editForm:name";

    @EJB
    private EventTypesFacadeLocal eventTypesFacade;

    // list + search
    private List<EventTypes> items = new ArrayList<>();
    private String keyword;

    // form
    private Integer id;
    private String name;
    private boolean editMode; // true = update, false = create

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        items = eventTypesFacade.search(keyword);
    }

    public void search() {
        reload();
    }

    public void clearSearch() {
        keyword = null;
        reload();
    }

    public void prepareCreate() {
        this.editMode = false;
        this.id = null;
        this.name = null;
    }

    public void prepareEdit(EventTypes e) {
        if (e == null) return;
        this.editMode = true;
        this.id = e.getEventTypeId();
        this.name = e.getName();
    }

    public void cancel() {
        prepareCreate();
    }

    public void save() {
        // normalize name (trim + collapse spaces)
        name = normalize(name);

        // Let JSF show message under the field
        if (name == null || name.isBlank()) {
            addFieldError("Name is required.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }

        try {
            if (editMode) {
                eventTypesFacade.updateName(id, name);
                addGlobalMsg(FacesMessage.SEVERITY_INFO, "Updated", "Event type updated successfully.");
            } else {
                eventTypesFacade.createByName(name);
                addGlobalMsg(FacesMessage.SEVERITY_INFO, "Created", "Event type created successfully.");
            }
            prepareCreate();
            reload();

        } catch (IllegalArgumentException ex) {
            // If your facade already throws a validation/duplicate message, show it clearly under Name
            String msg = ex.getMessage();
            if (looksLikeDuplicateMessage(msg)) {
                addFieldError("This event type name already exists. Please choose another name.");
            } else {
                addFieldError((msg == null || msg.isBlank())
                        ? "Invalid name. Please check and try again."
                        : msg);
            }
            FacesContext.getCurrentInstance().validationFailed();

        } catch (Exception ex) {
            // If duplicate comes from DB unique constraint, it is often wrapped -> detect it
            if (isDuplicateException(ex)) {
                addFieldError("This event type name already exists. Please choose another name.");
                FacesContext.getCurrentInstance().validationFailed();
            } else {
                addGlobalMsg(FacesMessage.SEVERITY_ERROR, "Error", "Something went wrong. Please try again.");
            }
        }
    }

    public void delete(EventTypes e) {
        try {
            if (e == null || e.getEventTypeId() == null) return;
            eventTypesFacade.deleteById(e.getEventTypeId());
            addGlobalMsg(FacesMessage.SEVERITY_INFO, "Deleted", "Event type deleted successfully.");
            reload();

            if (editMode && id != null && id.equals(e.getEventTypeId())) {
                prepareCreate();
            }
        } catch (Exception ex) {
            addGlobalMsg(FacesMessage.SEVERITY_ERROR, "Error", "Cannot delete this event type (maybe being used).");
        }
    }

    // ---------------- helpers ----------------

    private String normalize(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private boolean looksLikeDuplicateMessage(String msg) {
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("duplicate") || m.contains("exists") || m.contains("unique");
    }

    private boolean isDuplicateException(Throwable ex) {
        Throwable t = ex;
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null) {
                String m = msg.toLowerCase();
                // SQL Server duplicate key codes: 2601 / 2627 (often appear in message)
                if (m.contains("2601") || m.contains("2627")
                        || m.contains("duplicate") || m.contains("unique")
                        || m.contains("violation") || m.contains("constraint")) {
                    return true;
                }
            }
            t = t.getCause();
        }
        return false;
    }

    private void addGlobalMsg(FacesMessage.Severity sev, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(sev, summary, detail));
    }

    private void addFieldError(String message) {
        FacesContext.getCurrentInstance().addMessage(
                NAME_CLIENT_ID,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message)
        );
    }

    // ---------------- getters/setters ----------------

    public List<EventTypes> getItems() {
        return items;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEditMode() {
        return editMode;
    }
}
