package com.mypack.bean;

import com.mypack.entity.Users;
import com.mypack.sessionbean.UsersFacadeLocal;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.mindrot.jbcrypt.BCrypt;

@Named("loginBean")
@RequestScoped
public class LoginBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String identifier;   // email or phone
    private String password;
    private boolean rememberMe;

    // ✅ OAuth2 Web Client (Google Console)
    private static final String GOOGLE_CLIENT_ID =
            "718278911834-kbcfe7nbb79b263qdq0ha1vscven73kb.apps.googleusercontent.com";

    // ✅ MUST match Google Console exactly
    private static final String GOOGLE_REDIRECT_URI =
            "http://localhost:8082/FeastLink-war/google-oauth-callback";

    // ====== GET/SET ======
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public boolean isRememberMe() { return rememberMe; }
    public void setRememberMe(boolean rememberMe) { this.rememberMe = rememberMe; }
    // =====================

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // validate
        if (identifier == null || identifier.trim().isEmpty()
                || password == null || password.trim().isEmpty()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Missing information",
                    "Please enter your email/phone and password."
            ));
            return null;
        }

        String input = identifier.trim();

        // admin hardcode
        if ("admin".equalsIgnoreCase(input) && "123".equals(password)) {
            ctx.getExternalContext().getSessionMap().put("currentUserRole", "ADMIN");
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", input);
            return "/Admin/dashboard?faces-redirect=true";
        }

        // find by email/phone
        List<Users> list = usersFacade.findAll();
        Users matched = null;

        for (Users u : list) {
            boolean sameEmail = u.getEmail() != null && input.equalsIgnoreCase(u.getEmail());
            boolean samePhone = u.getPhone() != null && input.equals(u.getPhone());
            if (!(sameEmail || samePhone)) continue;

            boolean samePassword = false;
            String storedPassword = u.getPassword();

            if (storedPassword != null) {
                if (storedPassword.startsWith("$2a$") ||
                    storedPassword.startsWith("$2b$") ||
                    storedPassword.startsWith("$2y$")) {
                    try { samePassword = BCrypt.checkpw(password, storedPassword); }
                    catch (Exception ignore) { samePassword = false; }
                } else {
                    samePassword = password.equals(storedPassword);
                }
            }

            if (samePassword) { matched = u; break; }
        }

        if (matched == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid credentials",
                    "Email/phone or password is incorrect."
            ));
            return null;
        }

        // status check
        if (matched.getStatus() != null && !"ACTIVE".equalsIgnoreCase(matched.getStatus())) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Account is not active",
                    "Your account is currently " + matched.getStatus() + "."
            ));
            return null;
        }

        // update last login
        try { matched.setLastLoginAt(new Date()); usersFacade.edit(matched); } catch (Exception ignore) {}

        // session
        ctx.getExternalContext().getSessionMap().put("currentUser", matched);
        ctx.getExternalContext().getSessionMap().put("loginIdentifier", input);

        String role = matched.getRole();
        if (role != null && "MANAGER".equalsIgnoreCase(role)) {
            ctx.getExternalContext().getSessionMap().put("currentUserRole", "MANAGER");
            return "/Restaurant/dashboard?faces-redirect=true";
        }

        ctx.getExternalContext().getSessionMap().put("currentUserRole", "CUSTOMER");
        return "/Customer/index?faces-redirect=true";
    }

    // ✅ Redirect to Google OAuth2 consent screen
    public String loginWithGoogle() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ExternalContext ec = ctx.getExternalContext();

        try {
            String state = UUID.randomUUID().toString();
            ec.getSessionMap().put("google_oauth_state", state);

            String authUrl =
                    "https://accounts.google.com/o/oauth2/v2/auth"
                            + "?client_id=" + enc(GOOGLE_CLIENT_ID)
                            + "&redirect_uri=" + enc(GOOGLE_REDIRECT_URI)
                            + "&response_type=code"
                            + "&scope=" + enc("openid email profile")
                            + "&prompt=select_account"
                            + "&state=" + enc(state);

            ec.redirect(authUrl);
            ctx.responseComplete();
            return null;

        } catch (Exception e) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Google login failed",
                    "Unable to redirect to Google."
            ));
            return null;
        }
    }

    public String logout() {
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        return "/Customer/index?faces-redirect=true";
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
