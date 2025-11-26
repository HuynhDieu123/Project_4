package com.mypack.admin;

import com.mypack.entity.Users;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Named
@ViewScoped
public class UsersBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String keyword = "";
    private String roleFilter = "ALL";
    private String statusFilter = "ALL";

    // Pagination
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages;

    private List<Users> pageUsers;

    // Block action
    private Long selectedUserId;

    // Add user
    private Users newUser = new Users();
    private String newPassword;

    @PostConstruct
    public void init() {
        loadUsers();
    }

    /* LOAD USERS WITH FILTERS + PAGINATION */
    public void loadUsers() {
        int total = usersFacade.countFiltered(keyword, roleFilter, statusFilter);
        totalPages = (int) Math.ceil((double) total / pageSize);

        if (totalPages == 0) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;

        int start = (currentPage - 1) * pageSize;

        this.pageUsers = usersFacade.findFiltered(
                keyword, roleFilter, statusFilter, start, pageSize
        );
    }

    public void applyFilter() {
        currentPage = 1;
        loadUsers();
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadUsers();
        }
    }

    public void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadUsers();
        }
    }

    /* BLOCK / UNBLOCK */
    public void confirmBlock() {
        Users u = usersFacade.find(selectedUserId);
        if (u != null) {
            if ("ACTIVE".equals(u.getStatus())) {
                u.setStatus("BLOCKED");
            } else {
                u.setStatus("ACTIVE");
            }
            usersFacade.edit(u);
        }
        loadUsers();
    }

    /* CREATE USER */
public void createUser() {
    try {
        if (newUser == null) {
            newUser = new Users();
        }

        // Validate fullname
        if (newUser.getFullName() == null || newUser.getFullName().isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Full name is required", null));
            return;
        }

        // Validate email
        if (newUser.getEmail() == null || newUser.getEmail().isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Email is required", null));
            return;
        }

        // Check duplicate email
        if (usersFacade.findByEmail(newUser.getEmail()) != null) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Email already exists", null));
            return;
        }

        // Validate password
        if (newPassword == null || newPassword.isBlank()) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Password is required", null));
            return;
        }

        // Assign required database fields
        newUser.setPassword(newPassword);     // Ideally hash it
        newUser.setStatus("ACTIVE");
        newUser.setCreatedAt(new Date());     // REQUIRED
        newUser.setUpdatedAt(null);

        // Ensure role exists
        if (newUser.getRole() == null || newUser.getRole().isBlank()) {
            newUser.setRole("CUSTOMER");
        }

        // Persist user
        usersFacade.create(newUser);

        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "User created successfully!", null));

        // Reset
        newUser = new Users();
        newPassword = "";

    } catch (Exception e) {
        e.printStackTrace();
        throw e;
    }
}


    /* GETTERS & SETTERS */

    public List<Users> getPageUsers() { return pageUsers; }

    public int getCurrentPage() { return currentPage; }

    public int getTotalPages() { return totalPages; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getRoleFilter() { return roleFilter; }
    public void setRoleFilter(String roleFilter) { this.roleFilter = roleFilter; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public Long getSelectedUserId() { return selectedUserId; }
    public void setSelectedUserId(Long selectedUserId) { this.selectedUserId = selectedUserId; }

    public Users getNewUser() { return newUser; }
    public void setNewUser(Users newUser) { this.newUser = newUser; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
