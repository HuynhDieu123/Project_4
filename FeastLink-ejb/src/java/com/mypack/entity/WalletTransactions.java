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
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "WalletTransactions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "WalletTransactions.findAll", query = "SELECT w FROM WalletTransactions w"),
    @NamedQuery(name = "WalletTransactions.findByWalletTxnId", query = "SELECT w FROM WalletTransactions w WHERE w.walletTxnId = :walletTxnId"),
    @NamedQuery(name = "WalletTransactions.findByDirection", query = "SELECT w FROM WalletTransactions w WHERE w.direction = :direction"),
    @NamedQuery(name = "WalletTransactions.findByAmount", query = "SELECT w FROM WalletTransactions w WHERE w.amount = :amount"),
    @NamedQuery(name = "WalletTransactions.findByCreatedAt", query = "SELECT w FROM WalletTransactions w WHERE w.createdAt = :createdAt")})
public class WalletTransactions implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "WalletTxnId")
    private Long walletTxnId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 10)
    @Column(name = "Direction")
    private String direction;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Amount")
    private BigDecimal amount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "TransferId", referencedColumnName = "TransferId")
    @ManyToOne
    private BankTransfers transferId;
    @JoinColumn(name = "WalletId", referencedColumnName = "WalletId")
    @ManyToOne(optional = false)
    private Wallets walletId;

    public WalletTransactions() {
    }

    public WalletTransactions(Long walletTxnId) {
        this.walletTxnId = walletTxnId;
    }

    public WalletTransactions(Long walletTxnId, String direction, BigDecimal amount, Date createdAt) {
        this.walletTxnId = walletTxnId;
        this.direction = direction;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getWalletTxnId() {
        return walletTxnId;
    }

    public void setWalletTxnId(Long walletTxnId) {
        this.walletTxnId = walletTxnId;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public BankTransfers getTransferId() {
        return transferId;
    }

    public void setTransferId(BankTransfers transferId) {
        this.transferId = transferId;
    }

    public Wallets getWalletId() {
        return walletId;
    }

    public void setWalletId(Wallets walletId) {
        this.walletId = walletId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (walletTxnId != null ? walletTxnId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof WalletTransactions)) {
            return false;
        }
        WalletTransactions other = (WalletTransactions) object;
        if ((this.walletTxnId == null && other.walletTxnId != null) || (this.walletTxnId != null && !this.walletTxnId.equals(other.walletTxnId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.WalletTransactions[ walletTxnId=" + walletTxnId + " ]";
    }
    
}
