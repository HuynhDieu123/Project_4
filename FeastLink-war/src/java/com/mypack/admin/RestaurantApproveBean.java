package com.mypack.admin;

import com.mypack.entity.Restaurants;
import com.mypack.entity.Cities;
import com.mypack.entity.Areas;
import com.mypack.entity.Users;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;
import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("restaurantApproveBean")
@ViewScoped
public class RestaurantApproveBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    @EJB
    private AreasFacadeLocal areasFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    // ===== FILTERS (dùng chung cho cả 2 bảng) =====
    private String keyword = "";
    private Integer cityFilterId;

    // ===== DATA: RESTAURANTS =====
    private List<Restaurants> pendingRestaurants = new ArrayList<>();
    private List<Restaurants> pageRestaurants = new ArrayList<>();
    private int restaurantPageSize = 10;
    private int restaurantCurrentPage = 1;
    private int restaurantTotalPages = 1;

    // ===== DATA: MANAGER USERS =====
    private List<Users> pendingManagerUsers = new ArrayList<>();
    private List<Users> pageManagerUsers = new ArrayList<>();
    private int managerPageSize = 10;
    private int managerCurrentPage = 1;
    private int managerTotalPages = 1;

    // CITY LIST
    private List<Cities> cityList = new ArrayList<>();

    // ===== SELECTED =====
    private Long selectedRestaurantId;
    private Long selectedUserId;

    // =================================================
    // INIT
    // =================================================
    @PostConstruct
    public void init() {
        cityList = citiesFacade.findAll();
        loadPending();
    }

    // =================================================
    // LOAD & FILTER (CẢ 2 BẢNG)
    // =================================================
    public void loadPending() {
        // luôn reset về trang 1 khi lọc lại
        restaurantCurrentPage = 1;
        managerCurrentPage = 1;

        loadPendingRestaurants();
        loadPendingManagers();

        calculateRestaurantPagination();
        loadRestaurantPage();

        calculateManagerPagination();
        loadManagerPage();
    }

    private void loadPendingRestaurants() {
        List<Restaurants> source = restaurantsFacade.findAll();
        List<Restaurants> filtered = new ArrayList<>();

        for (Restaurants r : source) {

            // Chỉ lấy nhà hàng đang PENDING_APPROVAL
            if (!"PENDING_APPROVAL".equals(r.getStatus())) {
                continue;
            }

            // keyword: name / email / phone / contact
            boolean matchKeyword = (keyword == null || keyword.isEmpty());
            if (!matchKeyword) {
                String kw = keyword.toLowerCase();
                matchKeyword =
                        (r.getName() != null && r.getName().toLowerCase().contains(kw)) ||
                        (r.getEmail() != null && r.getEmail().toLowerCase().contains(kw)) ||
                        (r.getPhone() != null && r.getPhone().toLowerCase().contains(kw)) ||
                        (r.getContactPerson() != null && r.getContactPerson().toLowerCase().contains(kw));
            }

            // city filter (Area -> City)
            boolean matchCity = (cityFilterId == null || cityFilterId == 0);
            if (!matchCity) {
                Areas area = r.getAreaId();
                if (area != null && area.getCityId() != null) {
                    matchCity = cityFilterId.equals(area.getCityId().getCityId());
                }
            }

            if (matchKeyword && matchCity) {
                filtered.add(r);
            }
        }

        pendingRestaurants = filtered;
    }

    private void loadPendingManagers() {
        List<Users> source = usersFacade.findAll();
        List<Users> filtered = new ArrayList<>();

        for (Users u : source) {
            // ⭐ Chỉ lấy user có status = PENDING và role = CUSTOMER
            if (!"PENDING".equals(u.getStatus())) {
                continue;
            }
            if (!"CUSTOMER".equals(u.getRole())) {
                continue;
            }

            // keyword: fullName / email / phone
            boolean matchKeyword = (keyword == null || keyword.isEmpty());
            if (!matchKeyword) {
                String kw = keyword.toLowerCase();
                matchKeyword =
                        (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)) ||
                        (u.getPhone() != null && u.getPhone().toLowerCase().contains(kw));
            }

            // city filter (Users.cityId)
            boolean matchCity = (cityFilterId == null || cityFilterId == 0);
            if (!matchCity) {
                Cities c = u.getCityId();
                if (c != null) {
                    matchCity = cityFilterId.equals(c.getCityId());
                }
            }

            if (matchKeyword && matchCity) {
                filtered.add(u);
            }
        }

        pendingManagerUsers = filtered;
    }

    public void resetFilter() {
        keyword = "";
        cityFilterId = null;
        loadPending();
    }

    // =================================================
    // PAGINATION - RESTAURANTS
    // =================================================
    private void calculateRestaurantPagination() {
        if (pendingRestaurants == null || pendingRestaurants.isEmpty()) {
            restaurantTotalPages = 1;
            if (restaurantCurrentPage > 1) {
                restaurantCurrentPage = 1;
            }
            return;
        }

        restaurantTotalPages =
                (int) Math.ceil((double) pendingRestaurants.size() / restaurantPageSize);

        if (restaurantTotalPages < 1) {
            restaurantTotalPages = 1;
        }
        if (restaurantCurrentPage > restaurantTotalPages) {
            restaurantCurrentPage = restaurantTotalPages;
        }
    }

    public void loadRestaurantPage() {
        if (pendingRestaurants == null || pendingRestaurants.isEmpty()) {
            pageRestaurants = new ArrayList<>();
            return;
        }

        int start = (restaurantCurrentPage - 1) * restaurantPageSize;
        int end = Math.min(start + restaurantPageSize, pendingRestaurants.size());

        if (start < end) {
            pageRestaurants = pendingRestaurants.subList(start, end);
        } else {
            pageRestaurants = new ArrayList<>();
        }
    }

    public void nextRestaurantPage() {
        if (restaurantCurrentPage < restaurantTotalPages) {
            restaurantCurrentPage++;
            loadRestaurantPage();
        }
    }

    public void prevRestaurantPage() {
        if (restaurantCurrentPage > 1) {
            restaurantCurrentPage--;
            loadRestaurantPage();
        }
    }

    // =================================================
    // PAGINATION - MANAGERS
    // =================================================
    private void calculateManagerPagination() {
        if (pendingManagerUsers == null || pendingManagerUsers.isEmpty()) {
            managerTotalPages = 1;
            if (managerCurrentPage > 1) {
                managerCurrentPage = 1;
            }
            return;
        }

        managerTotalPages =
                (int) Math.ceil((double) pendingManagerUsers.size() / managerPageSize);

        if (managerTotalPages < 1) {
            managerTotalPages = 1;
        }
        if (managerCurrentPage > managerTotalPages) {
            managerCurrentPage = managerTotalPages;
        }
    }

    public void loadManagerPage() {
        if (pendingManagerUsers == null || pendingManagerUsers.isEmpty()) {
            pageManagerUsers = new ArrayList<>();
            return;
        }

        int start = (managerCurrentPage - 1) * managerPageSize;
        int end = Math.min(start + managerPageSize, pendingManagerUsers.size());

        if (start < end) {
            pageManagerUsers = pendingManagerUsers.subList(start, end);
        } else {
            pageManagerUsers = new ArrayList<>();
        }
    }

    public void nextManagerPage() {
        if (managerCurrentPage < managerTotalPages) {
            managerCurrentPage++;
            loadManagerPage();
        }
    }

    public void prevManagerPage() {
        if (managerCurrentPage > 1) {
            managerCurrentPage--;
            loadManagerPage();
        }
    }

    // =================================================
    // ACTIONS: APPROVE / REJECT RESTAURANT
    // =================================================
    public void approveRestaurant() {
        changeRestaurantStatus("ACTIVE", "Restaurant approved successfully");
    }

    public void rejectRestaurant() {
        changeRestaurantStatus("INACTIVE", "Restaurant rejected");
    }

    private void changeRestaurantStatus(String newStatus, String successMessage) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (selectedRestaurantId == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "No restaurant selected", null));
            return;
        }

        try {
            Restaurants r = restaurantsFacade.find(selectedRestaurantId);
            if (r == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Restaurant not found", null));
                return;
            }

            r.setStatus(newStatus);
            r.setUpdatedAt(new Date());
            restaurantsFacade.edit(r);

            loadPending(); // reload + reset phân trang

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    successMessage, null));

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error when updating restaurant status", null));
        }
    }

    // =================================================
    // ACTIONS: APPROVE / REJECT MANAGER UPGRADE
    // =================================================
    public void approveManager() {
        // CUSTOMER (PENDING) -> MANAGER / ACTIVE
        changeManagerStatus("MANAGER", "ACTIVE",
                "Manager upgrade approved successfully");
    }

    public void rejectManager() {
        // Trả về CUSTOMER / ACTIVE
        changeManagerStatus("CUSTOMER", "ACTIVE",
                "Manager upgrade request rejected");
    }

    private void changeManagerStatus(String newRole, String newStatus, String successMessage) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (selectedUserId == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "No user selected", null));
            return;
        }

        try {
            Users u = usersFacade.find(selectedUserId);
            if (u == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "User not found", null));
                return;
            }

            u.setRole(newRole);
            u.setStatus(newStatus);
            u.setUpdatedAt(new Date());
            usersFacade.edit(u);

            loadPending(); // reload + reset phân trang

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    successMessage, null));

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Error when updating user status", null));
        }
    }

    // =================================================
    // UI HELPERS
    // =================================================
    public String statusCssClass(String status) {
        if ("PENDING_APPROVAL".equals(status) || "PENDING".equals(status)) {
            return "bg-yellow-100 text-yellow-700";
        }
        if ("ACTIVE".equals(status)) return "bg-green-100 text-green-700";
        if ("INACTIVE".equals(status)) return "bg-gray-100 text-gray-700";
        return "bg-gray-100 text-gray-700";
    }

    public String statusLabel(String status) {
        if ("PENDING_APPROVAL".equals(status)) {
            return "PENDING";
        }
        if ("PENDING".equals(status)) {
            return "PENDING";
        }
        return status;
    }

    // =================================================
    // GETTERS / SETTERS
    // =================================================
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Integer getCityFilterId() { return cityFilterId; }
    public void setCityFilterId(Integer cityFilterId) { this.cityFilterId = cityFilterId; }

    public List<Cities> getCityList() { return cityList; }

    public List<Restaurants> getPendingRestaurants() { return pendingRestaurants; }
    public List<Restaurants> getPageRestaurants() { return pageRestaurants; }

    public List<Users> getPendingManagerUsers() { return pendingManagerUsers; }
    public List<Users> getPageManagerUsers() { return pageManagerUsers; }

    public int getRestaurantCurrentPage() { return restaurantCurrentPage; }
    public int getRestaurantTotalPages() { return restaurantTotalPages; }

    public int getManagerCurrentPage() { return managerCurrentPage; }
    public int getManagerTotalPages() { return managerTotalPages; }

    public Long getSelectedRestaurantId() { return selectedRestaurantId; }
    public void setSelectedRestaurantId(Long selectedRestaurantId) { this.selectedRestaurantId = selectedRestaurantId; }

    public Long getSelectedUserId() { return selectedUserId; }
    public void setSelectedUserId(Long selectedUserId) { this.selectedUserId = selectedUserId; }
}
