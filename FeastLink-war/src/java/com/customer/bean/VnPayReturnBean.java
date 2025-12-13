package com.customer.bean;

import com.mypack.entity.Payments;
import com.mypack.entity.Bookings;
import com.mypack.sessionbean.PaymentsFacadeLocal;
import com.mypack.sessionbean.BookingsFacadeLocal;
import com.mypack.vnpay.VnPayService;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import jakarta.faces.context.FacesContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

@Named("vnPayReturnBean")
@RequestScoped
public class VnPayReturnBean {

    @EJB
    private PaymentsFacadeLocal paymentsFacade;

    @EJB
    private BookingsFacadeLocal bookingsFacade;

    @EJB
    private VnPayService vnPayService;

    private boolean processed;
    private boolean success;
    private String message;
    private Long bookingId;
    private String txnRef;

    public void process() {
        if (processed) return;
        processed = true;

        FacesContext fc = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) fc.getExternalContext().getRequest();

        txnRef = req.getParameter("vnp_TxnRef");
        if (txnRef == null || txnRef.isBlank()) {
            success = false;
            message = "Missing vnp_TxnRef.";
            return;
        }

        // Verify secure hash
        boolean validSig = vnPayService.verifyReturn(req);
        if (!validSig) {
            success = false;
            message = "Invalid signature (vnp_SecureHash).";
            return;
        }

        String respCode = req.getParameter("vnp_ResponseCode");
        String txnStatus = req.getParameter("vnp_TransactionStatus");

        boolean ok = "00".equals(respCode) && ("00".equals(txnStatus) || txnStatus == null);

        Payments p = paymentsFacade.findByTransactionCode(txnRef);
        if (p == null) {
            success = false;
            message = "Payment not found for txnRef=" + txnRef;
            return;
        }

        // store raw response (query string)
        p.setRawResponse(req.getQueryString());

        if (ok) {
            p.setStatus("SUCCESS");
            p.setPaidAt(new Date());
            success = true;
            message = "Payment SUCCESS via VNPAY.";
        } else {
            p.setStatus("FAILED");
            success = false;
            message = "Payment FAILED via VNPAY. ResponseCode=" + respCode + ", TxnStatus=" + txnStatus;
        }

        paymentsFacade.edit(p);

        // Update booking status (optional nhưng nên có)
        Bookings b = p.getBookingId();
        if (b != null) {
            bookingId = b.getBookingId();
            if (ok) {
                // deposit vs full
                String type = p.getPaymentType();
                if ("DEPOSIT".equalsIgnoreCase(type)) {
                    b.setPaymentStatus("DEPOSIT_PAID");
                } else {
                    b.setPaymentStatus("PAID");
                }
                bookingsFacade.edit(b);
            }
        }
    }

    // ===== GETTERS for xhtml =====
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Long getBookingId() { return bookingId; }
    public String getTxnRef() { return txnRef; }
}
