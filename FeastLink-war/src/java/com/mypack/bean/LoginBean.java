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

        // 1. Account cứng admin / 123
        if ("admin".equalsIgnoreCase(identifier) && "123".equals(password)) {
            ctx.getExternalContext().getSessionMap().put("currentUserRole", "ADMIN");
            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Welcome, admin",
                    "You have signed in with administrator privileges."
            ));
            // TODO: đổi "index" thành trang chính của bạn
            return "index?faces-redirect=true";
        }
        

        // 2. Tìm trong DB theo email hoặc phone
        List<Users> list = usersFacade.findAll();
        Users matched = null;
        if (identifier != null && password != null) {
            for (Users u : list) {
                boolean sameEmail = u.getEmail() != null &&
                        identifier.equalsIgnoreCase(u.getEmail());
                boolean samePhone = u.getPhone() != null &&
                        identifier.equals(u.getPhone());
                if ((sameEmail || samePhone) && password.equals(u.getPassword())) {
                    matched = u;
                    break;
                }
            }
        }

        if (matched == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid credentials",
                    "Email/phone or password is incorrect."
            ));
            return null;
        }

        // 3. Đăng nhập thành công
        ctx.getExternalContext().getSessionMap().put("currentUser", matched);
        ctx.getExternalContext().getFlash().setKeepMessages(true);
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Welcome back",
                "You have successfully signed in."
        ));

        // TODO: đổi "index" thành trang chính của bạn
        return "register_manager?faces-redirect=true";
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().invalidateSession();
        ctx.getExternalContext().getFlash().setKeepMessages(true);
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                "Signed out",
                "You have been signed out of FeastLink."
        ));
        return "login?faces-redirect=true";
    }
}
