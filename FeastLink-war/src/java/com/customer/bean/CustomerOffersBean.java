package com.customer.bean;

import com.mypack.entity.PointSettings;
import com.mypack.entity.PointWallets;
import com.mypack.entity.Vouchers;
import com.mypack.entity.Bookings;
import com.mypack.entity.Users;

import com.mypack.sessionbean.PointSettingsFacadeLocal;
import com.mypack.sessionbean.PointWalletsFacadeLocal;
import com.mypack.sessionbean.VouchersFacadeLocal;
import com.mypack.sessionbean.BookingsFacadeLocal;

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
     * ✅ TÍNH LẠI ĐIỂM TỪ ĐẦU DỰA VÀO TẤT CẢ BOOKING PAID CỦA CUSTOMER
     *    (KHÔNG DÙNG lastSync, nên chỉ cần sửa DB + F5 là điểm cập nhật).
     */
    private void syncPointsFromPaidBookings() {
        if (!loggedIn || currentUser == null) {
            return;
        }

        if (wallet == null) {
            initWallet();
        }

        // Nếu chưa cấu hình PointSettings hợp lệ thì không cộng
        if (amountPerPoint == null || amountPerPoint <= 0L
                || pointsPerAmount == null || pointsPerAmount <= 0) {
            return;
        }

        long totalPoints = 0L;

        List<Bookings> allBookings;
        try {
            allBookings = bookingsFacade.findAll();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (allBookings == null || allBookings.isEmpty()) {
            // không có booking nào, reset về 0
            wallet.setCurrentPoints(0L);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.edit(wallet);
            return;
        }

        for (Bookings b : allBookings) {
            if (b == null) continue;

            // chỉ lấy booking của currentUser
            if (b.getCustomerId() == null
                    || b.getCustomerId().getUserId() == null
                    || !b.getCustomerId().getUserId().equals(currentUser.getUserId())) {
                continue;
            }

            // chỉ cộng điểm khi PaymentStatus = 'PAID'
            String payStatus = b.getPaymentStatus();
            if (payStatus == null || !"PAID".equalsIgnoreCase(payStatus.trim())) {
                continue;
            }

            BigDecimal totalAmount = b.getTotalAmount();
            if (totalAmount == null) {
                continue;
            }

            long total = totalAmount.longValue();

            // tổng tiền phải >= amountPerPoint mới cộng
            if (total < amountPerPoint) {
                continue;
            }

            long multiplier = total / amountPerPoint;
            if (multiplier <= 0L) {
                continue;
            }

            long earned = multiplier * pointsPerAmount;
            totalPoints += earned;
        }

        // Cập nhật ví: set thẳng tổng điểm mới tính được
        long oldPoints = (wallet.getCurrentPoints() != 0L) ? wallet.getCurrentPoints() : 0L;
        wallet.setCurrentPoints(totalPoints);
        wallet.setUpdatedAt(new Date());
        pointWalletsFacade.edit(wallet);

        // Nếu muốn báo message khi điểm thay đổi:
        if (totalPoints != oldPoints) {
            addMessage(FacesMessage.SEVERITY_INFO,
                    "Điểm đã được cập nhật",
                    "Số điểm hiện tại của bạn: " + totalPoints + " pts.");
        }
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

    public void redeemVoucher(Vouchers voucher) {
        lastRedeemedCode = null;
        lastRedeemedName = null;

        if (!loggedIn || currentUser == null) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Vui lòng đăng nhập",
                    "Bạn cần đăng nhập bằng tài khoản khách hàng để đổi voucher.");
            return;
        }

        if (voucher == null || voucher.getVoucherId() == null) {
            return;
        }

        try {
            if (wallet == null) {
                initWallet();
            }

            Vouchers managed = vouchersFacade.find(voucher.getVoucherId());
            if (managed == null) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "Không thể đổi",
                        "Voucher không tồn tại hoặc đã bị xoá.");
                return;
            }

            if (!Boolean.TRUE.equals(managed.getIsPointRedeemable())) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Không thể đổi",
                        "Voucher này không hỗ trợ đổi bằng điểm.");
                return;
            }

            String status = managed.getStatus();
            if (status == null || !"ACTIVE".equalsIgnoreCase(status.trim())) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Không thể đổi",
                        "Voucher này hiện không còn hiệu lực.");
                return;
            }

            Integer costObj = managed.getPointCost();
            if (costObj == null || costObj <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Không thể đổi",
                        "Voucher này chưa cấu hình chi phí điểm hợp lệ.");
                return;
            }
            long cost = costObj.longValue();

            Integer totalQty = managed.getTotalQuantity();
            if (totalQty != null && totalQty <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Hết lượt",
                        "Voucher này đã được sử dụng hết số lượng cho phép.");
                return;
            }

            Integer perLimit = managed.getPerUserLimit();
            if (perLimit != null && perLimit <= 0) {
                addMessage(FacesMessage.SEVERITY_WARN,
                        "Đã đạt giới hạn",
                        "Bạn đã đạt giới hạn đổi voucher này.");
                return;
            }

            long current = wallet.getCurrentPoints();

            if (current < cost) {
                addMessage(FacesMessage.SEVERITY_ERROR,
                        "Không đủ điểm",
                        "Bạn cần " + cost + " điểm nhưng chỉ có " + current + " điểm.");
                return;
            }

            // Trừ điểm
            wallet.setCurrentPoints(current - cost);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.edit(wallet);

            // Giảm số lượng voucher
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

            lastRedeemedCode = managed.getCode();
            lastRedeemedName = managed.getName();

            redeemedVoucherCount++;

            addMessage(FacesMessage.SEVERITY_INFO,
                    "Đổi voucher thành công",
                    "Bạn đã đổi voucher " + managed.getCode()
                            + " với " + cost + " điểm. Điểm còn lại: " + wallet.getCurrentPoints());

            loadRedeemableOffers();

        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Lỗi",
                    "Có lỗi xảy ra khi đổi voucher. Vui lòng thử lại sau.");
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
