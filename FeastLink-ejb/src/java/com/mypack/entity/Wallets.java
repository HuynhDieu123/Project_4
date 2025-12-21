/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
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
@Table(name = "Wallets")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Wallets.findAll", query = "SELECT w FROM Wallets w"),
    @NamedQuery(name = "Wallets.findByWalletId", query = "SELECT w FROM Wallets w WHERE w.walletId = :walletId"),
    @NamedQuery(name = "Wallets.findByCurrentBalance", query = "SELECT w FROM Wallets w WHERE w.currentBalance = :currentBalance"),
    @NamedQuery(name = "Wallets.findByStatus", query = "SELECT w FROM Wallets w WHERE w.status = :status"),
    @NamedQuery(name = "Wallets.findByCreatedAt", query = "SELECT w FROM Wallets w WHERE w.createdAt = :createdAt"),
    @NamedQuery(name = "Wallets.findByUpdatedAt", query = "SELECT w FROM Wallets w WHERE w.updatedAt = :updatedAt")})
public class Wallets implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "WalletId")
    private Long walletId;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "CurrentBalance")
    private BigDecimal currentBalance;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "walletId")
    private Collection<WalletTransactions> walletTransactionsCollection;

    public Wallets() {
    }

    public Wallets(Long walletId) {
        this.walletId = walletId;
    }

    public Wallets(Long walletId, BigDecimal currentBalance, String status, Date createdAt) {
        this.walletId = walletId;
        this.currentBalance = currentBalance;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
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
    public Collection<WalletTransactions> getWalletTransactionsCollection() {
        return walletTransactionsCollection;
    }

    public void setWalletTransactionsCollection(Collection<WalletTransactions> walletTransactionsCollection) {
        this.walletTransactionsCollection = walletTransactionsCollection;
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
        if (!(object instanceof Wallets)) {
            return false;
        }
        Wallets other = (Wallets) object;
        if ((this.walletId == null && other.walletId != null) || (this.walletId != null && !this.walletId.equals(other.walletId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Wallets[ walletId=" + walletId + " ]";
    }
    
}
