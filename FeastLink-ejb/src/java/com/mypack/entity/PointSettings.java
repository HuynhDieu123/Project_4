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
 * @author tuanc
 */
@Entity
@Table(name = "PointSettings")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "PointSettings.findAll", query = "SELECT p FROM PointSettings p"),
    @NamedQuery(name = "PointSettings.findBySettingId", query = "SELECT p FROM PointSettings p WHERE p.settingId = :settingId"),
    @NamedQuery(name = "PointSettings.findByAmountPerPoint", query = "SELECT p FROM PointSettings p WHERE p.amountPerPoint = :amountPerPoint"),
    @NamedQuery(name = "PointSettings.findByCreatedAt", query = "SELECT p FROM PointSettings p WHERE p.createdAt = :createdAt"),
    @NamedQuery(name = "PointSettings.findByUpdatedAt", query = "SELECT p FROM PointSettings p WHERE p.updatedAt = :updatedAt")})
public class PointSettings implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "SettingId")
    private Integer settingId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "AmountPerPoint")
    private long amountPerPoint;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @Column(name = "PointsPerAmount")
private Integer pointsPerAmount;


    public PointSettings() {
    }

    public PointSettings(Integer settingId) {
        this.settingId = settingId;
    }

    public PointSettings(Integer settingId, long amountPerPoint, Date createdAt) {
        this.settingId = settingId;
        this.amountPerPoint = amountPerPoint;
        this.createdAt = createdAt;
    }

    public Integer getSettingId() {
        return settingId;
    }

    public void setSettingId(Integer settingId) {
        this.settingId = settingId;
    }

    public long getAmountPerPoint() {
        return amountPerPoint;
    }

    public void setAmountPerPoint(long amountPerPoint) {
        this.amountPerPoint = amountPerPoint;
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
    public Integer getPointsPerAmount() {
    return pointsPerAmount;
}

public void setPointsPerAmount(Integer pointsPerAmount) {
    this.pointsPerAmount = pointsPerAmount;
}


    @Override
    public int hashCode() {
        int hash = 0;
        hash += (settingId != null ? settingId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof PointSettings)) {
            return false;
        }
        PointSettings other = (PointSettings) object;
        if ((this.settingId == null && other.settingId != null) || (this.settingId != null && !this.settingId.equals(other.settingId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.PointSettings[ settingId=" + settingId + " ]";
    }
    
}
