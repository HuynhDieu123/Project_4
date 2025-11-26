package com.mypack.bean;

import com.mypack.entity.Users;
import com.mypack.entity.Cities;
import com.mypack.sessionbean.UsersFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.inject.Named;
import jakarta.faces.view.ViewScoped;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.application.FacesMessage;
import jakarta.servlet.http.Part;

import java.io.Serializable;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@Named("profileBean")
@ViewScoped
public class ProfileBean implements Serializable {

    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    private Users currentUser;

    private String fullName;
    private String email;
    private String phone;
    private String address;
    private Integer cityId;

    // upload avatar
    private Part avatarPart;
    // path lưu trong DB, vd: /resources/images/upload_file/avatar_26_1764....png
    private String avatarUrl;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ExternalContext ec = ctx.getExternalContext();
        Object obj = ec.getSessionMap().get("currentUser");

        if (!(obj instanceof Users)) {
            redirectToLogin();
            return;
        }

        Users sessionUser = (Users) obj;
        currentUser = usersFacade.find(sessionUser.getUserId());
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        fullName = currentUser.getFullName();
        email    = currentUser.getEmail();
        phone    = currentUser.getPhone();
        address  = currentUser.getAddress();

        if (currentUser.getCityId() != null) {
            cityId = currentUser.getCityId().getCityId();
        }

        avatarUrl = currentUser.getAvatarUrl();   // có thể null
    }

    private void redirectToLogin() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            ExternalContext ec = ctx.getExternalContext();
            ec.redirect(ec.getRequestContextPath() + "/login.xhtml");
        } catch (IOException ignored) {}
    }

    // ===== GET/SET =====
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getCityId() { return cityId; }
    public void setCityId(Integer cityId) { this.cityId = cityId; }

    public Part getAvatarPart() { return avatarPart; }
    public void setAvatarPart(Part avatarPart) { this.avatarPart = avatarPart; }

    public String getAvatarUrl() { return avatarUrl; }

    // dùng trong xhtml: #{request.contextPath}#{profileBean.avatarSrc}
    public String getAvatarSrc() {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        // nếu DB đã lưu /resources/... thì trả nguyên
        if (avatarUrl.startsWith("/")) {
            return avatarUrl;
        }
        return "/resources/images/upload_file/" + avatarUrl;
    }

    public List<Cities> getCities() {
        return citiesFacade.findAll();
    }

    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) return "FL";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        String first = parts[0];
        String last  = parts[parts.length - 1];
        return (first.substring(0, 1) + last.substring(0, 1)).toUpperCase();
    }

    // ===== ACTIONS =====

    // TRẢ VỀ STRING để redirect lại profile → avatar hiện ngay
    public String saveProfile() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (currentUser == null) {
            redirectToLogin();
            return null;
        }

        currentUser.setFullName(fullName);
        currentUser.setEmail(email);
        currentUser.setPhone(phone);
        currentUser.setAddress(address);

        if (cityId != null) {
            Cities c = citiesFacade.find(cityId);
            currentUser.setCityId(c);
        } else {
            currentUser.setCityId(null);
        }

        // --- UPLOAD AVATAR GIỐNG CODE LOGO CỦA BẠN ---
        if (avatarPart != null && avatarPart.getSize() > 0) {
            try {
                String savedRelativePath = uploadAvatarToDisk(avatarPart, currentUser.getUserId());
                avatarUrl = savedRelativePath;               // cập nhật bean
                currentUser.setAvatarUrl(savedRelativePath); // cập nhật entity
            } catch (IOException ex) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Upload failed",
                        "Could not upload avatar image."
                ));
                return null;
            }
        }

       try {
            usersFacade.edit(currentUser);

            // update lại session cho header
            ctx.getExternalContext().getSessionMap().put("currentUser", currentUser);
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", currentUser.getEmail());

            // THÔNG BÁO THÀNH CÔNG – KHÔNG redirect, nên message hiện ngay
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Profile updated",
                    "Your profile has been saved successfully."
            ));
            

            // redirect lại /Customer/profile.xhtml
            return "/Customer/profile?faces-redirect=true";

        } catch (Exception ex) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Update failed",
                    "An error occurred while saving your profile."
            ));
            return null;
        }
    }

    private String uploadAvatarToDisk(Part filePart, Long userId) throws IOException {
        // giống hệt đoạn upload logo mà bạn gửi
        String uploadRoot = "E:\\ProjectSemIV\\Code\\Project_4\\FeastLink-war\\web\\resources\\images\\upload_file";

        File folder = new File(uploadRoot);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String submitted = filePart.getSubmittedFileName();
        String baseName  = submitted;

        if (submitted != null) {
            int slash = submitted.lastIndexOf('/');
            int back  = submitted.lastIndexOf('\\');
            int idx   = Math.max(slash, back);
            if (idx >= 0 && idx < submitted.length() - 1) {
                baseName = submitted.substring(idx + 1);
            }
        }

        String ext = "";
        int dot = baseName != null ? baseName.lastIndexOf('.') : -1;
        if (dot != -1) {
            ext = baseName.substring(dot);
        }

        String savedName = "avatar_" + userId + "_" + System.currentTimeMillis() + ext;
        File dest = new File(folder, savedName);

        try (InputStream in = filePart.getInputStream();
             FileOutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }

        // lưu trong DB dạng /resources/images/upload_file/...
        String relativePath = "/resources/images/upload_file/" + savedName;
        return relativePath;
    }
}
