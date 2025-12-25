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

    // Form đang nhập (thêm / sửa)
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

    // copy để không bị “dính” list khi sửa
    public void prepareEdit(Cuisines c) {
        Cuisines x = new Cuisines();
        x.setCuisineId(c.getCuisineId());
        x.setName(c.getName());
        currentCuisine = x;
    }

    public void save() {
        try {
            // normalize name: trim + gộp khoảng trắng
            String rawName = currentCuisine.getName();
            String name = normalizeName(rawName);

            if (name == null || name.isEmpty()) {
                addFieldError("editForm:name", "Cuisine name is required.");
                return;
            }

            currentCuisine.setName(name);

            Integer excludeId = currentCuisine.getCuisineId(); // null nếu create
            boolean duplicated = cuisinesFacade.existsByName(name, excludeId);

            if (duplicated) {
                addFieldError("editForm:name", "Cuisine name already exists. Please choose another name.");
                return; // giữ form value, không reset
            }

            if (currentCuisine.getCuisineId() == null) {
                cuisinesFacade.create(currentCuisine);
                addInfo("Thành công", "Thêm loại ẩm thực thành công.");
            } else {
                cuisinesFacade.edit(currentCuisine);
                addInfo("Thành công", "Cập nhật loại ẩm thực thành công.");
            }

            // reset form + reload list
            currentCuisine = new Cuisines();
            loadCuisines();

        } catch (Exception e) {
            e.printStackTrace();
            addError("Lỗi", "Có lỗi xảy ra khi lưu loại ẩm thực.");
        }
    }

    public void delete(Cuisines c) {
        try {
            cuisinesFacade.remove(c);
            addInfo("Thành công", "Xóa loại ẩm thực thành công.");
            loadCuisines();

            // nếu đang edit đúng item vừa xóa thì reset form
            if (currentCuisine != null && currentCuisine.getCuisineId() != null
                    && currentCuisine.getCuisineId().equals(c.getCuisineId())) {
                currentCuisine = new Cuisines();
            }
        } catch (Exception e) {
            e.printStackTrace();
            addError("Lỗi", "Không thể xóa loại ẩm thực (có thể đang được sử dụng).");
        }
    }

    // ================= helpers =================

    private String normalizeName(String s) {
        if (s == null) return null;
        // trim + gộp nhiều khoảng trắng thành 1
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
