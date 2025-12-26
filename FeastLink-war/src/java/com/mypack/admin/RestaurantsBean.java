package com.mypack.admin;

import com.mypack.entity.Areas;
import com.mypack.entity.Cities;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.sessionbean.AreasFacadeLocal;
import com.mypack.sessionbean.CitiesFacadeLocal;
import com.mypack.sessionbean.RestaurantReviewsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.*;

@Named("restaurantsBean")
@ViewScoped
public class RestaurantsBean implements Serializable {

    private static final long serialVersionUID = 1L;

    // ===== AUTO-BLOCK RULE =====
    private static final int BAD_THRESHOLD = 2;               // rating <= 2 => bad
    private static final int MIN_REVIEWS_TO_JUDGE = 10;
    private static final double MIN_AVG_RATING = 2.8;
    private static final double MAX_BAD_RATE = 0.50;

    @EJB private RestaurantsFacadeLocal restaurantsFacade;
    @EJB private CitiesFacadeLocal citiesFacade;
    @EJB private AreasFacadeLocal areasFacade;
    @EJB private RestaurantReviewsFacadeLocal reviewsFacade;

    // ===== FILTERS =====
    private String keyword = "";
    private String statusFilter = "ACTIVE";
    private Integer cityFilterId = 0;

    // ALL | NO | 1_2 | 3 | 4 | 5
    private String avgStarFilter = "ALL";

    // ===== PAGINATION =====
    private int currentPage = 1;
    private int pageSize = 10;
    private int totalPages = 1;

    // ===== ADD RESTAURANT =====
    private Restaurants newRestaurant = new Restaurants();
    private Integer newCityId = 0;
    private Integer newAreaId = 0;

    // ===== SELECTED =====
    private Long selectedRestaurantId;

    // ===== VIEW MODAL =====
    private Restaurants viewRestaurant;

    // ===== DATA =====
    private List<Restaurants> allRestaurants = new ArrayList<>();
    private List<Restaurants> pageRestaurants = new ArrayList<>();
    private List<Cities> cityList = new ArrayList<>();
    private List<Areas> areaList = new ArrayList<>();

    // map.get(restaurantId) = new Object[]{avg(Double), total(Long), bad(Long)}
    private Map<Long, Object[]> statsMap = new HashMap<>();

    // ===== VIEW REVIEWS =====
    private List<RestaurantReviews> viewReviews = new ArrayList<>();
    private boolean reviewApprovedOnly = false;
    private String reviewRatingFilter = "BAD"; // ALL | BAD | 1..5
    private int reviewPage = 1;
    private int reviewPageSize = 6;
    private int reviewTotalPages = 1;
    private long reviewTotalCount = 0;

    @PostConstruct
    public void init() {
        cityList = citiesFacade.findAll();
        areaList = areasFacade.findAll();
        applyFilter();
    }

    // =========================================================
    // FILTER
    // =========================================================
    public void applyFilter() {
        List<Restaurants> source = restaurantsFacade.findAll();
        List<Restaurants> filtered = new ArrayList<>();

        for (Restaurants r : source) {
            if (r == null) continue;

            String rowStatus = r.getStatus();

            // nếu bạn muốn giữ pending thì bỏ dòng này
            if ("PENDING_APPROVAL".equals(rowStatus)) continue;

            boolean matchKeyword = (keyword == null || keyword.trim().isEmpty());
            if (!matchKeyword) {
                String kw = keyword.trim().toLowerCase();
                matchKeyword =
                        (r.getName() != null && r.getName().toLowerCase().contains(kw))
                                || (r.getEmail() != null && r.getEmail().toLowerCase().contains(kw))
                                || (r.getPhone() != null && r.getPhone().toLowerCase().contains(kw))
                                || (r.getContactPerson() != null && r.getContactPerson().toLowerCase().contains(kw));
            }

            boolean matchStatus = (statusFilter == null || "ALL".equals(statusFilter))
                    || (rowStatus != null && rowStatus.equals(statusFilter));

            boolean matchCity = (cityFilterId == null || cityFilterId == 0);
            if (!matchCity) {
                Areas area = r.getAreaId();
                if (area != null && area.getCityId() != null) {
                    matchCity = cityFilterId.equals(area.getCityId().getCityId());
                }
            }

            if (matchKeyword && matchStatus && matchCity) filtered.add(r);
        }

        // Build stats for list (✅ avg double)
        buildStatsMapForRestaurants(filtered);

        // Apply avg star filter
        if (avgStarFilter != null && !"ALL".equals(avgStarFilter)) {
            List<Restaurants> byStar = new ArrayList<>();
            for (Restaurants r : filtered) {
                Long id = r.getRestaurantId();
                double avg = avgRating(id);
                long total = totalReviews(id);
                if (matchAvgStarFilter(avg, total)) byStar.add(r);
            }
            filtered = byStar;
        }

        allRestaurants = filtered;
        currentPage = 1;
        calculatePagination();
        loadPage();
    }

