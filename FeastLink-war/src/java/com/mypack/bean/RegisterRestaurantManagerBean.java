package com.mypack.bean;

import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.entity.Areas;
import com.mypack.entity.Cities;

import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    // Dùng để load Cities/Areas từ DB
    @EJB
    private AreasFacadeLocal areasFacade;

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
    private String city;               // tên thành phố (từ DB)
    private String area;               // tên khu vực (từ DB)
    private String description;

    // KHÔNG còn dùng taxCode, fanpage, servingStyle trên form, bỏ luôn cho sạch

    private String openTime;           // "HH:mm"
    private String closeTime;          // "HH:mm"

    // Logo: đường dẫn tương đối lưu trong DB
    private String logoUrl;
    // File upload từ form
    private Part logoFile;

    // ========= 3. Điều kiện nhận tiệc =========
    private Integer minGuests;
    private Integer maxGuests;
    private Integer minDays;

    // ========= 4. Điều khoản =========
    private boolean acceptedTerms;

    // ========= Thành phố / Khu vực (lấy từ DB) =========
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
                String loginUrl = ctx.getExternalContext().getRequestContextPath()
                        + "/login.xhtml?target=register_manager";
                ctx.getExternalContext().redirect(loginUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        // Prefill từ Users
        managerName  = currentUser.getFullName();
        managerPhone = currentUser.getPhone();
        managerEmail = currentUser.getEmail();

        // Load danh sách City từ DB (dựa trên bảng Areas → CityId → Name)
        loadCitiesFromDb();
        areaList = new ArrayList<>();

        // Nếu đã là manager → load nhà hàng để chỉnh sửa
        loadExistingRestaurant();
    }

    // Lấy danh sách thành phố (distinct theo Cities.Name) từ bảng Areas
    private void loadCitiesFromDb() {
        cityList = new ArrayList<>();
        List<Areas> allAreas = areasFacade.findAll();
        if (allAreas == null) {
            return;
        }

        for (Areas a : allAreas) {
            if (a == null) continue;
            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            String name = c.getName().trim();
            if (name.isEmpty()) continue;

            boolean exists = false;
            for (String s : cityList) {
                if (s.equalsIgnoreCase(name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                cityList.add(name);
            }
        }

        cityList.sort(String::compareToIgnoreCase);
    }

    private void loadExistingRestaurant() {
        List<RestaurantManagers> rms = restaurantManagersFacade.findAll();
        for (RestaurantManagers rm : rms) {
            if (rm.getUserId() != null
                    && rm.getUserId().getUserId().equals(currentUser.getUserId())) {
                existingRestaurant = rm.getRestaurantId();
                managerStatus = rm.getStatus();
                break;
            }
        }

        if (existingRestaurant != null) {
            restaurantName = existingRestaurant.getName();

            // KHÔNG còn dùng mainEventType → chỉ lấy thẳng description
            description = existingRestaurant.getDescription();

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

            // Logo: đường dẫn đã lưu trong DB
            logoUrl = existingRestaurant.getLogoUrl();

            // Nếu restaurant đã có AreaId → map ngược ra city/area để hiển thị
            Areas a = existingRestaurant.getAreaId();
            if (a != null) {
                Cities c = a.getCityId();
                if (c != null && c.getName() != null) {
                    city = c.getName();
                }
                if (a.getName() != null) {
                    area = a.getName();
                }
                // Khi đã có city → load areaList tương ứng
                onCityChange();
            }

            // Prefill giờ mở / đóng cửa nếu đã có trong DB
            if (existingRestaurant.getOpenTime() != null
                    || existingRestaurant.getCloseTime() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                if (existingRestaurant.getOpenTime() != null) {
                    openTime = timeFormat.format(existingRestaurant.getOpenTime());
                }
                if (existingRestaurant.getCloseTime() != null) {
                    closeTime = timeFormat.format(existingRestaurant.getCloseTime());
                }
            }
        }
    }

    // --------------------------------------------------------
    // Thành phố / Khu vực: lấy từ DB
    // --------------------------------------------------------
    public void onCityChange() {
        areaList = new ArrayList<>();

        if (isBlank(city)) {
            area = null;
            return;
        }

        String cityName = city.trim();
        List<Areas> allAreas = areasFacade.findAll();
        if (allAreas == null) {
            area = null;
            return;
        }

        for (Areas a : allAreas) {
            if (a == null || a.getName() == null) continue;
            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            if (c.getName().trim().equalsIgnoreCase(cityName)) {
                String areaName = a.getName().trim();
                if (!areaName.isEmpty()) {
                    areaList.add(areaName);
                }
            }
        }

        areaList.sort(String::compareToIgnoreCase);
        area = null; // reset chọn lại
    }

    // --------------------------------------------------------
    // Helper: trạng thái manager
    // --------------------------------------------------------
    private boolean isPendingManager() {
        return managerStatus != null && managerStatus.trim().equalsIgnoreCase("Pending")
               || managerStatus != null && managerStatus.trim().equalsIgnoreCase("PENDING_APPROVAL");
    }

    // --------------------------------------------------------
    // Map City + Area name → Areas entity (để set AreaId cho Restaurants)
    // --------------------------------------------------------
    private Areas resolveAreaEntity() {
        if (isBlank(city) || isBlank(area)) {
            return null;
        }

        String cityName = city.trim();
        String areaName = area.trim();

        List<Areas> all = areasFacade.findAll();
        if (all == null || all.isEmpty()) {
            return null;
        }

        for (Areas a : all) {
            if (a == null || a.getName() == null) continue;
            if (!a.getName().trim().equalsIgnoreCase(areaName)) continue;

            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            if (c.getName().trim().equalsIgnoreCase(cityName)) {
                return a;
            }
        }
        return null;
    }

    // --------------------------------------------------------
    // Submit
    // --------------------------------------------------------
    public String submit() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Luôn khóa 2 trường name + email theo currentUser (chống sửa tay)
        if (currentUser != null) {
            managerName  = currentUser.getFullName();
            managerEmail = currentUser.getEmail();
        }

        // Nếu hồ sơ đang Pending → không cho gửi lại
        if (isPendingManager()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Your application has been submitted and is awaiting approval. You cannot submit a new application.",
                    "This account has submitted a restaurant upgrade request and is awaiting FeastLink Admin approval. You do not need to resubmit."
            ));
            return null;
        }

        // Bắt buộc đồng ý điều khoản
        if (!acceptedTerms) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "You must agree to the Terms & Conditions before submitting a request.",
                    null
            ));
            return null;
        }

        // Kiểm tra logo: nếu không có file mới và cũng không có logo cũ → lỗi
        boolean noNewLogo      = (logoFile == null || logoFile.getSize() <= 0);
        boolean noExistingLogo = isBlank(logoUrl);

        // Validate bắt buộc (thêm openTime / closeTime)
        if (isBlank(managerName) || isBlank(managerPhone) || isBlank(managerEmail)
                || isBlank(restaurantName) || isBlank(restaurantAddress)
                || isBlank(city) || isBlank(area)
                || isBlank(openTime) || isBlank(closeTime)
                || (noNewLogo && noExistingLogo)
                || minGuests == null || minGuests < 0
                || minDays == null   || minDays   < 0) {

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please fill in all required fields (including Logo, City, Region and Opening/Closing hours).",
                    null
            ));
            return null;
        }

        if (maxGuests != null && maxGuests < 0) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Maximum (expected) number of guests must be >= 0.",
                    null
            ));
            return null;
        }

        try {
            boolean isNewRestaurant = (existingRestaurant == null);

            Restaurants restaurant = isNewRestaurant
                    ? new Restaurants()
                    : existingRestaurant;

            Date now = new Date();

            if (isNewRestaurant) {
                restaurant.setStatus("PENDING_APPROVAL");
                restaurant.setCreatedAt(now);
            }
            restaurant.setUpdatedAt(now);

            restaurant.setName(restaurantName);

            // Lưu description nguyên bản
            restaurant.setDescription(description);

            // Address text (hiển thị)
            String fullAddr = restaurantAddress;
            if (!isBlank(area)) {
                fullAddr += ", " + area;
            }
            if (!isBlank(city)) {
                fullAddr += ", " + city;
            }
            restaurant.setAddress(fullAddr);

            // Map sang AreaId (bắt buộc không null)
            Areas selectedArea = resolveAreaEntity();
            if (selectedArea == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "No corresponding Area found in the system.",
                        "Check Cities/Areas in DB or contact Admin FeastLink."
                ));
                return null;
            }
            restaurant.setAreaId(selectedArea);

            restaurant.setPhone(managerPhone);
            restaurant.setEmail(managerEmail);
            restaurant.setContactPerson(managerName);

            restaurant.setMinGuestCount(minGuests);
            restaurant.setMinDaysInAdvance(minDays);

            // ------------------ UPLOAD LOGO ------------------
            if (!noNewLogo && logoFile != null) {
                String uploadRoot = "E:\\ProjectSemIV\\Code\\Project_4\\FeastLink-war\\web\\resources\\images\\upload_file";

                File folder = new File(uploadRoot);
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String submitted = logoFile.getSubmittedFileName();
                String baseName  = submitted;
                if (submitted != null) {
                    int slash = submitted.lastIndexOf('/');
                    int back  = submitted.lastIndexOf('\\');
                    int idx   = Math.max(slash, back);
                    if (idx >= 0 && idx < submitted.length() - 1) {
                        baseName = submitted.substring(idx + 1);
                    }
                }
                String ext = "";
                int dot = baseName != null ? baseName.lastIndexOf('.') : -1;
                if (dot != -1) {
                    ext = baseName.substring(dot);
                }

                String savedName = "logo_" + currentUser.getUserId() + "_" + System.currentTimeMillis() + ext;
                File dest = new File(folder, savedName);

                try (InputStream in = logoFile.getInputStream();
                     FileOutputStream out = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }

                String relativePath = "/resources/images/upload_file/" + savedName;
                logoUrl = relativePath;
                restaurant.setLogoUrl(relativePath);
            } else {
                if (!isBlank(logoUrl)) {
                    restaurant.setLogoUrl(logoUrl);
                }
            }

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
                restaurantsFacade.create(restaurant);

                RestaurantManagers rm = new RestaurantManagers();
                rm.setUserId(currentUser);
                rm.setRestaurantId(restaurant);
                rm.setIsPrimary(true);
                rm.setStatus("PENDING_APPROVAL");
                rm.setCreatedAt(now);

                restaurantManagersFacade.create(rm);

                // update status user
                currentUser.setStatus("PENDING");
                usersFacade.edit(currentUser);

                FacesContext.getCurrentInstance()
                        .getExternalContext()
                        .getSessionMap()
                        .put("currentUser", currentUser);

                existingRestaurant = restaurant;
                managerStatus = "PENDING_APPROVAL";
            } else {
                restaurantsFacade.edit(restaurant);
            }

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Request for review sent successfully",
                    "Restaurant profile has been submitted to Admin FeastLink and is awaiting approval."
            ));

            return null;

        } catch (IOException e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Unable to save logo file on server. Request for review failed.",
                    e.getMessage()
            ));
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "An error occurred while saving restaurant information. Request for approval failed.",
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
        // index.xhtml ở root
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

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Part getLogoFile() { return logoFile; }
    public void setLogoFile(Part logoFile) { this.logoFile = logoFile; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }

    public Integer getMinDays() { return minDays; }
    public void setMinDays(Integer minDays) { this.minDays = minDays; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }

    public List<String> getCityList() { return cityList; }
    public void setCityList(List<String> cityList) { this.cityList = cityList; }

    public List<String> getAreaList() { return areaList; }
    public void setAreaList(List<String> areaList) { this.areaList = areaList; }
}
