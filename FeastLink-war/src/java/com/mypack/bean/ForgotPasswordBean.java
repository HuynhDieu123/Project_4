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

// Thêm import BCrypt
import org.mindrot.jbcrypt.BCrypt;

@Named("forgotPasswordBean")
@RequestScoped
public class ForgotPasswordBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String email;
    private String newPassword;
    private String confirmPassword;

    // ====== GET/SET ======
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
    // =====================

    public String resetPassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Field rỗng đã bị required="true" chặn trong xhtml

        // 1. Tìm user theo email
        Users user = null;
        List<Users> all = usersFacade.findAll();
        if (email != null) {
            for (Users u : all) {
                if (u.getEmail() != null &&
                    email.equalsIgnoreCase(u.getEmail())) {
                    user = u;
                    break;
                }
            }
        }

        if (user == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Email not found",
                    "We could not find an account with this email address."
            ));
            return null;
        }

        // 2. Check password mạnh (giống Register)
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{7,}$";
        if (newPassword == null || !newPassword.matches(passwordPattern)) {
            ctx.addMessage("forgotForm:newPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Weak password",
                    "Password must be at least 7 characters and include uppercase, lowercase, number and special character."
            ));
            return null;
        }

        // 3. Confirm new password
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            ctx.addMessage("forgotForm:confirmPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Password confirmation does not match",
                    "Please re-enter your new password."
            ));
            return null;
        }

        // 4. Cập nhật mật khẩu (hash BCrypt)
        try {
            String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            user.setPassword(hashedNewPassword);
            usersFacade.edit(user);

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Password updated",
                    "Your password has been successfully changed. Please sign in with your new password."
            ));

            return "login?faces-redirect=true";

        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Unable to update password",
                    "Something went wrong while updating your password. Please try again later."
            ));
            return null;
        }
    }
}
