package com.restaurant.bean;

import com.mypack.entity.Areas;
import com.mypack.entity.Cities;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Part;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Named("restaurantProfileBean")
@ViewScoped
public class RestaurantProfileBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== EJB =====
    @EJB
    private AreasFacadeLocal areasFacade;

    @EJB
    private CitiesFacadeLocal citiesFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    // ===== ENTITY =====
    private Restaurants restaurant;   // bản ghi Restaurants
    private Users currentUser;        // user đang login (MANAGER)

    // ===== BASIC PROFILE FIELDS =====
    private String name;
    private String description;
    private String phone;
    private String email;
    private String address;
    private String contactPerson;

    // Status: PENDING / ACTIVE / PRIVATE
    private String status;

    // Operating hours (HH:mm)
    private String openTimeText;
    private String closeTimeText;

    // Booking policy
    private Integer minGuests;
    private Integer minDaysBeforeBooking;
    private String cancelPolicy;

    // ===== SERVICE AREAS =====
    private List<Cities> allCities;
    private List<Areas> allAreas;
    private List<Areas> filteredAreas;
    private Integer selectedCityId;
    private Integer selectedAreaId;

    // ===== AVATAR (dùng chung Users.AvatarUrl) =====
    private Part avatarPart;   // file user chọn mới
    private String avatarUrl;  // giá trị thô trong DB (Users.AvatarUrl)

    // ==========================================================
    // HELPER: LẤY USER + RESTAURANT TỪ SESSION
    // ==========================================================
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;

        ExternalContext ec = ctx.getExternalContext();
        Map<String, Object> session = ec.getSessionMap();

        Object obj = session.get("currentUser");
        if (!(obj instanceof Users)) {
            return null;
        }

        currentUser = (Users) obj;

        if (currentUser.getEmail() == null) {
            return null;
        }

        String emailLogin = currentUser.getEmail();

        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null &&
                r.getEmail().equalsIgnoreCase(emailLogin)) {
                return r;
            }
        }
        return null;
    }

    // ==========================================================
    // INIT
    // ==========================================================
    @PostConstruct
    public void init() {
        // Lấy restaurant + currentUser
        restaurant = resolveCurrentRestaurant();

        // Avatar lấy từ Users.AvatarUrl
        if (currentUser != null) {
            avatarUrl = currentUser.getAvatarUrl();   // có thể null
        }

        if (restaurant != null) {
            name          = restaurant.getName();
            description   = restaurant.getDescription();
            phone         = restaurant.getPhone();
            email         = restaurant.getEmail();
            address       = restaurant.getAddress();
            contactPerson = restaurant.getContactPerson();

            status        = restaurant.getStatus();

            // giờ mở / đóng
            if (restaurant.getOpenTime() != null) {
                String t = restaurant.getOpenTime().toString(); // 08:00:00
                openTimeText = t.length() >= 5 ? t.substring(0, 5) : t;
            } else {
                openTimeText = "08:00";
            }

            if (restaurant.getCloseTime() != null) {
                String t = restaurant.getCloseTime().toString();
                closeTimeText = t.length() >= 5 ? t.substring(0, 5) : t;
            } else {
                closeTimeText = "22:00";
            }

            // booking policy
            minGuests = (restaurant.getMinGuestCount() != null)
                    ? restaurant.getMinGuestCount()
                    : 20;

            minDaysBeforeBooking = (restaurant.getMinDaysInAdvance() != null)
                    ? restaurant.getMinDaysInAdvance()
                    : 7;

            // city / area hiện tại
            Areas currentArea = restaurant.getAreaId();
            if (currentArea != null) {
                selectedAreaId = currentArea.getAreaId();
                if (currentArea.getCityId() != null) {
                    selectedCityId = currentArea.getCityId().getCityId();
                }
            }

        } else {
            // chưa có restaurant => tạo mới tạm
            restaurant = new Restaurants();
            status = "PENDING";
            openTimeText = "08:00";
            closeTimeText = "22:00";
            minGuests = 20;
            minDaysBeforeBooking = 7;
            cancelPolicy = "Full refund if cancelled 14 days before the event. "
                    + "50% refund between 7–13 days. No refund within 7 days.";
        }

        // dữ liệu city/area
        allCities = citiesFacade.findAll();
        allAreas  = areasFacade.findAll();
        filteredAreas = new ArrayList<>();
        updateFilteredAreas();
    }

    // ==========================================================
    // FILTER AREAS THEO CITY
    // ==========================================================
    private void updateFilteredAreas() {
        filteredAreas = new ArrayList<>();

        if (selectedCityId == null || allAreas == null) {
            selectedAreaId = null;
            return;
        }

        for (Areas a : allAreas) {
            if (a.getCityId() != null
                    && a.getCityId().getCityId() != null
                    && a.getCityId().getCityId().equals(selectedCityId)) {
                filteredAreas.add(a);
            }
        }

        if (selectedAreaId != null) {
            boolean stillValid = false;
            for (Areas a : filteredAreas) {
                if (a.getAreaId().equals(selectedAreaId)) {
                    stillValid = true;
                    break;
                }
            }
            if (!stillValid) {
                selectedAreaId = null;
            }
        }
    }

    // ==========================================================
    // STATUS: PUBLIC / PRIVATE
    // ==========================================================
    public void toggleVisibility() {
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }

        if ("PENDING".equalsIgnoreCase(status)) {
            return; // chưa cho đổi
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            status = "PRIVATE";
        } else if ("PRIVATE".equalsIgnoreCase(status)) {
            status = "ACTIVE";
        } else {
            status = "ACTIVE";
        }

        restaurant.setStatus(status);
        restaurantsFacade.edit(restaurant);
    }

    public boolean isPublicVisible() {
        return "ACTIVE".equalsIgnoreCase(status);
    }

    // ==========================================================
    // CLOUDINARY UPLOAD AVATAR (GIỐNG ProfileBean)
    // ==========================================================
    /**
     * Upload avatar (manager) lên Cloudinary → trả về secure_url.
     * Nếu lỗi đã add FacesMessage và trả null.
     */
    private String uploadAvatarToCloudinary(FacesContext ctx, Part filePart, Long userId) {
        try {
            // ==== 1. TẠM THỜI TẮT CHECK SSL (cho môi trường dev) ====
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
            // ==== HẾT PHẦN TẮT SSL CHECK ====

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

            // Folder chung cho avatar Users
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

            String boundary = "----FeastLinkRestaurantBoundary" + System.currentTimeMillis();
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
            System.out.println("Cloudinary restaurant avatar response: " + response);

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

    // ==========================================================
    // SAVE PROFILE
    // ==========================================================
    private Time parseTime(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String s = text.trim();
        if (s.length() == 5) {
            s = s + ":00";
        }
        try {
            return Time.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String saveProfile() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (restaurant == null || currentUser == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Không xác định được tài khoản / nhà hàng.",
                    null
            ));
            return null;
        }

        // copy field vào entity Restaurants
        restaurant.setName(name);
        restaurant.setDescription(description);
        restaurant.setPhone(phone);
        restaurant.setEmail(email);
        restaurant.setAddress(address);
        restaurant.setContactPerson(contactPerson);
        restaurant.setStatus(status);
        restaurant.setOpenTime(parseTime(openTimeText));
        restaurant.setCloseTime(parseTime(closeTimeText));
        restaurant.setMinGuestCount(minGuests);
        restaurant.setMinDaysInAdvance(minDaysBeforeBooking);

        // lưu Area
        if (selectedAreaId != null) {
            Areas area = areasFacade.find(selectedAreaId);
            restaurant.setAreaId(area);
        } else {
            restaurant.setAreaId(null);
        }

        // ====== UPLOAD AVATAR (nếu chọn file) -> Users.AvatarUrl ======
        if (avatarPart != null && avatarPart.getSize() > 0) {
            String secureUrl = uploadAvatarToCloudinary(ctx, avatarPart, currentUser.getUserId());
            if (secureUrl == null) {
                // đã có message lỗi, không lưu tiếp
                return null;
            }
            avatarUrl = secureUrl;
            currentUser.setAvatarUrl(secureUrl);
        }

        try {
            // lưu Restaurants
            if (restaurant.getRestaurantId() == null) {
                restaurantsFacade.create(restaurant);
            } else {
                restaurantsFacade.edit(restaurant);
            }

            // lưu Users (avatar mới)
            usersFacade.edit(currentUser);

            // update session
            ctx.getExternalContext().getSessionMap().put("currentUser", currentUser);
            ctx.getExternalContext().getSessionMap().put("loginIdentifier", currentUser.getEmail());

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Cập nhật thành công",
                    "Thông tin nhà hàng đã được lưu."
            ));

        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Lưu thất bại",
                    "Đã xảy ra lỗi khi lưu thông tin."
            ));
        }

        return null; // ở lại trang hiện tại
    }

    // ==========================================================
    // GETTERS / SETTERS
    // ==========================================================
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMinDaysBeforeBooking() { return minDaysBeforeBooking; }
    public void setMinDaysBeforeBooking(Integer minDaysBeforeBooking) {
        this.minDaysBeforeBooking = minDaysBeforeBooking;
    }

    public String getCancelPolicy() { return cancelPolicy; }
    public void setCancelPolicy(String cancelPolicy) { this.cancelPolicy = cancelPolicy; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOpenTimeText() { return openTimeText; }
    public void setOpenTimeText(String openTimeText) { this.openTimeText = openTimeText; }

    public String getCloseTimeText() { return closeTimeText; }
    public void setCloseTimeText(String closeTimeText) { this.closeTimeText = closeTimeText; }

    public List<Cities> getAllCities() {
        if (allCities == null) {
            allCities = citiesFacade.findAll();
        }
        return allCities;
    }

    public List<Areas> getFilteredAreas() { return filteredAreas; }

    public Integer getSelectedCityId() { return selectedCityId; }
    public void setSelectedCityId(Integer selectedCityId) {
        this.selectedCityId = selectedCityId;
        updateFilteredAreas();
    }

    public Integer getSelectedAreaId() { return selectedAreaId; }
    public void setSelectedAreaId(Integer selectedAreaId) { this.selectedAreaId = selectedAreaId; }

    public Part getAvatarPart() { return avatarPart; }
    public void setAvatarPart(Part avatarPart) { this.avatarPart = avatarPart; }

    // dùng trong xhtml: img src="#{restaurantProfileBean.avatarSrc}"
    public String getAvatarSrc() {
        if (avatarUrl == null || avatarUrl.trim().isEmpty()) {
            return null;
        }
        String url = avatarUrl.trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        String ctxPath = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestContextPath();
        return ctxPath + url;
    }

    public void setAvatarSrc(String avatarSrc) {
        this.avatarUrl = avatarSrc;
    }
}
