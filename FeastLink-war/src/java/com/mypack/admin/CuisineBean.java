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

    // Danh sách để hiển thị
    private List<Cuisines> cuisines = new ArrayList<>();

    // Ô search
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
            cuisines = cuisinesFacade.searchByName(keyword);
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

    public void prepareEdit(Cuisines c) {
        // clone đơn giản: gán trực tiếp entity (nếu không dùng dialog)
        currentCuisine = c;
    }

    public void save() {
        try {
            if (currentCuisine.getCuisineId() == null) {
                cuisinesFacade.create(currentCuisine);
                addMessage("Thêm loại ẩm thực thành công.");
            } else {
                cuisinesFacade.edit(currentCuisine);
                addMessage("Cập nhật loại ẩm thực thành công.");
            }
            // reset form
            currentCuisine = new Cuisines();
            loadCuisines();
        } catch (Exception e) {
            e.printStackTrace();
            addError("Có lỗi xảy ra khi lưu loại ẩm thực.");
        }
    }

    public void delete(Cuisines c) {
        try {
            cuisinesFacade.remove(c);
            addMessage("Xóa loại ẩm thực thành công.");
            loadCuisines();
        } catch (Exception e) {
            e.printStackTrace();
            addError("Không thể xóa loại ẩm thực (có thể đang được sử dụng).");
        }
    }

    private void addMessage(String msg) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Thành công", msg));
    }

    private void addError(String msg) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Lỗi", msg));
    }

    // ===== GETTER / SETTER =====
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
