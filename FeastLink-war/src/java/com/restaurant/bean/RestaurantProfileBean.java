package com.restaurant.bean;

import com.mypack.entity.Areas;
import com.mypack.entity.Cities;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named("restaurantProfileBean")
@ViewScoped
public class RestaurantProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ======== ENTITY CHÍNH ========
    private Restaurants restaurant;   // hàng trong bảng Restaurants

    // ======== BASIC PROFILE FIELDS (bind với xhtml) =========
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String contactPerson;

    // Mấy field booking policy tạm thời vẫn là demo (chưa có cột trong DB)
    private Integer minGuests;
    private Integer minDaysBeforeBooking;
    private String cancelPolicy;

    // ======== SERVICE AREAS =========
    @EJB
    private AreasFacadeLocal areasFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    /** Tất cả thành phố (Cities) */
    private List<Cities> allCities;

    /** Tất cả quận/huyện (Areas) */
    private List<Areas> allAreas;

    /** Các quận/huyện lọc theo city đang chọn */
    private List<Areas> filteredAreas;

    /** City đang được chọn trong dropdown */
    private Integer selectedCityId;

    /** Area đang được chọn trong dropdown (sẽ lưu vào Restaurants.AreaId) */
    private Integer selectedAreaId;

    // ==========================================================
    // INIT
    // ==========================================================
    @PostConstruct
    public void init() {
        // 1. Lấy restaurant hiện tại (tạm thời fix cứng id = 1)
        //    Sau này bạn thay bằng restaurantId từ session login.
        restaurant = restaurantsFacade.find(1L);

        if (restaurant != null) {
            name          = restaurant.getName();
            description   = restaurant.getDescription();
            phone         = restaurant.getPhone();
            email         = restaurant.getEmail();
            address       = restaurant.getAddress();
            contactPerson = restaurant.getContactPerson();

            // Nếu nhà hàng đã có AreaId => set city/area đã chọn
            Areas currentArea = restaurant.getAreaId();
            if (currentArea != null) {
                selectedAreaId = currentArea.getAreaId();
                if (currentArea.getCityId() != null) {
                    selectedCityId = currentArea.getCityId().getCityId();
                }
            }
        } else {
            // Trường hợp chưa có restaurant trong DB (hiếm gặp)
            restaurant = new Restaurants();
        }

        // 2. Demo booking policy (chưa lưu DB)
        minGuests = 20;
        minDaysBeforeBooking = 7;
        cancelPolicy = "Full refund if cancelled 14 days before the event. "
                + "50% refund between 7–13 days. No refund within 7 days.";

        // 3. Lấy dữ liệu dùng chung
        allCities = citiesFacade.findAll();
        allAreas  = areasFacade.findAll();

        filteredAreas = new ArrayList<>();
        // nếu đã biết city từ DB thì lọc luôn
        updateFilteredAreas();
    }

    // ==========================================================
    // LOGIC SERVICE AREAS
    // ==========================================================

    /** Cập nhật danh sách quận/huyện khi chọn city */
    private void updateFilteredAreas() {
        filteredAreas = new ArrayList<>();

        if (selectedCityId == null || allAreas == null) {
            selectedAreaId = null;
            return;
        }

        for (Areas a : allAreas) {
            if (a.getCityId() != null
                    && a.getCityId().getCityId() != null
                    && a.getCityId().getCityId().equals(selectedCityId)) {
                filteredAreas.add(a);
            }
        }

        /*
         * Nếu area hiện tại không thuộc city mới nữa thì reset.
         * Còn nếu còn nằm trong filteredAreas thì giữ nguyên.
         */
        if (selectedAreaId != null) {
            boolean stillValid = false;
            for (Areas a : filteredAreas) {
                if (a.getAreaId().equals(selectedAreaId)) {
                    stillValid = true;
                    break;
                }
            }
            if (!stillValid) {
                selectedAreaId = null;
            }
        }
    }

    /** Lưu profile: cập nhật lại entity Restaurants và ghi xuống DB */
    public String saveProfile() {
        // Map dữ liệu từ form sang entity
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setPhone(phone);
        restaurant.setEmail(email);
        restaurant.setAddress(address);
        restaurant.setContactPerson(contactPerson);

        // Gán AreaId cho restaurant nếu có chọn
        if (selectedAreaId != null) {
            Areas area = areasFacade.find(selectedAreaId);
            restaurant.setAreaId(area);
        } else {
            restaurant.setAreaId(null);
        }

        // Nếu đã có id -> edit, chưa có -> create
        if (restaurant.getRestaurantId() == null) {
            restaurantsFacade.create(restaurant);
        } else {
            restaurantsFacade.edit(restaurant);
        }

        // Không chuyển trang, chỉ ở lại và (nếu muốn) hiển thị growl sau này
        return null;
    }

    // ==========================================================
    // GETTERS / SETTERS
    // ==========================================================

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMinDaysBeforeBooking() { return minDaysBeforeBooking; }
    public void setMinDaysBeforeBooking(Integer minDaysBeforeBooking) {
        this.minDaysBeforeBooking = minDaysBeforeBooking;
    }

    public String getCancelPolicy() { return cancelPolicy; }
    public void setCancelPolicy(String cancelPolicy) { this.cancelPolicy = cancelPolicy; }

    // ---- City / Area master data ----
    public List<Cities> getAllCities() {
        if (allCities == null) {
            allCities = citiesFacade.findAll();
        }
        return allCities;
    }

    public List<Areas> getFilteredAreas() {
        return filteredAreas;
    }

    public Integer getSelectedCityId() {
        return selectedCityId;
    }
    public void setSelectedCityId(Integer selectedCityId) {
        this.selectedCityId = selectedCityId;
        updateFilteredAreas();   // mỗi lần đổi city thì lọc lại area
    }

    public Integer getSelectedAreaId() {
        return selectedAreaId;
    }
    public void setSelectedAreaId(Integer selectedAreaId) {
        this.selectedAreaId = selectedAreaId;
    }
}
