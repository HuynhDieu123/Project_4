package com.mypack.admin;

import com.mypack.entity.Restaurants;
import com.mypack.entity.Cities;
import com.mypack.entity.Areas;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;
import com.mypack.sessionbean.AreasFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("restaurantsBean")
@SessionScoped
public class RestaurantsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    @EJB
    private AreasFacadeLocal areasFacade;

    // ===== FILTERS (list page) =====
    private String keyword = "";
    // Mặc định chỉ show nhà hàng ACTIVE
    private String statusFilter = "ACTIVE";
    private Integer cityFilterId;

    // ===== PAGINATION =====
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages;

    // ===== ADD RESTAURANT (modal) =====
    private Restaurants newRestaurant = new Restaurants();
    private Integer newCityId;
    private Integer newAreaId;

    // ===== CHANGE STATUS =====
    private Long selectedRestaurantId;

    // ===== DATA =====
    private List<Restaurants> allRestaurants = new ArrayList<>();
    private List<Restaurants> pageRestaurants = new ArrayList<>();
    private List<Cities> cityList = new ArrayList<>();
    private List<Areas> areaList = new ArrayList<>();

    // =========================================================
    // INIT
    // =========================================================
    @PostConstruct
    public void init() {
        cityList = citiesFacade.findAll();
        areaList = areasFacade.findAll();
        loadRestaurants();
    }

    /**
     * Luôn gọi applyFilter() để load list theo filter hiện tại.
     */
    public void loadRestaurants() {
        applyFilter();
    }

    // =========================================================
    // FILTER
    // =========================================================
    public void applyFilter() {
        List<Restaurants> source = restaurantsFacade.findAll();
        List<Restaurants> filtered = new ArrayList<>();

        for (Restaurants r : source) {
            String rowStatus = r.getStatus();

            // ⭐ 1. BỎ QUA HOÀN TOÀN CÁC NHÀ HÀNG ĐANG PENDING_APPROVAL
            if ("PENDING_APPROVAL".equals(rowStatus)) {
                continue;   // dòng này đảm bảo pending không bao giờ vào list
            }

            // ----- keyword -----
            boolean matchKeyword = (keyword == null || keyword.isEmpty());
            if (!matchKeyword) {
                String kw = keyword.toLowerCase();
                matchKeyword
                        = (r.getName() != null && r.getName().toLowerCase().contains(kw))
                        || (r.getEmail() != null && r.getEmail().toLowerCase().contains(kw))
                        || (r.getPhone() != null && r.getPhone().toLowerCase().contains(kw))
                        || (r.getContactPerson() != null && r.getContactPerson().toLowerCase().contains(kw));
            }

            // ----- status (chỉ ACTIVE / INACTIVE) -----
            boolean matchStatus;
            if (statusFilter == null || "ALL".equals(statusFilter)) {
                // ALL = chấp nhận ACTIVE + INACTIVE (pending đã bị continue ở trên)
                matchStatus = true;
            } else {
                matchStatus = (rowStatus != null && rowStatus.equals(statusFilter));
            }

            // ----- city -----
            boolean matchCity = (cityFilterId == null || cityFilterId == 0);
            if (!matchCity) {
                Areas area = r.getAreaId();
                if (area != null && area.getCityId() != null) {
                    matchCity = cityFilterId.equals(area.getCityId().getCityId());
                }
            }

            if (matchKeyword && matchStatus && matchCity) {
                filtered.add(r);
            }
        }

        allRestaurants = filtered;
        currentPage = 1;
        calculatePagination();
        loadPage();
    }

    /**
     * Reset filter về trạng thái mặc định: chỉ ACTIVE, không keyword, không
     * city.
     */
    public void resetFilter() {
        keyword = "";
        statusFilter = "ACTIVE";
        cityFilterId = null;
        loadRestaurants();
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    private void calculatePagination() {
        totalPages = (int) Math.ceil((double) allRestaurants.size() / pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
    }

    public void loadPage() {
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allRestaurants.size());
        if (start < end) {
            pageRestaurants = allRestaurants.subList(start, end);
        } else {
            pageRestaurants = new ArrayList<>();
        }
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage();
        }
    }

    public void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPage();
        }
    }

    // =========================================================
    // ADD RESTAURANT
    // =========================================================
    public void resetForm() {
        newRestaurant = new Restaurants();
        newCityId = null;
        newAreaId = null;
    }

    /**
     * Ajax khi đổi City trong form Add → reset Area cho chắc chắn.
     */
    public void onNewCityChange() {
        newAreaId = null;
    }

    /**
     * Danh sách Area dùng cho form Add (lọc theo newCityId nếu có chọn city).
     */
    public List<Areas> getAreaOptionsForNew() {
        if (newCityId == null || newCityId == 0) {
            return areaList;
        }
        List<Areas> result = new ArrayList<>();
        for (Areas a : areaList) {
            if (a.getCityId() != null && a.getCityId().getCityId().equals(newCityId)) {
                result.add(a);
            }
        }
        return result;
    }

    public String createRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        boolean hasError = false;

        String name = newRestaurant.getName() != null ? newRestaurant.getName().trim() : "";
        String email = newRestaurant.getEmail() != null ? newRestaurant.getEmail().trim() : "";
        String address = newRestaurant.getAddress() != null ? newRestaurant.getAddress().trim() : "";

        // ===== NAME REQUIRED =====
        if (name.isEmpty()) {
            ctx.addMessage("addRestaurantForm:name",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Name is required", null));
            hasError = true;
        }

        // ===== CITY REQUIRED =====
        if (newCityId == null || newCityId == 0) {
            ctx.addMessage("addRestaurantForm:city",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "City is required", null));
            hasError = true;
        }

        // ===== AREA REQUIRED =====
        if (newAreaId == null || newAreaId == 0) {
            ctx.addMessage("addRestaurantForm:area",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Area is required", null));
            hasError = true;
        }

        // ===== ADDRESS REQUIRED =====
        if (address.isEmpty()) {
            ctx.addMessage("addRestaurantForm:address",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Address is required", null));
            hasError = true;
        }

        // ===== EMAIL DUPLICATE CHECK (nếu có nhập) =====
        if (!email.isEmpty()) {
            boolean emailExists = false;
            for (Restaurants existing : restaurantsFacade.findAll()) {
                if (existing.getEmail() != null
                        && existing.getEmail().equalsIgnoreCase(email)) {
                    emailExists = true;
                    break;
                }
            }
            if (emailExists) {
                ctx.addMessage("addRestaurantForm:email",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Email is already registered for another restaurant", null));
                hasError = true;
            }
        }

        // Gán Area cho nhà hàng (đã validate ở trên)
        if (newAreaId != null && newAreaId != 0) {
            Areas area = areasFacade.find(newAreaId);
            newRestaurant.setAreaId(area);
        } else {
            newRestaurant.setAreaId(null);
        }

        if (hasError) {
            return null;
        }

        try {
            if (newRestaurant.getStatus() == null || newRestaurant.getStatus().trim().isEmpty()) {
                newRestaurant.setStatus("PENDING_APPROVAL");
            }
            newRestaurant.setCreatedAt(new Date());
            newRestaurant.setUpdatedAt(null);

            restaurantsFacade.create(newRestaurant);

            // reload theo filter hiện tại (mặc định ACTIVE)
            loadRestaurants();
            resetForm();

            // đóng modal + show popup success
            ctx.getPartialViewContext().getEvalScripts().add(
                    "document.getElementById('addRestaurantModal').close();"
                    + "document.getElementById('successRestaurantModal').showModal();"
            );

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error when creating restaurant", null));
        }

        return null;
    }

    // =========================================================
    // CHANGE STATUS (ACTIVE <-> INACTIVE)
    // =========================================================
    public void confirmChangeStatus() {
        if (selectedRestaurantId == null) {
            return;
        }

        Restaurants r = restaurantsFacade.find(selectedRestaurantId);
        if (r != null) {
            String status = r.getStatus() != null ? r.getStatus() : "";
            if ("ACTIVE".equals(status)) {
                r.setStatus("INACTIVE");
            } else {
                r.setStatus("ACTIVE");
            }
            r.setUpdatedAt(new Date());
            restaurantsFacade.edit(r);
            loadRestaurants();
        }
    }

    // =========================================================
    // UI HELPERS
    // =========================================================
    public String statusCssClass(String status) {
        if ("ACTIVE".equals(status)) {
            return "bg-green-100 text-green-700";
        }
        if ("PENDING_APPROVAL".equals(status)) {
            return "bg-yellow-100 text-yellow-700";
        }
        if ("INACTIVE".equals(status)) {
            return "bg-gray-100 text-gray-700";
        }
        return "bg-gray-100 text-gray-700";
    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public Integer getCityFilterId() {
        return cityFilterId;
    }

    public void setCityFilterId(Integer cityFilterId) {
        this.cityFilterId = cityFilterId;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public List<Restaurants> getPageRestaurants() {
        return pageRestaurants;
    }

    public Restaurants getNewRestaurant() {
        return newRestaurant;
    }

    public void setNewRestaurant(Restaurants newRestaurant) {
        this.newRestaurant = newRestaurant;
    }

    public Integer getNewCityId() {
        return newCityId;
    }

    public void setNewCityId(Integer newCityId) {
        this.newCityId = newCityId;
    }

    public Integer getNewAreaId() {
        return newAreaId;
    }

    public void setNewAreaId(Integer newAreaId) {
        this.newAreaId = newAreaId;
    }

    public Long getSelectedRestaurantId() {
        return selectedRestaurantId;
    }

    public void setSelectedRestaurantId(Long selectedRestaurantId) {
        this.selectedRestaurantId = selectedRestaurantId;
    }

    public List<Cities> getCityList() {
        return cityList;
    }

    public List<Areas> getAreaList() {
        return areaList;
    }
}
