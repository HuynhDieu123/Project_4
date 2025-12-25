package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.RestaurantReviews;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantReviewsFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

@Named("restaurantReviewBean")
@ViewScoped
public class RestaurantReviewBean implements Serializable {

    @EJB
    private RestaurantReviewsFacadeLocal restaurantReviewsFacade;
    @EJB
    private BookingsFacadeLocal bookingsFacade;
    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    private Long restaurantId;

    // ===== session user =====
    private Users currentUser;
    private boolean loggedIn;

    // ===== stats approved =====
    private double avgRating = 0;
    private long approvedReviewCount = 0;

    private int count5, count4, count3, count2, count1;
    private double pct5, pct4, pct3, pct2, pct1;

    // ===== list =====
    private List<RestaurantReviews> approvedReviews = new ArrayList<>();
    private List<RestaurantReviews> myReviews = new ArrayList<>();

    private String sortOption = "recent";
    private int pageSize = 6;
    private int offset = 0;
    private boolean hasMore = false;

    // ===== eligibility for create =====
    private boolean canReview = false;
    private String canReviewMessage = "";
    private List<SelectItem> eligibleBookingItems = new ArrayList<>();

    // ===== modal/form =====
    private boolean showModal = false;
    private boolean editMode = false;
    private Long editingReviewId = null;

    private Integer formRating = null;
    private String formComment = "";
    private String formBookingId = ""; // string to avoid converter pain

    private final List<Integer> starList = Arrays.asList(1, 2, 3, 4, 5);

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        String idParam = ctx.getExternalContext().getRequestParameterMap().get("restaurantId");
        try {
            if (idParam != null && !idParam.trim().isEmpty()) {
                restaurantId = Long.valueOf(idParam.trim());
            }
        } catch (Exception ignored) {
        }

        // session currentUser (the same key you set in LoginBean)
        Object u = ctx.getExternalContext().getSessionMap().get("currentUser");
        if (u instanceof Users) {
            currentUser = (Users) u;
            loggedIn = true;
        } else {
            loggedIn = false;
            currentUser = null;
        }

