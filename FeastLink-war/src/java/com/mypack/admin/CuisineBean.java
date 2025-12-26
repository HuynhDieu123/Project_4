package com.mypack.admin;

import com.mypack.entity.Cuisines;
import com.mypack.sessionbean.CuisinesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("cuisineBean")
@ViewScoped
public class CuisineBean implements Serializable {

    @EJB
    private CuisinesFacadeLocal cuisinesFacade;

    private List<Cuisines> cuisines = new ArrayList<>();
    private String keyword;

    // Current form (create / edit)
    private Cuisines currentCuisine = new Cuisines();

    @PostConstruct
    public void init() {
        loadCuisines();
    }

    public void loadCuisines() {
        if (keyword == null || keyword.trim().isEmpty()) {
            cuisines = cuisinesFacade.findAll();
        } else {
            cuisines = cuisinesFacade.searchByName(keyword.trim());
        }
    }

    public void search() {
        loadCuisines();
    }

    public void clearSearch() {
        keyword = null;
        loadCuisines();
    }

    public void prepareCreate() {
        currentCuisine = new Cuisines();
    }

    // Copy to avoid binding directly to the list row
    public void prepareEdit(Cuisines c) {
        Cuisines x = new Cuisines();
        x.setCuisineId(c.getCuisineId());
        x.setName(c.getName());
        currentCuisine = x;
    }

    public void save() {
        try {
            // Normalize name: trim + collapse spaces
            String rawName = currentCuisine.getName();
            String name = normalizeName(rawName);

            if (name == null || name.isEmpty()) {
                addFieldError("editForm:name", "Cuisine name is required.");
                return;
            }

            currentCuisine.setName(name);

            Integer excludeId = currentCuisine.getCuisineId(); // null if create
            boolean duplicated = cuisinesFacade.existsByName(name, excludeId);

            if (duplicated) {
                addFieldError("editForm:name", "Cuisine name already exists. Please choose another name.");
                return; // keep form values
            }

            if (currentCuisine.getCuisineId() == null) {
                cuisinesFacade.create(currentCuisine);
                addInfo("Success", "Cuisine created successfully.");
            } else {
                cuisinesFacade.edit(currentCuisine);
                addInfo("Success", "Cuisine updated successfully.");
            }

            // Reset form + reload list
            currentCuisine = new Cuisines();
            loadCuisines();

        } catch (Exception e) {
            e.printStackTrace();
            addError("Error", "An error occurred while saving the cuisine.");
        }
    }

    public void delete(Cuisines c) {
        try {
            cuisinesFacade.remove(c);
            addInfo("Success", "Cuisine deleted successfully.");
            loadCuisines();

            // If currently editing the deleted item => reset form
            if (currentCuisine != null && currentCuisine.getCuisineId() != null
                    && currentCuisine.getCuisineId().equals(c.getCuisineId())) {
                currentCuisine = new Cuisines();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("Error", "Cannot delete this cuisine (it may be in use).");
        }
    }

    // ================= helpers =================

    private String normalizeName(String s) {
        if (s == null) return null;
        return s.trim().replaceAll("\\s+", " ");
    }

    private void addInfo(String title, String msg) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, title, msg));
    }

    private void addError(String title, String msg) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, title, msg));
    }

    private void addFieldError(String clientId, String msg) {
        FacesContext.getCurrentInstance()
                .addMessage(clientId, new FacesMessage(FacesMessage.SEVERITY_ERROR, msg, msg));
    }

    // ================= getter/setter =================

    public List<Cuisines> getCuisines() {
        return cuisines;
    }

    public void setCuisines(List<Cuisines> cuisines) {
        this.cuisines = cuisines;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Cuisines getCurrentCuisine() {
        return currentCuisine;
    }

    public void setCurrentCuisine(Cuisines currentCuisine) {
        this.currentCuisine = currentCuisine;
    }
}
