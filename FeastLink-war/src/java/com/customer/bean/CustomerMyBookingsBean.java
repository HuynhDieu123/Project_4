package com.customer.bean;

import com.mypack.entity.Bookings;
import com.mypack.entity.EventTypes;
import com.mypack.entity.ServiceTypes;
import com.mypack.entity.Users;
import com.mypack.entity.BookingCombos;
import com.mypack.entity.BookingMenuItems;

import com.mypack.sessionbean.BookingsFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import jakarta.faces.view.ViewScoped;

@Named("customerMyBookingsBean")
@ViewScoped
public class CustomerMyBookingsBean implements Serializable {

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    private List<Bookings> myBookings = new ArrayList<>();
    // ========= PAGINATION =========
    // Số booking mỗi trang (bro muốn 5, 6, 10 tùy chỉnh)
    private int pageSize = 5;

    // Trang hiện tại (bắt đầu từ 1)
    private int currentPage = 1;

    private final Locale displayLocale = Locale.US;
    private final SimpleDateFormat shortDateFmt
            = new SimpleDateFormat("EEE, dd MMM yyyy", displayLocale);
    private final SimpleDateFormat longDateFmt
            = new SimpleDateFormat("EEEE, dd MMMM yyyy", displayLocale);
    private final SimpleDateFormat timeFmt
            = new SimpleDateFormat("HH:mm", displayLocale);

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }

        Object userObj = ctx.getExternalContext()
                .getSessionMap()
                .get("currentUser");

        if (userObj instanceof Users) {
            Users currentUser = (Users) userObj;

            List<Bookings> all = bookingsFacade.findAll();
            if (all != null) {
                for (Bookings b : all) {
                    if (b == null || b.getCustomerId() == null) {
                        continue;
                    }
                    if (b.getCustomerId().getUserId()
                            .equals(currentUser.getUserId())) {
                        myBookings.add(b);
                    }
                }
            }

            // *** Sort: eventDate / createdAt DESC (mới nhất nằm trên cùng) ***
            Collections.sort(myBookings, new Comparator<Bookings>() {
                @Override
                public int compare(Bookings o1, Bookings o2) {
                    Date d1 = (o1 != null)
                            ? (o1.getEventDate() != null ? o1.getEventDate() : o1.getCreatedAt())
                            : null;
                    Date d2 = (o2 != null)
                            ? (o2.getEventDate() != null ? o2.getEventDate() : o2.getCreatedAt())
                            : null;

                    if (d1 == null && d2 == null) {
                        return 0;
                    }
                    if (d1 == null) {
                        return 1;   // d1 null => đứng sau
                    }
                    if (d2 == null) {
                        return -1;  // d2 null => đứng sau
                    }
                    // DESC
                    return d2.compareTo(d1);
                }
            });
        }
    }

    public List<Bookings> getMyBookings() {
        return myBookings;
    }

    // ========== STATS ==========
    public int getUpcomingCount() {
        int count = 0;
        LocalDate today = LocalDate.now();
        for (Bookings b : myBookings) {
            String status = safe(b.getBookingStatus());
            LocalDate event = toLocalDate(b.getEventDate());
            boolean isFutureOrToday = (event == null || !event.isBefore(today));
            if (isFutureOrToday
                    && ("PENDING".equalsIgnoreCase(status)
                    || "CONFIRMED".equalsIgnoreCase(status))) {
                count++;
            }
        }
        return count;
    }

    public int getCompletedCount() {
        int count = 0;
        for (Bookings b : myBookings) {
            if ("COMPLETED".equalsIgnoreCase(safe(b.getBookingStatus()))) {
                count++;
            }
        }
        return count;
    }

    public BigDecimal getTotalSpent() {
        BigDecimal sum = BigDecimal.ZERO;
        for (Bookings b : myBookings) {
            if ("COMPLETED".equalsIgnoreCase(safe(b.getBookingStatus()))
                    && b.getTotalAmount() != null) {
                sum = sum.add(b.getTotalAmount());
            }
        }
        return sum;
    }

    public String getFormattedTotalSpent() {
        BigDecimal total = getTotalSpent();
        NumberFormat nf = NumberFormat.getCurrencyInstance(displayLocale);
        return nf.format(total != null ? total : BigDecimal.ZERO);
    }

    // ========== FORMAT HỖ TRỢ ==========
    private LocalDate toLocalDate(Date date) {
        if (date == null) {
            return null;
        }

        // Nếu là java.sql.Date thì dùng toLocalDate() riêng
        if (date instanceof java.sql.Date) {
            return ((java.sql.Date) date).toLocalDate();
        }

        // Còn lại (java.util.Date bình thường) mới dùng toInstant()
        return date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public String formatDateShort(Date date) {
        return (date != null) ? shortDateFmt.format(date) : "";
    }

    public String formatDateLong(Date date) {
        return (date != null) ? longDateFmt.format(date) : "";
    }

    public String formatTime(Date date) {
        return (date != null) ? timeFmt.format(date) : "";
    }

    public String formatMoney(BigDecimal value) {
        if (value == null) {
            return "$0.00";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(displayLocale);
        return nf.format(value);
    }

        // ========== PACKAGE & MENU CHO TỪNG BOOKING ==========

    public boolean hasPackage(Bookings b) {
        return b != null
                && b.getBookingCombosCollection() != null
                && !b.getBookingCombosCollection().isEmpty();
    }

    public boolean hasMenuItems(Bookings b) {
        return b != null
                && b.getBookingMenuItemsCollection() != null
                && !b.getBookingMenuItemsCollection().isEmpty();
    }

    // Tên package (giả sử 1 booking chỉ có 1 combo chính)
    public String getPackageName(Bookings b) {
        if (!hasPackage(b)) {
            return "No package selected";
        }
        BookingCombos bc = b.getBookingCombosCollection().iterator().next();
        if (bc.getMenuCombos() != null && bc.getMenuCombos().getName() != null) {
            return bc.getMenuCombos().getName();
        }
        return "Package";
    }

    // Tóm tắt custom menu: số món
    public String getMenuSummary(Bookings b) {
        if (!hasMenuItems(b)) {
            return "No custom menu selected";
        }
        int dishes = 0;
        for (BookingMenuItems bmi : b.getBookingMenuItemsCollection()) {
            if (bmi.getQuantity() > 0) {
                dishes++;
            }
        }
        return dishes + " dishes";
    }

    // Subtotal package cho booking (từ BookingCombos.TotalPrice)
    public BigDecimal getPackageSubtotal(Bookings b) {
        if (!hasPackage(b)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BookingCombos bc : b.getBookingCombosCollection()) {
            if (bc.getTotalPrice() != null) {
                sum = sum.add(bc.getTotalPrice());
            }
        }
        return sum;
    }

    // Subtotal custom menu (từ BookingMenuItems.TotalPrice)
    public BigDecimal getMenuSubtotal(Bookings b) {
        if (!hasMenuItems(b)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (BookingMenuItems bmi : b.getBookingMenuItemsCollection()) {
            if (bmi.getTotalPrice() != null) {
                sum = sum.add(bmi.getTotalPrice());
            }
        }
        return sum;
    }

    // Other charges = Total - Package - Menu (không cho âm)
    public BigDecimal getOtherCharges(Bookings b) {
        if (b == null || b.getTotalAmount() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = b.getTotalAmount();
        BigDecimal pkg = getPackageSubtotal(b);
        BigDecimal menu = getMenuSubtotal(b);

        BigDecimal other = total.subtract(pkg.add(menu));
        if (other.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return other;
    }

    // ===== Bridge methods cho EL gọi kiểu method-expression =====

    // dùng cho #{customerMyBookingsBean.packageName(b)}
    public String packageName(Bookings b) {
        return getPackageName(b);
    }

    // dùng cho #{customerMyBookingsBean.menuSummary(b)}
    public String menuSummary(Bookings b) {
        return getMenuSummary(b);
    }

    // dùng cho #{customerMyBookingsBean.packageSubtotal(b)}
    public BigDecimal packageSubtotal(Bookings b) {
        return getPackageSubtotal(b);
    }

    // dùng cho #{customerMyBookingsBean.menuSubtotal(b)}
    public BigDecimal menuSubtotal(Bookings b) {
        return getMenuSubtotal(b);
    }

    // dùng cho #{customerMyBookingsBean.otherCharges(b)}
    public BigDecimal otherCharges(Bookings b) {
        return getOtherCharges(b);
    }

    // ========== HELPER DÙNG TRONG EL ==========
    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    // Cho phép hủy: đang Pending/Confirmed và còn cách ngày tiệc >= 3 ngày
    public boolean canCancel(Bookings b) {
        if (b == null) {
            return false;
        }
        String status = safe(b.getBookingStatus()).toUpperCase();
        if (!("PENDING".equals(status) || "CONFIRMED".equals(status))) {
            return false;
        }
        return canCancelByDate(b);
    }

    private boolean canCancelByDate(Bookings b) {
        Date date = b.getEventDate();
        if (date == null) {
            return true;  // demo: nếu thiếu ngày thì cho hủy
        }
        LocalDate event = toLocalDate(date);
        LocalDate today = LocalDate.now();
        // phải còn ít nhất 3 ngày
        return today.isBefore(event.minusDays(3));
    }

    public boolean isRemainingPositive(Bookings b) {
        if (b == null || b.getRemainingAmount() == null) {
            return false;
        }
        return b.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean needsPayment(Bookings b) {
        if (b == null) {
            return false;
        }
        String status = safe(b.getBookingStatus()).toUpperCase();
        String payment = safe(b.getPaymentStatus()).toUpperCase();
        return "CONFIRMED".equals(status) && !"PAID".equals(payment);
    }

    public String statusMessage(Bookings b) {
        if (b == null) {
            return "";
        }
        String status = safe(b.getBookingStatus()).toUpperCase();

        if ("CONFIRMED".equals(status)) {
            return "Free cancellation up to 3 days before the event.";
        }
        if ("PENDING".equals(status)) {
            return "We'll confirm your request within 24–48 hours.";
        }
        if ("COMPLETED".equals(status)) {
            return "You can rate this venue and download the invoice.";
        }
        if ("CANCELLED".equals(status)) {
            String reason = safe(b.getCancelReason());
            return reason.isEmpty()
                    ? "This booking was cancelled."
                    : "Cancelled: " + reason;
        }
        return "";
    }

    public String locationDisplay(Bookings b) {
        if (b == null) {
            return "";
        }
        String locType = safe(b.getLocationType());
        if ("AT_RESTAURANT".equalsIgnoreCase(locType)) {
            return "At restaurant";
        }
        String outside = safe(b.getOutsideAddress());
        return outside.isEmpty() ? locType : outside;
    }

    public String eventTypeDisplay(Bookings b) {
        if (b == null) {
            return "";
        }
        EventTypes et = b.getEventTypeId();
        return (et != null) ? safe(et.getName()) : "";
    }

    public String serviceTypeDisplay(Bookings b) {
        if (b == null) {
            return "";
        }
        ServiceTypes st = b.getServiceTypeId();
        return (st != null) ? safe(st.getName()) : "";
    }

    public String bookerDisplay(Bookings b) {
        if (b == null) {
            return "";
        }

        Users u = b.getCustomerId();

        // ƯU TIÊN lấy contact lưu trong booking
        String fullName = safe(b.getContactFullName());
        String phone = safe(b.getContactPhone());
        String email = safe(b.getContactEmail());

        // Nếu booking cũ chưa có contact ⇒ fallback qua account
        if (fullName.isEmpty() && u != null) {
            fullName = safe(u.getFullName());
        }
        if (phone.isEmpty() && u != null) {
            phone = safe(u.getPhone());
        }
        if (email.isEmpty() && u != null) {
            email = safe(u.getEmail());
        }

        StringBuilder sb = new StringBuilder();
        if (!fullName.isEmpty()) {
            sb.append(fullName);
        }
        if (!phone.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(phone);
        }
        if (!email.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(email);
        }

        return sb.toString();
    }

    // ========== ACTION: VIEW DETAILS ==========
    public String viewDetails(Long bookingId) {
        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Missing booking id.");
            return null;
        }
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.getExternalContext().getSessionMap()
                .put("selectedBookingId", bookingId);
        // sang trang chi tiết
        return "/Customer/booking-details";
    }

    // ========== ACTION: CANCEL BOOKING ==========
    public String cancelBooking(Long bookingId) {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (bookingId == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Missing booking id.");
            return null;
        }

        Bookings booking = bookingsFacade.find(bookingId);
        if (booking == null) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "Booking not found.");
            return null;
        }

        // Check quyền: chỉ được hủy booking của chính mình
        Object userObj = ctx.getExternalContext()
                .getSessionMap().get("currentUser");
        if (!(userObj instanceof Users)) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "You need to log in again.");
            return "/login";
        }

        Users currentUser = (Users) userObj;
        if (booking.getCustomerId() == null
                || !booking.getCustomerId().getUserId()
                        .equals(currentUser.getUserId())) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error", "You cannot cancel this booking.");
            return null;
        }

        // Check rule hủy
        if (!canCancel(booking)) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Cannot cancel",
                    "This booking can no longer be cancelled online.");
            return null;
        }

        // Cập nhật trạng thái
        booking.setBookingStatus("CANCELLED");
        booking.setCancelReason("Cancelled by customer via My Bookings page.");
        booking.setCancelTime(new Date());
        booking.setUpdatedAt(new Date());

        bookingsFacade.edit(booking);

        addMessage(FacesMessage.SEVERITY_INFO,
                "Booking cancelled",
                "Your booking has been cancelled. Refund policy will follow the venue's rules.");

        // load lại trang danh sách
        return "/Customer/my-bookings";
    }

    // Dùng cho sort: trả về millis của ngày tạo booking (hoặc eventDate nếu createdAt = null)
    public long bookingCreatedAtMillis(com.mypack.entity.Bookings b) {
        if (b == null) {
            return 0L;
        }

        java.util.Date created = b.getCreatedAt();
        if (created != null) {
            return created.getTime();
        }

        java.util.Date event = b.getEventDate();
        return (event != null) ? event.getTime() : 0L;
    }

    private void addMessage(FacesMessage.Severity severity,
            String summary,
            String detail) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ctx.addMessage(null, new FacesMessage(severity, summary, detail));
    }
    // ========= PAGINATION HELPERS =========

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        if (pageSize > 0) {
            this.pageSize = pageSize;
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    // Tổng số booking (dùng hiển thị "X bookings")
    public int getTotalRecords() {
        return (myBookings != null) ? myBookings.size() : 0;
    }

    // Tổng số trang
    public int getTotalPages() {
        int totalRecords = getTotalRecords();
        if (totalRecords == 0) {
            return 1;
        }
        return (int) Math.ceil((double) totalRecords / (double) pageSize);
    }

    // Danh sách booking chỉ cho trang hiện tại
    public List<Bookings> getPagedBookings() {
        if (myBookings == null || myBookings.isEmpty()) {
            return new ArrayList<>();
        }

        int totalPages = getTotalPages();

        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, myBookings.size());

        return myBookings.subList(fromIndex, toIndex);
    }

    // List số trang: [1, 2, 3, ...] để ui:repeat vẽ button
    public List<Integer> getPageNumbers() {
        List<Integer> pages = new ArrayList<>();
        int totalPages = getTotalPages();
        for (int i = 1; i <= totalPages; i++) {
            pages.add(i);
        }
        return pages;
    }

    // Bấm qua trang kế
    public String nextPage() {
        if (currentPage < getTotalPages()) {
            currentPage++;
        }
        return null; // ở lại trang hiện tại
    }

    // Bấm về trang trước
    public String previousPage() {
        if (currentPage > 1) {
            currentPage--;
        }
        return null;
    }

    // Bấm trực tiếp số trang
    public String changePage() {
        // currentPage đã được set bởi f:setPropertyActionListener
        return null;
    }

}
