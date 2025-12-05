package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Named("bookingManagementBean")
@ViewScoped
public class BookingManagementBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    // tất cả booking của nhà hàng hiện tại
    private List<Bookings> bookings = new ArrayList<>();

    // booking đang được chọn xem chi tiết
    private Bookings selectedBooking;

    // filter / sort
    private String statusFilter = "All";      // All, PENDING, CONFIRMED, COMPLETED, CANCELLED
    private String sortBy = "newest";         // newest, oldest, event_date, highest_value
    private String searchQuery = "";          // text search
    private String timeFilter = "All";        // All, Today, This week, This month

    // paging
    private int rowsPerPage = 10;
    private int currentPage = 1;

    // header
    private Date lastUpdated;

    // lý do từ chối (optional, nhập trong prompt JS)
    private String rejectReason;

    @PostConstruct
    public void init() {
        loadBookings();
    }

    public void loadBookings() {
        try {
            bookings = bookingsFacade.findAll();
            if (bookings == null) {
                bookings = new ArrayList<>();
            }
            lastUpdated = new Date();
        } catch (Exception ex) {
            bookings = new ArrayList<>();
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Error loading bookings", ex.getMessage())
            );
        }
    }

    /* ================= FILTER + SORT ================= */
    private List<Bookings> getFilteredList() {
        List<Bookings> result = new ArrayList<>(bookings);

        // 1. Lọc theo status
        if (statusFilter != null && !"All".equalsIgnoreCase(statusFilter)) {
            final String sf = statusFilter.toUpperCase();
            result.removeIf(b -> {
                String st = (b.getBookingStatus() != null)
                        ? b.getBookingStatus().toUpperCase()
                        : "";
                return !st.equals(sf);
            });
        }

        // 2. Lọc theo search text
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            final String q = searchQuery.trim().toLowerCase();
            result.removeIf(b -> !matchesSearch(b, q));
        }

        // 3. Lọc theo timeFilter (All, Today, This week, This month) dựa vào EventDate
        if (timeFilter != null && !"All".equalsIgnoreCase(timeFilter)) {
            final Date today = truncateTime(new Date());

            result.removeIf(b -> {
                Date eventDate = truncateTime(b.getEventDate());
                if (eventDate == null) {
                    return true;
                }

                if ("Today".equalsIgnoreCase(timeFilter)) {
                    return !eventDate.equals(today);
                } else if ("This week".equalsIgnoreCase(timeFilter)) {
                    Date weekFromNow = addDays(today, 7);
                    return eventDate.before(today) || !eventDate.before(weekFromNow);
                } else if ("This month".equalsIgnoreCase(timeFilter)) {
                    return eventDate.getMonth() != today.getMonth()
                            || eventDate.getYear() != today.getYear();
                }

                return false;
            });
        }

        // 4. Sort
        if ("oldest".equalsIgnoreCase(sortBy)) {
            // cũ nhất trước
            Collections.sort(result, Comparator.comparing(this::getSortCreatedDateSafe));
        } else if ("event_date".equalsIgnoreCase(sortBy)) {
            // sort theo ngày tiệc
            result.sort(Comparator.comparing(
                    Bookings::getEventDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        } else if ("highest_value".equalsIgnoreCase(sortBy)) {
            // tổng tiền cao nhất trước
            result.sort((a, b2) -> {
                BigDecimal ta = (a.getTotalAmount() != null) ? a.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal tb = (b2.getTotalAmount() != null) ? b2.getTotalAmount() : BigDecimal.ZERO;
                return tb.compareTo(ta);
            });
        } else {
            // newest (default) – mới nhất trước (dựa vào createdAt, fallback bookingId)
            result.sort((a, b2) -> getSortCreatedDateSafe(b2)
                    .compareTo(getSortCreatedDateSafe(a)));
        }

        return result;
    }

    /**
     * Kiểm tra 1 booking có match text search hay không
     */
    private boolean matchesSearch(Bookings b, String q) {
        if (b == null || q == null || q.isEmpty()) {
            return false;
        }

        // Booking code
        if (containsIgnoreCase(b.getBookingCode(), q)) {
            return true;
        }

        // 1) Contact trên booking (ƯU TIÊN)
        try {
            if (containsIgnoreCase(b.getContactFullName(), q)) {
                return true;
            }
            if (containsIgnoreCase(b.getContactPhone(), q)) {
                return true;
            }
            if (containsIgnoreCase(b.getContactEmail(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        // 2) Thông tin account (customerId) – fallback
        try {
            if (b.getCustomerId() != null) {
                if (containsIgnoreCase(b.getCustomerId().getFullName(), q)) {
                    return true;
                }
                if (containsIgnoreCase(b.getCustomerId().getPhone(), q)) {
                    return true;
                }
                if (containsIgnoreCase(b.getCustomerId().getEmail(), q)) {
                    return true;
                }
            }
        } catch (Exception ignore) {
        }

        // 3) Tên nhà hàng
        try {
            if (b.getRestaurantId() != null
                    && containsIgnoreCase(b.getRestaurantId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        // 4) Loại tiệc
        try {
            if (b.getEventTypeId() != null
                    && containsIgnoreCase(b.getEventTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        // 5) Dịch vụ
        try {
            if (b.getServiceTypeId() != null
                    && containsIgnoreCase(b.getServiceTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        // 6) Địa điểm ngoài
        try {
            if (containsIgnoreCase(b.getOutsideAddress(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        return false;
    }

    private boolean containsIgnoreCase(String value, String q) {
        return value != null && q != null && value.toLowerCase().contains(q);
    }

    /**
     * Lấy ngày để sort (ưu tiên createdAt, không có thì dùng bookingId làm
     * pseudo-date)
     */
    private Date getSortCreatedDateSafe(Bookings b) {
        try {
            if (b.getCreatedAt() != null) {
                return b.getCreatedAt();
            }
        } catch (Exception ignore) {
        }

        long idAsTime = (b.getBookingId() != null) ? b.getBookingId().longValue() : 0L;
        return new Date(idAsTime);
    }

    /* ================= PAGINATION ================= */
    public List<Bookings> getPagedBookings() {
        List<Bookings> filtered = getFilteredList();
        if (filtered.isEmpty()) {
            return filtered;
        }

        int from = (currentPage - 1) * rowsPerPage;
        if (from < 0) {
            from = 0;
        }
        if (from >= filtered.size()) {
            currentPage = 1;
            from = 0;
        }
        int to = Math.min(from + rowsPerPage, filtered.size());
        return filtered.subList(from, to);
    }

    public int getTotalFilteredBookings() {
        return getFilteredList().size();
    }

    public int getTotalPages() {
        int size = getTotalFilteredBookings();
        if (size == 0) {
            return 1;
        }
        return (int) Math.ceil((double) size / (double) rowsPerPage);
    }

    /* ================= STATS ================= */
    public int getUpcomingCount() {
        Date now = truncateTime(new Date());
        int count = 0;
        for (Bookings b : bookings) {
            if (b.getBookingStatus() != null
                    && "CONFIRMED".equalsIgnoreCase(b.getBookingStatus())
                    && b.getEventDate() != null
                    && truncateTime(b.getEventDate()).after(now)) {
                count++;
            }
        }
        return count;
    }

    public int getPendingCount() {
        return countByStatus("PENDING");
    }

    public int getCompletedCount() {
        return countByStatus("COMPLETED");
    }

    public int getCancelledCount() {
        return countByStatus("CANCELLED");
    }

    private int countByStatus(String status) {
        int c = 0;
        for (Bookings b : bookings) {
            if (b.getBookingStatus() != null
                    && status.equalsIgnoreCase(b.getBookingStatus())) {
                c++;
            }
        }
        return c;
    }

    /**
     * Tổng doanh thu dựa trên danh sách đang được filter (loại bỏ booking đã
     * hủy)
     */
    public BigDecimal getTotalRevenue() {
        BigDecimal sum = BigDecimal.ZERO;

        // dùng danh sách đã lọc (status, search, timeFilter...)
        for (Bookings b : getFilteredList()) {
            if (b == null) {
                continue;
            }

            // trạng thái booking
            String bookingStatus = (b.getBookingStatus() != null)
                    ? b.getBookingStatus().trim().toUpperCase()
                    : "";

            // CHỈ tính khi booking đã COMPLETED
            if (!"COMPLETED".equals(bookingStatus)) {
                continue;
            }

            // trạng thái payment
            String paymentStatus = (b.getPaymentStatus() != null)
                    ? b.getPaymentStatus().trim().toUpperCase()
                    : "";

            // CHỈ tính khi đã thanh toán đủ
            // (nếu trong DB bạn đang để 'PAID' thì đổi chuỗi này lại cho đúng)
            if (!"PAID_IN_FULL".equals(paymentStatus)) {
                continue;
            }

            BigDecimal total = b.getTotalAmount();
            if (total == null) {
                continue;
            }

            sum = sum.add(total);
        }

        return sum;
    }

    /* ================= ACTIONS ================= */
    public String updateStatus(Bookings booking, String newStatus) {
        if (booking == null || newStatus == null) {
            return null;
        }

        try {
            Date now = new Date();

            // cập nhật trạng thái
            booking.setBookingStatus(newStatus);
            try {
                booking.setUpdatedAt(now);
            } catch (Exception ignore) {
            }

            // nếu là CANCELLED -> lưu lý do & thời gian huỷ (nếu entity có field)
            if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                String reason = (rejectReason != null) ? rejectReason.trim() : "";
                if (!reason.isEmpty()) {
                    try {
                        booking.setRejectReason(reason);
                    } catch (Exception ignore) {
                    }
                    try {
                        booking.setCancelReason(reason);
                    } catch (Exception ignore) {
                    }
                }
                try {
                    booking.setCancelTime(now);
                } catch (Exception ignore) {
                }
            }

            bookingsFacade.edit(booking);
            loadBookings();

            // reset lý do để không dính qua booking khác
            rejectReason = null;

            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "Booking status updated to " + newStatus
                    )
            );
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Update failed",
                            ex.getMessage()
                    )
            );
        }
        return null;
    }

    /**
     * Cập nhật trạng thái thanh toán của booking
     */
    public String updatePaymentStatus(Bookings booking, String newStatus) {
        if (booking == null || newStatus == null) {
            return null;
        }

        try {
            Date now = new Date();

            booking.setPaymentStatus(newStatus);
            try {
                booking.setUpdatedAt(now);
            } catch (Exception ignore) {
            }

            bookingsFacade.edit(booking);
            loadBookings();

            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_INFO,
                            "Success",
                            "Payment status updated to " + newStatus
                    )
            );
        } catch (Exception ex) {
            FacesContext.getCurrentInstance().addMessage(
                    null,
                    new FacesMessage(
                            FacesMessage.SEVERITY_ERROR,
                            "Update payment failed",
                            ex.getMessage()
                    )
            );
        }
        return null;
    }

    // mở / đóng panel chi tiết
    public void selectBooking(Bookings booking) {
        this.selectedBooking = booking;
    }

    public void closeDetail() {
        this.selectedBooking = null;
    }

    /* ================= Date helper ================= */
    private Date truncateTime(Date d) {
        if (d == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        Date res = new Date(d.getYear(), d.getMonth(), d.getDate());
        return res;
    }

    private Date addDays(Date d, int days) {
        if (d == null) {
            return null;
        }
        @SuppressWarnings("deprecation")
        Date res = new Date(d.getYear(), d.getMonth(), d.getDate() + days);
        return res;
    }

    /* ================= GET / SET ================= */
    public List<Bookings> getBookings() {
        return bookings;
    }

    public void setBookings(List<Bookings> bookings) {
        this.bookings = bookings;
    }

    public Bookings getSelectedBooking() {
        return selectedBooking;
    }

    public void setSelectedBooking(Bookings selectedBooking) {
        this.selectedBooking = selectedBooking;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
        this.currentPage = 1;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
        this.currentPage = 1;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        this.currentPage = 1;
    }

    public String getTimeFilter() {
        return timeFilter;
    }

    public void setTimeFilter(String timeFilter) {
        this.timeFilter = timeFilter;
        this.currentPage = 1;
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
        this.currentPage = 1;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
