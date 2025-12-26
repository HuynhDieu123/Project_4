/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Vouchers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Vouchers.findAll", query = "SELECT v FROM Vouchers v"),
    @NamedQuery(name = "Vouchers.findByVoucherId", query = "SELECT v FROM Vouchers v WHERE v.voucherId = :voucherId"),
    @NamedQuery(name = "Vouchers.findByCode", query = "SELECT v FROM Vouchers v WHERE v.code = :code"),
    @NamedQuery(name = "Vouchers.findByName", query = "SELECT v FROM Vouchers v WHERE v.name = :name"),
    @NamedQuery(name = "Vouchers.findByScope", query = "SELECT v FROM Vouchers v WHERE v.scope = :scope"),
    @NamedQuery(name = "Vouchers.findByDiscountType", query = "SELECT v FROM Vouchers v WHERE v.discountType = :discountType"),
    @NamedQuery(name = "Vouchers.findByDiscountValue", query = "SELECT v FROM Vouchers v WHERE v.discountValue = :discountValue"),
    @NamedQuery(name = "Vouchers.findByMaxDiscount", query = "SELECT v FROM Vouchers v WHERE v.maxDiscount = :maxDiscount"),
    @NamedQuery(name = "Vouchers.findByMinOrderAmount", query = "SELECT v FROM Vouchers v WHERE v.minOrderAmount = :minOrderAmount"),
    @NamedQuery(name = "Vouchers.findByTotalQuantity", query = "SELECT v FROM Vouchers v WHERE v.totalQuantity = :totalQuantity"),
    @NamedQuery(name = "Vouchers.findByPerUserLimit", query = "SELECT v FROM Vouchers v WHERE v.perUserLimit = :perUserLimit"),
    @NamedQuery(name = "Vouchers.findByIsPointRedeemable", query = "SELECT v FROM Vouchers v WHERE v.isPointRedeemable = :isPointRedeemable"),
    @NamedQuery(name = "Vouchers.findByPointCost", query = "SELECT v FROM Vouchers v WHERE v.pointCost = :pointCost"),
    @NamedQuery(name = "Vouchers.findByStartAt", query = "SELECT v FROM Vouchers v WHERE v.startAt = :startAt"),
    @NamedQuery(name = "Vouchers.findByEndAt", query = "SELECT v FROM Vouchers v WHERE v.endAt = :endAt"),
    @NamedQuery(name = "Vouchers.findByStatus", query = "SELECT v FROM Vouchers v WHERE v.status = :status"),
    @NamedQuery(name = "Vouchers.findByCreatedAt", query = "SELECT v FROM Vouchers v WHERE v.createdAt = :createdAt"),
    @NamedQuery(name = "Vouchers.findByUpdatedAt", query = "SELECT v FROM Vouchers v WHERE v.updatedAt = :updatedAt")})
public class Vouchers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "VoucherId")
    private Long voucherId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 50)
    @Column(name = "Code")
    private String code;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "Name")
    private String name;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Scope")
    private String scope;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "DiscountType")
    private String discountType;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "DiscountValue")
    private BigDecimal discountValue;
    @Column(name = "MaxDiscount")
    private BigDecimal maxDiscount;
    @Column(name = "MinOrderAmount")
    private BigDecimal minOrderAmount;
    @Column(name = "TotalQuantity")
    private Integer totalQuantity;
    @Column(name = "PerUserLimit")
    private Integer perUserLimit;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsPointRedeemable")
    private boolean isPointRedeemable;
    @Column(name = "PointCost")
    private Integer pointCost;
    @Basic(optional = false)
    @NotNull
    @Column(name = "StartAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startAt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EndAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endAt;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @OneToMany(mappedBy = "voucherId")
    private Collection<PointTransactions> pointTransactionsCollection;
    

    public Vouchers() {
    }

    public Vouchers(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Vouchers(Long voucherId, String code, String name, String scope, String discountType, BigDecimal discountValue, boolean isPointRedeemable, Date startAt, Date endAt, String status, Date createdAt) {
        this.voucherId = voucherId;
        this.code = code;
        this.name = name;
        this.scope = scope;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.isPointRedeemable = isPointRedeemable;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public void setPerUserLimit(Integer perUserLimit) {
        this.perUserLimit = perUserLimit;
    }

    public boolean getIsPointRedeemable() {
        return isPointRedeemable;
    }

    public void setIsPointRedeemable(boolean isPointRedeemable) {
        this.isPointRedeemable = isPointRedeemable;
    }

    public Integer getPointCost() {
        return pointCost;
    }

    public void setPointCost(Integer pointCost) {
        this.pointCost = pointCost;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @XmlTransient
    public Collection<PointTransactions> getPointTransactionsCollection() {
        return pointTransactionsCollection;
    }

    public void setPointTransactionsCollection(Collection<PointTransactions> pointTransactionsCollection) {
        this.pointTransactionsCollection = pointTransactionsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (voucherId != null ? voucherId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Vouchers)) {
            return false;
        }
        Vouchers other = (Vouchers) object;
        if ((this.voucherId == null && other.voucherId != null) || (this.voucherId != null && !this.voucherId.equals(other.voucherId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Vouchers[ voucherId=" + voucherId + " ]";
    }
    
}
