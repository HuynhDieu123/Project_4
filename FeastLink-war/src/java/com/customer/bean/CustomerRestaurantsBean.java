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
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Named("customerRestaurantsBean")
@RequestScoped
public class CustomerRestaurantsBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private List<RestaurantCard> restaurantCards;
    private List<String> cityOptions;
    private List<String> areaOptions;
    private List<String> eventTypeOptions;

    @PostConstruct
    public void init() {
        restaurantCards = new ArrayList<>();

        List<Restaurants> all = restaurantsFacade != null ? restaurantsFacade.findAll() : null;
        if (all == null) {
            return;
        }

        for (Restaurants r : all) {
            if (r == null) {
                continue;
            }

            RestaurantCard card = new RestaurantCard();
            card.setId(r.getRestaurantId());
            card.setName(safe(r.getName()));
            card.setDescription(safe(r.getDescription()));

            // City / Area
            String cityName = "";
            String areaName = "";
            try {
                if (r.getAreaId() != null) {
                    areaName = safe(r.getAreaId().getName());
                    if (r.getAreaId().getCityId() != null) {
                        cityName = safe(r.getAreaId().getCityId().getName());
                    }
                }
            } catch (Exception ignore) {
            }
            card.setCity(cityName);
            card.setDistrict(areaName);

            // Booking / cancel / deposit
            card.setAdvanceBookingDays(nvlInt(r.getMinDaysInAdvance(), 0));
            card.setCancelDays(nvlInt(r.getCancelFullRefundDays(), 0));
            card.setDepositPercent(nvlDecimal(r.getDefaultDepositPercent(), 0d));

            // Sức chứa: dùng MinGuestCount, Max gấp 3 lần (demo)
            int minGuests = nvlInt(r.getMinGuestCount(), 0);
            int maxGuests = minGuests > 0 ? minGuests * 3 : 200;
            card.setCapacityMin(minGuests > 0 ? minGuests : 30);
            card.setCapacityMax(maxGuests);

            // Ảnh: lấy từ RestaurantImages (ưu tiên IsPrimary)
            String imageUrl = null;
            try {
                Collection<RestaurantImages> images = r.getRestaurantImagesCollection();
                if (images != null && !images.isEmpty()) {
                    RestaurantImages first = null;
                    for (RestaurantImages img : images) {
                        if (img == null) {
                            continue;
                        }
                        if (Boolean.TRUE.equals(img.getIsPrimary())) {
                            imageUrl = safe(img.getImageUrl());
                            break;
                        }
                        if (first == null) {
                            first = img;
                        }
                    }
                    if (imageUrl == null && first != null) {
                        imageUrl = safe(first.getImageUrl());
                    }
                }
            } catch (Exception ignore) {
            }
            if (imageUrl == null || imageUrl.isEmpty()) {
                // fallback nếu chưa có ảnh
                imageUrl = "/FeastLink-war/resources/images/restaurant-placeholder.jpg";
            }
            card.setImage(imageUrl);

            // Giá / bàn từ combo: lấy combo có PriceTotal nhỏ nhất
            double pricePerTable = 0d;
            try {
                Collection<MenuCombos> combos = r.getMenuCombosCollection();
                if (combos != null) {
                    for (MenuCombos combo : combos) {
                        if (combo == null) {
                            continue;
                        }
                        BigDecimal total = combo.getPriceTotal();
                        if (total != null) {
                            double p = total.doubleValue(); // giá 1 bàn (combo cho 1 bàn)
                            if (pricePerTable == 0d || p < pricePerTable) {
                                pricePerTable = p;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            if (pricePerTable <= 0d) {
                pricePerTable = 75d; // demo default USD
            }
            card.setPricePerGuest(pricePerTable); // tạm reuse field này, ý nghĩa là "price per table"

            // Rating & reviews từ RestaurantReviews
            double sumRating = 0d;
            int countRating = 0;
            try {
                Collection<RestaurantReviews> reviews = r.getRestaurantReviewsCollection();
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

                        Integer rating = rev.getRating();
                        if (rating != null && rating > 0) {
                            sumRating += rating;
                            countRating++;
                        }

                    }
                }
            } catch (Exception ignore) {
            }
            double avgRating = countRating > 0 ? (sumRating / countRating) : 4.7d;
            card.setRating(avgRating);
            card.setReviews(countRating);

            String badge = null;
            if (avgRating >= 4.9 && countRating >= 100) {
                badge = "TOP RATED";
            } else if (avgRating >= 4.8 && countRating >= 50) {
                badge = "VIP";
            } else if (countRating <= 10 && countRating > 0) {
                badge = "NEW";
            }
            card.setBadge(badge);

            // Event types: distinct từ Bookings
            Set<String> types = new HashSet<>();
            try {
                Collection<Bookings> bookings = r.getBookingsCollection();
                if (bookings != null) {
                    for (Bookings b : bookings) {
                        if (b == null) {
                            continue;
                        }
                        EventTypes et = b.getEventTypeId();
                        if (et != null && et.getName() != null) {
                            types.add(et.getName());
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            if (types.isEmpty()) {
                // fallback demo
                types.add("Wedding");
                types.add("Corporate");
            }
            card.setEventTypes(new ArrayList<>(types));

            // Availability demo
            String availability = "available";
            if (avgRating >= 4.8 && countRating > 50) {
                availability = "limited";
            }
            card.setAvailability(availability);

            restaurantCards.add(card);
        }
        buildFilterOptions();

    }

    private void buildFilterOptions() {
        Set<String> cities = new TreeSet<>();
        Set<String> areas = new TreeSet<>();
        Set<String> events = new TreeSet<>();

        if (restaurantCards != null) {
            for (RestaurantCard c : restaurantCards) {
                if (c == null) {
                    continue;
                }

                if (c.getCity() != null && !c.getCity().isBlank()) {
                    cities.add(c.getCity());
                }
                if (c.getDistrict() != null && !c.getDistrict().isBlank()) {
                    areas.add(c.getDistrict());
                }
                if (c.getEventTypes() != null) {
                    for (String et : c.getEventTypes()) {
                        if (et != null && !et.isBlank()) {
                            events.add(et);
                        }
                    }
                }
            }
        }

        cityOptions = new ArrayList<>(cities);
        areaOptions = new ArrayList<>(areas);
        eventTypeOptions = new ArrayList<>(events);
    }

    private int nvlInt(Integer v, int dft) {
        return v != null ? v : dft;
    }

    private double nvlDecimal(BigDecimal v, double dft) {
        return v != null ? v.doubleValue() : dft;
    }

    private String safe(String s) {
        return s != null ? s : "";
    }

    public List<RestaurantCard> getRestaurantCards() {
        return restaurantCards;
    }

    /**
     * JSON string để nhúng vào JS.
     */
    public String getRestaurantsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < restaurantCards.size(); i++) {
            RestaurantCard c = restaurantCards.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{");
            appendJsonField(sb, "id", String.valueOf(c.getId()), true);
            appendJsonField(sb, "name", c.getName(), true);
            appendJsonField(sb, "image", c.getImage(), true);
            appendJsonField(sb, "city", c.getCity(), true);
            appendJsonField(sb, "district", c.getDistrict(), true);
            appendJsonField(sb, "description", c.getDescription(), true);

            sb.append("\"rating\":")
                    .append(String.format(Locale.US, "%.1f", c.getRating()))
                    .append(",");
            sb.append("\"reviews\":").append(c.getReviews()).append(",");

            if (c.getBadge() != null && !c.getBadge().isEmpty()) {
                appendJsonField(sb, "badge", c.getBadge(), true);
            } else {
                sb.append("\"badge\":null,");
            }

            sb.append("\"capacityMin\":").append(c.getCapacityMin()).append(",");
            sb.append("\"capacityMax\":").append(c.getCapacityMax()).append(",");

            sb.append("\"pricePerGuest\":")
                    .append(String.format(Locale.US, "%.2f", c.getPricePerGuest()))
                    .append(",");

            // eventTypes
            sb.append("\"eventTypes\":[");
            List<String> types = c.getEventTypes();
            for (int j = 0; j < types.size(); j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(types.get(j))).append("\"");
            }
            sb.append("],");

            sb.append("\"advanceBookingDays\":").append(c.getAdvanceBookingDays()).append(",");
            sb.append("\"cancelDays\":").append(c.getCancelDays()).append(",");
            sb.append("\"depositPercent\":")
                    .append(String.format(Locale.US, "%.0f", c.getDepositPercent()))
                    .append(",");

            appendJsonField(sb, "availability", c.getAvailability(), false);

            sb.append("}");
        }

        sb.append("]");
        return sb.toString();
    }

    private void appendJsonField(StringBuilder sb, String name, String value, boolean withComma) {
        sb.append("\"").append(escapeJson(name)).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append("\"").append(escapeJson(value)).append("\"");
        }
        if (withComma) {
            sb.append(",");
        }
    }

    private String escapeJson(String s) {
        if (s == null) {
            return "";
        }
        String result = s;
        result = result.replace("\\", "\\\\");
        result = result.replace("\"", "\\\"");
        result = result.replace("\r", "\\r");
        result = result.replace("\n", "\\n");
        return result;
    }

    /**
     * Model cho 1 card nhà hàng.
     */
    public static class RestaurantCard implements Serializable {

        private Long id;
        private String name;
        private String image;
        private String city;
        private String district;
        private String description;
        private double rating;
        private int reviews;
        private String badge;
        private int capacityMin;
        private int capacityMax;
        private double pricePerGuest;
        private List<String> eventTypes = new ArrayList<>();
        private int advanceBookingDays;
        private int cancelDays;
        private double depositPercent;
        private String availability;

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

        public String getImage() {
            return image;
        }

        public void setImage(String image) {
            this.image = image;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public double getRating() {
            return rating;
        }

        public void setRating(double rating) {
            this.rating = rating;
        }

        public int getReviews() {
            return reviews;
        }

        public void setReviews(int reviews) {
            this.reviews = reviews;
        }

        public String getBadge() {
            return badge;
        }

        public void setBadge(String badge) {
            this.badge = badge;
        }

        public int getCapacityMin() {
            return capacityMin;
        }

        public void setCapacityMin(int capacityMin) {
            this.capacityMin = capacityMin;
        }

        public int getCapacityMax() {
            return capacityMax;
        }

        public void setCapacityMax(int capacityMax) {
            this.capacityMax = capacityMax;
        }

        public double getPricePerGuest() {
            return pricePerGuest;
        }

        public void setPricePerGuest(double pricePerGuest) {
            this.pricePerGuest = pricePerGuest;
        }

        public List<String> getEventTypes() {
            return eventTypes;
        }

        public void setEventTypes(List<String> eventTypes) {
            this.eventTypes = eventTypes;
        }

        public int getAdvanceBookingDays() {
            return advanceBookingDays;
        }

        public void setAdvanceBookingDays(int advanceBookingDays) {
            this.advanceBookingDays = advanceBookingDays;
        }

        public int getCancelDays() {
            return cancelDays;
        }

        public void setCancelDays(int cancelDays) {
            this.cancelDays = cancelDays;
        }

        public double getDepositPercent() {
            return depositPercent;
        }

        public void setDepositPercent(double depositPercent) {
            this.depositPercent = depositPercent;
        }

        public String getAvailability() {
            return availability;
        }

        public void setAvailability(String availability) {
            this.availability = availability;
        }

    }

    public List<String> getCityOptions() {
        return cityOptions;
    }

    public List<String> getAreaOptions() {
        return areaOptions;
    }

    public List<String> getEventTypeOptions() {
        return eventTypeOptions;
    }

}
