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

    @EJB
    private UserVouchersFacadeLocal userVouchersFacade;

    @EJB
    private UsersFacadeLocal usersFacade;

    // ====== USER / LOGIN STATE ======
    private Users currentUser;
    private boolean loggedIn;

    private PointWallets wallet;
    private List<Vouchers> redeemableOffers;

    private Long amountPerPoint;     // USD
    private Integer pointsPerAmount; // points

    private String lastRedeemedCode;
    private String lastRedeemedName;

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

        loggedIn = (currentUser != null
                && currentUser.getRole() != null
                && "CUSTOMER".equalsIgnoreCase(currentUser.getRole().trim()));

        if (!loggedIn) {
            wallet = null;
            redeemableOffers = new ArrayList<>();
            redeemedVoucherCount = 0;
            return;
        }

        loadPointRule();
        initWallet();

        // ✅ BẬT SYNC để điểm tự lên theo booking PAID
        syncPointsFromPaidBookings();

        loadRedeemableOffers();
        redeemedVoucherCount = 0;
    }

    // =====================================================
    private void loadPointRule() {
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

    private void initWallet() {
        if (!loggedIn || currentUser == null) {
            wallet = null;
            return;
        }

        try {
            wallet = null;
            Long currentUid = currentUser.getUserId();

            List<PointWallets> all = pointWalletsFacade.findAll();
            if (all != null && !all.isEmpty() && currentUid != null) {
                for (PointWallets w : all) {
                    if (w == null) continue;
                    if (w.getUserId() == null) continue;

                    if (currentUid.equals(w.getUserId())) {
                        wallet = w;
                        break;
                    }
                }
            }

            if (wallet == null) {
                wallet = new PointWallets();
                wallet.setUserId(currentUid);
                wallet.setCurrentPoints(0L); // OK dù field là long
                wallet.setUpdatedAt(new Date());
                pointWalletsFacade.create(wallet);
            }

            // ✅ FIX: Không check null vì getter là primitive long

        } catch (Exception ex) {
            ex.printStackTrace();

            wallet = new PointWallets();
            wallet.setUserId(currentUser.getUserId());
            wallet.setCurrentPoints(0L);
            wallet.setUpdatedAt(new Date());
        }
    }

    /**
     * Earned = điểm từ booking PAID
     * Spent  = điểm đã đổi voucher (UserVouchers) nếu có
     * Net    = max(0, Earned - Spent)
     */
    private void syncPointsFromPaidBookings() {
        if (!loggedIn || currentUser == null) return;

        if (wallet == null) initWallet();
        if (wallet == null) return;

        if (amountPerPoint == null || amountPerPoint <= 0L
                || pointsPerAmount == null || pointsPerAmount <= 0) {
            return;
        }

        long earnedPoints = 0L;

        List<Bookings> allBookings;
        try {
            allBookings = bookingsFacade.findAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (allBookings != null) {
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

                long total = totalAmount.longValue(); // giữ logic của bạn

                if (total < amountPerPoint) continue;

                long multiplier = total / amountPerPoint;
                if (multiplier <= 0L) continue;

                earnedPoints += (multiplier * pointsPerAmount);
            }
        }

        long spentPoints = calcSpentPointsFromUserVouchersSafe();

        long netPoints = earnedPoints - spentPoints;
        if (netPoints < 0L) netPoints = 0L;

        long oldPoints = wallet.getCurrentPoints(); // ✅ primitive long

        wallet.setCurrentPoints(netPoints);
        wallet.setUpdatedAt(new Date());

        try {
            pointWalletsFacade.edit(wallet);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (netPoints != oldPoints) {
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Points updated.",
                    "Current points: " + netPoints + " pts.");
        }
    }

    private long calcSpentPointsFromUserVouchersSafe() {
        try {
            if (userVouchersFacade == null || currentUser == null || currentUser.getUserId() == null) return 0L;

            List<UserVouchers> all = userVouchersFacade.findAll();
            if (all == null || all.isEmpty()) return 0L;

            long spent = 0L;

            for (UserVouchers uv : all) {
                if (uv == null) continue;
                if (uv.getUserId() == null || uv.getVoucherId() == null) continue;
                if (uv.getUserId().getUserId() == null) continue;

                if (!uv.getUserId().getUserId().equals(currentUser.getUserId())) continue;

                // ✅ FIX: quantity là primitive int -> không null
                int qty = uv.getQuantity();
                if (qty <= 0) continue;

                // pointCost thường là Integer, để safe vẫn check null
                Integer costObj = uv.getVoucherId().getPointCost();
                int cost = (costObj == null) ? 0 : costObj;
                if (cost <= 0) continue;

                spent += (long) qty * (long) cost;
            }

            return spent;
        } catch (Exception ex) {
            // DB thiếu bảng UserVouchers / lỗi mapping -> bỏ qua
            return 0L;
        }
    }

    private void loadRedeemableOffers() {
        redeemableOffers = new ArrayList<>();
        List<Vouchers> all = vouchersFacade.findAll();
        if (all == null) return;

        for (Vouchers v : all) {
            if (v == null) continue;

            Boolean pointRedeemable = v.getIsPointRedeemable();
            String status = v.getStatus();
            Integer pointCost = v.getPointCost();

            Integer totalQty = v.getTotalQuantity();
            Integer perLimit = v.getPerUserLimit();

            boolean stillHasQuantity = true;
            if (totalQty != null && totalQty <= 0) stillHasQuantity = false;
            if (perLimit != null && perLimit <= 0) stillHasQuantity = false;

            if (Boolean.TRUE.equals(pointRedeemable)
                    && status != null
                    && "ACTIVE".equalsIgnoreCase(status.trim())
                    && pointCost != null
                    && stillHasQuantity) {
                redeemableOffers.add(v);
            }
        }
    }

    private void saveUserVoucherClaim(Users user, Vouchers voucher) {
        if (user == null || user.getUserId() == null || voucher == null || voucher.getVoucherId() == null) {
            return;
        }

        try {
            Users managedUser = user;
            try {
                Users tmp = usersFacade.find(user.getUserId());
                if (tmp != null) managedUser = tmp;
            } catch (Exception ignore) {}

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
                int q = existing.getQuantity(); // primitive int
                if (q < 0) q = 0;
                existing.setQuantity(q + 1);

                if (existing.getStatus() == null || existing.getStatus().trim().isEmpty()) {
                    existing.setStatus("ACTIVE");
                }
                existing.setClaimedAt(now);

                userVouchersFacade.edit(existing);
            }

        } catch (Exception ex) {
            // nếu DB thiếu bảng UserVouchers -> bỏ qua, không chặn redeem
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

        if (voucher == null || voucher.getVoucherId() == null) return;

        try {
            if (wallet == null) initWallet();

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

            long current = wallet.getCurrentPoints(); // ✅ primitive long

            if (current < cost) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "Not enough points",
                        "You need " + cost + " points, but only " + current + " point.");
                return;
            }

            wallet.setCurrentPoints(current - cost);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.edit(wallet);

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

            // ✅ Lưu claim (nếu bảng tồn tại)
            saveUserVoucherClaim(currentUser, managed);

            lastRedeemedCode = managed.getCode();
            lastRedeemedName = managed.getName();

            redeemedVoucherCount++;

            addMessage(FacesMessage.SEVERITY_INFO,
                    "Voucher redemption successful.",
                    "You have redeemed the voucher. " + managed.getCode()
                            + " with " + cost + " points. Remaining points: " + wallet.getCurrentPoints());

            loadRedeemableOffers();

        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error",
                    "An error occurred while redeeming the voucher. Please try again later.");
        }
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // ===== GETTERS =====
    public PointWallets getWallet() { return wallet; }
    public List<Vouchers> getRedeemableOffers() { return redeemableOffers; }
    public Long getAmountPerPoint() { return amountPerPoint; }
    public Integer getPointsPerAmount() { return pointsPerAmount; }
    public String getLastRedeemedCode() { return lastRedeemedCode; }
    public String getLastRedeemedName() { return lastRedeemedName; }
    public int getRedeemedVoucherCount() { return redeemedVoucherCount; }
    public Users getCurrentUser() { return currentUser; }
    public boolean isLoggedIn() { return loggedIn; }
}
