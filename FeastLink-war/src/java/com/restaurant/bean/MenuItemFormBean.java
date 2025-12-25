package com.restaurant.bean;

import com.mypack.entity.Cuisines;
import com.mypack.entity.MenuCategories;
import com.mypack.entity.MenuItems;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.CuisinesFacadeLocal;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@Named("menuItemFormBean")
@ViewScoped
public class MenuItemFormBean implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L; // 5MB

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private CuisinesFacadeLocal cuisinesFacade;

    @EJB
    private MenuCategoriesFacadeLocal menuCategoriesFacade;

    private MenuItems newItem;
    private boolean editMode;

    // Cuisines.CuisineId = INT  -> Integer
    private Integer cuisineId;
    // MenuCategories.CategoryId = BIGINT -> Long
    private Long categoryId;

    // list cho dropdown
    private List<Cuisines> cuisines;
    private List<MenuCategories> categories;

    // ✅ Upload field
    private Part imageFile;

    // session context
    private Users currentUser;

    public MenuItemFormBean() {
    }

    // ===== Helper: lấy currentUser =====
    private Users resolveCurrentUser() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;
        ExternalContext ext = ctx.getExternalContext();
        Map<String, Object> session = ext.getSessionMap();
        return (Users) session.get("currentUser");
    }

    // ===== Helper: lấy restaurant theo user đang login =====
    private Restaurants resolveCurrentRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return null;

        Map<String, Object> session = ctx.getExternalContext().getSessionMap();
        Users u = (Users) session.get("currentUser");
        if (u == null || u.getEmail() == null) {
            return null;
        }
        String email = u.getEmail();

        // Tạm thời map qua email: Users.Email == Restaurants.Email
        List<Restaurants> all = restaurantsFacade.findAll();
        for (Restaurants r : all) {
            if (r.getEmail() != null && r.getEmail().equalsIgnoreCase(email)) {
                return r;
            }
        }
        return null;
    }

    // ======================================================
    // INIT
    // ======================================================
    @PostConstruct
    public void init() {
        // load list cho dropdown
        cuisines = cuisinesFacade.findAll();
        categories = menuCategoriesFacade.findAll();

        currentUser = resolveCurrentUser();

        // đọc itemId từ query string (?itemId=...)
        Map<String, String> params = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        String idStr = params.get("itemId");

        if (idStr != null && !idStr.isBlank()) {
            // ---- EDIT MODE ----
            Long id = Long.valueOf(idStr);
            newItem = menuItemsFacade.find(id);
            editMode = true;

            if (newItem != null) {
                if (newItem.getCuisineId() != null) {
                    cuisineId = newItem.getCuisineId().getCuisineId();
                }
                if (newItem.getCategoryId() != null) {
                    categoryId = newItem.getCategoryId().getCategoryId();
                }
            }
        } else {
            // ---- NEW MODE ----
            editMode = false;
            newItem = new MenuItems();
            newItem.setIsVegetarian(false);
            newItem.setStatus("ACTIVE");
            newItem.setIsDeleted(false);

            // Gán nhà hàng theo user đang login
            Restaurants r = resolveCurrentRestaurant();
            newItem.setRestaurantId(r);
        }
    }

    // ======================================================
    // SAVE
    // ======================================================
    public String save() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        // Ensure restaurant context
        Restaurants r = (newItem != null) ? newItem.getRestaurantId() : null;
        if (r == null) {
            r = resolveCurrentRestaurant();
            if (newItem != null) {
                newItem.setRestaurantId(r);
            }
        }
        if (r == null) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Restaurant context is missing.",
                    "Please login with a Restaurant Manager account linked to a restaurant."
            ));
            return null;
        }

        // gán Cuisine từ id
        if (cuisineId != null) {
            Cuisines c = cuisinesFacade.find(cuisineId);
            newItem.setCuisineId(c);
        } else {
            newItem.setCuisineId(null);
        }

        // gán Category từ id
        if (categoryId != null) {
            MenuCategories cat = menuCategoriesFacade.find(categoryId);
            newItem.setCategoryId(cat);
        } else {
            newItem.setCategoryId(null);
        }

        // ✅ If a file is selected -> upload to Cloudinary -> save secure_url into imageUrl
        if (imageFile != null && imageFile.getSize() > 0) {
            String validateMsg = validateImageFile(imageFile);
            if (validateMsg != null) {
                ctx.addMessage("newItemForm:itemImage", new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        validateMsg,
                        null
                ));
                return null;
            }

            String url = uploadToCloudinary(ctx, imageFile, r.getRestaurantId());
            if (url == null) {
                // uploadToCloudinary already added FacesMessage
                return null;
            }

            newItem.setImageUrl(url);
        }

        // persist
        if (editMode) {
            menuItemsFacade.edit(newItem);
        } else {
            menuItemsFacade.create(newItem);
        }

        return "/Restaurant/menu-packages?faces-redirect=true";
    }

    public String cancel() {
        return "/Restaurant/menu-packages?faces-redirect=true";
    }

    // ======================================================
    // UI helper: show current image
    // ======================================================
    public String getCurrentImageUrl() {
        if (newItem == null || newItem.getImageUrl() == null || newItem.getImageUrl().trim().isEmpty()) {
            return null;
        }
        String url = newItem.getImageUrl().trim();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (!url.startsWith("/")) {
            url = "/" + url;
        }
        String ctxPath = FacesContext.getCurrentInstance()
                .getExternalContext()
                .getRequestContextPath();
        return ctxPath + url;
    }

    // ======================================================
    // VALIDATE UPLOAD
    // ======================================================
    private String validateImageFile(Part part) {
        if (part == null || part.getSize() <= 0) {
            return "Please choose an image file to upload.";
        }
        if (part.getSize() > MAX_UPLOAD_BYTES) {
            return "Image is too large. Max size is 5MB.";
        }

        String ct = part.getContentType();
        if (ct == null || !ct.toLowerCase(Locale.ENGLISH).startsWith("image/")) {
            return "Invalid file type. Please upload an image (PNG/JPG/WebP).";
        }
        return null;
    }

    // ======================================================
    // CLOUDINARY UPLOAD (same style as your RestaurantImagesBean)
    // ======================================================
    private String uploadToCloudinary(FacesContext ctx, Part filePart, Long restaurantId) {
        try {
            // ---- DEV ONLY: disable SSL verify (same as your existing beans) ----
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                    @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                    @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override public boolean verify(String hostname, SSLSession session) { return true; }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            // ---- end DEV ONLY ----

            ServletContext servletContext =
                    (ServletContext) ctx.getExternalContext().getContext();

            String cloudName = servletContext.getInitParameter("cloudinary.cloud_name");
            String apiKey    = servletContext.getInitParameter("cloudinary.api_key");
            String apiSecret = servletContext.getInitParameter("cloudinary.api_secret");

            if (isBlank(cloudName) || isBlank(apiKey) || isBlank(apiSecret)) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Cloudinary configuration is missing.",
                        "Please contact FeastLink Admin."
                ));
                return null;
            }

            String folder = "feastlink/menu-items";
            long timestamp = System.currentTimeMillis() / 1000L;

            long userIdForName = (currentUser != null && currentUser.getUserId() != null)
                    ? currentUser.getUserId()
                    : 0L;

            String rid = (restaurantId != null) ? String.valueOf(restaurantId) : "0";
            String publicId = "menu_item_restaurant_" + rid + "_user_" + userIdForName + "_" + timestamp;

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
                        : ("menu_item_" + rid + "_" + timestamp + ".png");

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

            return response.substring(start, end).replace("\\/", "/");

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

    // ========== GET / SET ==========
    public MenuItems getNewItem() {
        return newItem;
    }

    public void setNewItem(MenuItems newItem) {
        this.newItem = newItem;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    public Integer getCuisineId() {
        return cuisineId;
    }

    public void setCuisineId(Integer cuisineId) {
        this.cuisineId = cuisineId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public List<Cuisines> getCuisines() {
        return cuisines;
    }

    public void setCuisines(List<Cuisines> cuisines) {
        this.cuisines = cuisines;
    }

    public List<MenuCategories> getCategories() {
        return categories;
    }

    public void setCategories(List<MenuCategories> categories) {
        this.categories = categories;
    }

    public Part getImageFile() {
        return imageFile;
    }

    public void setImageFile(Part imageFile) {
        this.imageFile = imageFile;
    }
}
