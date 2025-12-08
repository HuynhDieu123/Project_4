package com.restaurant.bean;

import com.mypack.entity.RestaurantImages;
import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.RestaurantImagesFacadeLocal;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Named("restaurantImagesBean")
@ViewScoped
public class RestaurantImagesBean implements Serializable {

    @EJB
    private RestaurantImagesFacadeLocal restaurantImagesFacade;

    @EJB
    private RestaurantManagersFacadeLocal restaurantManagersFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    // ---- context ----
    private Users currentUser;
    private Restaurants currentRestaurant;
    private Long currentRestaurantId;

    // ---- form fields ----
    private Part imageFile;
    private String caption;
    private boolean primaryImage;

    // ---- view data ----
    private RestaurantImages primaryBanner;
    private List<RestaurantImages> otherImages;

    // ================== GET/SET ==================
    public Part getImageFile() {
        return imageFile;
    }

    public void setImageFile(Part imageFile) {
        this.imageFile = imageFile;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public boolean isPrimaryImage() {
        return primaryImage;
    }

    public void setPrimaryImage(boolean primaryImage) {
        this.primaryImage = primaryImage;
    }

    public RestaurantImages getPrimaryBanner() {
        return primaryBanner;
    }

    public List<RestaurantImages> getOtherImages() {
        return otherImages;
    }

    // ================== INIT ==================
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ExternalContext ext = ctx.getExternalContext();
        Map<String, Object> session = ext.getSessionMap();

        currentUser = (Users) session.get("currentUser");
        if (currentUser == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "You must be logged in to manage restaurant images.",
                    null
            ));
            return;
        }

        // Tìm RestaurantManagers theo currentUser (dùng findAll rồi filter)
        RestaurantManagers managerForUser = null;
        List<RestaurantManagers> managers = restaurantManagersFacade.findAll();
        if (managers != null) {
            for (RestaurantManagers rm : managers) {
                if (rm.getUserId() != null
                        && rm.getUserId().getUserId() != null
                        && rm.getUserId().getUserId().equals(currentUser.getUserId())) {
                    managerForUser = rm;
                    break;
                }
            }
        }

        if (managerForUser == null || managerForUser.getRestaurantId() == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Restaurant context is missing.",
                    "Please make sure your manager account is linked to a restaurant."
            ));
            return;
        }

        currentRestaurant = managerForUser.getRestaurantId();
        currentRestaurantId = currentRestaurant.getRestaurantId();

        reloadImages();
    }

    // ================== ACTIONS ==================

    public void uploadImage() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (currentRestaurant == null || currentRestaurantId == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Restaurant context is missing.",
                    null
            ));
            return;
        }

        if (imageFile == null || imageFile.getSize() <= 0) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Please choose an image file to upload.",
                    null
            ));
            return;
        }

        String imageUrl = uploadToCloudinary(ctx, imageFile);
        if (imageUrl == null) {
            // uploadToCloudinary đã push FacesMessage error
            return;
        }

        // Nếu chọn primary banner thì hạ primary cũ xuống
        if (primaryImage && primaryBanner != null && primaryBanner.getIsPrimary()) {
            primaryBanner.setIsPrimary(false);
            restaurantImagesFacade.edit(primaryBanner);
        }

        RestaurantImages img = new RestaurantImages();
        img.setRestaurantId(currentRestaurant); // ManyToOne tới Restaurants

        img.setImageUrl(imageUrl);
        img.setCaption(caption != null ? caption.trim() : "");
        img.setIsPrimary(primaryImage);

        // Sort order: nếu là primary => 1, nếu không => max + 1
        Integer sortOrder;
        if (primaryImage) {
            sortOrder = 1;
        } else {
            sortOrder = getNextSortOrder();
        }
        img.setSortOrder(sortOrder);
        img.setCreatedAt(new Date());

        restaurantImagesFacade.create(img);

        // reset form
        imageFile = null;
        caption = null;
        primaryImage = false;

        // reload list
        reloadImages();

        String msg = img.getIsPrimary()
                ? "Primary banner uploaded successfully."
                : "Image uploaded successfully.";
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                msg,
                null
        ));
    }

    public void deleteImage(RestaurantImages img) {
        if (img == null || img.getImageId() == null) {
            return;
        }

        FacesContext ctx = FacesContext.getCurrentInstance();

        RestaurantImages managed = restaurantImagesFacade.find(img.getImageId());
        if (managed != null) {
            restaurantImagesFacade.remove(managed);
        }

        reloadImages();

        String msg = img.getIsPrimary()
                ? "Primary banner deleted."
                : "Image deleted.";
        ctx.addMessage(null, new FacesMessage(
                FacesMessage.SEVERITY_INFO,
                msg,
                null
        ));
    }

    // ================== HELPERS ==================

    private void reloadImages() {
        primaryBanner = null;
        otherImages = new ArrayList<>();

        if (currentRestaurantId == null) {
            return;
        }

        List<RestaurantImages> all = restaurantImagesFacade.findAll();
        if (all == null) {
            return;
        }

        for (RestaurantImages img : all) {
            if (img.getRestaurantId() != null
                    && img.getRestaurantId().getRestaurantId() != null
                    && img.getRestaurantId().getRestaurantId().equals(currentRestaurantId)) {

                if (img.getIsPrimary()) {
                    primaryBanner = img;
                } else {
                    otherImages.add(img);
                }
            }
        }

        // sort other images theo SortOrder, rồi tới ImageId
        Collections.sort(otherImages, new Comparator<RestaurantImages>() {
            @Override
            public int compare(RestaurantImages a, RestaurantImages b) {
                Integer o1 = a.getSortOrder();
                Integer o2 = b.getSortOrder();

                if (o1 == null && o2 == null) {
                    Long id1 = a.getImageId();
                    Long id2 = b.getImageId();
                    if (id1 == null || id2 == null) return 0;
                    return id1.compareTo(id2);
                }
                if (o1 == null) return 1;
                if (o2 == null) return -1;

                int cmp = o1.compareTo(o2);
                if (cmp != 0) return cmp;

                Long id1 = a.getImageId();
                Long id2 = b.getImageId();
                if (id1 == null || id2 == null) return 0;
                return id1.compareTo(id2);
            }
        });
    }

    private Integer getNextSortOrder() {
        int max = 0;
        if (primaryBanner != null && primaryBanner.getSortOrder() != null) {
            max = Math.max(max, primaryBanner.getSortOrder());
        }
        if (otherImages != null) {
            for (RestaurantImages img : otherImages) {
                if (img.getSortOrder() != null && img.getSortOrder() > max) {
                    max = img.getSortOrder();
                }
            }
        }
        return max + 1;
    }

    /**
     * Dùng trong xhtml để trả về URL đầy đủ
     * - Nếu đã là http(s) thì trả thẳng
     * - Nếu là đường dẫn local (/images/...) thì thêm contextPath
     */
    public String resolveImageUrl(RestaurantImages img) {
        if (img == null || img.getImageUrl() == null) {
            return "";
        }
        String url = img.getImageUrl();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        ExternalContext ext = FacesContext.getCurrentInstance().getExternalContext();
        String ctxPath = ext.getRequestContextPath();
        return ctxPath + url;
    }

    // ================== CLOUDINARY UPLOAD ==================
    /**
     * Upload lên Cloudinary, tắt SSL check (DEV ONLY).
     * Trả về secure_url hoặc null nếu lỗi.
     */
    private String uploadToCloudinary(FacesContext ctx, Part filePart) {
        try {
            // ---- Tạm thời tắt SSL verify (DEV ONLY) ----
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) { }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            // ---- Hết tắt SSL ----

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

            long userIdForName = currentUser != null && currentUser.getUserId() != null
                    ? currentUser.getUserId()
                    : 0L;

            String publicId = "restaurant_" + currentRestaurantId + "_user_" + userIdForName + "_" + timestamp;

            String toSign = "folder=" + folder
                    + "&public_id=" + publicId
                    + "&timestamp=" + timestamp
                    + apiSecret;

            String signature = sha1Hex(toSign);

            String urlStr = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
            java.net.URL url = new java.net.URL(urlStr);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");

            String boundary = "----FeastLinkBoundary" + System.currentTimeMillis();
            String CRLF = "\r\n";

            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream();
                 OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

                // helper field text
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
                        : ("restaurant_" + currentRestaurantId + "_" + timestamp + ".png");
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
            if (end == -1) {
                end = response.length();
            }

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

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate SHA-1", e);
        }
    }
}
