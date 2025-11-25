package com.mypack.bean;

import com.mypack.entity.Users;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import java.io.Serializable;
import java.util.List;

@Named("loginBean")
@RequestScoped
public class LoginBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String identifier;   // email hoặc phone
    private String password;
    private boolean rememberMe;

    // ====== GET/SET ======
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
    // =====================

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 0. Validate input đơn giản
        if (identifier == null || identifier.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Missing information",
                    "Please enter your email/phone and password."
            ));
            return null;   // ở lại login.xhtml
        }

        // 1. Account cứng admin / 123
        if ("admin".equalsIgnoreCase(identifier) && "123".equals(password)) {
            ctx.getExternalContext().getSessionMap().put("currentUserRole", "ADMIN");

            // Lưu thông tin để header hiển thị tên/email
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", identifier);

            // Trang sau khi đăng nhập admin (tạm cho về Customer/index)
            return "/Customer/index?faces-redirect=true";
        }

        // 2. Tìm trong DB theo email hoặc phone
        List<Users> list = usersFacade.findAll();
        Users matched = null;

        for (Users u : list) {
            boolean sameEmail = u.getEmail() != null
                    && identifier.equalsIgnoreCase(u.getEmail());
            boolean samePhone = u.getPhone() != null
                    && identifier.equals(u.getPhone());
            boolean samePassword = u.getPassword() != null
                    && password.equals(u.getPassword());

            if ((sameEmail || samePhone) && samePassword) {
                matched = u;
                break;
            }
        }

        if (matched == null) {
            // Sai thông tin -> ở lại trang login, KHÔNG redirect
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid credentials",
                    "Email/phone or password is incorrect."
            ));
            return null;
        }

        // 3. Đăng nhập thành công
        ctx.getExternalContext().getSessionMap().put("currentUser", matched);
        ctx.getExternalContext().getSessionMap().put("currentUserRole", "CUSTOMER");

        // Dùng chuỗi người dùng nhập (email/phone) để hiển thị trên header
        ctx.getExternalContext().getSessionMap().put("loginIdentifier", identifier);

        // Sau khi login thành công, chuyển sang trang bạn muốn
        return "/Customer/index?faces-redirect=true";
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Hủy session hiện tại (xóa luôn currentUser, loginIdentifier, ...)
        ctx.getExternalContext().invalidateSession();

        // Quay về trang login
        return "login?faces-redirect=true";
    }
}
