package com.mypack.vnpay;

import jakarta.ejb.Stateless;
import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TreeMap;

@Stateless
public class VnPayService {

    public String buildRedirectUrl(HttpServletRequest req,
                                  String txnRef,
                                  BigDecimal amountVnd,
                                  String orderInfo) {

        // VNPAY amount = VND * 100 (integer)
        long vnpAmount = amountVnd
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact() * 100L;

        String tmnCode = VnPayConfig.tmnCode();
        String payUrl = VnPayConfig.payUrl();
        String hashSecret = VnPayConfig.hashSecret();

        String returnUrl = VnPayConfig.fixedReturnUrlOrNull();
        if (returnUrl == null) {
            // build dynamic return URL
            String scheme = req.getScheme();
            String host = req.getServerName();
            int port = req.getServerPort();
            String contextPath = req.getContextPath();

            String base = scheme + "://" + host + ((port == 80 || port == 443) ? "" : (":" + port));
            returnUrl = base + contextPath + "/faces/vnpay_return.xhtml";
        }

        String ipAddr = getClientIp(req);

        // date format yyyyMMddHHmmss (GMT+7)
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String createDate = fmt.format(now.getTime());
        now.add(Calendar.MINUTE, 15);
        String expireDate = fmt.format(now.getTime());

        TreeMap<String, String> vnp = new TreeMap<>();
        vnp.put("vnp_Version", "2.1.0");
        vnp.put("vnp_Command", "pay");
        vnp.put("vnp_TmnCode", tmnCode);
        vnp.put("vnp_Amount", String.valueOf(vnpAmount));
        vnp.put("vnp_CurrCode", "VND");
        vnp.put("vnp_TxnRef", txnRef);
        vnp.put("vnp_OrderInfo", orderInfo);
        vnp.put("vnp_OrderType", "other");
        vnp.put("vnp_Locale", "vn");
        vnp.put("vnp_ReturnUrl", returnUrl);
        vnp.put("vnp_IpAddr", ipAddr);
        vnp.put("vnp_CreateDate", createDate);
        vnp.put("vnp_ExpireDate", expireDate);

        // hash data uses sorted params
        String hashData = VnPayUtil.buildQueryString(vnp, true);
        String secureHash = VnPayUtil.hmacSHA512(hashSecret, hashData);

        String queryUrl = VnPayUtil.buildQueryString(vnp, true);
        return payUrl + "?" + queryUrl + "&vnp_SecureHash=" + secureHash;
    }

    public boolean verifyReturn(HttpServletRequest req) {
        String receivedHash = req.getParameter("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) return false;

        TreeMap<String, String> vnp = new TreeMap<>();
        req.getParameterMap().forEach((k, arr) -> {
            if (k == null) return;
            if (!k.startsWith("vnp_")) return;
            if ("vnp_SecureHash".equals(k) || "vnp_SecureHashType".equals(k)) return;
            if (arr != null && arr.length > 0) vnp.put(k, arr[0]);
        });

        String hashData = VnPayUtil.buildQueryString(vnp, true);
        String expected = VnPayUtil.hmacSHA512(VnPayConfig.hashSecret(), hashData);

        return expected.equalsIgnoreCase(receivedHash);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
