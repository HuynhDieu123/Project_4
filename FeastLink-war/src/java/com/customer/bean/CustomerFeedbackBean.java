package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.Feedbacks;
import com.mypack.entity.Restaurants;
import com.mypack.entity.Users;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.FeedbacksFacadeLocal;
import com.mypack.sessionbean.RestaurantsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Feedback hệ thống từ Customer & Restaurant Manager.
 * Dùng cho trang Customer/feedback.xhtml
 */
@Named("customerFeedbackBean")
@SessionScoped
public class CustomerFeedbackBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private RestaurantsFacadeLocal restaurantsFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private FeedbacksFacadeLocal feedbacksFacade;

    // ====== USER / CONTEXT ======
    private Users currentUser;

    // ====== FORM FIELDS (LEFT PANEL) ======
    private String contactEmail;         // Email liên hệ
    private Long selectedRestaurantId;   // RestaurantId
    private Long selectedBookingId;      // BookingId
    private String title;                // Tiêu đề feedback *
    private String description;          // Nội dung chi tiết *

    // ====== DROPDOWN DATA ======
    private List<Restaurants> restaurants;
    private List<Bookings> userBookings;

    // ====== FEEDBACK STATUS (RIGHT PANEL) ======
    private Feedbacks lastCreatedFeedback;

    // ====== LỊCH SỬ FEEDBACK (PHÍA DƯỚI TRANG) ======
    private List<Feedbacks> myFeedbackHistory;

    // ----------------------------------------------------
    // INIT
    // ----------------------------------------------------
    @PostConstruct
    public void init() {
        try {
            resolveCurrentUserFromSession();
            if (currentUser != null) {
                contactEmail = currentUser.getEmail();
            }

            loadRestaurants();
            loadUserBookings();
            loadLastFeedbackForCurrentUser();
            loadFeedbackHistoryForCurrentUser();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Cố gắng lấy Users đang đăng nhập từ session mà không phụ thuộc tên key.
     */
    private void resolveCurrentUserFromSession() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx == null) {
                return;
            }
            ExternalContext ec = ctx.getExternalContext();
            if (ec == null) {
                return;
            }
            Map<String, Object> sessionMap = ec.getSessionMap();
            if (sessionMap == null) {
                return;
            }

            // 1. Thử vài key thường gặp
            Object userObj = sessionMap.get("currentUser");
            if (userObj == null) {
                userObj = sessionMap.get("loggedInUser");
            }
            if (userObj == null) {
                userObj = sessionMap.get("user");
            }

            if (userObj instanceof Users) {
                currentUser = (Users) userObj;
                return;
            }

            // 2. Không tìm thấy theo key -> quét hết session,
            //   lấy object đầu tiên là Users
            for (Object value : sessionMap.values()) {
                if (value instanceof Users) {
                    currentUser = (Users) value;
                    break;
                }
            }
        } catch (Exception e) {
            currentUser = null;
        }
    }

    private void loadRestaurants() {
        try {
            restaurants = restaurantsFacade.findAll();
        } catch (Exception e) {
            restaurants = new ArrayList<>();
        }
    }

    private void loadUserBookings() {
        try {
            userBookings = new ArrayList<>();

            List<Bookings> all = bookingsFacade.findAll();
            if (all == null) {
                return;
            }

            // Nếu có user đăng nhập -> lọc booking theo CustomerId
            if (currentUser != null) {
                Long currentUserId = currentUser.getUserId();
                for (Bookings b : all) {
                    if (b.getCustomerId() != null
                            && b.getCustomerId().getUserId() != null
                            && b.getCustomerId().getUserId().equals(currentUserId)) {
                        userBookings.add(b);
                    }
                }
            } else {
                // Chưa login: không load booking (giữ list rỗng)
                userBookings = new ArrayList<>();
            }

        } catch (Exception e) {
            userBookings = new ArrayList<>();
        }
    }

    private void loadLastFeedbackForCurrentUser() {
        try {
            lastCreatedFeedback = null;

            if (currentUser == null) {
                return;
            }

            List<Feedbacks> all = feedbacksFacade.findAll();
            if (all == null) {
                return;
            }

            Long currentUserId = currentUser.getUserId();
            for (Feedbacks f : all) {
                if (f.getReporterUserId() != null
                        && f.getReporterUserId().getUserId() != null
                        && f.getReporterUserId().getUserId().equals(currentUserId)) {

                    if (lastCreatedFeedback == null) {
                        lastCreatedFeedback = f;
                    } else {
                        Date oldCreated = lastCreatedFeedback.getCreatedAt();
                        Date newCreated = f.getCreatedAt();
                        if (oldCreated == null || (newCreated != null && newCreated.after(oldCreated))) {
                            lastCreatedFeedback = f;
                        }
                    }
                }
            }
        } catch (Exception e) {
            lastCreatedFeedback = null;
        }
    }

    /**
     * Lấy toàn bộ lịch sử feedback của user hiện tại (để hiển thị ở dưới).
     */
    private void loadFeedbackHistoryForCurrentUser() {
        try {
            myFeedbackHistory = new ArrayList<>();

            if (currentUser == null) {
                return;
            }

            List<Feedbacks> all = feedbacksFacade.findAll();
            if (all == null) {
                return;
            }

            Long currentUserId = currentUser.getUserId();
            for (Feedbacks f : all) {
                if (f.getReporterUserId() != null
                        && f.getReporterUserId().getUserId() != null
                        && f.getReporterUserId().getUserId().equals(currentUserId)) {
                    myFeedbackHistory.add(f);
                }
            }

            // Sắp xếp mới nhất lên trên
            Collections.sort(myFeedbackHistory, new Comparator<Feedbacks>() {
                @Override
                public int compare(Feedbacks o1, Feedbacks o2) {
                    Date d1 = o1.getCreatedAt();
                    Date d2 = o2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1); // desc
                }
            });

        } catch (Exception e) {
            myFeedbackHistory = new ArrayList<>();
        }
    }

    /**
     * Làm mới lastCreatedFeedback từ DB (dùng khi Admin vừa update status / note).
     * Không đụng tới logic nào khác.
     */
    private void refreshLastFeedbackFromDb() {
        try {
            if (currentUser == null) {
                return;
            }

            // Nếu đã có feedback gần nhất -> reload theo ID
            if (lastCreatedFeedback != null && lastCreatedFeedback.getFeedbackId() != null) {
                Feedbacks fresh = feedbacksFacade.find(lastCreatedFeedback.getFeedbackId());
                if (fresh != null) {
                    lastCreatedFeedback = fresh;
                    return;
                }
            }

            // Nếu chưa có hoặc find() trả null -> load lại như cũ
            loadLastFeedbackForCurrentUser();
        } catch (Exception e) {
            // Không để crash UI
        }
    }

    // ----------------------------------------------------
    // ACTION: SUBMIT FEEDBACK
    // ----------------------------------------------------
    public String submitFeedback() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (currentUser == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "You need to log in before submitting feedback.",
                        null));
                return null;
            }

            if (title == null || title.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Please enter a title for your feedback.",
                        null));
                return null;
            }

            if (description == null || description.trim().isEmpty()) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_WARN,
                        "Please describe the content of your feedback in detail.",
                        null));
                return null;
            }

            Feedbacks fb = new Feedbacks();
            fb.setTitle(title.trim());
            fb.setDescription(description.trim());
            fb.setStatus("NEW");      // theo mẫu trong DB
            fb.setCreatedAt(new Date());
            fb.setReporterUserId(currentUser);

            if (selectedRestaurantId != null) {
                Restaurants r = restaurantsFacade.find(selectedRestaurantId);
                fb.setRestaurantId(r);
            }

            if (selectedBookingId != null) {
                Bookings b = bookingsFacade.find(selectedBookingId);
                fb.setBookingId(b);
            }

            fb.setAdminHandlerId(null);
            fb.setResolutionNote(null);
            fb.setUpdatedAt(null);
            fb.setResolvedAt(null);

            feedbacksFacade.create(fb);
            lastCreatedFeedback = fb; // cập nhật panel bên phải

            // reset form (giữ lại email & nhà hàng)
            title = null;
            description = null;
            selectedBookingId = null;

            // load lại booking + feedback (trong trường hợp DB thay đổi trigger khác)
            loadUserBookings();
            loadLastFeedbackForCurrentUser();
            loadFeedbackHistoryForCurrentUser();

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Feedback submitted successfully. FeastLink admin will review and respond soon.",
                    null));

        } catch (Exception ex) {
            ex.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "An error occurred while submitting feedback. Please try again later.",
                    null));
        }

        return null; // ở lại trang
    }

    // ----------------------------------------------------
    // HELPER GETTERS CHO PANEL STATUS
    // ----------------------------------------------------
    public String getDisplayAccountName() {
        return currentUser != null ? currentUser.getFullName() : "";
    }

    public String getDisplayStatus() {
        refreshLastFeedbackFromDb();
        if (lastCreatedFeedback == null || lastCreatedFeedback.getStatus() == null) {
            return "NEW";
        }
        return lastCreatedFeedback.getStatus();
    }

    public Long getLastFeedbackId() {
        refreshLastFeedbackFromDb();
        return lastCreatedFeedback != null ? lastCreatedFeedback.getFeedbackId() : null;
    }

    public String getLastAdminName() {
        refreshLastFeedbackFromDb();
        if (lastCreatedFeedback != null
                && lastCreatedFeedback.getAdminHandlerId() != null) {
            return lastCreatedFeedback.getAdminHandlerId().getFullName();
        }
        return "";
    }

    public Date getLastCreatedAt() {
        refreshLastFeedbackFromDb();
        return lastCreatedFeedback != null ? lastCreatedFeedback.getCreatedAt() : null;
    }

    public Date getLastUpdatedAt() {
        refreshLastFeedbackFromDb();
        return lastCreatedFeedback != null ? lastCreatedFeedback.getUpdatedAt() : null;
    }

    public Date getLastResolvedAt() {
        refreshLastFeedbackFromDb();
        return lastCreatedFeedback != null ? lastCreatedFeedback.getResolvedAt() : null;
    }

    public String getLastResolutionNote() {
        refreshLastFeedbackFromDb();
        return lastCreatedFeedback != null ? lastCreatedFeedback.getResolutionNote() : null;
    }

    // Lịch sử feedback – luôn load mới để thấy update của Admin
    public List<Feedbacks> getMyFeedbackHistory() {
        loadFeedbackHistoryForCurrentUser();
        return myFeedbackHistory;
    }

    // ----------------------------------------------------
    // GET/SET CÒN LẠI
    // ----------------------------------------------------

    public Users getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Users currentUser) {
        this.currentUser = currentUser;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Long getSelectedRestaurantId() {
        return selectedRestaurantId;
    }

    public void setSelectedRestaurantId(Long selectedRestaurantId) {
        this.selectedRestaurantId = selectedRestaurantId;
    }

    public Long getSelectedBookingId() {
        return selectedBookingId;
    }

    public void setSelectedBookingId(Long selectedBookingId) {
        this.selectedBookingId = selectedBookingId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Restaurants> getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(List<Restaurants> restaurants) {
        this.restaurants = restaurants;
    }

    public List<Bookings> getUserBookings() {
        return userBookings;
    }

    public void setUserBookings(List<Bookings> userBookings) {
        this.userBookings = userBookings;
    }

    public Feedbacks getLastCreatedFeedback() {
        return lastCreatedFeedback;
    }

    public void setLastCreatedFeedback(Feedbacks lastCreatedFeedback) {
        this.lastCreatedFeedback = lastCreatedFeedback;
    }

    public List<Feedbacks> getMyFeedbackHistoryInternal() {
        return myFeedbackHistory;
    }

    public void setMyFeedbackHistoryInternal(List<Feedbacks> myFeedbackHistory) {
        this.myFeedbackHistory = myFeedbackHistory;
    }
}
