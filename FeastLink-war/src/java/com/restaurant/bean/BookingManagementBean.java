package com.restaurant.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.RestaurantManagers;
import com.mypack.entity.Users;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;

import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.RestaurantManagersFacadeLocal;
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

    // dùng để tìm nhà hàng của manager
    @EJB
    private RestaurantManagersFacadeLocal restaurantManagersFacade;

    // id nhà hàng hiện tại (của manager đang login)
    private Integer currentRestaurantId;

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
        // 1. xác định nhà hàng hiện tại từ user login
        resolveCurrentRestaurant();
        // 2. load booking của nhà hàng đó
        loadBookings();
    }

    /**
     * Lấy Users đang login từ session ("currentUser") rồi tìm
     * RestaurantManagers để lấy restaurantId
     */
    private void resolveCurrentRestaurant() {
        try {
            FacesContext ctx = FacesContext.getCurrentInstance();
            if (ctx == null) {
                return;
            }

            Object obj = ctx.getExternalContext()
                    .getSessionMap()
                    .get("currentUser"); // LoginBean set key này

            if (!(obj instanceof Users)) {
                return;
            }

            Users user = (Users) obj;

            // tìm manager theo userId
            List<RestaurantManagers> managers = restaurantManagersFacade.findAll();
            if (managers == null) {
                return;
            }

            for (RestaurantManagers rm : managers) {
                if (rm == null || rm.getUserId() == null) {
                    continue;
                }
                if (rm.getUserId().getUserId() == null) {
                    continue;
                }

                if (rm.getUserId().getUserId().equals(user.getUserId())
                        && rm.getRestaurantId() != null
                        && rm.getRestaurantId().getRestaurantId() != null) {

                    Object ridObj = rm.getRestaurantId().getRestaurantId();
                    if (ridObj instanceof Number) {
                        currentRestaurantId = ((Number) ridObj).intValue();
                    } else {
                        try {
                            currentRestaurantId = Integer.valueOf(ridObj.toString());
                        } catch (NumberFormatException ignore) {
                            currentRestaurantId = null;
                        }
                    }
                    break;
                }
            }

            System.out.println("BookingManagementBean - currentRestaurantId = " + currentRestaurantId);

        } catch (Exception ex) {
            currentRestaurantId = null;
        }
    }

    public void loadBookings() {
        try {
            bookings = bookingsFacade.findAll();
            if (bookings == null) {
                bookings = new ArrayList<>();
            }

            // =============== LỌC THEO NHÀ HÀNG HIỆN TẠI ===============
            // Nếu đã xác định được nhà hàng của manager -> chỉ giữ booking của nhà hàng đó.
            // Nếu chưa xác định được (currentRestaurantId == null) thì GIỮ NGUYÊN,
            // để còn thấy tất cả booking (không bị clear như trước).
            if (currentRestaurantId != null) {
                final String ridStr = String.valueOf(currentRestaurantId);

                bookings.removeIf(b -> {
                    if (b == null || b.getRestaurantId() == null
                            || b.getRestaurantId().getRestaurantId() == null) {
                        // booking không gắn nhà hàng -> bỏ
                        return true;
                    }
                    String bidStr = String.valueOf(b.getRestaurantId().getRestaurantId());
                    // khác id nhà hàng hiện tại -> bỏ
                    return !bidStr.equals(ridStr);
                });
            }

        } catch (Exception ex) {
            bookings = new ArrayList<>();
        }
    }

    // helper: kiểm tra booking thuộc nhà hàng hiện tại
    // helper: kiểm tra booking thuộc nhà hàng hiện tại
    private boolean belongsToCurrentRestaurant(Bookings b) {
        if (b == null || b.getRestaurantId() == null
                || b.getRestaurantId().getRestaurantId() == null) {
            return false;
        }

        // Nếu chưa xác định được nhà hàng hiện tại -> cho qua hết
        // (để các counter vẫn đếm bình thường)
        if (currentRestaurantId == null) {
            return true;
        }

        String ridStr = String.valueOf(currentRestaurantId);
        String bidStr = String.valueOf(b.getRestaurantId().getRestaurantId());
        return bidStr.equals(ridStr);
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
            Collections.sort(result, Comparator.comparing(this::getSortCreatedDateSafe));
        } else if ("event_date".equalsIgnoreCase(sortBy)) {
            result.sort(Comparator.comparing(
                    Bookings::getEventDate,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        } else if ("highest_value".equalsIgnoreCase(sortBy)) {
            result.sort((a, b2) -> {
                BigDecimal ta = (a.getTotalAmount() != null) ? a.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal tb = (b2.getTotalAmount() != null) ? b2.getTotalAmount() : BigDecimal.ZERO;
                return tb.compareTo(ta);
            });
        } else {
            // newest (default)
            result.sort((a, b2) -> getSortCreatedDateSafe(b2)
                    .compareTo(getSortCreatedDateSafe(a)));
        }

        return result;
    }

    private boolean matchesSearch(Bookings b, String q) {
        if (b == null || q == null || q.isEmpty()) {
            return false;
        }

        if (containsIgnoreCase(b.getBookingCode(), q)) {
            return true;
        }

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

        try {
            if (b.getRestaurantId() != null
                    && containsIgnoreCase(b.getRestaurantId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getEventTypeId() != null
                    && containsIgnoreCase(b.getEventTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

        try {
            if (b.getServiceTypeId() != null
                    && containsIgnoreCase(b.getServiceTypeId().getName(), q)) {
                return true;
            }
        } catch (Exception ignore) {
        }

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
                    && truncateTime(b.getEventDate()).after(now)
                    && belongsToCurrentRestaurant(b)) {
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
                    && status.equalsIgnoreCase(b.getBookingStatus())
                    && belongsToCurrentRestaurant(b)) {
                c++;
            }
        }
        return c;
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bookings b : getFilteredList()) {
            if (b == null) {
                continue;
            }

            String bookingStatus = (b.getBookingStatus() != null)
                    ? b.getBookingStatus().trim().toUpperCase()
                    : "";
            if (!"COMPLETED".equals(bookingStatus)) {
                continue;
            }

            String paymentStatus = (b.getPaymentStatus() != null)
                    ? b.getPaymentStatus().trim().toUpperCase()
                    : "";
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

            booking.setBookingStatus(newStatus);
            try {
                booking.setUpdatedAt(now);
            } catch (Exception ignore) {
            }

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

    /* ================= Menu & Package helper cho selectedBooking ================= */
    // Có package không?
    public boolean getHasSelectedPackage() {
        if (selectedBooking == null
                || selectedBooking.getBookingCombosCollection() == null) {
            return false;
        }
        return !selectedBooking.getBookingCombosCollection().isEmpty();
    }

    // Có custom menu không?
    public boolean getHasSelectedMenuItems() {
        if (selectedBooking == null
                || selectedBooking.getBookingMenuItemsCollection() == null) {
            return false;
        }
        return !selectedBooking.getBookingMenuItemsCollection().isEmpty();
    }

    // Tên package (lấy combo đầu tiên)
    public String getSelectedBookingPackageName() {
        if (!getHasSelectedPackage()) {
            return null;
        }

        BookingCombos bc = selectedBooking.getBookingCombosCollection()
                .iterator().next();

        // NOTE: nếu trong BookingCombos quan hệ tới MenuCombos tên khác "menuCombos"
        // thì đổi lại cho đúng, ví dụ: bc.getCombo().getName()
        if (bc.getMenuCombos() != null && bc.getMenuCombos().getName() != null) {
            return bc.getMenuCombos().getName();
        }
        return null;
    }

    // Tổng tiền package của booking này
    public BigDecimal getSelectedBookingPackageSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (!getHasSelectedPackage()) {
            return total;
        }

        for (BookingCombos bc : selectedBooking.getBookingCombosCollection()) {
            if (bc.getTotalPrice() != null) {
                total = total.add(bc.getTotalPrice());
            }
        }
        return total;
    }

    // Tổng tiền custom menu
    public BigDecimal getSelectedBookingMenuSubtotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (!getHasSelectedMenuItems()) {
            return total;
        }

        for (BookingMenuItems bmi : selectedBooking.getBookingMenuItemsCollection()) {
            if (bmi.getTotalPrice() != null) {
                total = total.add(bmi.getTotalPrice());
            }
        }
        return total;
    }

    // Số món trong custom menu
    public int getSelectedBookingMenuItemCount() {
        if (!getHasSelectedMenuItems()) {
            return 0;
        }
        return selectedBooking.getBookingMenuItemsCollection().size();
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

    public Integer getCurrentRestaurantId() {
        return currentRestaurantId;
    }

    public void setCurrentRestaurantId(Integer currentRestaurantId) {
        this.currentRestaurantId = currentRestaurantId;
    }

}
