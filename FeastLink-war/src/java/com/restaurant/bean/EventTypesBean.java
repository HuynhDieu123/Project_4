package com.restaurant.bean;

import com.mypack.entity.EventTypes;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.EventTypesFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Named("eventTypesBean")
@ViewScoped
public class EventTypesBean implements Serializable {

    @EJB
    private EventTypesFacadeLocal eventTypesFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    // TẤT CẢ LOẠI TIỆC
    private List<EventTypes> allEventTypes = new ArrayList<>();
    // SAU KHI FILTER
    private List<EventTypes> filteredEventTypes = new ArrayList<>();

    // TỪ KHÓA SEARCH
    private String keyword = "";

    // ĐANG CREATE / EDIT
    private EventTypes editingEventType;

    // XÓA
    private EventTypes deleteTarget;
    private long deleteTargetUsageCount;

    // MAP ĐẾM SỐ BOOKING CHO MỖI EVENT TYPE
    private Map<Integer, Long> usageCounts = new HashMap<>();

    // ========= PHÂN TRANG =========
    private int pageSize = 10;    // số dòng / trang
    private int currentPage = 1;  // trang hiện tại (1-based)
    private int totalPages = 1;   // tổng số trang

    @PostConstruct
    public void init() {
        reload();
    }

    public void reload() {
        allEventTypes = eventTypesFacade.findAll();

        usageCounts = new HashMap<>();
        for (EventTypes et : allEventTypes) {
            Integer id = et.getEventTypeId();
            if (id != null) {
                long c = bookingsFacade.countByEventType(id);
                usageCounts.put(id, c);
            }
        }

        applyFilter();
    }

    // ========== FILTER ==========
    public void applyFilter() {
        filteredEventTypes = new ArrayList<>();
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();

        for (EventTypes et : allEventTypes) {
            boolean match = kw.isEmpty()
                    || (et.getName() != null
                    && et.getName().toLowerCase().contains(kw));
            if (match) {
                filteredEventTypes.add(et);
            }
        }

        // Cập nhật phân trang
        recalcPagination();
    }

    public void resetFilter() {
        keyword = "";
        applyFilter();
    }

    private void recalcPagination() {
        if (filteredEventTypes == null || filteredEventTypes.isEmpty()) {
            totalPages = 1;
            currentPage = 1;
            return;
        }
        totalPages = (int) Math.ceil(filteredEventTypes.size() / (double) pageSize);
        if (totalPages < 1) {
            totalPages = 1;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }
    }

    // ========== LẤY DATA THEO TRANG ==========
    public List<EventTypes> getPageEventTypes() {
        if (filteredEventTypes == null || filteredEventTypes.isEmpty()) {
            return new ArrayList<>();
        }
        int from = (currentPage - 1) * pageSize;
        if (from < 0) {
            from = 0;
        }
        if (from >= filteredEventTypes.size()) {
            from = 0;
        }
        int to = Math.min(from + pageSize, filteredEventTypes.size());
        return filteredEventTypes.subList(from, to);
    }

    // ========== STATS ==========
    public long getTotalCount() {
        return allEventTypes.size();
    }

    public long getUsedCount() {
        return usageCounts.values().stream()
                .filter(c -> c != null && c > 0L)
                .count();
    }

    public long getUnusedCount() {
        return getTotalCount() - getUsedCount();
    }

    public long getUsageCount(EventTypes et) {
        if (et == null || et.getEventTypeId() == null) {
            return 0L;
        }
        return usageCounts.getOrDefault(et.getEventTypeId(), 0L);
    }

    // Để dùng trong EL: #{eventTypesBean.usageCount(t)}
    public long usageCount(EventTypes et) {
        return getUsageCount(et);
    }

    // Mở modal chế độ tạo mới
    public String openCreate() {
        System.out.println(">>> openCreate() called");
        editingEventType = new EventTypes();
        return null;
    }

// Mở modal chế độ chỉnh sửa
    public String openEdit(EventTypes et) {
        System.out.println(">>> openEdit() called for id = "
                + (et != null ? et.getEventTypeId() : null));
        editingEventType = et;
        return null;
    }

