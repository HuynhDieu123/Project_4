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
@Table(name = "PointTransactions")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PointTransactions.findAll", query = "SELECT p FROM PointTransactions p"),
    @NamedQuery(name = "PointTransactions.findByPointTxnId", query = "SELECT p FROM PointTransactions p WHERE p.pointTxnId = :pointTxnId"),
    @NamedQuery(name = "PointTransactions.findByPoints", query = "SELECT p FROM PointTransactions p WHERE p.points = :points"),
    @NamedQuery(name = "PointTransactions.findByType", query = "SELECT p FROM PointTransactions p WHERE p.type = :type"),
    @NamedQuery(name = "PointTransactions.findByDescription", query = "SELECT p FROM PointTransactions p WHERE p.description = :description"),
    @NamedQuery(name = "PointTransactions.findByCreatedAt", query = "SELECT p FROM PointTransactions p WHERE p.createdAt = :createdAt")})
public class PointTransactions implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "PointTxnId")
    private Long pointTxnId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Points")
    private long points;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "Type")
    private String type;
    @Size(max = 255)
    @Column(name = "Description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "VoucherId", referencedColumnName = "VoucherId")
    @ManyToOne
    private Vouchers voucherId;

    public PointTransactions() {
    }

    public PointTransactions(Long pointTxnId) {
        this.pointTxnId = pointTxnId;
    }

    public PointTransactions(Long pointTxnId, long points, String type, Date createdAt) {
        this.pointTxnId = pointTxnId;
        this.points = points;
        this.type = type;
        this.createdAt = createdAt;
    }

    public Long getPointTxnId() {
        return pointTxnId;
    }

    public void setPointTxnId(Long pointTxnId) {
        this.pointTxnId = pointTxnId;
    }

    public long getPoints() {
        return points;
    }

    public void setPoints(long points) {
        this.points = points;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
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
        hash += (pointTxnId != null ? pointTxnId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PointTransactions)) {
            return false;
        }
        PointTransactions other = (PointTransactions) object;
        if ((this.pointTxnId == null && other.pointTxnId != null) || (this.pointTxnId != null && !this.pointTxnId.equals(other.pointTxnId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.PointTransactions[ pointTxnId=" + pointTxnId + " ]";
    }
    
}
