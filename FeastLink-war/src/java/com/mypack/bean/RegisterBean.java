package com.mypack.bean;

import com.mypack.entity.Users;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import java.io.Serializable;
import java.util.Date; 
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

@Named("registerBean")
@RequestScoped
public class RegisterBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;

    // ====== GET/SET ======
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
    // ======================

    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // 1) Full name >= 6 ký tự
        String trimmedName = fullName != null ? fullName.trim() : "";
        if (trimmedName.length() < 6) {
            ctx.addMessage("registerForm:fullName", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Full name is too short",
                    "Full name must be at least 6 characters long."
            ));
            return null;
        }

        // 2) Email đúng định dạng
        String trimmedEmail = email != null ? email.trim() : "";
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (trimmedEmail.isEmpty() || !trimmedEmail.matches(emailPattern)) {
            ctx.addMessage("registerForm:email", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid email",
                    "Please enter a valid email address (e.g., name@example.com)."
            ));
            return null;
        }

        // 3) Phone = 10 số (sau khi normalize)
        String phoneNorm = normalizePhone(phone);
        if (!phoneNorm.matches("\\d{10}")) {
            ctx.addMessage("registerForm:phone", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid phone number",
                    "Phone number must contain exactly 10 digits."
            ));
            return null;
        }

        // 4) Password mạnh: >=7, hoa + thường + số + ký tự đặc biệt
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{7,}$";
        if (password == null || !password.matches(passwordPattern)) {
            ctx.addMessage("registerForm:password", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Weak password",
                    "Password must be at least 7 characters and include uppercase, lowercase, number and special character."
            ));
            return null;
        }

        // 5) Confirm password
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            ctx.addMessage("registerForm:confirmPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Password confirmation does not match",
                    "Please re-enter your password."
            ));
            return null;
        }

        // 6) Check trùng Email + trùng Phone (✅ phần bạn cần)
        List<Users> allUsers = usersFacade.findAll();

        for (Users u : allUsers) {
            // --- check email duplicate ---
            String dbEmail = (u.getEmail() == null) ? "" : u.getEmail().trim();
            if (!dbEmail.isEmpty() && trimmedEmail.equalsIgnoreCase(dbEmail)) {
                ctx.addMessage("registerForm:email", new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Email already registered",
                        "This email address is already in use. Please use another email or sign in."
                ));
                return null;
            }

            // --- check phone duplicate (normalize cả 2 bên) ---
            String dbPhoneRaw = extractUserPhone(u); // trong entity bạn có getPhone() rồi thì dùng luôn
            String dbPhoneNorm = normalizePhone(dbPhoneRaw);

            if (!dbPhoneNorm.isEmpty() && phoneNorm.equals(dbPhoneNorm)) {
                ctx.addMessage("registerForm:phone", new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Phone number already registered",
                        "This phone number is already in use. Please use another phone number."
                ));
                return null;
            }
        }

        // 7) Tạo user mới
        try {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            Users user = new Users();
            user.setFullName(trimmedName);
            user.setEmail(trimmedEmail);
            user.setPhone(phoneNorm);              // ✅ lưu phone đã normalize (10 số)
            user.setPassword(hashedPassword);
            user.setRole("CUSTOMER");
            user.setStatus("ACTIVE");
            user.setAvatarUrl(null);
            user.setAddress(null);
            user.setCityId(null);
            user.setCreatedAt(new Date());
            user.setUpdatedAt(null);
            user.setLastLoginAt(null);

            usersFacade.create(user);

            ctx.getExternalContext().getFlash().setKeepMessages(true);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Account created",
                    "Your FeastLink account has been created. Please sign in."
            ));

            return "login?faces-redirect=true";

        } catch (Exception ex) {
            ex.printStackTrace();

            String msg = ex.getMessage();
            if (msg != null && (msg.contains("UNIQUE KEY") || msg.contains("duplicate key"))) {
                // Nếu DB có unique cho Email/Phone → vẫn bắt được ở đây
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Duplicate information",
                        "Email or phone number already exists. Please use another one."
                ));
            } else {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Unable to create account",
                        "Something went wrong while creating your account. Please try again later."
                ));
            }
            return null;
        }
    }

    // ================= Helpers =================

    /**
     * Chuẩn hóa SĐT:
     * - bỏ mọi ký tự không phải số
     * - +84 / 84xxxxxxxxx -> 0xxxxxxxxx
     */
    private String normalizePhone(String p) {
        if (p == null) return "";
        String digits = p.replaceAll("[^0-9]", "");
        if (digits.startsWith("84") && digits.length() >= 11) {
            digits = "0" + digits.substring(2);
        }
        return digits;
    }

    /**
     * Nếu entity Users của bạn đã có getPhone() thì dùng luôn.
     * Mình viết hàm này để chắc chắn không null và không lỗi.
     */
    private String extractUserPhone(Users u) {
        try {
            if (u == null) return "";
            String p = u.getPhone(); // bạn đang dùng user.setPhone(...) nên entity chắc có getPhone()
            return p != null ? p.trim() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
