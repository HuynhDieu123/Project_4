package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.MenuCombos;
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.entity.RestaurantImages;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
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
import com.mypack.entity.RestaurantCapacitySettings;
import com.mypack.sessionbean.RestaurantCapacitySettingsFacadeLocal;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import com.mypack.entity.MenuItems;

/**
 * Bean dùng cho trang Customer/restaurants.xhtml Đọc dữ liệu nhà hàng từ DB và
 * chuyển thành JSON để restaurants.js sử dụng.
 */
@Named("customerRestaurantsBean")
@RequestScoped
public class CustomerRestaurantsBean implements Serializable {

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private List<RestaurantCard> restaurantCards;
    private List<String> cityOptions;
    private List<String> areaOptions;
    private List<String> eventTypeOptions;
    private List<String> cuisineOptions;

    @EJB
    private EventTypesFacadeLocal eventTypesFacade;

    @EJB
    private RestaurantCapacitySettingsFacadeLocal capacitySettingsFacade;

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

            // ===== CHỈ LẤY NHÀ HÀNG STATUS = 'ACTIVE' =====
            try {
                String status = r.getStatus();
                if (status == null || !"ACTIVE".equalsIgnoreCase(status)) {
                    // Nếu không phải ACTIVE (PENDING / PRIVATE / v.v...) thì bỏ qua
                    continue;
                }
            } catch (Exception ignore) {
                // Nếu đọc status lỗi thì cũng bỏ qua nhà hàng này
                continue;
            }
            // ===============================================

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