    public String save() {
        if (editingEventType == null) {
            return null;
        }

        // === CHECK TRÙNG TÊN TRƯỚC KHI LƯU ===
        if (isDuplicateName(editingEventType)) {
            // Không lưu, giữ form lại cho user chỉnh
            return null;
        }

        try {
            if (editingEventType.getEventTypeId() == null) {
                // CREATE
                eventTypesFacade.create(editingEventType);
                addMessage(FacesMessage.SEVERITY_INFO,
                        "Created", "Event type created successfully.");
            } else {
                // UPDATE
                eventTypesFacade.edit(editingEventType);
                addMessage(FacesMessage.SEVERITY_INFO,
                        "Updated", "Event type updated successfully.");
            }

            // clear form + reload list/pagination/stat
            editingEventType = null;
            reload();

        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Cannot save event type: " + ex.getMessage());
        }

        return null; // rất quan trọng cho JSF action
    }

    private boolean isDuplicateName(EventTypes candidate) {
        if (candidate == null) {
            return false;
        }

        String rawName = candidate.getName();
        if (rawName == null) {
            return false;
        }

        String normalized = rawName.trim();
        Integer currentId = candidate.getEventTypeId();

        for (EventTypes et : allEventTypes) {
            if (et == null || et.getName() == null) {
                continue;
            }

            // Bỏ qua chính nó khi edit
            if (currentId != null && currentId.equals(et.getEventTypeId())) {
                continue;
            }

            if (et.getName().trim().equalsIgnoreCase(normalized)) {
                // Bắn lỗi vào đúng ô nameInput trong form eventTypesForm
                FacesContext.getCurrentInstance().addMessage(
                        "eventTypesForm:nameInput",
                        new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "Duplicated name",
                                "Event type name already exists."
                        )
                );
                return true;
            }
        }

        // Chuẩn hóa lại name (trim) để lưu
        candidate.setName(normalized);
        return false;
    }

// Hủy tạo / sửa
    public String cancelEdit() {
        editingEventType = null;
        return null;
    }

// ========== DELETE ==========
    public String prepareDelete(EventTypes et) {
        deleteTarget = et;
        deleteTargetUsageCount = (et != null) ? getUsageCount(et) : 0L;

        // ➜ Đóng luôn form edit nếu đang mở
        editingEventType = null;

        return null;
    }

    public String confirmDelete() {
        if (deleteTarget == null) {
            return null;
        }

        try {
            long usage = getUsageCount(deleteTarget);
            if (usage > 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Cannot delete",
                        "This event type is used in " + usage + " booking(s) and cannot be deleted.");
                // Giữ dialog cho user đọc, không đóng
                return null;
            }

            // Xóa được
            eventTypesFacade.remove(deleteTarget);
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Deleted", "Event type deleted.");

            // ĐÓNG CẢ 2 MODAL: delete + edit
            deleteTarget = null;
            deleteTargetUsageCount = 0L;
            editingEventType = null;

            // Reload lại list + stats
            reload();

        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Failed to delete event type.");
        }

        return null;
    }

    public String cancelDelete() {
        deleteTarget = null;
        deleteTargetUsageCount = 0L;
        return null;
    }

// ========== PHÂN TRANG ==========
    public String nextPage() {
        if (currentPage < totalPages) {
            currentPage++;
        }
        return null;
    }

    public String prevPage() {
        if (currentPage > 1) {
            currentPage--;
        }
        return null;
    }

    public void changePage() {
        // đảm bảo currentPage nằm trong khoảng 1..totalPages
        if (currentPage < 1) {
            currentPage = 1;
        }
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
    }

    // ========== UTIL ==========
    private void addMessage(FacesMessage.Severity sev, String summary, String detail) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(sev, summary, detail));
    }

    // ========== GET/SET ==========
    public List<EventTypes> getFilteredEventTypes() {
        return filteredEventTypes;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public EventTypes getEditingEventType() {
        return editingEventType;
    }

    // Được dùng bởi f:setPropertyActionListener trong xhtml
    public void setEditingEventType(EventTypes editingEventType) {
        this.editingEventType = editingEventType;
    }

    public EventTypes getDeleteTarget() {
        return deleteTarget;
    }

    public long getDeleteTargetUsageCount() {
        return deleteTargetUsageCount;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
        recalcPagination();
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }
}
