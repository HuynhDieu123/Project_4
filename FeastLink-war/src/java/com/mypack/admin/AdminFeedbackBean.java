package com.mypack.admin;

import com.mypack.entity.Feedbacks;
import com.mypack.entity.Users;
import com.mypack.sessionbean.FeedbacksFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Named("adminFeedbackBean")
@ViewScoped
public class AdminFeedbackBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private FeedbacksFacadeLocal feedbacksFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    private List<Feedbacks> feedbackList;
    private List<Users> adminUsers;

    private String statusFilter = "ALL";
    private String keyword;

    // Feedback đang chọn để cập nhật
    private Feedbacks selectedFeedback;
    private Long selectedAdminId;
    private String editStatus;
    private String editResolutionNote;

    // ----------------------------------------------------
    @PostConstruct
    public void init() {
        loadAdminUsers();
        loadFeedbacks();
    }

    private void loadAdminUsers() {
        try {
            adminUsers = new ArrayList<>();
            List<Users> all = usersFacade.findAll();
            if (all != null) {
                for (Users u : all) {
                    String role = u.getRole();
                    if (role != null &&
                        (role.equalsIgnoreCase("ADMIN")
                                || role.equalsIgnoreCase("SUPPORT")
                                || role.equalsIgnoreCase("MODERATOR"))) {
                        adminUsers.add(u);
                    }
                }
            }
        } catch (Exception e) {
            adminUsers = new ArrayList<>();
        }
    }

    public void loadFeedbacks() {
        try {
            feedbackList = new ArrayList<>();
            List<Feedbacks> all = feedbacksFacade.findAll();
            if (all == null) return;

            String kw = (keyword != null) ? keyword.trim().toLowerCase() : null;

            for (Feedbacks f : all) {
                // filter status
                if (!"ALL".equals(statusFilter)) {
                    String st = f.getStatus();
                    if (st == null || !statusFilter.equalsIgnoreCase(st)) {
                        continue;
                    }
                }

                // filter keyword theo title / description
                if (kw != null && !kw.isEmpty()) {
                    String title = (f.getTitle() != null) ? f.getTitle().toLowerCase() : "";
                    String desc = (f.getDescription() != null) ? f.getDescription().toLowerCase() : "";
                    if (!title.contains(kw) && !desc.contains(kw)) {
                        continue;
                    }
                }

                feedbackList.add(f);
            }

            // sắp xếp mới nhất lên đầu
            Collections.sort(feedbackList, new Comparator<Feedbacks>() {
                @Override
                public int compare(Feedbacks o1, Feedbacks o2) {
                    Date d1 = o1.getCreatedAt();
                    Date d2 = o2.getCreatedAt();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d2.compareTo(d1);
                }
            });
        } catch (Exception e) {
            feedbackList = new ArrayList<>();
        }
    }

    public String applyFilter() {
        loadFeedbacks();
        return null;
    }

    public String clearFilter() {
        keyword = null;
        statusFilter = "ALL";
        loadFeedbacks();
        return null;
    }

    public String prepareEdit(Feedbacks feedback) {
        if (feedback == null) {
            return null;
        }

        // load lại từ DB để đảm bảo managed
        selectedFeedback = feedbacksFacade.find(feedback.getFeedbackId());
        if (selectedFeedback != null) {
            editStatus = selectedFeedback.getStatus();
            editResolutionNote = selectedFeedback.getResolutionNote();
            if (selectedFeedback.getAdminHandlerId() != null) {
                selectedAdminId = selectedFeedback.getAdminHandlerId().getUserId();
            } else {
                selectedAdminId = null;
            }
        }

        return null;
    }

    public String updateFeedback() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        try {
            if (selectedFeedback == null || selectedFeedback.getFeedbackId() == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Chưa chọn feedback để cập nhật.",
                        null));
                return null;
            }

            Feedbacks fb = feedbacksFacade.find(selectedFeedback.getFeedbackId());
            if (fb == null) {
                ctx.addMessage(null, new FacesMessage(
                        FacesMessage.SEVERITY_ERROR,
                        "Feedback không tồn tại (có thể đã bị xóa).",
                        null));
                loadFeedbacks();
                return null;
            }

            // cập nhật trạng thái + note
            fb.setStatus(editStatus);
            fb.setResolutionNote(editResolutionNote);
            fb.setUpdatedAt(new Date());

            // gán admin phụ trách nếu chọn
            if (selectedAdminId != null) {
                Users admin = usersFacade.find(selectedAdminId);
                fb.setAdminHandlerId(admin);
            }

            // nếu status là RESOLVED thì set ResolvedAt (nếu chưa có)
            if (editStatus != null &&
                    (editStatus.equalsIgnoreCase("RESOLVED")
                            || editStatus.equalsIgnoreCase("CLOSED"))) {
                if (fb.getResolvedAt() == null) {
                    fb.setResolvedAt(new Date());
                }
            }

            feedbacksFacade.edit(fb);

            selectedFeedback = fb;
            loadFeedbacks();

            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Đã cập nhật feedback thành công.",
                    null));

        } catch (Exception e) {
            e.printStackTrace();
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Có lỗi xảy ra khi cập nhật feedback.",
                    null));
        }

        return null;
    }

    // ===== GET/SET =====

    public List<Feedbacks> getFeedbackList() {
        return feedbackList;
    }

    public List<Users> getAdminUsers() {
        return adminUsers;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Feedbacks getSelectedFeedback() {
        return selectedFeedback;
    }

    public void setSelectedFeedback(Feedbacks selectedFeedback) {
        this.selectedFeedback = selectedFeedback;
    }

    public Long getSelectedAdminId() {
        return selectedAdminId;
    }

    public void setSelectedAdminId(Long selectedAdminId) {
        this.selectedAdminId = selectedAdminId;
    }

    public String getEditStatus() {
        return editStatus;
    }

    public void setEditStatus(String editStatus) {
        this.editStatus = editStatus;
    }

    public String getEditResolutionNote() {
        return editResolutionNote;
    }

    public void setEditResolutionNote(String editResolutionNote) {
        this.editResolutionNote = editResolutionNote;
    }
}
