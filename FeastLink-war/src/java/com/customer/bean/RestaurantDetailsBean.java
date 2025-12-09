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
import com.mypack.sessionbean.MenuCategoriesFacadeLocal;
import com.mypack.sessionbean.MenuItemsFacadeLocal;

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

        // fallback if id is invalid: pick first restaurant
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

        // ==== Capacity (use minGuest * 3 as max demo) ====
        Integer minGuests = restaurant.getMinGuestCount();
        capacityMin = (minGuests != null && minGuests > 0) ? minGuests : 30;
        capacityMax = capacityMin * 3;

        // ==== Images ====
        heroImageUrl = null;
        galleryImages = new ArrayList<>();
        try {
            Collection<RestaurantImages> images = restaurant.getRestaurantImagesCollection();
            if (images != null) {
                RestaurantImages first = null;
                for (RestaurantImages img : images) {
                    if (img == null) continue;
                    if (Boolean.TRUE.equals(img.getIsPrimary()) && heroImageUrl == null) {
                        heroImageUrl = safe(img.getImageUrl());
                    }
                    galleryImages.add(safe(img.getImageUrl()));
                    if (first == null) first = img;
                }
                if ((heroImageUrl == null || heroImageUrl.isEmpty()) && first != null) {
                    heroImageUrl = safe(first.getImageUrl());
                }
            }
        } catch (Exception ignored) {}
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

        // ==== Price per guest from combos ====
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
        } catch (Exception ignored) {}

        if (pricePerGuestFrom <= 0d) {
            pricePerGuestFrom = 75d; // fallback demo
        }

        // ==== Event types (distinct) from bookings ====
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

        // ==== Custom menu (MenuCategories + MenuItems) ====
        loadMenuSections();
    }

    private void loadMenuSections() {
        menuSections = new ArrayList<>();
        if (restaurant == null) return;

        Long restaurantId = restaurant.getRestaurantId();
        if (restaurantId == null) return;

        List<MenuCategories> allCategories = menuCategoriesFacade.findAll();
        List<MenuItems> allItems = menuItemsFacade.findAll();

        if (allCategories == null) allCategories = new ArrayList<>();
        if (allItems == null) allItems = new ArrayList<>();

        for (MenuCategories cat : allCategories) {
            if (cat == null) continue;

            // filter by restaurant
            if (cat.getRestaurantId() == null
                    || cat.getRestaurantId().getRestaurantId() == null
                    || !restaurantId.equals(cat.getRestaurantId().getRestaurantId())) {
                continue;
            }

            // only active categories if there is IsActive flag
            try {
                Boolean active = cat.getIsActive();
                if (active != null && !active) {
                    continue;
                }
            } catch (Exception ignored) {}

            List<MenuItemCard> itemsForCategory = new ArrayList<>();

            for (MenuItems mi : allItems) {
                if (mi == null) continue;

                // filter by restaurant
                if (mi.getRestaurantId() == null
                        || mi.getRestaurantId().getRestaurantId() == null
                        || !restaurantId.equals(mi.getRestaurantId().getRestaurantId())) {
                    continue;
                }

                // filter by category
                if (mi.getCategoryId() == null
                        || mi.getCategoryId().getCategoryId() == null
                        || !cat.getCategoryId().equals(mi.getCategoryId().getCategoryId())) {
                    continue;
                }

                boolean skip = false;
                try {
                    String status = mi.getStatus();
                    if (status != null && !"AVAILABLE".equalsIgnoreCase(status)) {
                        skip = true;
                    }
                } catch (Exception ignored) {}

                try {
                    Boolean deleted = mi.getIsDeleted();
                    if (deleted != null && deleted) {
                        skip = true;
                    }
                } catch (Exception ignored) {}

                if (skip) continue;

                MenuItemCard card = new MenuItemCard();
                card.setId(mi.getMenuItemId());
                card.setName(safe(mi.getName()));
                card.setDescription(safe(mi.getDescription()));

                try {
                    card.setImageUrl(mi.getImageUrl());
                } catch (Exception ignored) {}

                try {
                    card.setPricePerPerson(mi.getPricePerPerson());
                } catch (Exception ignored) {}

                try {
                    Boolean veg = mi.getIsVegetarian();
                    if (veg != null) {
                        card.setVegetarian(veg);
                    }
                } catch (Exception ignored) {}

                itemsForCategory.add(card);
            }

            if (!itemsForCategory.isEmpty()) {
                menuSections.add(new MenuSection(cat, itemsForCategory));
            }
        }
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
    }

    // ====== Inner class: menu section (category + items) ======
    public static class MenuSection implements Serializable {
        private MenuCategories category;
        private List<MenuItemCard> items;

        public MenuSection(MenuCategories category, List<MenuItemCard> items) {
            this.category = category;
            this.items = items;
        }

        public MenuCategories getCategory() {
            return category;
        }

        public void setCategory(MenuCategories category) {
            this.category = category;
        }

        public List<MenuItemCard> getItems() {
            return items;
        }

        public void setItems(List<MenuItemCard> items) {
            this.items = items;
        }
    }
}
