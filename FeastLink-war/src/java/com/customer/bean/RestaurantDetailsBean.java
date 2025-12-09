package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.RestaurantImages;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

@Named("restaurantDetailsBean")
@RequestScoped
public class RestaurantDetailsBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private Restaurants restaurant;

    // Summary info
    private double avgRating;
    private int reviewCount;
    private String badge;
    private double pricePerGuestFrom;
    private int capacityMin;
    private int capacityMax;
    private String cityName;
    private String areaName;
    private String heroImageUrl;
    private List<String> galleryImages;
    private List<String> eventTypes;

    // Combos (Menu & Packages)
    private List<ComboCard> combos;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) return;

        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
        String idParam = params.get("restaurantId");

        Long id = null;
        try {
            if (idParam != null && !idParam.isEmpty()) {
                id = Long.valueOf(idParam);
            }
        } catch (NumberFormatException ignored) {}

        if (id != null) {
            restaurant = restaurantsFacade.find(id);
        }

        // fallback nếu id sai: lấy nhà hàng đầu tiên
        if (restaurant == null) {
            List<Restaurants> all = restaurantsFacade.findAll();
            if (all != null && !all.isEmpty()) {
                restaurant = all.get(0);
            }
        }
        if (restaurant == null) return;

        // ==== City / Area ====
        cityName = "";
        areaName = "";
        try {
            if (restaurant.getAreaId() != null) {
                areaName = safe(restaurant.getAreaId().getName());
                if (restaurant.getAreaId().getCityId() != null) {
                    cityName = safe(restaurant.getAreaId().getCityId().getName());
                }
            }
        } catch (Exception ignored) {}

        // ==== Capacity (dùng minGuest * 3 làm max demo) ====
        Integer minGuests = restaurant.getMinGuestCount();
        capacityMin = (minGuests != null && minGuests > 0) ? minGuests : 30;
        capacityMax = capacityMin * 3;

      // =================== IMAGES (IsPrimary + SortOrder) ===================
heroImageUrl = null;
galleryImages = new ArrayList<>();
try {
    Collection<RestaurantImages> imagesCol = restaurant.getRestaurantImagesCollection();
    if (imagesCol != null && !imagesCol.isEmpty()) {

        // Chuyển sang List để sort + BỎ QUA ảnh không có URL (coi như đã xóa)
        List<RestaurantImages> images = new ArrayList<>();
        for (RestaurantImages img : imagesCol) {
            if (img == null) continue;

            String url = safe(img.getImageUrl()); // safe() trả "" nếu null
            if (url.trim().isEmpty()) {
                // ảnh này không còn đường dẫn => coi như đã xóa, không add vào
                continue;
            }

            images.add(img);
        }

        if (!images.isEmpty()) {
            // Sắp xếp theo SortOrder tăng dần (null sẽ nằm cuối)
            images.sort((a, b) -> {
                Integer sa = a.getSortOrder();   // giữ nguyên getter của bạn
                Integer sb = b.getSortOrder();
                if (sa == null && sb == null) return 0;
                if (sa == null) return 1;
                if (sb == null) return -1;
                return sa.compareTo(sb);
            });

            // Tìm ảnh IsPrimary = 1 để làm banner (nếu không có thì lấy ảnh đầu tiên)
            RestaurantImages primary = null;
            for (RestaurantImages img : images) {
                Boolean isPrimary = img.getIsPrimary(); // giữ nguyên getter hiện tại
                if (isPrimary != null && isPrimary) {
                    primary = img;
                    break;
                }
            }
            if (primary == null) {
                primary = images.get(0);
            }

            // Ảnh banner
            heroImageUrl = safe(primary.getImageUrl());

            // Ảnh gallery dưới: lấy hết theo SortOrder
            for (RestaurantImages img : images) {
                String url = safe(img.getImageUrl());
                if (!url.trim().isEmpty()) {
                    galleryImages.add(url);
                }
            }
        }
    }
} catch (Exception e) {
    // bạn có thể log ra để debug nếu muốn
    e.printStackTrace();
}