        loadStatsApproved();
        loadMyReviews();
        computeEligibility();
        loadApprovedReviews(true);
    }

    // ===================== LOADERS =====================
    private void loadStatsApproved() {
        approvedReviewCount = restaurantReviewsFacade.countApprovedByRestaurant(restaurantId);

        // reset counts
        count5 = count4 = count3 = count2 = count1 = 0;

        List<Object[]> rows = restaurantReviewsFacade.ratingBreakdownApproved(restaurantId);
        if (rows != null) {
            for (Object[] r : rows) {
                if (r == null || r.length < 2) {
                    continue;
                }
                int rating = ((Number) r[0]).intValue();
                int cnt = ((Number) r[1]).intValue();
                switch (rating) {
                    case 5:
                        count5 = cnt;
                        break;
                    case 4:
                        count4 = cnt;
                        break;
                    case 3:
                        count3 = cnt;
                        break;
                    case 2:
                        count2 = cnt;
                        break;
                    case 1:
                        count1 = cnt;
                        break;
                    default:
                        break;
                }

            }

            // ===== compute avg from breakdown (avoid integer avg from DB) =====
            long totalFromBreakdown = (long) count1 + count2 + count3 + count4 + count5;

            long weightedSum
                    = 1L * count1
                    + 2L * count2
                    + 3L * count3
                    + 4L * count4
                    + 5L * count5;

// Nếu breakdown có dữ liệu thì ưu tiên dùng breakdown để khớp UI
            if (totalFromBreakdown > 0) {
                approvedReviewCount = totalFromBreakdown; // optional: giúp "Based on X reviews" khớp breakdown luôn
                avgRating = weightedSum * 1.0 / totalFromBreakdown; // QUAN TRỌNG: *1.0 để ra double
            } else {
                avgRating = 0.0;
            }

        }

        pct5 = percent(count5, approvedReviewCount);
        pct4 = percent(count4, approvedReviewCount);
        pct3 = percent(count3, approvedReviewCount);
        pct2 = percent(count2, approvedReviewCount);
        pct1 = percent(count1, approvedReviewCount);
    }

    private void loadMyReviews() {
        myReviews = new ArrayList<>();
        if (!loggedIn || restaurantId == null) {
            return;
        }

        List<RestaurantReviews> list = restaurantReviewsFacade.findByRestaurantAndCustomer(
                restaurantId, currentUser.getUserId()
        );
        if (list != null) {
            myReviews = list;
        }
    }

    private void computeEligibility() {
        canReview = false;
        canReviewMessage = "";
        eligibleBookingItems = new ArrayList<>();

        if (restaurantId == null) {
            return;
        }

        if (!loggedIn) {
            canReviewMessage = "Please sign in to write a review.";
            return;
        }

        List<Bookings> eligible = bookingsFacade.findCompletedBookingsForReview(restaurantId, currentUser.getUserId());
        if (eligible == null || eligible.isEmpty()) {
            canReviewMessage = "You can review only after you have a COMPLETED booking (and not reviewed yet).";
            return;
        }

        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        for (Bookings b : eligible) {
            String label = "Booking #" + safe(b.getBookingCode()) + " • " + (b.getEventDate() != null ? df.format(b.getEventDate()) : "");
            eligibleBookingItems.add(new SelectItem(String.valueOf(b.getBookingId()), label));
        }

        canReview = true;

        // auto pick first booking
        if (formBookingId == null || formBookingId.isBlank()) {
            formBookingId = String.valueOf(eligible.get(0).getBookingId());
        }
    }

    private void loadApprovedReviews(boolean reset) {
        if (restaurantId == null) {
            return;
        }

        if (reset) {
            offset = 0;
            approvedReviews = new ArrayList<>();
        }

        List<RestaurantReviews> page = restaurantReviewsFacade.findApprovedByRestaurant(
                restaurantId, offset, pageSize, sortOption
        );

        if (page != null && !page.isEmpty()) {
            approvedReviews.addAll(page);
            offset += page.size();
        }

        // hasMore?
        hasMore = (approvedReviews.size() < approvedReviewCount);
    }

    // ===================== ACTIONS =====================
    public void changeSort() {
        loadApprovedReviews(true);
    }

    public void loadMore() {
        loadApprovedReviews(false);
    }

    public void openCreate() {
        if (!canReview) {
            addMsg(FacesMessage.SEVERITY_WARN, "Not eligible", canReviewMessage);
            return;
        }
        editMode = false;
        editingReviewId = null;
        formRating = null;
        formComment = "";
        showModal = true;
    }

    public void openEdit(Long reviewId) {
        if (!loggedIn || reviewId == null) {
            return;
        }

        RestaurantReviews rr = restaurantReviewsFacade.find(reviewId);
        if (rr == null || rr.getIsDeleted()) {
            return;
        }

        // only owner can edit
        if (rr.getCustomerId() == null || !Objects.equals(rr.getCustomerId().getUserId(), currentUser.getUserId())) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Forbidden", "You can edit only your own review.");
            return;
        }

        editMode = true;
        editingReviewId = reviewId;
        formRating = rr.getRating();
        formComment = safe(rr.getComment());
        showModal = true;
    }

    public void closeModal() {
        showModal = false;
    }

    public void submit() {
        if (!loggedIn) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Login required", "Please sign in first.");
            return;
        }

        if (formRating == null || formRating < 1 || formRating > 5) {
            addFieldMsg(CID_RATING, FacesMessage.SEVERITY_ERROR,
                    "Please choose a rating",
                    "Tap a star from 1 to 5.");
            return;
        }

        if (formComment == null || formComment.trim().isEmpty()) {
            addFieldMsg(CID_COMMENT, FacesMessage.SEVERITY_ERROR,
                    "Comment is required",
                    "Please write a short comment.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }

        if (editMode) {
            updateReview();
        } else {
            createReview();
        }
    }

    // Backward compatible: giữ tên cũ addMsg để khỏi sửa hàng loạt chỗ gọi
    private void addMsg(FacesMessage.Severity sev, String sum, String detail) {
        addGlobalMsg(sev, sum, detail);
    }

    private void createReview() {
        if (!canReview) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Not eligible", canReviewMessage);
            return;
        }

        Long bookingId;
        try {
            bookingId = Long.valueOf(formBookingId);
        } catch (Exception e) {
            addFieldMsg(CID_BOOKING, FacesMessage.SEVERITY_ERROR,
                    "Please choose a booking",
                    "Select your completed booking to submit this review.");
            return;
        }

        Bookings b = bookingsFacade.find(bookingId);
        if (b == null) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Not found", "Booking not found.");
            return;
        }

        // security check: must be your COMPLETED booking & same restaurant
        if (!Objects.equals(b.getCustomerId().getUserId(), currentUser.getUserId())
                || b.getRestaurantId() == null
                || !Objects.equals(b.getRestaurantId().getRestaurantId(), restaurantId)
                || b.getBookingStatus() == null
                || !"COMPLETED".equalsIgnoreCase(b.getBookingStatus())) {

            addMsg(FacesMessage.SEVERITY_ERROR, "Not allowed", "This booking is not eligible for review.");
            return;
        }

        RestaurantReviews rr = new RestaurantReviews();
        rr.setBookingId(b);
        rr.setRestaurantId(b.getRestaurantId());
        rr.setCustomerId(currentUser);
        rr.setRating(formRating);
        rr.setComment(formComment.trim());
        rr.setCreatedAt(new Date());
        rr.setIsDeleted(false);

        // IMPORTANT:
        // nếu bạn muốn review hiện ngay công khai -> set true
        // nếu muốn duyệt -> set false
        rr.setIsApproved(true);

        restaurantReviewsFacade.create(rr);

        addMsg(FacesMessage.SEVERITY_INFO, "Success", "Your review has been submitted.");
        showModal = false;

        // reload all
        loadStatsApproved();
        loadMyReviews();
        computeEligibility();
        loadApprovedReviews(true);
    }

    private void updateReview() {
        if (editingReviewId == null) {
            return;
        }

        RestaurantReviews rr = restaurantReviewsFacade.find(editingReviewId);
        if (rr == null || rr.getIsDeleted()) {
            return;
        }

        if (rr.getCustomerId() == null || !Objects.equals(rr.getCustomerId().getUserId(), currentUser.getUserId())) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Forbidden", "You can edit only your own review.");
            return;
        }

        rr.setRating(formRating);
        rr.setComment(formComment.trim());

        // nếu bạn muốn sửa xong phải duyệt lại => rr.setIsApproved(false);
        // còn muốn vẫn public luôn => giữ nguyên
        restaurantReviewsFacade.edit(rr);

        addMsg(FacesMessage.SEVERITY_INFO, "Updated", "Your review has been updated.");
        showModal = false;

        loadStatsApproved();
        loadMyReviews();
        loadApprovedReviews(true);
    }

    public void deleteReview(Long reviewId) {
        if (!loggedIn || reviewId == null) {
            return;
        }

        RestaurantReviews rr = restaurantReviewsFacade.find(reviewId);
        if (rr == null || rr.getIsDeleted()) {
            return;
        }

        if (rr.getCustomerId() == null || !Objects.equals(rr.getCustomerId().getUserId(), currentUser.getUserId())) {
            addMsg(FacesMessage.SEVERITY_ERROR, "Forbidden", "You can delete only your own review.");
            return;
        }

        rr.setIsDeleted(true);
        restaurantReviewsFacade.edit(rr);

        addMsg(FacesMessage.SEVERITY_INFO, "Deleted", "Your review has been deleted.");

        loadStatsApproved();
        loadMyReviews();
        computeEligibility();
        loadApprovedReviews(true);
    }

    public boolean isMyReview(RestaurantReviews rr) {
        if (!loggedIn || rr == null || rr.getCustomerId() == null) {
            return false;
        }
        return rr.getCustomerId().getUserId() != null
                && rr.getCustomerId().getUserId().equals(currentUser.getUserId());
    }

    // ===================== HELPERS =====================
    private double percent(long part, long total) {
        if (total <= 0) {
            return 0;
        }
        return (part * 100.0) / total;
    }

    public String initials(String name) {
        String s = safe(name).trim();
        if (s.isEmpty()) {
            return "?";
        }
        String[] parts = s.split("\\s+");
        String first = parts[0].substring(0, 1).toUpperCase();
        String last = (parts.length >= 2) ? parts[parts.length - 1].substring(0, 1).toUpperCase() : "";
        return first + last;
    }

    public boolean myReview(RestaurantReviews rr) {
        if (!loggedIn || rr == null || rr.getCustomerId() == null) {
            return false;
        }
        return rr.getCustomerId().getUserId() != null
                && rr.getCustomerId().getUserId().equals(currentUser.getUserId());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static final String CID_RATING = "reviewsForm:ratingVal";
    private static final String CID_COMMENT = "reviewsForm:comment";
    private static final String CID_BOOKING = "reviewsForm:bookingSel";

    private void addGlobalMsg(FacesMessage.Severity sev, String sum, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(null, new FacesMessage(sev, sum, detail));
        }
    }

    private void addFieldMsg(String clientId, FacesMessage.Severity sev, String sum, String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            ctx.addMessage(clientId, new FacesMessage(sev, sum, detail));
            // giúp JSF hiểu là validation fail trong AJAX -> giữ message đúng chỗ
            ctx.validationFailed();
        }
    }

    // ===================== GETTERS =====================
    public Long getRestaurantId() {
        return restaurantId;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public long getApprovedReviewCount() {
        return approvedReviewCount;
    }

    public int getCount5() {
        return count5;
    }

    public int getCount4() {
        return count4;
    }

    public int getCount3() {
        return count3;
    }

    public int getCount2() {
        return count2;
    }

    public int getCount1() {
        return count1;
    }

    public double getPct5() {
        return pct5;
    }

    public double getPct4() {
        return pct4;
    }

    public double getPct3() {
        return pct3;
    }

    public double getPct2() {
        return pct2;
    }

    public double getPct1() {
        return pct1;
    }

    public List<RestaurantReviews> getApprovedReviews() {
        return approvedReviews;
    }

    public List<RestaurantReviews> getMyReviews() {
        return myReviews;
    }

    public List<Integer> getStarList() {
        return starList;
    }

    public String getSortOption() {
        return sortOption;
    }

    public void setSortOption(String sortOption) {
        this.sortOption = sortOption;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public boolean isCanReview() {
        return canReview;
    }

    public String getCanReviewMessage() {
        return canReviewMessage;
    }

    public List<SelectItem> getEligibleBookingItems() {
        return eligibleBookingItems;
    }

    public boolean isShowModal() {
        return showModal;
    }

    public boolean isEditMode() {
        return editMode;
    }

    public Integer getFormRating() {
        return formRating;
    }

    public void setFormRating(Integer formRating) {
        this.formRating = formRating;
    }

    public String getFormComment() {
        return formComment;
    }

    public void setFormComment(String formComment) {
        this.formComment = formComment;
    }

    public String getFormBookingId() {
        return formBookingId;
    }

    public void setFormBookingId(String formBookingId) {
        this.formBookingId = formBookingId;
    }

    public void setRating(int r) {
        this.formRating = r;
    }

}