            // cuisines for this restaurant (from DB via relationship)
            Set<String> cs = new TreeSet<>();
            try {
                Collection<MenuItems> items = r.getMenuItemsCollection(); // nếu tên khác thì đổi getter
                if (items != null) {
                    for (MenuItems mi : items) {
                        if (mi == null) {
                            continue;
                        }
                        if (mi.getCuisineId() != null && mi.getCuisineId().getName() != null) {
                            String cname = mi.getCuisineId().getName().trim();
                            if (!cname.isEmpty()) {
                                cs.add(cname);
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }
            card.setCuisines(new ArrayList<>(cs));

            // Booking / cancel / deposit
            card.setAdvanceBookingDays(nvlInt(r.getMinDaysInAdvance(), 0));
            card.setCancelDays(nvlInt(r.getCancelFullRefundDays(), 0));
            card.setDepositPercent(nvlDecimal(r.getDefaultDepositPercent(), 0d));

// ===== Capacity settings (chỉ gọi 1 lần) =====
            RestaurantCapacitySettings st = null;
            try {
                st = capacitySettingsFacade.findByRestaurant(r);
            } catch (Exception ignore) {
            }

            Integer maxGuestsPerSlot = (st != null) ? st.getMaxGuestsPerSlot() : null;
            Integer maxBookingsPerDay = (st != null) ? st.getMaxBookingsPerDay() : null;
            Integer slotDurationMin = (st != null) ? st.getDefaultSlotDurationMin() : null;

// sẽ dùng cho JS tính FULL theo ngày/giờ
            card.setMaxBookingsPerDay(maxBookingsPerDay);
            card.setSlotDurationMin(slotDurationMin);

            int minGuests = nvlInt(r.getMinGuestCount(), 0);
            int capacityMin = (minGuests > 0) ? minGuests : 30;

            int capacityMax = (maxGuestsPerSlot != null && maxGuestsPerSlot > 0)
                    ? maxGuestsPerSlot
                    : capacityMin * 3; // fallback nếu chưa có settings

            if (capacityMax < capacityMin) {
                capacityMax = capacityMin;
            }

            card.setCapacityMin(capacityMin);
            card.setCapacityMax(capacityMax);

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
                imageUrl = null;
            }
            card.setImage(imageUrl);

            // Giá / bàn từ combo: lấy combo có PriceTotal nhỏ nhất + đếm combos
            double pricePerTable = 0d;
            int combosCount = 0;

            try {
                Collection<MenuCombos> combos = r.getMenuCombosCollection();
                if (combos != null) {
                    for (MenuCombos combo : combos) {
                        if (combo == null) {
                            continue;
                        }

                        combosCount++;

                        BigDecimal total = combo.getPriceTotal();
                        if (total != null) {
                            double p = total.doubleValue();
                            if (pricePerTable == 0d || p < pricePerTable) {
                                pricePerTable = p;
                            }
                        }
                    }
                }
            } catch (Exception ignore) {
            }

            card.setCombosCount(combosCount);
            card.setHasCombos(combosCount > 0);

            if (pricePerTable <= 0d) {
                pricePerTable = 75d; // demo default USD
            }
            card.setPricePerGuest(pricePerTable);

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

            // ===== Build bookings lite for availability by datetime =====
            List<BookingLite> blist = new ArrayList<>();

            try {
                Collection<Bookings> bookings = r.getBookingsCollection();
                if (bookings != null) {
                    for (Bookings b : bookings) {
                        if (b == null) {
                            continue;
                        }

                        String bs = null;
                        try {
                            bs = b.getBookingStatus();
                        } catch (Exception ignore) {
                        }

                        // chỉ tính booking còn chiếm chỗ (loại CANCELLED/REJECTED)
                        if (bs == null) {
                            continue;
                        }
                        String s = bs.trim().toUpperCase();
                        if ("CANCELLED".equals(s) || "REJECTED".equals(s)) {
                            continue;
                        }

                        BookingLite x = new BookingLite();

                        // EventDate -> yyyy-MM-dd
                        String dateStr = "";
                        try {
                            Object d = b.getEventDate();
                            if (d != null) {
                                dateStr = d.toString();
                            }
                        } catch (Exception ignore) {
                        }
                        x.setDate(dateStr);

                        // Start/End -> HH:mm
                        String stt = null;
                        String ett = null;

                        try {
                            Object t1 = b.getStartTime();
                            if (t1 != null) {
                                String v = t1.toString();       // thường HH:mm:ss
                                stt = v.length() >= 5 ? v.substring(0, 5) : v;
                            }
                        } catch (Exception ignore) {
                        }

                        try {
                            Object t2 = b.getEndTime();
                            if (t2 != null) {
                                String v = t2.toString();
                                ett = v.length() >= 5 ? v.substring(0, 5) : v;
                            }
                        } catch (Exception ignore) {
                        }

                        x.setStart(stt);
                        x.setEnd(ett);

                        x.setGuests(nvlInt(b.getGuestCount(), 0));
                        x.setStatus(bs);

                        blist.add(x);
                    }
                }
            } catch (Exception ignore) {
            }

            card.setBookings(blist);

            // Availability demo
            String availability = "available";
            if (avgRating >= 4.8 && countRating > 50) {
                availability = "limited";
            }
            card.setAvailability(availability);

            restaurantCards.add(card);
        }
        buildFilterOptions();
        loadEventTypeOptions();

    }

    private void buildFilterOptions() {
        Set<String> cities = new TreeSet<>();
        Set<String> areas = new TreeSet<>();
        Set<String> events = new TreeSet<>();
        Set<String> cuisines = new TreeSet<>();

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
                if (c.getCuisines() != null) {
                    for (String cu : c.getCuisines()) {
                        if (cu != null && !cu.isBlank()) {
                            cuisines.add(cu);
                        }
                    }
                }

            }
        }

