package com.mypack.bean;

import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Named("registerRestaurantManagerBean")
@ViewScoped
public class RegisterRestaurantManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========= EJB =========
    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantManagersFacadeLocal restaurantManagersFacade;

    // ========= User hiện tại =========
    private Users currentUser;

    // ========= 1. Thông tin quản lý =========
    private String managerName;
    private String managerPhone;
    private String managerEmail;
    private String managerRole;

    // ========= 2. Thông tin nhà hàng =========
    private String restaurantName;
    private String restaurantBrandName;
    private String restaurantAddress;  // địa chỉ chi tiết
    private String city;               // thành phố (text, không lấy từ DB)
    private String area;               // khu vực (text)
    private String description;

    private String taxCode;
    private String fanpage;
    private String openTime;           // "09:00"
    private String closeTime;          // "22:00"
    private String servingStyle;

    // Logo bằng link
    private String logoUrl;

    // ========= 3. Điều kiện nhận tiệc =========
    private Integer minGuests;
    private Integer maxGuests;
    private Integer minDays;

    // ========= 4. Điều khoản & Loại tiệc =========
    private boolean acceptedTerms;

    // Loại tiệc phục vụ chính (bind từ hidden input)
    private String mainEventType;
    private final List<String> eventTypeOptions = Arrays.asList(
            "Wedding",
            "Birthday",
            "Corporate / Gala",
            "Conference",
            "Private Dining"
    );

    // ========= Thành phố / Khu vực (gợi ý tĩnh) =========
    private List<String> cityList;
    private List<String> areaList;

    // ========= Nhà hàng đã tồn tại (nếu user đã là manager) =========
    private Restaurants existingRestaurant;
    // Trạng thái của bản ghi RestaurantManagers (Pending / Active / Approved...)
    private String managerStatus;

    // --------------------------------------------------------
    // INIT
    // --------------------------------------------------------
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        Object obj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (obj instanceof Users) {
            currentUser = (Users) obj;
        }

        // Nếu chưa đăng nhập → về trang login
        if (currentUser == null) {
            try {
                String loginUrl = ctx.getExternalContext().getRequestContextPath() + "/login.xhtml";
                ctx.getExternalContext().redirect(loginUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Danh sách thành phố tĩnh
        cityList = Arrays.asList(
                "TP. Hồ Chí Minh",
                "Hà Nội",
                "Đà Nẵng",
                "Cần Thơ",
                "Nha Trang"
        );
        areaList = new ArrayList<>();

        // Prefill từ Users
        managerName  = currentUser.getFullName();
        managerPhone = currentUser.getPhone();
        managerEmail = currentUser.getEmail();

        // Nếu đã là manager → load nhà hàng để chỉnh sửa
        loadExistingRestaurant();

    }

    private void loadExistingRestaurant() {
        List<RestaurantManagers> rms = restaurantManagersFacade.findAll();
        for (RestaurantManagers rm : rms) {
            if (rm.getUserId() != null &&
                    rm.getUserId().getUserId().equals(currentUser.getUserId())) {
                existingRestaurant = rm.getRestaurantId();
                managerStatus = rm.getStatus();     // Lưu lại trạng thái manager
                break;
            }
        }

        if (existingRestaurant != null) {
            restaurantName = existingRestaurant.getName();

            // Tách description + loại tiệc chính
            String fullDesc = existingRestaurant.getDescription();
            parseMainEventFromDescription(fullDesc);

            restaurantAddress = existingRestaurant.getAddress();
            managerPhone      = existingRestaurant.getPhone() != null
                                ? existingRestaurant.getPhone()
                                : managerPhone;
            managerEmail      = existingRestaurant.getEmail() != null
                                ? existingRestaurant.getEmail()
                                : managerEmail;
            managerName       = existingRestaurant.getContactPerson() != null
                                ? existingRestaurant.getContactPerson()
                                : managerName;

            minGuests = existingRestaurant.getMinGuestCount();
            minDays   = existingRestaurant.getMinDaysInAdvance();

            // Logo
            logoUrl = existingRestaurant.getLogoUrl();
        }
    }

    /**
     * Tách loại tiệc chính & phần description hiển thị từ chuỗi description lưu trong DB.
     * Format lưu: [Main Event: Wedding] Mô tả nhà hàng...
     */
    private void parseMainEventFromDescription(String fullDesc) {
        if (isBlank(fullDesc)) {
            this.description   = null;
            this.mainEventType = null;
            return;
        }

        String trimmed = fullDesc.trim();
        String prefix  = "[Main Event:";
        if (trimmed.startsWith(prefix)) {
            int end = trimmed.indexOf(']');
            if (end > prefix.length()) {
                String inside = trimmed.substring(prefix.length(), end).trim(); // "Wedding"
                if (inside.startsWith(":")) {
                    inside = inside.substring(1).trim();
                }
                mainEventType = inside;

                String rest = trimmed.substring(end + 1).trim();
                description = rest.isEmpty() ? null : rest;
                return;
            }
        }

        // Không match pattern → chỉ là mô tả
        this.description   = fullDesc;
        this.mainEventType = null;
    }

    // --------------------------------------------------------
    // Thành phố / Khu vực: gợi ý theo city (tĩnh, không dùng DB)
    // --------------------------------------------------------
    public void onCityChange() {
        areaList = new ArrayList<>();

        if (city == null || city.trim().isEmpty()) {
            area = null;
            return;
        }

        switch (city) {
            case "TP. Hồ Chí Minh":
                areaList.addAll(Arrays.asList(
                        "Quận 1", "Quận 3", "Quận 5", "Quận 7", "Quận 10",
                        "Quận Tân Bình", "Quận Bình Thạnh",
                        "Thành phố Thủ Đức",
                        "Khu ven / Ngoại thành"
                ));
                break;
            case "Hà Nội":
                areaList.addAll(Arrays.asList(
                        "Quận Hoàn Kiếm", "Quận Ba Đình", "Quận Đống Đa",
                        "Quận Cầu Giấy", "Quận Hai Bà Trưng",
                        "Quận Long Biên", "Quận Nam Từ Liêm", "Quận Bắc Từ Liêm",
                        "Khu ven / Ngoại thành"
                ));
                break;
            case "Đà Nẵng":
                areaList.addAll(Arrays.asList(
                        "Quận Hải Châu", "Quận Sơn Trà", "Quận Ngũ Hành Sơn",
                        "Quận Liên Chiểu", "Quận Cẩm Lệ", "Huyện Hòa Vang"
                ));
                break;
            case "Cần Thơ":
                areaList.addAll(Arrays.asList(
                        "Quận Ninh Kiều", "Quận Bình Thủy", "Quận Cái Răng",
                        "Quận Ô Môn", "Huyện Phong Điền",
                        "Khu ven / Ngoại thành"
                ));
                break;
            case "Nha Trang":
                areaList.addAll(Arrays.asList(
                        "Trung tâm thành phố",
                        "Khu biển Trần Phú",
                        "Khu Bắc Nha Trang",
                        "Khu phía Tây Nha Trang",
                        "Khu ven / Ngoại thành"
                ));
                break;
            default:
                areaList.addAll(Arrays.asList(
                        "Trung tâm",
                        "Quận nội thành",
                        "Khu ven / Ngoại thành"
                ));
                break;
        }

        area = null;
    }

    // --------------------------------------------------------
    // Helper: trạng thái manager
    // --------------------------------------------------------
    private boolean isPendingManager() {
        return managerStatus != null && managerStatus.trim().equalsIgnoreCase("Pending");
    }

    private boolean isApprovedManager() {
        if (managerStatus == null) return false;
        String st = managerStatus.trim();
        // tuỳ bạn đặt tên trong DB, mình handle luôn 2 case phổ biến
        return st.equalsIgnoreCase("Active") || st.equalsIgnoreCase("Approved");
    }

    // --------------------------------------------------------
    // Submit
    // --------------------------------------------------------
    public String submit() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Nếu hồ sơ đang Pending → không cho gửi lại
        if (isPendingManager()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Hồ sơ đã gửi thành công và đang chờ xét duyệt",
                    "Tài khoản này đã gửi yêu cầu nâng cấp nhà hàng và đang chờ Admin FeastLink xét duyệt. Bạn không cần gửi lại."
            ));
            return null;
        }

        // Bắt buộc đồng ý điều khoản
        if (!acceptedTerms) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Bạn phải đồng ý với Điều khoản & Cam kết trước khi gửi yêu cầu.",
                    null
            ));
            return null;
        }

        // Validate bắt buộc
        if (isBlank(managerName) || isBlank(managerPhone) || isBlank(managerEmail) ||
            isBlank(restaurantName) || isBlank(restaurantAddress) ||
            isBlank(city) || isBlank(area) ||
            isBlank(logoUrl) ||
            isBlank(mainEventType) ||
            minGuests == null || minGuests < 0 ||
            minDays == null   || minDays   < 0) {

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Vui lòng điền đầy đủ các trường bắt buộc (bao gồm Logo và Loại tiệc chính).",
                    null
            ));
            return null;
        }

        if (maxGuests != null && maxGuests < 0) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Số khách tối đa (dự kiến) phải >= 0.",
                    null
            ));
            return null;
        }

        try {
            boolean isNewRestaurant = (existingRestaurant == null);

            Restaurants restaurant = isNewRestaurant
                    ? new Restaurants()
                    : existingRestaurant;

            // ====== BẮT BUỘC: status + createdAt + updatedAt ======
            Date now = new Date();

            if (isNewRestaurant) {
                restaurant.setStatus("Pending");      // tối đa 30 ký tự
                restaurant.setCreatedAt(now);
            }
            restaurant.setUpdatedAt(now);
            // =======================================================

            restaurant.setName(restaurantName);

            // Gộp description + loại tiệc chính (nếu dùng cách [Main Event: ...])
            String descToSave = description;
            if (!isBlank(mainEventType)) {
                String marker = "[Main Event: " + mainEventType + "]";
                if (isBlank(descToSave)) {
                    descToSave = marker;
                } else {
                    descToSave = marker + " " + descToSave.trim();
                }
            }
            restaurant.setDescription(descToSave);

            // Gộp địa chỉ chi tiết + khu vực + thành phố
            String fullAddr = restaurantAddress;
            if (!isBlank(area)) {
                fullAddr += ", " + area;
            }
            if (!isBlank(city)) {
                fullAddr += ", " + city;
            }
            restaurant.setAddress(fullAddr);

            restaurant.setPhone(managerPhone);
            restaurant.setEmail(managerEmail);
            restaurant.setContactPerson(managerName);

            restaurant.setMinGuestCount(minGuests);
            restaurant.setMinDaysInAdvance(minDays);

            // Lưu link logo
            restaurant.setLogoUrl(logoUrl);

            // parse giờ mở/đóng cửa (HH:mm) → Date (TIME) nếu có
            Date open = parseTime(openTime);
            if (open != null) {
                restaurant.setOpenTime(open);
            }
            Date close = parseTime(closeTime);
            if (close != null) {
                restaurant.setCloseTime(close);
            }

            if (isNewRestaurant) {
                // Tạo mới
                restaurantsFacade.create(restaurant);

                // Tạo bản ghi RestaurantManagers gắn với user hiện tại
                RestaurantManagers rm = new RestaurantManagers();
                rm.setUserId(currentUser);
                rm.setRestaurantId(restaurant);
                rm.setIsPrimary(true);
                rm.setStatus("Pending");     // <= 20 ký tự
                rm.setCreatedAt(now);

                restaurantManagersFacade.create(rm);

                existingRestaurant = restaurant;
                managerStatus = "Pending";
            } else {
                // Cập nhật (khi đã là manager)
                restaurantsFacade.edit(restaurant);
            }

            // Thông báo theo trường hợp
            if (isNewRestaurant) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Gửi yêu cầu xét duyệt thành công",
                        "Hồ sơ nhà hàng đã được gửi tới Admin FeastLink và đang chờ xét duyệt."
                ));
            } else {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_INFO,
                        "Cập nhật thông tin nhà hàng thành công",
                        "Thông tin nhà hàng đã được lưu lại. Nếu có thay đổi lớn, FeastLink có thể xem xét lại hồ sơ của bạn."
                ));
            }

            return null; // ở lại trang

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Có lỗi xảy ra khi lưu thông tin nhà hàng. Gửi yêu cầu xét duyệt chưa thành công.",
                    e.getMessage()
            ));
            return null;
        }
    }

    // Parse "HH:mm" → Date (TIME)
    private Date parseTime(String timeStr) {
        if (isBlank(timeStr)) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return sdf.parse(timeStr.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    public String cancel() {
        return "index?faces-redirect=true";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ================== GETTER / SETTER ==================

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getManagerPhone() { return managerPhone; }
    public void setManagerPhone(String managerPhone) { this.managerPhone = managerPhone; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }

    public String getManagerRole() { return managerRole; }
    public void setManagerRole(String managerRole) { this.managerRole = managerRole; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantBrandName() { return restaurantBrandName; }
    public void setRestaurantBrandName(String restaurantBrandName) { this.restaurantBrandName = restaurantBrandName; }

    public String getRestaurantAddress() { return restaurantAddress; }
    public void setRestaurantAddress(String restaurantAddress) { this.restaurantAddress = restaurantAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTaxCode() { return taxCode; }
    public void setTaxCode(String taxCode) { this.taxCode = taxCode; }

    public String getFanpage() { return fanpage; }
    public void setFanpage(String fanpage) { this.fanpage = fanpage; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public String getServingStyle() { return servingStyle; }
    public void setServingStyle(String servingStyle) { this.servingStyle = servingStyle; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }

    public Integer getMinDays() { return minDays; }
    public void setMinDays(Integer minDays) { this.minDays = minDays; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }

    public String getMainEventType() { return mainEventType; }
    public void setMainEventType(String mainEventType) { this.mainEventType = mainEventType; }

    public List<String> getEventTypeOptions() { return eventTypeOptions; }

    public List<String> getCityList() { return cityList; }
    public void setCityList(List<String> cityList) { this.cityList = cityList; }

    public List<String> getAreaList() { return areaList; }
    public void setAreaList(List<String> areaList) { this.areaList = areaList; }
}
