package com.customer.bean;

import com.mypack.entity.PointWallets;
import com.mypack.entity.Vouchers;
import com.mypack.sessionbean.PointWalletsFacadeLocal;
import com.mypack.sessionbean.VouchersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Customer Offers & Points page
 * - Hiển thị số điểm hiện tại (PointWallets)
 * - Hiển thị danh sách voucher có thể đổi bằng điểm (Vouchers)
 * - Cho phép test: nạp 100 điểm & đổi điểm lấy voucher (trừ điểm trong DB)
 */
@Named("customerOffersBean")
@ViewScoped
public class CustomerOffersBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PointWalletsFacadeLocal pointWalletsFacade;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    private PointWallets wallet;
    private List<Vouchers> redeemableOffers;

    @PostConstruct
    public void init() {
        initWallet();
        loadRedeemableOffers();
    }

    /**
     * Lấy ví điểm hiện tại.
     * Nếu DB chưa có record nào -> tự tạo 1 wallet mới với 0 điểm (để test).
     */
    private void initWallet() {
        List<PointWallets> all = pointWalletsFacade.findAll();
        if (all != null && !all.isEmpty()) {
            wallet = all.get(0);  // đơn giản: lấy ví đầu tiên
        } else {
            wallet = new PointWallets();
            wallet.setCurrentPoints(0L);
            wallet.setUpdatedAt(new Date());
            pointWalletsFacade.create(wallet);
        }
    }

    /**
     * Lấy danh sách voucher có thể đổi bằng điểm
     * Điều kiện: isPointRedeemable = true & status = 'ACTIVE'
     * (lọc trên danh sách findAll)
     */
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

            if (Boolean.TRUE.equals(pointRedeemable)
                    && status != null
                    && "ACTIVE".equalsIgnoreCase(status.trim())) {
                redeemableOffers.add(v);
            }
        }
    }

    /**
     * Nút test: +100 điểm vào ví.
     * (Dùng để bạn kiểm tra thêm/sửa DB có OK không).
     */
    public void addTestPoints() {
        if (wallet == null) {
            initWallet();
        }
        long current = wallet.getCurrentPoints();
        long added = 100L;

        wallet.setCurrentPoints(current + added);
        wallet.setUpdatedAt(new Date());
        pointWalletsFacade.edit(wallet);

        addMessage(FacesMessage.SEVERITY_INFO,
                "Points added",
                "Đã cộng " + added + " điểm vào ví. Điểm hiện tại: " + wallet.getCurrentPoints());
    }

    /**
     * Đổi voucher bằng điểm: trừ pointCost khỏi currentPoints.
     */
    public void redeemVoucher(Vouchers voucher) {
        if (voucher == null) {
            return;
        }
        if (wallet == null) {
            initWallet();
        }

        Integer pointCostObj = voucher.getPointCost();
        if (pointCostObj == null) {
            addMessage(FacesMessage.SEVERITY_WARN,
                    "Không thể đổi",
                    "Voucher này chưa cấu hình chi phí điểm.");
            return;
        }

        long cost = pointCostObj.longValue();
        long current = wallet.getCurrentPoints();

        if (current < cost) {
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Không đủ điểm",
                    "Bạn cần " + cost + " điểm nhưng chỉ có " + current + " điểm.");
            return;
        }

        wallet.setCurrentPoints(current - cost);
        wallet.setUpdatedAt(new Date());
        pointWalletsFacade.edit(wallet);

        addMessage(FacesMessage.SEVERITY_INFO,
                "Đổi voucher thành công",
                "Bạn đã đổi voucher " + voucher.getCode()
                        + " với " + cost + " điểm. Điểm còn lại: " + wallet.getCurrentPoints());
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // ========== GET/SET ==========

    public PointWallets getWallet() {
        return wallet;
    }

    public List<Vouchers> getRedeemableOffers() {
        return redeemableOffers;
    }
}
