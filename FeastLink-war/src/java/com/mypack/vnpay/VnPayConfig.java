package com.mypack.vnpay;

import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

public class VnPayConfig {

    private VnPayConfig() {}

    public static String tmnCode() {
        return initParam("vnpay.tmnCode");
    }

    public static String hashSecret() {
        return initParam("vnpay.hashSecret");
    }

    public static String payUrl() {
        return initParam("vnpay.payUrl");
    }

    public static String fixedReturnUrlOrNull() {
        String v = initParam("vnpay.returnUrl");
        return (v == null || v.isBlank()) ? null : v;
    }

    private static String initParam(String name) {
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        return ec.getInitParameter(name);
    }
}
