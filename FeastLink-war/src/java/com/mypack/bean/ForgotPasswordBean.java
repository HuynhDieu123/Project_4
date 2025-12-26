package com.mypack.bean;

import com.mypack.entity.Users;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;

@Named("forgotPasswordBean")
@RequestScoped
public class ForgotPasswordBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String email;
    private String phone; // ✅ SĐT bắt buộc
    private String newPassword;
    private String confirmPassword;

    // ===== GET/SET =====
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    // ===================

    public String resetPassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        String emailNorm = (email == null) ? "" : email.trim().toLowerCase();
        String phoneNorm = normalizePhone(phone);

        // Validate input basic
        if (emailNorm.isEmpty()) {
            ctx.addMessage("forgotForm:email", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Email is required", "Please enter your email."
            ));
            return null;
        }
        if (phoneNorm.isEmpty()) {
            ctx.addMessage("forgotForm:phone", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR, "Phone is required", "Please enter your registered phone number."
            ));
            return null;
        }

        // 1) Find user by email
        Users user = findUserByEmail(emailNorm);

        // Email không tồn tại -> báo chung (không nói email có tồn tại hay không)
        if (user == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid information",
                    "Incorrect email or phone number."
            ));
            return null;
        }

        // 2) Check phone matches with that email
        String dbPhone = normalizePhone(extractUserPhone(user));
        if (dbPhone.isEmpty() || !dbPhone.equals(phoneNorm)) {
            // ✅ Hiện đúng yêu cầu: SĐT không đúng với Gmail đã đăng ký (ngay dưới ô SĐT)
            ctx.addMessage("forgotForm:phone", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "The phone number does not match the registered Gmail address..",
                    "Please enter the correct phone number registered with this email address.."
            ));
            return null;
        }

        // 3) Password strong check
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{7,}$";
        if (newPassword == null || !newPassword.matches(passwordPattern)) {
            ctx.addMessage("forgotForm:newPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Weak password",
                    "Password must be at least 7 characters and include uppercase, lowercase, number and special character."
            ));
            return null;
        }

        // 4) Confirm
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            ctx.addMessage("forgotForm:confirmPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Password confirmation does not match",
                    "Please re-enter your new password."
            ));
            return null;
        }

        // 5) Hash password (deploy-safe)
        try {
            String hashedNewPassword = bcryptHash(newPassword);
            if (hashedNewPassword == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Server configuration error",
                        "BCrypt library is missing on server. Please add jbcrypt jar."
                ));
                return null;
            }

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

    // ===================== Helpers =====================

    private Users findUserByEmail(String emailNorm) {
        if (emailNorm == null || emailNorm.isEmpty()) return null;
        List<Users> all = usersFacade.findAll();
        for (Users u : all) {
            String uEmail = (u.getEmail() == null) ? "" : u.getEmail().trim().toLowerCase();
            if (!uEmail.isEmpty() && uEmail.equals(emailNorm)) return u;
        }
        return null;
    }

    /**
     * Lấy SĐT từ Users entity bằng reflection để tránh lỗi compile nếu getter khác tên.
     * Bạn có thể thêm tên getter của bạn vào mảng candidates nếu cần.
     */
    private String extractUserPhone(Users user) {
        if (user == null) return "";
        String[] candidates = {
                "getPhone", "getPhoneNumber", "getPhonenumber",
                "getPhoneNo", "getMobile", "getMobileNumber"
        };
        for (String mName : candidates) {
            try {
                Method m = user.getClass().getMethod(mName);
                Object v = m.invoke(user);
                if (v != null) return String.valueOf(v).trim();
            } catch (Exception ignore) { }
        }
        return "";
    }

    /**
     * Normalize SĐT: chỉ giữ chữ số, xử lý +84 -> 0
     */
    private String normalizePhone(String p) {
        if (p == null) return "";
        String digits = p.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            // 84xxxxxxxxx -> 0xxxxxxxxx
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    /**
     * BCrypt hash bằng reflection:
     * org.mindrot.jbcrypt.BCrypt.hashpw(raw, BCrypt.gensalt())
     */
    private String bcryptHash(String raw) {
        try {
            Class<?> c = Class.forName("org.mindrot.jbcrypt.BCrypt");
            Method gensalt = c.getMethod("gensalt");
            Method hashpw = c.getMethod("hashpw", String.class, String.class);
            String salt = (String) gensalt.invoke(null);
            return (String) hashpw.invoke(null, raw, salt);
        } catch (Exception e) {
            return null;
        }
    }
}
