package com.mypack.bean;

import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.entity.Areas;
import com.mypack.entity.Cities;

import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;


@Named("registerRestaurantManagerBean")
@ViewScoped
public class RegisterRestaurantManagerBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========= EJB =========
    @EJB
    private UsersFacadeLocal usersFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private RestaurantManagersFacadeLocal restaurantManagersFacade;

    @EJB
    private AreasFacadeLocal areasFacade;

    // ========= User hiện tại =========
    private Users currentUser;

    // ========= 1. Thông tin quản lý =========
    private String managerName;
    private String managerPhone;
    private String managerEmail;
    private String managerRole;

    // ========= 2. Thông tin nhà hàng =========
    private String restaurantName;
    private String restaurantBrandName;
    private String restaurantAddress;
    private String city;
    private String area;
    private String description;

    private String openTime;   // "HH:mm"
    private String closeTime;  // "HH:mm"

    // Logo URL (Cloudinary)
    private String logoUrl;
    private Part logoFile;

    // ========= 3. Điều kiện nhận tiệc =========
    private Integer minGuests;
    private Integer maxGuests;
    private Integer minDays;

    // ========= 4. Điều khoản =========
    private boolean acceptedTerms;

    // ========= Thành phố / Khu vực =========
    private List<String> cityList;
    private List<String> areaList;

    // ========= Restaurant & Manager Status =========
    private Restaurants existingRestaurant;
    private String managerStatus;

    // ===================================================
    // INIT
    // ===================================================
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        Object obj = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (obj instanceof Users) {
            currentUser = (Users) obj;
        }

        if (currentUser == null) {
            try {
                String loginUrl = ctx.getExternalContext().getRequestContextPath()
                        + "/login.xhtml?target=register_manager";
                ctx.getExternalContext().redirect(loginUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        managerName  = currentUser.getFullName();
        managerPhone = currentUser.getPhone();
        managerEmail = currentUser.getEmail();

        loadCitiesFromDb();
        areaList = new ArrayList<>();

        loadExistingRestaurant();
    }

    // ===================================================
    // Load Cities / Areas
    // ===================================================
    private void loadCitiesFromDb() {
        cityList = new ArrayList<>();
        List<Areas> allAreas = areasFacade.findAll();
        if (allAreas == null) return;

        for (Areas a : allAreas) {
            if (a == null) continue;
            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            String name = c.getName().trim();
            if (name.isEmpty()) continue;

            boolean exists = false;
            for (String s : cityList) {
                if (s.equalsIgnoreCase(name)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) cityList.add(name);
        }
        cityList.sort(String::compareToIgnoreCase);
    }

    private void loadExistingRestaurant() {
        List<RestaurantManagers> rms = restaurantManagersFacade.findAll();
        for (RestaurantManagers rm : rms) {
            if (rm.getUserId() != null
                    && rm.getUserId().getUserId().equals(currentUser.getUserId())) {
                existingRestaurant = rm.getRestaurantId();
                managerStatus = rm.getStatus();
                break;
            }
        }

        if (existingRestaurant != null) {
            restaurantName = existingRestaurant.getName();
            description    = existingRestaurant.getDescription();
            restaurantAddress = existingRestaurant.getAddress();

            managerPhone = existingRestaurant.getPhone() != null
                    ? existingRestaurant.getPhone()
                    : managerPhone;
            managerEmail = existingRestaurant.getEmail() != null
                    ? existingRestaurant.getEmail()
                    : managerEmail;
            managerName = existingRestaurant.getContactPerson() != null
                    ? existingRestaurant.getContactPerson()
                    : managerName;

            minGuests = existingRestaurant.getMinGuestCount();
            minDays   = existingRestaurant.getMinDaysInAdvance();

            logoUrl = existingRestaurant.getLogoUrl();

            Areas a = existingRestaurant.getAreaId();
            if (a != null) {
                Cities c = a.getCityId();
                if (c != null && c.getName() != null) city = c.getName();
                if (a.getName() != null) area = a.getName();
                onCityChange();
            }

            if (existingRestaurant.getOpenTime() != null
                    || existingRestaurant.getCloseTime() != null) {
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                if (existingRestaurant.getOpenTime() != null) {
                    openTime = timeFormat.format(existingRestaurant.getOpenTime());
                }
                if (existingRestaurant.getCloseTime() != null) {
                    closeTime = timeFormat.format(existingRestaurant.getCloseTime());
                }
            }
        }
    }

    public void onCityChange() {
        areaList = new ArrayList<>();
        if (isBlank(city)) {
            area = null;
            return;
        }

        String cityName = city.trim();
        List<Areas> allAreas = areasFacade.findAll();
        if (allAreas == null) {
            area = null;
            return;
        }

        for (Areas a : allAreas) {
            if (a == null || a.getName() == null) continue;
            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            if (c.getName().trim().equalsIgnoreCase(cityName)) {
                String areaName = a.getName().trim();
                if (!areaName.isEmpty()) areaList.add(areaName);
            }
        }
        areaList.sort(String::compareToIgnoreCase);
        area = null;
    }

    private boolean isPendingManager() {
        return (managerStatus != null &&
                (managerStatus.trim().equalsIgnoreCase("Pending")
                 || managerStatus.trim().equalsIgnoreCase("PENDING_APPROVAL")));
    }

    private Areas resolveAreaEntity() {
        if (isBlank(city) || isBlank(area)) return null;

        String cityName = city.trim();
        String areaName = area.trim();

        List<Areas> all = areasFacade.findAll();
        if (all == null || all.isEmpty()) return null;

        for (Areas a : all) {
            if (a == null || a.getName() == null) continue;
            if (!a.getName().trim().equalsIgnoreCase(areaName)) continue;

            Cities c = a.getCityId();
            if (c == null || c.getName() == null) continue;

            if (c.getName().trim().equalsIgnoreCase(cityName)) return a;
        }
        return null;
    }

    // ===================================================
    // SUBMIT
    // ===================================================
    public String submit() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (currentUser != null) {
            managerName  = currentUser.getFullName();
            managerEmail = currentUser.getEmail();
        }

        if (isPendingManager()) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Your application has been submitted and is awaiting approval. You cannot submit a new application.",
                    "This account has submitted a restaurant upgrade request and is awaiting FeastLink Admin approval. You do not need to resubmit."
            ));
            return null;
        }

        if (!acceptedTerms) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "You must agree to the Terms & Conditions before submitting a request.",
                    null
            ));
            return null;
        }

        boolean noNewLogo      = (logoFile == null || logoFile.getSize() <= 0);
        boolean noExistingLogo = isBlank(logoUrl);

        if (isBlank(managerName) || isBlank(managerPhone) || isBlank(managerEmail)
                || isBlank(restaurantName) || isBlank(restaurantAddress)
                || isBlank(city) || isBlank(area)
                || isBlank(openTime) || isBlank(closeTime)
                || (noNewLogo && noExistingLogo)
                || minGuests == null || minGuests < 0
                || minDays == null   || minDays   < 0) {

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Please fill in all required fields (including Logo, City, Region and Opening/Closing hours).",
                    null
            ));
            return null;
        }

        if (maxGuests != null && maxGuests < 0) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Maximum (expected) number of guests must be >= 0.",
                    null
            ));
            return null;
        }

        try {
            boolean isNewRestaurant = (existingRestaurant == null);
            Restaurants restaurant = isNewRestaurant ? new Restaurants() : existingRestaurant;
            Date now = new Date();

            if (isNewRestaurant) {
                restaurant.setStatus("PENDING_APPROVAL");
                restaurant.setCreatedAt(now);
            }
            restaurant.setUpdatedAt(now);

            restaurant.setName(restaurantName);
            restaurant.setDescription(description);

            String fullAddr = restaurantAddress;
            if (!isBlank(area)) fullAddr += ", " + area;
            if (!isBlank(city)) fullAddr += ", " + city;
            restaurant.setAddress(fullAddr);

            Areas selectedArea = resolveAreaEntity();
            if (selectedArea == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "No corresponding Area found in the system.",
                        "Check Cities/Areas in DB or contact Admin FeastLink."
                ));
                return null;
            }
            restaurant.setAreaId(selectedArea);

            restaurant.setPhone(managerPhone);
            restaurant.setEmail(managerEmail);
            restaurant.setContactPerson(managerName);

            restaurant.setMinGuestCount(minGuests);
            restaurant.setMinDaysInAdvance(minDays);

            // ===== Upload logo lên Cloudinary (bắt lỗi riêng) =====
            if (!noNewLogo && logoFile != null) {
                String secureUrl = uploadLogoToCloudinary(ctx, logoFile);
                if (secureUrl == null) {
                    // đã có FacesMessage cụ thể → dừng submit, không quăng Exception chung
                    return null;
                }
                logoUrl = secureUrl;
                restaurant.setLogoUrl(secureUrl);
            } else if (!isBlank(logoUrl)) {
                restaurant.setLogoUrl(logoUrl);
            }

            Date open = parseTime(openTime);
            if (open != null) restaurant.setOpenTime(open);
            Date close = parseTime(closeTime);
            if (close != null) restaurant.setCloseTime(close);

            if (isNewRestaurant) {
                restaurantsFacade.create(restaurant);

                RestaurantManagers rm = new RestaurantManagers();
                rm.setUserId(currentUser);
                rm.setRestaurantId(restaurant);
                rm.setIsPrimary(true);
                rm.setStatus("PENDING_APPROVAL");
                rm.setCreatedAt(now);
                restaurantManagersFacade.create(rm);

                currentUser.setStatus("PENDING");
                usersFacade.edit(currentUser);
                ctx.getExternalContext().getSessionMap().put("currentUser", currentUser);

                existingRestaurant = restaurant;
                managerStatus = "PENDING_APPROVAL";
            } else {
                restaurantsFacade.edit(restaurant);
            }

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Request for review sent successfully",
                    "Restaurant profile has been submitted to Admin FeastLink and is awaiting approval."
            ));
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "An error occurred while saving restaurant information. Request for approval failed.",
                    e.toString()
            ));
            return null;
        }
    }

   /**
 * Upload logo lên Cloudinary bằng HTTPS nhưng TẮT kiểm tra SSL (DEV ONLY).
 * Trả về secure_url hoặc null nếu lỗi (đã push FacesMessage rõ ràng).
 */
