package com.mypack.admin;

import com.mypack.entity.Users;
import com.mypack.entity.Cities;
import com.mypack.sessionbean.UsersFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;

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

import org.mindrot.jbcrypt.BCrypt;

@Named("usersBean")
@SessionScoped
public class UsersBean implements Serializable {

    private static final long serialVersionUID = 1L;

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
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();

        for (Users u : usersFacade.findAll()) {

            boolean matchKeyword = kw.isEmpty()
                    || (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw))
                    || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw));

            boolean matchRole = "ALL".equals(roleFilter) || (u.getRole() != null && roleFilter.equals(u.getRole()));
            boolean matchStatus = "ALL".equals(statusFilter) || (u.getStatus() != null && statusFilter.equals(u.getStatus()));

            if (matchKeyword && matchRole && matchStatus) {
                filtered.add(u);
            }
        }

        allUsers = filtered;
        currentPage = 1;

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

        if (start < end) {
            pageUsers = allUsers.subList(start, end);
        } else {
            pageUsers = new ArrayList<>();
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

    // ===============================================
    // ADD USER
    // ===============================================
    public void resetForm() {
        newUser = new Users();
        newPassword = "";
        newCityId = null;
    }

    public String createUser() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        boolean hasError = false;

        String fullName = newUser.getFullName() != null ? newUser.getFullName().trim() : "";
        String email = newUser.getEmail() != null ? newUser.getEmail().trim() : "";
        String password = newPassword != null ? newPassword.trim() : "";
        String role = newUser.getRole() != null ? newUser.getRole().trim() : "";

        if (fullName.isEmpty()) {
            ctx.addMessage("fullName",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Full name is required.", null));
            hasError = true;
        }

        if (email.isEmpty()) {
            ctx.addMessage("email",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is required.", null));
            hasError = true;
        } else {
            boolean emailExists = false;
            for (Users existing : usersFacade.findAll()) {
                if (existing.getEmail() != null && existing.getEmail().equalsIgnoreCase(email)) {
                    emailExists = true;
                    break;
                }
            }
            if (emailExists) {
                ctx.addMessage("email",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is already registered.", null));
                hasError = true;
            }
        }

        if (password.isEmpty()) {
            ctx.addMessage("password",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Password is required.", null));
            hasError = true;
        } else {
            String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$";
            if (!password.matches(regex)) {
                ctx.addMessage("password",
                        new FacesMessage(FacesMessage.SEVERITY_ERROR,
                                "Password must be at least 6 characters and include upper, lower, number, and special character.",
                                null));
                hasError = true;
            }
        }

        if (role.isEmpty()) {
            ctx.addMessage("role",
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Please select a role.", null));
            hasError = true;
        }

        if (hasError) return null;

        try {
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            newUser.setPassword(hashed);

            newUser.setStatus("ACTIVE");
            newUser.setCreatedAt(new Date());
            newUser.setUpdatedAt(null);

            if (newCityId != null && newCityId != 0) {
                Cities c = citiesFacade.find(newCityId);
                newUser.setCityId(c);
            } else {
                newUser.setCityId(null);
            }

            usersFacade.create(newUser);

            loadUsers();
            resetForm();

            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "User created successfully!", null));
            ctx.getPartialViewContext().getEvalScripts().add(
                    "document.getElementById('addUserModal').close();"
                    + "document.getElementById('successModal').showModal();"
            );

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error while creating user.", null));
        }

        return null;
    }

    // ===============================================
    // BLOCK / UNBLOCK USER
    // ===============================================
    public void confirmBlock() {
        if (selectedUserId == null) return;

        FacesContext ctx = FacesContext.getCurrentInstance();
        Users u = usersFacade.find(selectedUserId);

        if (u != null) {
            if ("ACTIVE".equals(u.getStatus())) {
                u.setStatus("BLOCKED");
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_WARN, "User has been blocked.", null));
            } else {
                u.setStatus("ACTIVE");
                ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "User has been unblocked.", null));
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
