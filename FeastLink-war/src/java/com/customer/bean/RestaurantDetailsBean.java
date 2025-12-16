package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.RestaurantImages;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import com.mypack.entity.MenuCategories;
import com.mypack.entity.MenuItems;
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;

import java.util.ArrayList;
import java.util.List;
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

    @EJB
    private MenuCategoriesFacadeLocal menuCategoriesFacade;

    @EJB
    private MenuItemsFacadeLocal menuItemsFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;
    private Integer maxGuestsPerSlot;

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

    // Custom menu (á la carte): category + list of menu item cards
    private List<MenuSection> menuSections = new ArrayList<>();

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Map<String, String> params = ctx.getExternalContext().getRequestParameterMap();
        String idParam = params.get("restaurantId");

        Long id = null;
        try {
            if (idParam != null && !idParam.isEmpty()) {
                id = Long.valueOf(idParam);
            }
        } catch (NumberFormatException ignored) {
        }

        if (id != null) {
            restaurant = restaurantsFacade.find(id);
        }

        // fallback if id is invalid: pick first restaurant
        if (restaurant == null) {
            List<Restaurants> all = restaurantsFacade.findAll();
            if (all != null && !all.isEmpty()) {
                restaurant = all.get(0);
            }
        }
        if (restaurant == null) {
            return;
        }

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
        } catch (Exception ignored) {
        }

        // ==== Capacity (use minGuest * 3 as max demo) ====
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
                    if (img == null) {
                        continue;
                    }

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
                        if (sa == null && sb == null) {
                            return 0;
                        }
                        if (sa == null) {
                            return 1;
                        }
                        if (sb == null) {
                            return -1;
                        }
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
                    if (rev == null) {
                        continue;
                    }

                    Boolean deleted = rev.getIsDeleted();
                    if (deleted != null && deleted) {
                        continue;
                    }

                    Boolean approved = rev.getIsApproved();
                    if (approved != null && !approved) {
                        continue;
                    }

                    Integer r = rev.getRating();
                    if (r != null && r > 0) {
                        sumRating += r;
                        reviewCount++;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        avgRating = reviewCount > 0 ? (sumRating / reviewCount) : 0.0d;

        if (avgRating >= 4.9 && reviewCount >= 200) {
            badge = "TOP RATED";
        } else if (avgRating >= 4.8 && reviewCount >= 100) {
            badge = "VIP";
        } else if (reviewCount <= 10 && reviewCount > 0) {
            badge = "NEW";
        } else {
            badge = null;
        }

        // ==== Price per guest from combos ====
        pricePerGuestFrom = 0d;
        combos = new ArrayList<>();
        try {
            Collection<MenuCombos> comboEntities = restaurant.getMenuCombosCollection();
            if (comboEntities != null) {
                for (MenuCombos combo : comboEntities) {
                    if (combo == null) {
                        continue;
                    }

                    BigDecimal total = combo.getPriceTotal();
                    Integer guests = combo.getMinGuests();

                    double totalVal = (total != null) ? total.doubleValue() : 0d;
                    int minG = (guests != null && guests > 0) ? guests : 10;

                    // price per guest for "Starting from"
                    if (totalVal > 0 && minG > 0) {
                        double perGuest = totalVal / minG;
                        if (pricePerGuestFrom == 0d || perGuest < pricePerGuestFrom) {
                            pricePerGuestFrom = perGuest;
                        }
                    }

                    // build combo card list
                    ComboCard c = new ComboCard();
                    c.setComboId(combo.getComboId());
                    c.setName(safe(combo.getName()));
                    c.setDescription(safe(combo.getDescription()));
                    c.setPriceTotal(totalVal);
                    c.setMinGuests(minG);

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
        } catch (Exception ignored) {
        }

        if (pricePerGuestFrom <= 0d) {
            pricePerGuestFrom = 75d; // fallback demo
        }

        // ==== Event types (distinct) from bookings ====
        Set<String> typeSet = new LinkedHashSet<>();
        try {
            Collection<Bookings> bookings = restaurant.getBookingsCollection();
            if (bookings != null) {
                for (Bookings b : bookings) {
                    if (b == null) {
                        continue;
                    }
                    EventTypes et = b.getEventTypeId();
                    if (et != null && et.getName() != null) {
                        typeSet.add(et.getName());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (typeSet.isEmpty()) {
            typeSet.add("Wedding");
            typeSet.add("Corporate");
        }
        eventTypes = new ArrayList<>(typeSet);

        // ==== Custom menu (MenuCategories + MenuItems) ====
        loadMenuSections();
    }

    private void loadMenuSections() {
        menuSections = new ArrayList<>();
        if (restaurant == null || restaurant.getRestaurantId() == null) {
            return;
        }

        Long restaurantId = restaurant.getRestaurantId();

        List<MenuCategories> allCategories = menuCategoriesFacade.findAll();
        List<MenuItems> allItems = menuItemsFacade.findAll();
        if (allCategories == null) {
            allCategories = new ArrayList<>();
        }
        if (allItems == null) {
            allItems = new ArrayList<>();
        }

        // 1) Lấy danh sách categoryId thuộc restaurant này
        Set<Long> restaurantCategoryIds = new HashSet<>();
        List<MenuCategories> restaurantCats = new ArrayList<>();

        for (MenuCategories cat : allCategories) {
            if (cat == null) {
                continue;
            }

            if (cat.getRestaurantId() == null
                    || cat.getRestaurantId().getRestaurantId() == null
                    || !restaurantId.equals(cat.getRestaurantId().getRestaurantId())) {
                continue;
            }

            try {
                Boolean active = cat.getIsActive();
                if (active != null && !active) {
                    continue;
                }
            } catch (Exception ignored) {
            }

            if (cat.getCategoryId() != null) {
                restaurantCategoryIds.add(cat.getCategoryId());
            }
            restaurantCats.add(cat);
        }

        // 2) Build section theo category đúng
        for (MenuCategories cat : restaurantCats) {
            List<MenuItems> itemsForCategory = new ArrayList<>();

            for (MenuItems mi : allItems) {
                if (mi == null) {
                    continue;
                }

                // filter restaurant
                if (mi.getRestaurantId() == null
                        || mi.getRestaurantId().getRestaurantId() == null
                        || !restaurantId.equals(mi.getRestaurantId().getRestaurantId())) {
                    continue;
                }

                // status + deleted
                if (isSkipMenuItem(mi)) {
                    continue;
                }

                // đúng category
                if (mi.getCategoryId() == null || mi.getCategoryId().getCategoryId() == null) {
                    continue;
                }
                if (!cat.getCategoryId().equals(mi.getCategoryId().getCategoryId())) {
                    continue;
                }

                itemsForCategory.add(mi);
            }

            if (!itemsForCategory.isEmpty()) {
                menuSections.add(new MenuSection(cat, itemsForCategory));
            }
        }

        // 3) Gom nhóm OTHER: (a) category null, (b) category “orphan” không thuộc restaurant
        List<MenuItems> otherItems = new ArrayList<>();

        for (MenuItems mi : allItems) {
            if (mi == null) {
                continue;
            }

            if (mi.getRestaurantId() == null
                    || mi.getRestaurantId().getRestaurantId() == null
                    || !restaurantId.equals(mi.getRestaurantId().getRestaurantId())) {
                continue;
            }

            if (isSkipMenuItem(mi)) {
                continue;
            }

            Long catId = null;
            if (mi.getCategoryId() != null) {
                catId = mi.getCategoryId().getCategoryId();
            }

            // Nếu catId thuộc restaurantCategoryIds -> đã được add ở section chính
            if (catId != null && restaurantCategoryIds.contains(catId)) {
                continue;
            }

            // còn lại: null category hoặc orphan category => dồn vào Other
            otherItems.add(mi);
        }

        if (!otherItems.isEmpty()) {
            MenuCategories other = new MenuCategories();
            other.setCategoryId(-1L);
            other.setName("Other");
            other.setDescription("Uncategorized / mismatched category items");
            menuSections.add(new MenuSection(other, otherItems));
        }
    }

    private void loadCapacitySettingsFor(Restaurants r) {
        maxGuestsPerSlot = null;
        if (r == null) {
            return;
        }

        try {
            RestaurantCapacitySettings s = capacitySettingsFacade.findByRestaurant(r);
            if (s != null) {
                maxGuestsPerSlot = s.getMaxGuestsPerSlot();
            }
        } catch (Exception ignore) {
        }

        // fallback để UI không trống
        Integer min = (r.getMinGuestCount() != null) ? r.getMinGuestCount() : 0;
        if (maxGuestsPerSlot == null || maxGuestsPerSlot <= 0) {
            maxGuestsPerSlot = min; // hoặc min*3 nếu bro muốn giữ logic cũ khi chưa có settings
        }
        if (maxGuestsPerSlot < min) {
            maxGuestsPerSlot = min;
        }
    }

    private boolean isSkipMenuItem(MenuItems mi) {
        boolean skip = false;

        try {
            String status = mi.getStatus();
            if (status != null) {
                String s = status.trim().toUpperCase();
                if (!("ACTIVE".equals(s) || "AVAILABLE".equals(s))) {
                    skip = true;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Boolean deleted = mi.getIsDeleted();
            if (deleted != null && deleted) {
                skip = true;
            }
        } catch (Exception ignored) {
        }

        return skip;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    // ===== getters for xhtml =====
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

    public List<MenuSection> getMenuSections() {
        return menuSections;
    }

    // ====== Inner class: combo card ======
    public static class ComboCard implements Serializable {

        private Long comboId;
        private String name;
        private String description;
        private double priceTotal;
        private int minGuests;
        private String idealRange;
        private boolean vip;
        private String highlightTag;

        public Long getComboId() {
            return comboId;
        }

        public void setComboId(Long comboId) {
            this.comboId = comboId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getPriceTotal() {
            return priceTotal;
        }

        public void setPriceTotal(double priceTotal) {
            this.priceTotal = priceTotal;
        }

        public int getMinGuests() {
            return minGuests;
        }

        public void setMinGuests(int minGuests) {
            this.minGuests = minGuests;
        }

        public String getIdealRange() {
            return idealRange;
        }

        public void setIdealRange(String idealRange) {
            this.idealRange = idealRange;
        }

        public boolean isVip() {
            return vip;
        }

        public void setVip(boolean vip) {
            this.vip = vip;
        }

        public String getHighlightTag() {
            return highlightTag;
        }

        public void setHighlightTag(String highlightTag) {
            this.highlightTag = highlightTag;
        }

        public double getPricePerPerson() {
            return (priceTotal > 0) ? priceTotal : 0d;
        }

    }

    // ====== Inner class: menu item card (for UI) ======
    public static class MenuItemCard implements Serializable {

        private Long id;
        private String name;
        private String description;
        private BigDecimal pricePerPerson;
        private boolean vegetarian;
        private String imageUrl;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigDecimal getPricePerPerson() {
            return pricePerPerson;
        }

        public void setPricePerPerson(BigDecimal pricePerPerson) {
            this.pricePerPerson = pricePerPerson;
        }

        public boolean isVegetarian() {
            return vegetarian;
        }

        public void setVegetarian(boolean vegetarian) {
            this.vegetarian = vegetarian;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public void setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
        }

        // alias để EL dùng thống nhất như entity MenuItems
        public Long getMenuItemId() {
            return id;
        }

        public void setMenuItemId(Long v) {
            this.id = v;
        }

        public boolean getIsVegetarian() {
            return vegetarian;
        }

        public void setIsVegetarian(boolean v) {
            this.vegetarian = v;
        }

    }

    // ====== Inner class: menu section (category + items) ======
    public static class MenuSection implements Serializable {

        private MenuCategories category;
        private List<MenuItems> items;

        public MenuSection(MenuCategories category, List<MenuItems> items) {
            this.category = category;
            this.items = items;
        }

        public MenuCategories getCategory() {
            return category;
        }

        public void setCategory(MenuCategories category) {
            this.category = category;
        }

        public List<MenuItems> getItems() {
            return items;
        }

        public void setItems(List<MenuItems> items) {
            this.items = items;
        }

    }

    public boolean isHasReviews() {
        return reviewCount > 0;
    }

    public int getRatingPercent() {
        double pct = (avgRating / 5.0d) * 100.0d;
        if (pct < 0) {
            pct = 0;
        }
        if (pct > 100) {
            pct = 100;
        }
        return (int) Math.round(pct);
    }

}