    public void resetFilter() {
        keyword = "";
        statusFilter = "ACTIVE";
        cityFilterId = 0;
        avgStarFilter = "ALL";
        applyFilter();
    }

    private boolean matchAvgStarFilter(double avg, long total) {
        switch (avgStarFilter) {
            case "NO":
                return total == 0;
            case "1_2":
                return total > 0 && avg < 2.5;
            case "3":
                return total > 0 && avg >= 2.5 && avg < 3.5;
            case "4":
                return total > 0 && avg >= 3.5 && avg < 4.5;
            case "5":
                return total > 0 && avg >= 4.5;
            default:
                return true;
        }
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    private void calculatePagination() {
        totalPages = (int) Math.ceil(allRestaurants.size() / (double) pageSize);
        if (totalPages < 1) totalPages = 1;
        if (currentPage > totalPages) currentPage = totalPages;
        if (currentPage < 1) currentPage = 1;
    }

    public void loadPage() {
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allRestaurants.size());
        if (start < end) pageRestaurants = allRestaurants.subList(start, end);
        else pageRestaurants = new ArrayList<>();
    }

    public void nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
            loadPage();
        }
    }

    public void prevPage() {
        if (currentPage > 1) {
            currentPage--;
            loadPage();
        }
    }

    // =========================================================
    // STATS MAP
    // =========================================================
    private void buildStatsMapForRestaurants(List<Restaurants> list) {
        statsMap = new HashMap<>();
        List<Long> ids = new ArrayList<>();
        for (Restaurants r : list) {
            if (r != null && r.getRestaurantId() != null) ids.add(r.getRestaurantId());
        }
        if (ids.isEmpty()) return;

        int chunkSize = 400;
        for (int i = 0; i < ids.size(); i += chunkSize) {
            List<Long> sub = ids.subList(i, Math.min(i + chunkSize, ids.size()));
            Map<Long, Object[]> part = reviewsFacade.statsByRestaurantIds(sub, BAD_THRESHOLD);
            if (part != null) statsMap.putAll(part);
        }

        for (Long id : ids) {
            statsMap.putIfAbsent(id, new Object[]{0.0, 0L, 0L});
        }
    }

    // ✅ Avg REAL
    public double avgRating(Long restaurantId) {
        Object[] s = statsMap.get(restaurantId);
        if (s == null || s[0] == null) return 0.0;
        return ((Number) s[0]).doubleValue();
    }

    public long totalReviews(Long restaurantId) {
        Object[] s = statsMap.get(restaurantId);
        if (s == null || s[1] == null) return 0L;
        return ((Number) s[1]).longValue();
    }

    public long badReviews(Long restaurantId) {
        Object[] s = statsMap.get(restaurantId);
        if (s == null || s[2] == null) return 0L;
        return ((Number) s[2]).longValue();
    }

    public double badRatePercent(Long restaurantId) {
        long total = totalReviews(restaurantId);
        if (total <= 0) return 0.0;
        return badReviews(restaurantId) * 100.0 / (double) total;
    }

    // =========================================================
    // AREA DISPLAY FIX (nếu dữ liệu dính " - City#1")
    // =========================================================
    public String displayAreaName(Areas area) {
        if (area == null || area.getName() == null) return "—";
        String n = area.getName().trim();
        int idx = n.indexOf(" - ");
        if (idx > 0) return n.substring(0, idx).trim();
        return n;
    }

    // =========================================================
    // QUALITY / AUTO-BLOCK
    // =========================================================
    public boolean isAutoBlockCandidate(Long restaurantId) {
        long total = totalReviews(restaurantId);
        if (total < MIN_REVIEWS_TO_JUDGE) return false;

        double avg = avgRating(restaurantId);
        double badRate = badReviews(restaurantId) / (double) total;

        return (avg < MIN_AVG_RATING) || (badRate >= MAX_BAD_RATE);
    }

    public String qualityLabel(Long restaurantId) {
        long total = totalReviews(restaurantId);
        if (total < MIN_REVIEWS_TO_JUDGE) return "Not enough data";
        if (isAutoBlockCandidate(restaurantId)) return "Bad";
        double avg = avgRating(restaurantId);
        double badRate = badReviews(restaurantId) / (double) total;
        if (avg >= 4.2 && badRate <= 0.10) return "Excellent";
        return "Good";
    }

    public String qualityCssClass(Long restaurantId) {
        String label = qualityLabel(restaurantId);
        switch (label) {
            case "Bad": return "bg-red-100 text-red-700";
            case "Excellent": return "bg-green-100 text-green-700";
            case "Good": return "bg-blue-100 text-blue-700";
            default: return "bg-gray-100 text-gray-700";
        }
    }

    // =========================================================
    // VIEW MODAL
    // =========================================================
    public void openView() {
        if (selectedRestaurantId == null) return;

        viewRestaurant = restaurantsFacade.find(selectedRestaurantId);

        // đảm bảo stats có cho modal
        if (viewRestaurant != null && viewRestaurant.getRestaurantId() != null
                && !statsMap.containsKey(viewRestaurant.getRestaurantId())) {
            Map<Long, Object[]> one = reviewsFacade.statsByRestaurantIds(
                    Collections.singletonList(viewRestaurant.getRestaurantId()), BAD_THRESHOLD
            );
            if (one != null) statsMap.putAll(one);
            statsMap.putIfAbsent(viewRestaurant.getRestaurantId(), new Object[]{0.0, 0L, 0L});
        }

        reviewRatingFilter = "BAD";
        reviewApprovedOnly = false;
        reviewPage = 1;
        loadViewReviews();

        FacesContext.getCurrentInstance().getPartialViewContext().getEvalScripts().add(
                "document.getElementById('viewRestaurantModal').showModal();"
        );
    }

    // =========================================================
    // REVIEWS EXPLORER (in-memory)
    // =========================================================
    public void onReviewFilterChange() {
        reviewPage = 1;
        loadViewReviews();
    }

    public void applyReviewFilter() {
        reviewPage = 1;
        loadViewReviews();
    }

    public void loadViewReviews() {
        viewReviews = new ArrayList<>();
        reviewTotalCount = 0;
        reviewTotalPages = 1;

        if (viewRestaurant == null || viewRestaurant.getRestaurantId() == null) return;
        Long rid = viewRestaurant.getRestaurantId();

        List<RestaurantReviews> all = reviewsFacade.findAll();
        List<RestaurantReviews> filtered = new ArrayList<>();

        for (RestaurantReviews rr : all) {
            if (rr == null) continue;
            if (rr.getRestaurantId() == null || rr.getRestaurantId().getRestaurantId() == null) continue;
            if (!rid.equals(rr.getRestaurantId().getRestaurantId())) continue;

            if (Boolean.TRUE.equals(rr.getIsDeleted())) continue;
            if (reviewApprovedOnly && !Boolean.TRUE.equals(rr.getIsApproved())) continue;

            int rating = rr.getRating();
            if (!matchReviewRatingFilter(rating)) continue;

            filtered.add(rr);
        }

        filtered.sort((a, b) -> {
            Date d1 = (a != null) ? a.getCreatedAt() : null;
            Date d2 = (b != null) ? b.getCreatedAt() : null;
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });

        reviewTotalCount = filtered.size();
        reviewTotalPages = (int) Math.ceil(reviewTotalCount / (double) reviewPageSize);
        if (reviewTotalPages < 1) reviewTotalPages = 1;
        if (reviewPage > reviewTotalPages) reviewPage = reviewTotalPages;
        if (reviewPage < 1) reviewPage = 1;

        int start = (reviewPage - 1) * reviewPageSize;
        int end = (int) Math.min(start + reviewPageSize, reviewTotalCount);

        viewReviews = (start < end) ? filtered.subList(start, end) : new ArrayList<>();
    }

    private boolean matchReviewRatingFilter(int rating) {
        if (reviewRatingFilter == null || "ALL".equals(reviewRatingFilter)) return true;
        if ("BAD".equals(reviewRatingFilter)) return rating >= 1 && rating <= 2;
        if (reviewRatingFilter.matches("[1-5]")) return rating == Integer.parseInt(reviewRatingFilter);
        return true;
    }

    public void nextReviewPage() {
        if (reviewPage < reviewTotalPages) {
            reviewPage++;
            loadViewReviews();
        }
    }

    public void prevReviewPage() {
        if (reviewPage > 1) {
            reviewPage--;
            loadViewReviews();
        }
    }

    public String starText(int rating) {
        if (rating < 0) rating = 0;
        if (rating > 5) rating = 5;
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    // =========================================================
    // BLOCK / UNBLOCK / AUTO BLOCK
    // =========================================================
    public void blockSelected() {
        if (selectedRestaurantId == null) return;
        Restaurants r = restaurantsFacade.find(selectedRestaurantId);
        if (r == null) return;

        r.setStatus("BLOCKED");
        r.setUpdatedAt(new Date());
        restaurantsFacade.edit(r);

        viewRestaurant = r;
        applyFilter();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Restaurant has been BLOCKED", null));
    }

    public void unblockSelected() {
        if (selectedRestaurantId == null) return;
        Restaurants r = restaurantsFacade.find(selectedRestaurantId);
        if (r == null) return;

        r.setStatus("ACTIVE");
        r.setUpdatedAt(new Date());
        restaurantsFacade.edit(r);

        viewRestaurant = r;
        applyFilter();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Restaurant has been UNBLOCKED", null));
    }

    public void autoBlockCurrentPage() {
        int blocked = 0;

        for (Restaurants r : pageRestaurants) {
            if (r == null || r.getRestaurantId() == null) continue;
            if ("BLOCKED".equals(r.getStatus())) continue;

            if (isAutoBlockCandidate(r.getRestaurantId())) {
                r.setStatus("BLOCKED");
                r.setUpdatedAt(new Date());
                restaurantsFacade.edit(r);
                blocked++;
            }
        }

        applyFilter();

        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                        blocked > 0 ? ("Auto-blocked " + blocked + " restaurant(s) on this page")
                                : "No auto-block candidates on this page",
                        null));
    }

    public void confirmChangeStatus() {
        if (selectedRestaurantId == null) return;

        Restaurants r = restaurantsFacade.find(selectedRestaurantId);
        if (r == null) return;

        String status = r.getStatus() != null ? r.getStatus() : "";
        r.setStatus("ACTIVE".equals(status) ? "INACTIVE" : "ACTIVE");
        r.setUpdatedAt(new Date());
        restaurantsFacade.edit(r);

        applyFilter();
    }

    public String statusCssClass(String status) {
        if ("ACTIVE".equals(status)) return "bg-green-100 text-green-700";
        if ("PENDING_APPROVAL".equals(status)) return "bg-yellow-100 text-yellow-700";
        if ("INACTIVE".equals(status)) return "bg-gray-100 text-gray-700";
        if ("BLOCKED".equals(status)) return "bg-red-100 text-red-700";
        return "bg-gray-100 text-gray-700";
    }

    // =========================================================
    // ADD RESTAURANT (giữ core)
    // =========================================================
    public void onNewCityChange() { newAreaId = 0; }

    public List<Areas> getAreaOptionsForNew() {
        if (newCityId == null || newCityId == 0) return areaList;
        List<Areas> res = new ArrayList<>();
        for (Areas a : areaList) {
            if (a.getCityId() != null && a.getCityId().getCityId().equals(newCityId)) res.add(a);
        }
        return res;
    }

    public String createRestaurant() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        boolean hasError = false;

        String name = newRestaurant.getName() != null ? newRestaurant.getName().trim() : "";
        String address = newRestaurant.getAddress() != null ? newRestaurant.getAddress().trim() : "";
        String email = newRestaurant.getEmail() != null ? newRestaurant.getEmail().trim() : "";

        if (name.isEmpty()) { ctx.addMessage("addRestaurantForm:name", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Name is required", null)); hasError = true; }
        if (newCityId == null || newCityId == 0) { ctx.addMessage("addRestaurantForm:city", new FacesMessage(FacesMessage.SEVERITY_ERROR, "City is required", null)); hasError = true; }
        if (newAreaId == null || newAreaId == 0) { ctx.addMessage("addRestaurantForm:area", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Area is required", null)); hasError = true; }
        if (address.isEmpty()) { ctx.addMessage("addRestaurantForm:address", new FacesMessage(FacesMessage.SEVERITY_ERROR, "Address is required", null)); hasError = true; }

        if (!email.isEmpty()) {
            for (Restaurants ex : restaurantsFacade.findAll()) {
                if (ex.getEmail() != null && ex.getEmail().equalsIgnoreCase(email)) {
                    ctx.addMessage("addRestaurantForm:email",
                            new FacesMessage(FacesMessage.SEVERITY_ERROR, "Email is already registered", null));
                    hasError = true;
                    break;
                }
            }
        }

        if (hasError) return null;

        try {
            Areas area = areasFacade.find(newAreaId);
            newRestaurant.setAreaId(area);
            if (newRestaurant.getStatus() == null || newRestaurant.getStatus().trim().isEmpty()) {
                newRestaurant.setStatus("PENDING_APPROVAL");
            }
            newRestaurant.setCreatedAt(new Date());
            newRestaurant.setUpdatedAt(null);

            restaurantsFacade.create(newRestaurant);

            applyFilter();
            newRestaurant = new Restaurants();
            newCityId = 0;
            newAreaId = 0;

            ctx.getPartialViewContext().getEvalScripts().add(
                    "document.getElementById('addRestaurantModal').close();"
                            + "document.getElementById('successRestaurantModal').showModal();"
            );
        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error when creating restaurant", null));
        }
        return null;
    }

    // =========================================================
    // GETTERS / SETTERS
    // =========================================================
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public String getStatusFilter() { return statusFilter; }
    public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

    public Integer getCityFilterId() { return cityFilterId; }
    public void setCityFilterId(Integer cityFilterId) { this.cityFilterId = cityFilterId; }

    public String getAvgStarFilter() { return avgStarFilter; }
    public void setAvgStarFilter(String avgStarFilter) { this.avgStarFilter = avgStarFilter; }

    public int getCurrentPage() { return currentPage; }
    public int getTotalPages() { return totalPages; }
    public List<Restaurants> getPageRestaurants() { return pageRestaurants; }

    public Restaurants getNewRestaurant() { return newRestaurant; }
    public Integer getNewCityId() { return newCityId; }
    public void setNewCityId(Integer newCityId) { this.newCityId = newCityId; }
    public Integer getNewAreaId() { return newAreaId; }
    public void setNewAreaId(Integer newAreaId) { this.newAreaId = newAreaId; }

    public Long getSelectedRestaurantId() { return selectedRestaurantId; }
    public void setSelectedRestaurantId(Long selectedRestaurantId) { this.selectedRestaurantId = selectedRestaurantId; }

    public List<Cities> getCityList() { return cityList; }
    public List<Areas> getAreaList() { return areaList; }

    public Restaurants getViewRestaurant() { return viewRestaurant; }

    public List<RestaurantReviews> getViewReviews() { return viewReviews; }
    public boolean isReviewApprovedOnly() { return reviewApprovedOnly; }
    public void setReviewApprovedOnly(boolean reviewApprovedOnly) { this.reviewApprovedOnly = reviewApprovedOnly; }
    public String getReviewRatingFilter() { return reviewRatingFilter; }
    public void setReviewRatingFilter(String reviewRatingFilter) { this.reviewRatingFilter = reviewRatingFilter; }
    public int getReviewPage() { return reviewPage; }
    public int getReviewTotalPages() { return reviewTotalPages; }
    public long getReviewTotalCount() { return reviewTotalCount; }
}