private String uploadLogoToCloudinary(FacesContext ctx, Part filePart) {
    try {
        // ====== 1. TẠM THỜI TẮT CHECK SSL (CHỈ DÙNG CHO DEV) ======
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

        String folder   = "feastlink/restaurants";
        long timestamp  = System.currentTimeMillis() / 1000L;
        String publicId = "logo_user_" + currentUser.getUserId() + "_" + timestamp;

        String toSign = "folder=" + folder
                + "&public_id=" + publicId
                + "&timestamp=" + timestamp
                + apiSecret;

        String signature = sha1Hex(toSign);

        String urlStr = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
        URL url = new URL(urlStr);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection(); // dùng HttpsURLConnection
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        String boundary = "----FeastLinkBoundary" + System.currentTimeMillis();
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
                    : ("logo_" + currentUser.getUserId() + ".png");
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
        System.out.println("Cloudinary response: " + response);

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

    // Parse "HH:mm" → Date
    private Date parseTime(String timeStr) {
        if (isBlank(timeStr)) return null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            return sdf.parse(timeStr.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    public String cancel() {
        return "index?faces-redirect=true";
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ================== GETTER / SETTER ==================
    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getManagerPhone() { return managerPhone; }
    public void setManagerPhone(String managerPhone) { this.managerPhone = managerPhone; }

    public String getManagerEmail() { return managerEmail; }
    public void setManagerEmail(String managerEmail) { this.managerEmail = managerEmail; }

    public String getManagerRole() { return managerRole; }
    public void setManagerRole(String managerRole) { this.managerRole = managerRole; }

    public String getRestaurantName() { return restaurantName; }
    public void setRestaurantName(String restaurantName) { this.restaurantName = restaurantName; }

    public String getRestaurantBrandName() { return restaurantBrandName; }
    public void setRestaurantBrandName(String restaurantBrandName) { this.restaurantBrandName = restaurantBrandName; }

    public String getRestaurantAddress() { return restaurantAddress; }
    public void setRestaurantAddress(String restaurantAddress) { this.restaurantAddress = restaurantAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getOpenTime() { return openTime; }
    public void setOpenTime(String openTime) { this.openTime = openTime; }

    public String getCloseTime() { return closeTime; }
    public void setCloseTime(String closeTime) { this.closeTime = closeTime; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public Part getLogoFile() { return logoFile; }
    public void setLogoFile(Part logoFile) { this.logoFile = logoFile; }

    public Integer getMinGuests() { return minGuests; }
    public void setMinGuests(Integer minGuests) { this.minGuests = minGuests; }

    public Integer getMaxGuests() { return maxGuests; }
    public void setMaxGuests(Integer maxGuests) { this.maxGuests = maxGuests; }

    public Integer getMinDays() { return minDays; }
    public void setMinDays(Integer minDays) { this.minDays = minDays; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }

    public List<String> getCityList() { return cityList; }
    public void setCityList(List<String> cityList) { this.cityList = cityList; }

    public List<String> getAreaList() { return areaList; }
    public void setAreaList(List<String> areaList) { this.areaList = areaList; }
}
