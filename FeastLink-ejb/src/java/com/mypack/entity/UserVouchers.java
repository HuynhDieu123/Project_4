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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "UserVouchers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "UserVouchers.findAll", query = "SELECT u FROM UserVouchers u"),
    @NamedQuery(name = "UserVouchers.findByUserVoucherId", query = "SELECT u FROM UserVouchers u WHERE u.userVoucherId = :userVoucherId"),
    @NamedQuery(name = "UserVouchers.findByQuantity", query = "SELECT u FROM UserVouchers u WHERE u.quantity = :quantity"),
    @NamedQuery(name = "UserVouchers.findByUsedQuantity", query = "SELECT u FROM UserVouchers u WHERE u.usedQuantity = :usedQuantity"),
    @NamedQuery(name = "UserVouchers.findByStatus", query = "SELECT u FROM UserVouchers u WHERE u.status = :status"),
    @NamedQuery(name = "UserVouchers.findByClaimedAt", query = "SELECT u FROM UserVouchers u WHERE u.claimedAt = :claimedAt"),
    @NamedQuery(name = "UserVouchers.findByUsedAt", query = "SELECT u FROM UserVouchers u WHERE u.usedAt = :usedAt")})
public class UserVouchers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "UserVoucherId")
    private Long userVoucherId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UsedQuantity")
    private int usedQuantity;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ClaimedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date claimedAt;
    @Column(name = "UsedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date usedAt;
    @JoinColumn(name = "UsedBookingId", referencedColumnName = "BookingId")
    @ManyToOne
    private Bookings usedBookingId;
    @JoinColumn(name = "UserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users userId;
    @JoinColumn(name = "VoucherId", referencedColumnName = "VoucherId")
    @ManyToOne(optional = false)
    private Vouchers voucherId;

    public UserVouchers() {
    }

    public UserVouchers(Long userVoucherId) {
        this.userVoucherId = userVoucherId;
    }

    public UserVouchers(Long userVoucherId, int quantity, int usedQuantity, String status, Date claimedAt) {
        this.userVoucherId = userVoucherId;
        this.quantity = quantity;
        this.usedQuantity = usedQuantity;
        this.status = status;
        this.claimedAt = claimedAt;
    }

    public Long getUserVoucherId() {
        return userVoucherId;
    }

    public void setUserVoucherId(Long userVoucherId) {
        this.userVoucherId = userVoucherId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getUsedQuantity() {
        return usedQuantity;
    }

    public void setUsedQuantity(int usedQuantity) {
        this.usedQuantity = usedQuantity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getClaimedAt() {
        return claimedAt;
    }

    public void setClaimedAt(Date claimedAt) {
        this.claimedAt = claimedAt;
    }

    public Date getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Bookings getUsedBookingId() {
        return usedBookingId;
    }

    public void setUsedBookingId(Bookings usedBookingId) {
        this.usedBookingId = usedBookingId;
    }

    public Users getUserId() {
        return userId;
    }

    public void setUserId(Users userId) {
        this.userId = userId;
    }

    public Vouchers getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Vouchers voucherId) {
        this.voucherId = voucherId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userVoucherId != null ? userVoucherId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof UserVouchers)) {
            return false;
        }
        UserVouchers other = (UserVouchers) object;
        if ((this.userVoucherId == null && other.userVoucherId != null) || (this.userVoucherId != null && !this.userVoucherId.equals(other.userVoucherId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.UserVouchers[ userVoucherId=" + userVoucherId + " ]";
    }
    
}
