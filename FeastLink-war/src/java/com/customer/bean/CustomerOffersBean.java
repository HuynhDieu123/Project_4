package com.customer.bean;

import com.mypack.entity.PointSettings;
import com.mypack.entity.PointWallets;
import com.mypack.entity.Vouchers;
import com.mypack.entity.Bookings;
import com.mypack.entity.Users;
import com.mypack.entity.UserVouchers;

import com.mypack.sessionbean.PointSettingsFacadeLocal;
import com.mypack.sessionbean.PointWalletsFacadeLocal;
import com.mypack.sessionbean.VouchersFacadeLocal;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.sessionbean.UserVouchersFacadeLocal;
import com.mypack.sessionbean.UsersFacadeLocal;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Named("customerOffersBean")
@RequestScoped
public class CustomerOffersBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PointWalletsFacadeLocal pointWalletsFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    @EJB
    private PointSettingsFacadeLocal pointSettingsFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    // ✅ NEW: lưu voucher user đã đổi
    @EJB
    private UserVouchersFacadeLocal userVouchersFacade;

    // ✅ NEW (an toàn): lấy Users managed theo id
    @EJB
    private UsersFacadeLocal usersFacade;

    // ====== USER / LOGIN STATE ======
    private Users currentUser;
    private boolean loggedIn;

    // point wallet
    private PointWallets wallet;

    // list of vouchers that can be redeemed
    private List<Vouchers> redeemableOffers;

    // rule from PointSettings
    private Long amountPerPoint;     // USD
    private Integer pointsPerAmount; // points

    // last redeemed voucher (for "Voucher unlocked" block in xhtml)
    private String lastRedeemedCode;
    private String lastRedeemedName;

    // đếm số voucher đã đổi (trong phiên xem trang này)
    private int redeemedVoucherCount;

    // =====================================================
    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            Object obj = ctx.getExternalContext().getSessionMap().get("currentUser");
            if (obj instanceof Users) {
                currentUser = (Users) obj;
            }
        }

        // chỉ coi là loggedIn nếu có currentUser và role = CUSTOMER
        loggedIn = (currentUser != null
                && currentUser.getRole() != null
                && "CUSTOMER".equalsIgnoreCase(currentUser.getRole().trim()));

        if (!loggedIn) {
            wallet = null;
            redeemableOffers = new ArrayList<>();
            redeemedVoucherCount = 0;
            return;
        }

        // ĐÃ LOGIN CUSTOMER
        loadPointRule();              // đọc cấu hình điểm từ PointSettings
        initWallet();                 // lấy / tạo ví theo UserId
        syncPointsFromPaidBookings(); // TÍNH LẠI điểm từ DB
        loadRedeemableOffers();       // load list voucher có thể đổi
        redeemedVoucherCount = 0;
    }

    // =====================================================
    private void loadPointRule() {
        // default nếu chưa cấu hình (100 USD => 1 point)
        amountPerPoint = 100L;
        pointsPerAmount = 1;

        try {
            List<PointSettings> list = pointSettingsFacade.findAll();
            if (list != null && !list.isEmpty()) {
                PointSettings ps = list.get(0);

                if (ps != null) {
                    Long amountObj = ps.getAmountPerPoint();
                    Integer pointsObj = ps.getPointsPerAmount();

                    if (amountObj != null && pointsObj != null
                            && amountObj > 0L && pointsObj > 0) {
                        amountPerPoint = amountObj;
                        pointsPerAmount = pointsObj;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * LẤY VÍ ĐIỂM CỦA CUSTOMER ĐANG ĐĂNG NHẬP
     * - Duyệt pointWalletsFacade.findAll()
     * - Tìm wallet có UserId trùng với currentUser.userId
     * - Nếu chưa có thì tạo mới với 0 điểm
     */
    private void initWallet() {
        if (!loggedIn || currentUser == null) {
            wallet = null;
            return;
        }

        try {
            wallet = null;

            Long currentUid = currentUser.getUserId(); // userId của user đang login

            List<PointWallets> all = pointWalletsFacade.findAll();
            if (all != null && !all.isEmpty() && currentUid != null) {
                for (PointWallets w : all) {
                    if (w == null) continue;
                    if (w.getUserId() == null) continue;

                    if (currentUid.equals(w.getUserId())) {
                        wallet = w; // tìm được ví của current user
                        break;
                    }
                }
            }

            // Nếu không tìm thấy ví cho user hiện tại -> tạo mới
            if (wallet == null) {
                wallet = new PointWallets();
                wallet.setUserId(currentUid);  // gắn đúng UserId đang login
                wallet.setCurrentPoints(0L);
                wallet.setUpdatedAt(new Date());
                pointWalletsFacade.create(wallet);
            }

        } catch (Exception ex) {
            ex.printStackTrace();

            if (wallet == null) {
                wallet = new PointWallets();
                wallet.setUserId(currentUser.getUserId());
                wallet.setCurrentPoints(0L);
                wallet.setUpdatedAt(new Date());
            }
        }
    }

    
    /**
     * ✅ SYNC ĐIỂM:
     * - EarnedPoints: tính từ tất cả Booking của Customer có PaymentStatus = 'PAID'
     * - SpentPoints: tính từ lịch sử đổi Voucher (bảng UserVouchers) = SUM(quantity * voucher.pointCost)
     *
     * => CurrentPoints = max(0, EarnedPoints - SpentPoints)
     *
     * Lý do phải làm vậy:
     * Trước đây bạn "set thẳng" CurrentPoints = EarnedPoints, nên khi user đổi voucher (đã trừ điểm)
     * mà đi trang khác quay lại, init() chạy lại sẽ gọi syncPointsFromPaidBookings() và cộng lại như cũ.
     */
    private void syncPointsFromPaidBookings() {
        if (!loggedIn || currentUser == null) {
            return;
        }

        if (wallet == null) {
            initWallet();
        }

        // Nếu chưa cấu hình PointSettings hợp lệ thì không sync
        if (amountPerPoint == null || amountPerPoint <= 0L
                || pointsPerAmount == null || pointsPerAmount <= 0) {
            return;
        }

        long earned = calcEarnedPointsFromPaidBookings();
        long spent = calcSpentPointsFromRedeemedVouchers();

        long newPoints = earned - spent;
        if (newPoints < 0L) newPoints = 0L;

        long oldPoints = safeWalletPoints(wallet);

        // Chỉ update khi có thay đổi (tránh update DB liên tục mỗi lần load trang)
        if (wallet != null && newPoints != oldPoints) {
            wallet.setCurrentPoints(newPoints);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.edit(wallet);
        }
    }

    /**
     * Safe getter for current points.
     * Works whether PointWallets#getCurrentPoints() returns primitive long or boxed Long.
     */
    private long safeWalletPoints(PointWallets w) {
        if (w == null) return 0L;
        // If getCurrentPoints() returns primitive long, it will auto-box into Long here.
        Long p = w.getCurrentPoints();
        return (p == null) ? 0L : p.longValue();
    }

    /**
     * Tính tổng điểm Earned dựa theo tất cả booking PAID của customer.
     */
    private long calcEarnedPointsFromPaidBookings() {
        long totalPoints = 0L;

        List<Bookings> allBookings;
        try {
            allBookings = bookingsFacade.findAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0L;
        }

        if (allBookings == null || allBookings.isEmpty()) {
            return 0L;
        }

        for (Bookings b : allBookings) {
            if (b == null) continue;

            if (b.getCustomerId() == null
                    || b.getCustomerId().getUserId() == null
                    || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
                continue;
            }

            String payStatus = b.getPaymentStatus();
            if (payStatus == null || !"PAID".equalsIgnoreCase(payStatus.trim())) {
                continue;
            }

            BigDecimal totalAmount = b.getTotalAmount();
            if (totalAmount == null) continue;

            long total = totalAmount.longValue();

            // tổng tiền phải >= amountPerPoint mới cộng
            if (total < amountPerPoint) continue;

            long multiplier = total / amountPerPoint;
            if (multiplier <= 0L) continue;

            long earned = multiplier * pointsPerAmount;
            totalPoints += earned;
        }

        return totalPoints;
    }

    /**
     * Tính tổng điểm đã "Spent" dựa theo các voucher user đã đổi.
     * SpentPoints = SUM(UserVouchers.quantity * Vouchers.pointCost)
     */
    private long calcSpentPointsFromRedeemedVouchers() {
        long spent = 0L;

        List<UserVouchers> all;
        try {
            all = userVouchersFacade.findAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0L;
        }

        if (all == null || all.isEmpty()) {
            return 0L;
        }

        for (UserVouchers uv : all) {
            if (uv == null) continue;
            if (uv.getUserId() == null || uv.getVoucherId() == null) continue;
            if (uv.getUserId().getUserId() == null) continue;

            if (!uv.getUserId().getUserId().equals(currentUser.getUserId())) {
                continue;
            }

            int qty = uv.getQuantity();
            if (qty <= 0) continue;

            Integer costObj = uv.getVoucherId().getPointCost();
            if (costObj == null || costObj <= 0) continue;

            spent += (long) qty * (long) costObj;
        }

        return spent;
    }


    private void loadRedeemableOffers() {
        redeemableOffers = new ArrayList<>();
        List<Vouchers> all = vouchersFacade.findAll();
        if (all == null) {
            return;
        }

        for (Vouchers v : all) {
            if (v == null) continue;

            Boolean pointRedeemable = v.getIsPointRedeemable();
            String status = v.getStatus();
            Integer pointCost = v.getPointCost();

            Integer totalQty = v.getTotalQuantity();
            Integer perLimit = v.getPerUserLimit();

            boolean stillHasQuantity = true;
            if (totalQty != null && totalQty <= 0) {
                stillHasQuantity = false;
            }
            if (perLimit != null && perLimit <= 0) {
                stillHasQuantity = false;
            }

            if (Boolean.TRUE.equals(pointRedeemable)
                    && status != null
                    && "ACTIVE".equalsIgnoreCase(status.trim())
                    && pointCost != null
                    && stillHasQuantity) {
                redeemableOffers.add(v);
            }
        }
    }

    
    // ✅ NEW: Lưu vào bảng UserVouchers (nếu đã có thì +quantity)
    // IMPORTANT: Method này bây giờ là "nguồn sự thật" để tính SpentPoints,
    // nên KHÔNG được nuốt lỗi âm thầm. Nếu lỗi thì redeem sẽ rollback best-effort.
    private void saveUserVoucherClaim(Users user, Vouchers voucher) {
        if (user == null || user.getUserId() == null || voucher == null || voucher.getVoucherId() == null) {
            throw new IllegalArgumentException("Invalid user/voucher for saving claim.");
        }

        try {
            Users managedUser = user;
            try {
                Users tmp = usersFacade.find(user.getUserId());
                if (tmp != null) managedUser = tmp;
            } catch (Exception ignore) {
            }

            UserVouchers existing = null;
            List<UserVouchers> all = userVouchersFacade.findAll();
            if (all != null && !all.isEmpty()) {
                for (UserVouchers uv : all) {
                    if (uv == null) continue;
                    if (uv.getUserId() == null || uv.getVoucherId() == null) continue;
                    if (uv.getUserId().getUserId() == null || uv.getVoucherId().getVoucherId() == null) continue;

                    if (uv.getUserId().getUserId().equals(managedUser.getUserId())
                            && uv.getVoucherId().getVoucherId().equals(voucher.getVoucherId())) {
                        existing = uv;
                        break;
                    }
                }
            }

            Date now = new Date();

            if (existing == null) {
                UserVouchers uv = new UserVouchers();
                uv.setUserId(managedUser);
                uv.setVoucherId(voucher);

                uv.setQuantity(1);
                uv.setUsedQuantity(0);
                uv.setStatus("ACTIVE");
                uv.setClaimedAt(now);

                uv.setUsedAt(null);
                uv.setUsedBookingId(null);

                userVouchersFacade.create(uv);
            } else {
                int q = existing.getQuantity();
                if (q < 0) q = 0;
                existing.setQuantity(q + 1);

                if (existing.getStatus() == null || existing.getStatus().trim().isEmpty()) {
                    existing.setStatus("ACTIVE");
                }
                existing.setClaimedAt(now);

                userVouchersFacade.edit(existing);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save UserVouchers claim", ex);
        }
    }

    
    public void redeemVoucher(Vouchers voucher) {
        lastRedeemedCode = null;
        lastRedeemedName = null;

        if (!loggedIn || currentUser == null) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Please log in",
                    "You need to log in with your customer account to redeem the voucher.");
            return;
        }

        if (voucher == null || voucher.getVoucherId() == null) {
            return;
        }

        boolean walletDeducted = false;
        boolean voucherEdited = false;

        long oldWalletPoints = 0L;
        Integer oldTotalQty = null;
        Integer oldPerLimit = null;

        try {
            if (wallet == null) {
                initWallet();
            }

            if (wallet == null) {
                addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Cannot load your point wallet.");
                return;
            }

            Vouchers managed = vouchersFacade.find(voucher.getVoucherId());
            if (managed == null) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "Cannot be changed",
                        "The voucher does not exist or has been deleted.");
                return;
            }

            if (!Boolean.TRUE.equals(managed.getIsPointRedeemable())) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Cannot be changed",
                        "This voucher cannot be redeemed using points.");
                return;
            }

            String status = managed.getStatus();
            if (status == null || !"ACTIVE".equalsIgnoreCase(status.trim())) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Cannot be changed",
                        "This voucher is no longer valid.");
                return;
            }

            Integer costObj = managed.getPointCost();
            if (costObj == null || costObj <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Cannot be changed",
                        "This voucher has not yet been configured with valid point costs.");
                return;
            }
            long cost = costObj.longValue();

            Integer totalQty = managed.getTotalQuantity();
            if (totalQty != null && totalQty <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Turn over",
                        "This voucher has been used up to the allowed number.");
                return;
            }

            Integer perLimit = managed.getPerUserLimit();
            if (perLimit != null && perLimit <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "The limit has been reached.",
                        "You have reached the redemption limit for this voucher.");
                return;
            }

            long current = safeWalletPoints(wallet);
            oldWalletPoints = current;

            if (current < cost) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "Not enough points",
                        "You need " + cost + " points, but only " + current + " point.");
                return;
            }

            // === 1) Trừ điểm trong ví ===
            wallet.setCurrentPoints(current - cost);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.edit(wallet);
            walletDeducted = true;

            // === 2) Giảm số lượng voucher (best effort theo design hiện tại) ===
            oldTotalQty = totalQty;
            oldPerLimit = perLimit;

            if (totalQty != null) {
                int newTotal = totalQty - 1;
                if (newTotal < 0) newTotal = 0;
                managed.setTotalQuantity(newTotal);
            }
            if (perLimit != null) {
                int newPer = perLimit - 1;
                if (newPer < 0) newPer = 0;
                managed.setPerUserLimit(newPer);
            }

            managed.setUpdatedAt(new Date());
            vouchersFacade.edit(managed);
            voucherEdited = true;

            // === 3) Lưu lịch sử đổi voucher (dùng để tính điểm đã tiêu) ===
            saveUserVoucherClaim(currentUser, managed);

            lastRedeemedCode = managed.getCode();
            lastRedeemedName = managed.getName();

            redeemedVoucherCount++;

            addMessage(FacesMessage.SEVERITY_INFO,
                    "Voucher redemption successful.",
                    "You have redeemed the voucher " + managed.getCode()
                            + " with " + cost + " points. Remaining points: " + wallet.getCurrentPoints());

            loadRedeemableOffers();

        } catch (Exception ex) {
            // rollback best-effort để tránh trạng thái "trừ điểm nhưng không lưu lịch sử"
            try {
                if (walletDeducted && wallet != null) {
                    wallet.setCurrentPoints(oldWalletPoints);
                    wallet.setUpdatedAt(new Date());
                    pointWalletsFacade.edit(wallet);
                }
            } catch (Exception ignore) {
            }

            try {
                if (voucherEdited) {
                    Vouchers rollback = vouchersFacade.find(voucher.getVoucherId());
                    if (rollback != null) {
                        rollback.setTotalQuantity(oldTotalQty);
                        rollback.setPerUserLimit(oldPerLimit);
                        rollback.setUpdatedAt(new Date());
                        vouchersFacade.edit(rollback);
                    }
                }
            } catch (Exception ignore) {
            }

            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "An error occurred while redeeming the voucher. Please try again later.");
        }
    }

    // =====================================================
    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // =============== GET / SET ===============
    public PointWallets getWallet() {
        return wallet;
    }

    public List<Vouchers> getRedeemableOffers() {
        return redeemableOffers;
    }

    public Long getAmountPerPoint() {
        return amountPerPoint;
    }

    public Integer getPointsPerAmount() {
        return pointsPerAmount;
    }

    public String getLastRedeemedCode() {
        return lastRedeemedCode;
    }

    public String getLastRedeemedName() {
        return lastRedeemedName;
    }

    public int getRedeemedVoucherCount() {
        return redeemedVoucherCount;
    }

    public Users getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }
}
