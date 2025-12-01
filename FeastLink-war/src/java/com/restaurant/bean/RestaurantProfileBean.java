package com.restaurant.bean;

import com.mypack.entity.Areas;
import com.mypack.sessionbean.AreasFacadeLocal;
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

    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String contactPerson;
    private Integer minGuests;
    private Integer minDaysBeforeBooking;
    private String cancelPolicy;

    // ======== SERVICE AREAS =========
    @EJB
    private AreasFacadeLocal areasFacade;

    /** Tất cả quận/huyện trong CSDL */
    private List<Areas> allAreas;

    /** Các quận/huyện mà nhà hàng đã chọn phục vụ */
    private List<Areas> serviceAreas;

    /** Area đang được chọn trong dropdown */
    private Integer selectedAreaId;

    @PostConstruct
    public void init() {
        // Demo dữ liệu profile (tạm thời hard-code như cũ)
        name = "The Grand Eatery";
        description = "A modern fine-dining restaurant specializing in catering.";
        phone = "(555) 123-4567";
        email = "contact@grandeatery.com";
        address = "123 Luxury Lane, Metropolis, 10001";
        contactPerson = "Alex Doe";
        minGuests = 20;
        minDaysBeforeBooking = 7;
        cancelPolicy = "Full refund if cancelled 14 days before the event. "
                + "50% refund between 7–13 days. No refund within 7 days.";

        // Lấy tất cả quận/huyện từ CSDL
        allAreas = areasFacade.findAll();

        // Các khu vực nhà hàng đang phục vụ (ban đầu để trống,
        // sau này có thể load từ bảng liên kết RestaurantAreas)
        serviceAreas = new ArrayList<>();

        // Nếu muốn demo có sẵn 1–2 khu vực:
        // if (!allAreas.isEmpty()) {
        //     serviceAreas.add(allAreas.get(0));
        // }
    }

    /** Lưu profile (tạm thời chỉ in ra console) */
    public String saveProfile() {
        System.out.println("Saving restaurant profile for: " + name);
        System.out.println("Service areas:");
        for (Areas a : serviceAreas) {
            System.out.println("- " + a.getCityId().getName() + " / " + a.getName());
        }
        // TODO: sau này lưu vào bảng RestaurantAreas
        return null;
    }

    /** Thêm một quận/huyện vào danh sách phục vụ */
    public void addServiceArea() {
        if (selectedAreaId == null) {
            return;
        }

        Areas area = areasFacade.find(selectedAreaId);
        if (area == null) {
            return;
        }

        if (!serviceAreas.contains(area)) {
            serviceAreas.add(area);
        }

        // reset dropdown
        selectedAreaId = null;
    }

    /** Xóa một quận/huyện khỏi danh sách phục vụ */
    public void removeServiceArea(Integer areaId) {
        if (areaId == null || serviceAreas == null) {
            return;
        }
        serviceAreas.removeIf(a -> areaId.equals(a.getAreaId()));
    }

    // ============ GETTERS / SETTERS ============

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

    public List<Areas> getAllAreas() {
        if (allAreas == null) {
            allAreas = areasFacade.findAll();
        }
        return allAreas;
    }

    public List<Areas> getServiceAreas() {
        return serviceAreas;
    }

    public void setServiceAreas(List<Areas> serviceAreas) {
        this.serviceAreas = serviceAreas;
    }

    public Integer getSelectedAreaId() {
        return selectedAreaId;
    }

    public void setSelectedAreaId(Integer selectedAreaId) {
        this.selectedAreaId = selectedAreaId;
    }
}
