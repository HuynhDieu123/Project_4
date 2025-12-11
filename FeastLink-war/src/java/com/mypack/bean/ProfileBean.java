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
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.Serializable;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
    // URL lưu trong DB (có thể là Cloudinary URL, hoặc đường dẫn /resources/... cũ)
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

        avatarUrl = currentUser.getAvatarUrl();   // có thể null (chưa upload)
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
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    // dùng trong xhtml: img src="#{profileBean.avatarSrc}"
    public String getAvatarSrc() {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        String url = avatarUrl.trim();

        // Nếu đã là URL tuyệt đối (Cloudinary)
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }

        // Nếu là đường dẫn tương đối /resources/... → thêm contextPath
        String ctxPath = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestContextPath();
        return ctxPath + url;
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

        // --- UPLOAD AVATAR LÊN CLOUDINARY (giống logic logo, nhưng folder khác) ---
        if (avatarPart != null && avatarPart.getSize() > 0) {
            String secureUrl = uploadAvatarToCloudinary(ctx, avatarPart, currentUser.getUserId());
            if (secureUrl == null) {
                // đã có message lỗi cụ thể, không save DB
                return null;
            }
            avatarUrl = secureUrl;
            currentUser.setAvatarUrl(secureUrl);
        }

        try {
            usersFacade.edit(currentUser);

            // update lại session cho header
            ctx.getExternalContext().getSessionMap().put("currentUser", currentUser);
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", currentUser.getEmail());

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

    /**
     * Upload avatar lên Cloudinary (DEV: tắt kiểm tra SSL để tránh PKIX error như lúc nãy)
     */
    private String uploadAvatarToCloudinary(FacesContext ctx, Part filePart, Long userId) {
        try {
            // ====== 1. TẠM THỜI TẮT CHECK SSL (CHỈ CHO MÔI TRƯỜNG DEV / ĐỒ ÁN) ======
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            // ====== HẾT PHẦN TẮT SSL CHECK ======

            ServletContext servletContext =
                    (ServletContext) ctx.getExternalContext().getContext();

            String cloudName = servletContext.getInitParameter("cloudinary.cloud_name");
            String apiKey    = servletContext.getInitParameter("cloudinary.api_key");
            String apiSecret = servletContext.getInitParameter("cloudinary.api_secret");

            if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cloudinary configuration is missing. Please contact FeastLink Admin.",
                        null
                ));
                return null;
            }

            // Folder riêng cho avatar user
            String folder   = "feastlink/avatars";
            long timestamp  = System.currentTimeMillis() / 1000L;
            String publicId = "avatar_user_" + userId + "_" + timestamp;

            String toSign = "folder=" + folder
                    + "&public_id=" + publicId
                    + "&timestamp=" + timestamp
                    + apiSecret;

            String signature = sha1Hex(toSign);

            String urlStr = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
            URL url = new URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String boundary = "----FeastLinkProfileBoundary" + System.currentTimeMillis();
            String CRLF = "\r\n";

            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream();
                 OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

                // helper: field text
                java.util.function.BiConsumer<String, String> writeField = (name, value) -> {
                    try {
                        osw.write("--" + boundary + CRLF);
                        osw.write("Content-Disposition: form-data; name=\"" + name + "\"" + CRLF);
                        osw.write(CRLF);
                        osw.write(value + CRLF);
                        osw.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                };

                writeField.accept("api_key", apiKey);
                writeField.accept("timestamp", String.valueOf(timestamp));
                writeField.accept("signature", signature);
                writeField.accept("folder", folder);
                writeField.accept("public_id", publicId);

                String submitted = filePart.getSubmittedFileName();
                String fileName = (submitted != null && !submitted.isEmpty())
                        ? submitted
                        : ("avatar_" + userId + ".png");
                String contentType = filePart.getContentType();
                if (isBlank(contentType)) {
                    contentType = "application/octet-stream";
                }

                osw.write("--" + boundary + CRLF);
                osw.write("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + CRLF);
                osw.write("Content-Type: " + contentType + CRLF);
                osw.write(CRLF);
                osw.flush();

                try (InputStream in = filePart.getInputStream()) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                }

                osw.write(CRLF);
                osw.write("--" + boundary + "--" + CRLF);
                osw.flush();
            }

            int status = conn.getResponseCode();
            InputStream respStream = (status >= 200 && status < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(respStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }

            String response = sb.toString();
            System.out.println("Cloudinary avatar response: " + response);

            if (status < 200 || status >= 300) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cloudinary upload failed (HTTP " + status + ").",
                        response
                ));
                return null;
            }

            String marker = "\"secure_url\":\"";
            int idx = response.indexOf(marker);
            if (idx == -1) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cloudinary response does not contain secure_url.",
                        response
                ));
                return null;
            }
            int start = idx + marker.length();
            int end = response.indexOf('"', start);
            if (end == -1) end = response.length();

            String secureUrl = response.substring(start, end)
                    .replace("\\/", "/");
            return secureUrl;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Cannot create signature for Cloudinary.",
                    e.toString()
            ));
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Cloudinary upload failed due to an unexpected error.",
                    e.toString()
            ));
            return null;
        }
    }

    // SHA-1 hex
    private String sha1Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
