package com.mypack.admin;

import com.mypack.entity.Users;
import com.mypack.entity.Cities;
import com.mypack.sessionbean.UsersFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import java.io.Serializable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

@Named("usersBean")
@SessionScoped
public class UsersBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    // FILTERS
    private String keyword = "";
    private String roleFilter = "ALL";
    private String statusFilter = "ALL";

    // PAGINATION
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages;

    // ADD USER
    private Users newUser = new Users();
    private String newPassword;
    private Integer newCityId;

    // BLOCK / UNBLOCK
    private Long selectedUserId;

    // DATA
    private List<Users> allUsers = new ArrayList<>();
    private List<Users> pageUsers = new ArrayList<>();
    private List<Cities> cityList = new ArrayList<>();


    // ===============================================
    // INIT
    // ===============================================
    @PostConstruct
    public void init() {
        loadCities();
        loadUsers();
    }


    public void loadCities() {
        cityList = citiesFacade.findAll();
    }


    public void loadUsers() {
        allUsers = usersFacade.findAll();
        applyFilter();
    }


    // ===============================================
    // FILTERING
    // ===============================================
    public void applyFilter() {

        List<Users> filtered = new ArrayList<>();

        for (Users u : usersFacade.findAll()) {

            boolean matchKeyword = keyword == null || keyword.isEmpty()
                    || u.getFullName().toLowerCase().contains(keyword.toLowerCase())
                    || u.getEmail().toLowerCase().contains(keyword.toLowerCase());

            boolean matchRole = roleFilter.equals("ALL") || u.getRole().equals(roleFilter);

            boolean matchStatus = statusFilter.equals("ALL") || u.getStatus().equals(statusFilter);

            if (matchKeyword && matchRole && matchStatus) {
                filtered.add(u);
            }
        }

        allUsers = filtered;

        calculatePagination();
        loadPage();
    }


    // ===============================================
    // PAGINATION
    // ===============================================
    private void calculatePagination() {
        totalPages = (int) Math.ceil((double) allUsers.size() / pageSize);
        if (totalPages < 1) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
    }


    public void loadPage() {
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allUsers.size());

        if (start < end)
            pageUsers = allUsers.subList(start, end);
        else
            pageUsers = new ArrayList<>();
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


    // ===============================================
    // ADD USER
    // ===============================================
    public void resetForm() {
        newUser = new Users();
        newPassword = "";
        newCityId = null;
    }

    public String createUser() {

        try {

            // ==========================
            // HASH PASSWORD (BCrypt)
            // ==========================
            String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(12));
            newUser.setPassword(hashed);

            // Default fields
            newUser.setStatus("ACTIVE");
            newUser.setCreatedAt( new Date());
            newUser.setUpdatedAt(null);

            // CITY
            if (newCityId != null && newCityId != 0) {
                Cities c = citiesFacade.find(newCityId);
                newUser.setCityId(c);
            }

            usersFacade.create(newUser);

            // REFRESH DATA
            loadUsers();

            // Reset form
            resetForm();

            // Show popup success
            FacesContext.getCurrentInstance().getPartialViewContext()
                    .getEvalScripts().add("successModal.showModal()");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    // ===============================================
    // BLOCK / UNBLOCK USER
    // ===============================================
    public void confirmBlock() {
        if (selectedUserId == null)
            return;

        Users u = usersFacade.find(selectedUserId);

        if (u != null) {

            if ("ACTIVE".equals(u.getStatus())) {
                u.setStatus("BLOCKED");
            } else {
                u.setStatus("ACTIVE");
            }

            u.setUpdatedAt(new Date());
            usersFacade.edit(u);

            loadUsers();
        }
    }


    // ===============================================
    // GETTERS & SETTERS
    // ===============================================
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getRoleFilter() { return roleFilter; }
    public void setRoleFilter(String roleFilter) { this.roleFilter = roleFilter; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }

    public List<Users> getPageUsers() { return pageUsers; }

    public Users getNewUser() { return newUser; }
    public void setNewUser(Users newUser) { this.newUser = newUser; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public Integer getNewCityId() { return newCityId; }
    public void setNewCityId(Integer newCityId) { this.newCityId = newCityId; }

    public Long getSelectedUserId() { return selectedUserId; }
    public void setSelectedUserId(Long selectedUserId) { this.selectedUserId = selectedUserId; }

    public List<Cities> getCityList() { return cityList; }

}