if (heroImageUrl == null || heroImageUrl.isEmpty()) {
    heroImageUrl = "/FeastLink-war/resources/images/restaurant-placeholder.jpg";
}


        // ==== Rating & Reviews ====
        double sumRating = 0d;
        reviewCount = 0;
        try {
            Collection<RestaurantReviews> reviews = restaurant.getRestaurantReviewsCollection();
            if (reviews != null) {
                for (RestaurantReviews rev : reviews) {
                    if (rev == null) continue;

                    Boolean deleted = rev.getIsDeleted();
                    if (deleted != null && deleted) continue;

                    Boolean approved = rev.getIsApproved();
                    if (approved != null && !approved) continue;

                    Integer r = rev.getRating();
                    if (r != null && r > 0) {
                        sumRating += r;
                        reviewCount++;
                    }
                }
            }
        } catch (Exception ignored) {}
        avgRating = reviewCount > 0 ? (sumRating / reviewCount) : 4.8d;

        if (avgRating >= 4.9 && reviewCount >= 200) {
            badge = "TOP RATED";
        } else if (avgRating >= 4.8 && reviewCount >= 100) {
            badge = "VIP";
        } else if (reviewCount <= 10 && reviewCount > 0) {
            badge = "NEW";
        } else {
            badge = null;
        }

        // ==== Price per guest từ combo ====
        pricePerGuestFrom = 0d;
        combos = new ArrayList<>();
        try {
            Collection<MenuCombos> comboEntities = restaurant.getMenuCombosCollection();
            if (comboEntities != null) {
                for (MenuCombos combo : comboEntities) {
                    if (combo == null) continue;

                    BigDecimal total = combo.getPriceTotal();
                    Integer guests = combo.getMinGuests();

                    double totalVal = (total != null) ? total.doubleValue() : 0d;
                    int minG = (guests != null && guests > 0) ? guests : 10;

                    // tính giá / khách để dùng cho "Starting from"
                    if (totalVal > 0 && minG > 0) {
                        double perGuest = totalVal / minG;
                        if (pricePerGuestFrom == 0d || perGuest < pricePerGuestFrom) {
                            pricePerGuestFrom = perGuest;
                        }
                    }

                    // đưa vào danh sách combo cho section "Menu & Packages"
                    ComboCard c = new ComboCard();
                    c.setComboId(combo.getComboId());
                    c.setName(safe(combo.getName()));
                    c.setDescription(safe(combo.getDescription()));
                    c.setPriceTotal(totalVal);
                    c.setMinGuests(minG);

                    // Ideal range & tag demo
                    c.setIdealRange(minG + "–" + (minG * 2) + " guests");

                    if (totalVal >= 800) {
                        c.setVip(true);
                        c.setHighlightTag("VIP PACKAGE");
                    } else {
                        c.setVip(false);
                        c.setHighlightTag("PACKAGE");
                    }

                    combos.add(c);
                }
            }
        } catch (Exception ignored) {}

        if (pricePerGuestFrom <= 0d) {
            pricePerGuestFrom = 75d; // fallback demo
        }

        // ==== Event types (distinct) từ Bookings ====
        Set<String> typeSet = new LinkedHashSet<>();
        try {
            Collection<Bookings> bookings = restaurant.getBookingsCollection();
            if (bookings != null) {
                for (Bookings b : bookings) {
                    if (b == null) continue;
                    EventTypes et = b.getEventTypeId();
                    if (et != null && et.getName() != null) {
                        typeSet.add(et.getName());
                    }
                }
            }
        } catch (Exception ignored) {}
        if (typeSet.isEmpty()) {
            typeSet.add("Wedding");
            typeSet.add("Corporate");
        }
        eventTypes = new ArrayList<>(typeSet);
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    // ===== getters cho xhtml =====

    public Restaurants getRestaurant() {
        return restaurant;
    }

    public String getDisplayName() {
        return restaurant != null ? safe(restaurant.getName()) : "";
    }

    public double getAvgRating() {
        return avgRating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public String getBadge() {
        return badge;
    }

    public double getPricePerGuestFrom() {
        return pricePerGuestFrom;
    }

    public int getCapacityMin() {
        return capacityMin;
    }

    public int getCapacityMax() {
        return capacityMax;
    }

    public String getCityName() {
        return cityName;
    }

    public String getAreaName() {
        return areaName;
    }

    public String getHeroImageUrl() {
        return heroImageUrl;
    }

    public List<String> getGalleryImages() {
        return galleryImages;
    }

    public List<String> getEventTypes() {
        return eventTypes;
    }

    public List<ComboCard> getCombos() {
        return combos;
    }

    // ====== Inner class cho combo card ======
    public static class ComboCard implements Serializable {
        private Long comboId;
        private String name;
        private String description;
        private double priceTotal;
        private int minGuests;
        private String idealRange;
        private boolean vip;
        private String highlightTag;

        public Long getComboId() { return comboId; }
        public void setComboId(Long comboId) { this.comboId = comboId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public double getPriceTotal() { return priceTotal; }
        public void setPriceTotal(double priceTotal) { this.priceTotal = priceTotal; }

        public int getMinGuests() { return minGuests; }
        public void setMinGuests(int minGuests) { this.minGuests = minGuests; }

        public String getIdealRange() { return idealRange; }
        public void setIdealRange(String idealRange) { this.idealRange = idealRange; }

        public boolean isVip() { return vip; }
        public void setVip(boolean vip) { this.vip = vip; }

        public String getHighlightTag() { return highlightTag; }
        public void setHighlightTag(String highlightTag) { this.highlightTag = highlightTag; }
    }
}
