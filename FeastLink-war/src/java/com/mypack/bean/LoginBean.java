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

        String input = identifier.trim();

        // 1. Account cứng admin / 123
        if ("admin".equalsIgnoreCase(input) && "123".equals(password)) {
            ctx.getExternalContext().getSessionMap().put("currentUserRole", "ADMIN");
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", input);

            // KHÔNG dùng ?faces-redirect=true để tránh lỗi Flash
            return "/Customer/index";
        }

        // 2. Tìm trong DB theo email hoặc phone
        List<Users> list = usersFacade.findAll();
        Users matched = null;

        for (Users u : list) {
            boolean sameEmail = u.getEmail() != null
                    && input.equalsIgnoreCase(u.getEmail());
            boolean samePhone = u.getPhone() != null
                    && input.equals(u.getPhone());
            boolean samePassword = u.getPassword() != null
                    && password.equals(u.getPassword());

            if ((sameEmail || samePhone) && samePassword) {
                matched = u;
                break;
            }
        }

        if (matched == null) {
            // Sai thông tin -> ở lại trang login
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid credentials",
                    "Email/phone or password is incorrect."
            ));
            return null;
        }

        // 3. Đăng nhập thành công
        ctx.getExternalContext().getSessionMap().put("currentUser", matched);
        // nếu entity Users có field role thì có thể dùng matched.getRole()
        ctx.getExternalContext().getSessionMap().put("currentUserRole", "CUSTOMER");
        ctx.getExternalContext().getSessionMap().put("loginIdentifier", input);

        // Về trang Customer home – KHÔNG redirect
        return "/Customer/index";
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Hủy session hiện tại (xóa luôn currentUser, loginIdentifier, ...)
        ctx.getExternalContext().invalidateSession();

        // Quay về trang login – KHÔNG redirect
        return "login";
    }
}