        cityOptions = new ArrayList<>(cities);
        areaOptions = new ArrayList<>(areas);
        eventTypeOptions = new ArrayList<>(events);
        cuisineOptions = new ArrayList<>(cuisines);

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
            // cuisines
            sb.append("\"cuisines\":[");
            List<String> cus = c.getCuisines();
            for (int j = 0; j < cus.size(); j++) {
                if (j > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(escapeJson(cus.get(j))).append("\"");
            }
            sb.append("],");

            sb.append("\"advanceBookingDays\":").append(c.getAdvanceBookingDays()).append(",");
            sb.append("\"cancelDays\":").append(c.getCancelDays()).append(",");
            sb.append("\"depositPercent\":")
                    .append(String.format(Locale.US, "%.0f", c.getDepositPercent()))
                    .append(",");
            // combos
            sb.append("\"hasCombos\":").append(c.isHasCombos()).append(",");
            sb.append("\"combosCount\":").append(c.getCombosCount()).append(",");

// settings
            if (c.getMaxBookingsPerDay() == null) {
                sb.append("\"maxBookingsPerDay\":null,");
            } else {
                sb.append("\"maxBookingsPerDay\":").append(c.getMaxBookingsPerDay()).append(",");
            }

            if (c.getSlotDurationMin() == null) {
                sb.append("\"slotDurationMin\":null,");
            } else {
                sb.append("\"slotDurationMin\":").append(c.getSlotDurationMin()).append(",");
            }

// bookings lite
            sb.append("\"bookings\":[");
            List<BookingLite> bl = c.getBookings();
            for (int k = 0; k < bl.size(); k++) {
                BookingLite x = bl.get(k);
                if (k > 0) {
                    sb.append(",");
                }
                sb.append("{");
                appendJsonField(sb, "date", x.getDate(), true);
                appendJsonField(sb, "start", x.getStart(), true);
                appendJsonField(sb, "end", x.getEnd(), true);
                sb.append("\"guests\":").append(x.getGuests()).append(",");
                appendJsonField(sb, "status", x.getStatus(), false);
                sb.append("}");
            }
            sb.append("],");

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

    private void loadEventTypeOptions() {
        eventTypeOptions = eventTypesFacade.findAll()
                .stream()
                .map(EventTypes::getName) // lấy cột Name
                .distinct()
                .sorted()
                .toList();
    }

    public static class BookingLite implements Serializable {

        private String date;   // yyyy-MM-dd
        private String start;  // HH:mm
        private String end;    // HH:mm
        private int guests;
        private String status;

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public int getGuests() {
            return guests;
        }

        public void setGuests(int guests) {
            this.guests = guests;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
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
        private List<String> cuisines = new ArrayList<>();

        private int advanceBookingDays;
        private int cancelDays;
        private double depositPercent;
        private String availability;

        // combos
        private boolean hasCombos;
        private int combosCount;

// capacity settings for FULL calculation
        private Integer maxBookingsPerDay;
        private Integer slotDurationMin;

// bookings lite for date/time availability
        private List<BookingLite> bookings = new ArrayList<>();

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

        public List<String> getCuisines() {
            return cuisines;
        }

        public void setCuisines(List<String> cuisines) {
            this.cuisines = (cuisines != null) ? cuisines : new ArrayList<>();
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

        public boolean isHasCombos() {
            return hasCombos;
        }

        public void setHasCombos(boolean hasCombos) {
            this.hasCombos = hasCombos;
        }

        public int getCombosCount() {
            return combosCount;
        }

        public void setCombosCount(int combosCount) {
            this.combosCount = combosCount;
        }

        public Integer getMaxBookingsPerDay() {
            return maxBookingsPerDay;
        }

        public void setMaxBookingsPerDay(Integer maxBookingsPerDay) {
            this.maxBookingsPerDay = maxBookingsPerDay;
        }

        public Integer getSlotDurationMin() {
            return slotDurationMin;
        }

        public void setSlotDurationMin(Integer slotDurationMin) {
            this.slotDurationMin = slotDurationMin;
        }

        public List<BookingLite> getBookings() {
            return bookings;
        }

        public void setBookings(List<BookingLite> bookings) {
            this.bookings = (bookings != null) ? bookings : new ArrayList<>();
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

    public List<String> getCuisineOptions() {
        return cuisineOptions != null ? cuisineOptions : Collections.emptyList();
    }

}
