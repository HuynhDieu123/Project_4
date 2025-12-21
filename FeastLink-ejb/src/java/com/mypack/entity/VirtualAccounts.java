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
import jakarta.persistence.Lob;
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
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "VirtualAccounts")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "VirtualAccounts.findAll", query = "SELECT v FROM VirtualAccounts v"),
    @NamedQuery(name = "VirtualAccounts.findByAccountId", query = "SELECT v FROM VirtualAccounts v WHERE v.accountId = :accountId"),
    @NamedQuery(name = "VirtualAccounts.findByBankCode", query = "SELECT v FROM VirtualAccounts v WHERE v.bankCode = :bankCode"),
    @NamedQuery(name = "VirtualAccounts.findByAccountNumber", query = "SELECT v FROM VirtualAccounts v WHERE v.accountNumber = :accountNumber"),
    @NamedQuery(name = "VirtualAccounts.findByDisplayName", query = "SELECT v FROM VirtualAccounts v WHERE v.displayName = :displayName"),
    @NamedQuery(name = "VirtualAccounts.findByFailedCount", query = "SELECT v FROM VirtualAccounts v WHERE v.failedCount = :failedCount"),
    @NamedQuery(name = "VirtualAccounts.findByLockedUntil", query = "SELECT v FROM VirtualAccounts v WHERE v.lockedUntil = :lockedUntil"),
    @NamedQuery(name = "VirtualAccounts.findByStatus", query = "SELECT v FROM VirtualAccounts v WHERE v.status = :status"),
    @NamedQuery(name = "VirtualAccounts.findByCreatedAt", query = "SELECT v FROM VirtualAccounts v WHERE v.createdAt = :createdAt"),
    @NamedQuery(name = "VirtualAccounts.findByUpdatedAt", query = "SELECT v FROM VirtualAccounts v WHERE v.updatedAt = :updatedAt")})
public class VirtualAccounts implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "AccountId")
    private Long accountId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "BankCode")
    private String bankCode;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "AccountNumber")
    private String accountNumber;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "DisplayName")
    private String displayName;
    @Lob
    @Column(name = "PinSalt")
    private byte[] pinSalt;
    @Lob
    @Column(name = "PinHash")
    private byte[] pinHash;
    @Basic(optional = false)
    @NotNull
    @Column(name = "FailedCount")
    private int failedCount;
    @Column(name = "LockedUntil")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lockedUntil;
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "toAccountNumber")
    private Collection<BankTransfers> bankTransfersCollection;

    public VirtualAccounts() {
    }

    public VirtualAccounts(Long accountId) {
        this.accountId = accountId;
    }

    public VirtualAccounts(Long accountId, String bankCode, String accountNumber, String displayName, int failedCount, String status, Date createdAt) {
        this.accountId = accountId;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.displayName = displayName;
        this.failedCount = failedCount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public byte[] getPinSalt() {
        return pinSalt;
    }

    public void setPinSalt(byte[] pinSalt) {
        this.pinSalt = pinSalt;
    }

    public byte[] getPinHash() {
        return pinHash;
    }

    public void setPinHash(byte[] pinHash) {
        this.pinHash = pinHash;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public Date getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Date lockedUntil) {
        this.lockedUntil = lockedUntil;
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
    public Collection<BankTransfers> getBankTransfersCollection() {
        return bankTransfersCollection;
    }

    public void setBankTransfersCollection(Collection<BankTransfers> bankTransfersCollection) {
        this.bankTransfersCollection = bankTransfersCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (accountId != null ? accountId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof VirtualAccounts)) {
            return false;
        }
        VirtualAccounts other = (VirtualAccounts) object;
        if ((this.accountId == null && other.accountId != null) || (this.accountId != null && !this.accountId.equals(other.accountId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.VirtualAccounts[ accountId=" + accountId + " ]";
    }
    
}
