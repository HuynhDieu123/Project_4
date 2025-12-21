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
@Table(name = "BankTransfers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BankTransfers.findAll", query = "SELECT b FROM BankTransfers b"),
    @NamedQuery(name = "BankTransfers.findByTransferId", query = "SELECT b FROM BankTransfers b WHERE b.transferId = :transferId"),
    @NamedQuery(name = "BankTransfers.findByTransferCode", query = "SELECT b FROM BankTransfers b WHERE b.transferCode = :transferCode"),
    @NamedQuery(name = "BankTransfers.findByAmount", query = "SELECT b FROM BankTransfers b WHERE b.amount = :amount"),
    @NamedQuery(name = "BankTransfers.findByFeeAmount", query = "SELECT b FROM BankTransfers b WHERE b.feeAmount = :feeAmount"),
    @NamedQuery(name = "BankTransfers.findByMessage", query = "SELECT b FROM BankTransfers b WHERE b.message = :message"),
    @NamedQuery(name = "BankTransfers.findByStatus", query = "SELECT b FROM BankTransfers b WHERE b.status = :status"),
    @NamedQuery(name = "BankTransfers.findByFailReason", query = "SELECT b FROM BankTransfers b WHERE b.failReason = :failReason"),
    @NamedQuery(name = "BankTransfers.findByCreatedAt", query = "SELECT b FROM BankTransfers b WHERE b.createdAt = :createdAt"),
    @NamedQuery(name = "BankTransfers.findByConfirmedAt", query = "SELECT b FROM BankTransfers b WHERE b.confirmedAt = :confirmedAt"),
    @NamedQuery(name = "BankTransfers.findByCompletedAt", query = "SELECT b FROM BankTransfers b WHERE b.completedAt = :completedAt")})
public class BankTransfers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "TransferId")
    private Long transferId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 40)
    @Column(name = "TransferCode")
    private String transferCode;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "Amount")
    private BigDecimal amount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FeeAmount")
    private BigDecimal feeAmount;
    @Size(max = 140)
    @Column(name = "Message")
    private String message;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 255)
    @Column(name = "FailReason")
    private String failReason;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "ConfirmedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date confirmedAt;
    @Column(name = "CompletedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;
    @OneToMany(mappedBy = "transferId")
    private Collection<WalletTransactions> walletTransactionsCollection;
    @JoinColumn(name = "ToAccountNumber", referencedColumnName = "AccountNumber")
    @ManyToOne(optional = false)
    private VirtualAccounts toAccountNumber;

    public BankTransfers() {
    }

    public BankTransfers(Long transferId) {
        this.transferId = transferId;
    }

    public BankTransfers(Long transferId, String transferCode, BigDecimal amount, BigDecimal feeAmount, String status, Date createdAt) {
        this.transferId = transferId;
        this.transferCode = transferCode;
        this.amount = amount;
        this.feeAmount = feeAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getTransferCode() {
        return transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Date confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Date completedAt) {
        this.completedAt = completedAt;
    }

    @XmlTransient
    public Collection<WalletTransactions> getWalletTransactionsCollection() {
        return walletTransactionsCollection;
    }

    public void setWalletTransactionsCollection(Collection<WalletTransactions> walletTransactionsCollection) {
        this.walletTransactionsCollection = walletTransactionsCollection;
    }

    public VirtualAccounts getToAccountNumber() {
        return toAccountNumber;
    }

    public void setToAccountNumber(VirtualAccounts toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (transferId != null ? transferId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BankTransfers)) {
            return false;
        }
        BankTransfers other = (BankTransfers) object;
        if ((this.transferId == null && other.transferId != null) || (this.transferId != null && !this.transferId.equals(other.transferId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BankTransfers[ transferId=" + transferId + " ]";
    }
    
}
