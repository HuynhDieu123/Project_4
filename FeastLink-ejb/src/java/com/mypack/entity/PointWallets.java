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
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "PointWallets")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PointWallets.findAll", query = "SELECT p FROM PointWallets p"),
    @NamedQuery(name = "PointWallets.findByWalletId", query = "SELECT p FROM PointWallets p WHERE p.walletId = :walletId"),
    @NamedQuery(name = "PointWallets.findByCurrentPoints", query = "SELECT p FROM PointWallets p WHERE p.currentPoints = :currentPoints"),
    @NamedQuery(name = "PointWallets.findByUpdatedAt", query = "SELECT p FROM PointWallets p WHERE p.updatedAt = :updatedAt")})
public class PointWallets implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "WalletId")
    private Long walletId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CurrentPoints")
    private long currentPoints;
    @Basic(optional = false)
    @NotNull
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public PointWallets() {
    }

    public PointWallets(Long walletId) {
        this.walletId = walletId;
    }

    public PointWallets(Long walletId, long currentPoints, Date updatedAt) {
        this.walletId = walletId;
        this.currentPoints = currentPoints;
        this.updatedAt = updatedAt;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public long getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(long currentPoints) {
        this.currentPoints = currentPoints;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (walletId != null ? walletId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PointWallets)) {
            return false;
        }
        PointWallets other = (PointWallets) object;
        if ((this.walletId == null && other.walletId != null) || (this.walletId != null && !this.walletId.equals(other.walletId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.PointWallets[ walletId=" + walletId + " ]";
    }
    
}
