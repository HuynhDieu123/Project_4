package com.mypack.admin;

import com.mypack.entity.Vouchers;
import com.mypack.sessionbean.VouchersFacadeLocal;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Named("adminVoucherBean")
@ViewScoped
public class AdminVoucherBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private VouchersFacadeLocal vouchersFacade;

    // LIST + FILTER
    private List<Vouchers> allVouchers;      // full list from DB
    private List<Vouchers> voucherList;      // filtered list

    private String keyword;                  // search by code or name
    private String statusFilter = "ALL";     // ACTIVE/INACTIVE/EXPIRED/ALL
    private String scopeFilter = "ALL";      // GLOBAL/RESTAURANT/CUSTOMER/ALL

    // EDIT / CREATE
    private Vouchers selectedVoucher;
    private boolean createMode = false;      // true = create new, false = edit

    @PostConstruct
    public void init() {
        reloadAll();
    }

    private void reloadAll() {
        allVouchers = vouchersFacade.findAll();
        if (allVouchers == null) {
            allVouchers = new ArrayList<>();
        }
        applyFilterInternal();
    }

    private void applyFilterInternal() {
        voucherList = new ArrayList<>();
        String kw = (keyword != null) ? keyword.trim().toLowerCase() : null;

        for (Vouchers v : allVouchers) {
            if (v == null) continue;

            boolean match = true;

            // keyword theo code / name
            if (kw != null && !kw.isEmpty()) {
                String code = v.getCode() != null ? v.getCode().toLowerCase() : "";
                String name = v.getName() != null ? v.getName().toLowerCase() : "";
                if (!code.contains(kw) && !name.contains(kw)) {
                    match = false;
                }
            }

            // filter theo status
            if (match && statusFilter != null && !"ALL".equals(statusFilter)) {
                String st = v.getStatus() != null ? v.getStatus() : "";
                if (!statusFilter.equalsIgnoreCase(st)) {
                    match = false;
                }
            }

            // filter theo scope
            if (match && scopeFilter != null && !"ALL".equals(scopeFilter)) {
                String sc = v.getScope() != null ? v.getScope() : "";
                if (!scopeFilter.equalsIgnoreCase(sc)) {
                    match = false;
                }
            }

            if (match) {
                voucherList.add(v);
            }
        }
    }

    // ========== ACTIONS ==========

    public String applyFilter() {
        applyFilterInternal();
        return null;
    }

    public String clearFilter() {
        keyword = null;
        statusFilter = "ALL";
        scopeFilter = "ALL";
        applyFilterInternal();
        return null;
    }

    /** Open create form */
    public String prepareCreate() {
        selectedVoucher = new Vouchers();
        createMode = true;

        // default values
        selectedVoucher.setStatus("ACTIVE");
        selectedVoucher.setScope("CUSTOMER");        // or GLOBAL if you prefer
        selectedVoucher.setDiscountType("PERCENT");  // PERCENT / AMOUNT
        selectedVoucher.setIsPointRedeemable(false);
        selectedVoucher.setCreatedAt(new Date());
        selectedVoucher.setUpdatedAt(new Date());

        return null;
    }

    /** Open edit form (reload from DB) */
    public String prepareEdit(Vouchers v) {
        if (v != null && v.getVoucherId() != null) {
            selectedVoucher = vouchersFacade.find(v.getVoucherId());
            createMode = false;
        }
        return null;
    }

    public String cancelEdit() {
        selectedVoucher = null;
        createMode = false;
        return null;
    }

    /** Save: if createMode=true then create, otherwise edit */
    public String saveVoucher() {
        if (selectedVoucher == null) {
            return null;
        }
        try {
            Date now = new Date();
            if (selectedVoucher.getCreatedAt() == null) {
                selectedVoucher.setCreatedAt(now);
            }
            selectedVoucher.setUpdatedAt(now);

            if (createMode) {
                vouchersFacade.create(selectedVoucher);
                addMessage(FacesMessage.SEVERITY_INFO,
                        "Voucher created successfully",
                        "Voucher " + selectedVoucher.getCode() + " has been added.");
            } else {
                vouchersFacade.edit(selectedVoucher);
                addMessage(FacesMessage.SEVERITY_INFO,
                        "Voucher updated successfully",
                        "Voucher " + selectedVoucher.getCode() + " has been updated.");
            }

            selectedVoucher = null;
            createMode = false;
            reloadAll();

        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error saving voucher",
                    ex.getMessage());
        }
        return null;
    }

    /** Delete voucher */
    public String deleteVoucher(Vouchers v) {
        if (v == null || v.getVoucherId() == null) {
            return null;
        }
        try {
            Vouchers managed = vouchersFacade.find(v.getVoucherId());
            if (managed != null) {
                vouchersFacade.remove(managed);
                addMessage(FacesMessage.SEVERITY_INFO,
                        "Voucher deleted successfully",
                        "Voucher " + managed.getCode() + " has been deleted.");
                reloadAll();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            addMessage(FacesMessage.SEVERITY_ERROR,
                    "Error deleting voucher",
                    ex.getMessage());
        }
        return null;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance()
                .addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // ========== OPTIONS FOR SELECT (if needed) ==========

    public List<String> getScopeOptions() {
        return Arrays.asList("GLOBAL", "RESTAURANT", "CUSTOMER");
    }

    public List<String> getDiscountTypeOptions() {
        return Arrays.asList("PERCENT", "AMOUNT");
    }

    public List<String> getStatusOptions() {
        return Arrays.asList("ACTIVE", "INACTIVE", "EXPIRED");
    }

    // ========== GET/SET ==========

    public List<Vouchers> getVoucherList() {
        return voucherList;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatusFilter() {
        return statusFilter;
    }

    public void setStatusFilter(String statusFilter) {
        this.statusFilter = statusFilter;
    }

    public String getScopeFilter() {
        return scopeFilter;
    }

    public void setScopeFilter(String scopeFilter) {
        this.scopeFilter = scopeFilter;
    }

    public Vouchers getSelectedVoucher() {
        return selectedVoucher;
    }

    public void setSelectedVoucher(Vouchers selectedVoucher) {
        this.selectedVoucher = selectedVoucher;
    }

    public boolean isCreateMode() {
        return createMode;
    }

    public void setCreateMode(boolean createMode) {
        this.createMode = createMode;
    }
}
