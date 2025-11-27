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

// Thêm import BCrypt
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

        // Lưu ý: field rỗng đã bị chặn bởi required="true" trong xhtml

        // 1. Full name >= 6 ký tự
        String trimmedName = fullName != null ? fullName.trim() : "";
        if (trimmedName.length() < 6) {
            ctx.addMessage("registerForm:fullName", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Full name is too short",
                    "Full name must be at least 6 characters long."
            ));
            return null;
        }

        // 2. Phone = 10 số
        String trimmedPhone = phone != null ? phone.trim() : "";
        if (!trimmedPhone.matches("\\d{10}")) {
            ctx.addMessage("registerForm:phone", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Invalid phone number",
                    "Phone number must contain exactly 10 digits."
            ));
            return null;
        }

        // 3. Password mạnh: >=7, hoa + thường + số + ký tự đặc biệt
        String passwordPattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{7,}$";
        if (password == null || !password.matches(passwordPattern)) {
            ctx.addMessage("registerForm:password", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Weak password",
                    "Password must be at least 7 characters and include uppercase, lowercase, number and special character."
            ));
            return null;
        }

        // 4. Confirm password
        if (confirmPassword == null || !password.equals(confirmPassword)) {
            ctx.addMessage("registerForm:confirmPassword", new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Password confirmation does not match",
                    "Please re-enter your password."
            ));
            return null;
        }

        // 5. Check email trùng (tránh lỗi UNIQUE KEY)
        List<Users> allUsers = usersFacade.findAll();
        for (Users u : allUsers) {
            if (u.getEmail() != null &&
                email != null &&
                email.equalsIgnoreCase(u.getEmail())) {

                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Email already registered",
                        "This email address is already in use. Please use another email or sign in."
                ));
                return null;
            }
        }

        // 6. Tạo user mới
        try {
            // Hash password bằng BCrypt trước khi lưu
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            Users user = new Users();
            user.setFullName(fullName);
            user.setEmail(email);
            user.setPhone(phone);
            user.setPassword(hashedPassword);  // <-- dùng hashed password
            user.setRole("CUSTOMER");
            user.setStatus("ACTIVE");
            user.setAvatarUrl(null);
            user.setAddress(null);
            user.setCityId(null);          // nếu bạn có CityId thì sau này set sau
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
            if (msg != null &&
                (msg.contains("UNIQUE KEY") || msg.contains("duplicate key"))) {

                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Email already registered",
                        "This email address is already in use. Please use another email or sign in."
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
}
