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
@Table(name = "RestaurantManagers")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RestaurantManagers.findAll", query = "SELECT r FROM RestaurantManagers r"),
    @NamedQuery(name = "RestaurantManagers.findByManagerId", query = "SELECT r FROM RestaurantManagers r WHERE r.managerId = :managerId"),
    @NamedQuery(name = "RestaurantManagers.findByIsPrimary", query = "SELECT r FROM RestaurantManagers r WHERE r.isPrimary = :isPrimary"),
    @NamedQuery(name = "RestaurantManagers.findByStatus", query = "SELECT r FROM RestaurantManagers r WHERE r.status = :status"),
    @NamedQuery(name = "RestaurantManagers.findByCreatedAt", query = "SELECT r FROM RestaurantManagers r WHERE r.createdAt = :createdAt")})
public class RestaurantManagers implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ManagerId")
    private Long managerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsPrimary")
    private boolean isPrimary;
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
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @JoinColumn(name = "UserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users userId;

    public RestaurantManagers() {
    }

    public RestaurantManagers(Long managerId) {
        this.managerId = managerId;
    }

    public RestaurantManagers(Long managerId, boolean isPrimary, String status, Date createdAt) {
        this.managerId = managerId;
        this.isPrimary = isPrimary;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
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

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Users getUserId() {
        return userId;
    }

    public void setUserId(Users userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (managerId != null ? managerId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RestaurantManagers)) {
            return false;
        }
        RestaurantManagers other = (RestaurantManagers) object;
        if ((this.managerId == null && other.managerId != null) || (this.managerId != null && !this.managerId.equals(other.managerId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.RestaurantManagers[ managerId=" + managerId + " ]";
    }
    
}
