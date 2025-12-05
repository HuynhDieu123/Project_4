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
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

@Named("restaurantProfileBean")
@ViewScoped
public class RestaurantProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ======== ENTITY CHÍNH ========
    private Restaurants restaurant;   // hàng trong bảng Restaurants

    // ======== BASIC PROFILE FIELDS =========
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String contactPerson;

    // Status lấy từ cột Status trong bảng Restaurants
    // PENDING / ACTIVE / PRIVATE
    private String status;

    // Operating hours (bind với input type="time", format: HH:mm)
    private String openTimeText;   // ví dụ "08:00"
    private String closeTimeText;  // ví dụ "22:00"

    // Booking policy: LƯU THẬT vào DB
    // MinGuests  -> cột MinGuestCount
    // MinDays... -> cột MinDaysInAdvance
    // cancelPolicy: vẫn demo, chưa có cột DB
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

            status        = restaurant.getStatus(); // lấy từ DB

            // ====== GIỜ MỞ / ĐÓNG ======
            if (restaurant.getOpenTime() != null) {
                // "08:00:00" -> "08:00"
                String t = restaurant.getOpenTime().toString();
                openTimeText = t.length() >= 5 ? t.substring(0, 5) : t;
            } else {
                openTimeText = "08:00";
            }

            if (restaurant.getCloseTime() != null) {
                String t = restaurant.getCloseTime().toString();
                closeTimeText = t.length() >= 5 ? t.substring(0, 5) : t;
            } else {
                closeTimeText = "22:00";
            }

            // ====== BOOKING POLICY (LẤY TỪ DB) ======
            // MinGuests
            if (restaurant.getMinGuestCount() != null) {
                minGuests = restaurant.getMinGuestCount();
            } else {
                minGuests = 20; // default nếu DB null
            }

            // MinDaysInAdvance
            if (restaurant.getMinDaysInAdvance() != null) {
                minDaysBeforeBooking = restaurant.getMinDaysInAdvance();
            } else {
                minDaysBeforeBooking = 7; // default nếu DB null
            }


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
            status = "PENDING";
            openTimeText = "08:00";
            closeTimeText = "22:00";

            // Default booking policy cho nhà hàng mới
            minGuests = 20;
            minDaysBeforeBooking = 7;
            cancelPolicy = "Full refund if cancelled 14 days before the event. "
                    + "50% refund between 7–13 days. No refund within 7 days.";
        }

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

        // Nếu area hiện tại không thuộc city mới nữa thì reset.
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

    // ==========================================================
    // STATUS: PUBLIC / PRIVATE
    // ==========================================================

    /** Toggle giữa ACTIVE (Public) và PRIVATE, không cho đổi khi PENDING. */
    public void toggleVisibility() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }

        // Nếu đang pending duyệt thì không cho tự đổi
        if ("PENDING".equalsIgnoreCase(status)) {
            return;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            status = "PRIVATE";
        } else if ("PRIVATE".equalsIgnoreCase(status)) {
            status = "ACTIVE";
        } else {
            // các giá trị khác treat như ACTIVE
            status = "ACTIVE";
        }

        restaurant.setStatus(status);
        restaurantsFacade.edit(restaurant);
    }

    /** Dùng trong EL: restaurantProfileBean.publicVisible */
    public boolean isPublicVisible() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    // ==========================================================
    // SAVE PROFILE
    // ==========================================================

    /** Parse string HH:mm (hoặc HH:mm:ss) sang java.sql.Time */
    private Time parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        // nếu chỉ có HH:mm thì thêm :00
        if (s.length() == 5) {
            s = s + ":00";
        }
        try {
            return Time.valueOf(s);
        } catch (IllegalArgumentException ex) {
            // nếu sai format thì trả null, bạn có thể log hoặc validate thêm
            return null;
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

        // Lưu status về DB (giữ nguyên nếu đang PENDING / ACTIVE / PRIVATE)
        restaurant.setStatus(status);

        // ====== LƯU GIỜ MỞ / ĐÓNG ======
        restaurant.setOpenTime(parseTime(openTimeText));
        restaurant.setCloseTime(parseTime(closeTimeText));

        // ====== LƯU BOOKING POLICY xuống DB ======
        // map vào các cột MinGuestCount & MinDaysInAdvance
        restaurant.setMinGuestCount(minGuests);
        restaurant.setMinDaysInAdvance(minDaysBeforeBooking);
        // cancelPolicy hiện chưa có cột => chưa lưu

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

        // Ở lại trang
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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOpenTimeText() { return openTimeText; }
    public void setOpenTimeText(String openTimeText) { this.openTimeText = openTimeText; }

    public String getCloseTimeText() { return closeTimeText; }
    public void setCloseTimeText(String closeTimeText) { this.closeTimeText = closeTimeText; }

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
