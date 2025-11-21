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
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "RestaurantCapacitySettings")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RestaurantCapacitySettings.findAll", query = "SELECT r FROM RestaurantCapacitySettings r"),
    @NamedQuery(name = "RestaurantCapacitySettings.findByCapacityId", query = "SELECT r FROM RestaurantCapacitySettings r WHERE r.capacityId = :capacityId"),
    @NamedQuery(name = "RestaurantCapacitySettings.findByMaxGuestsPerSlot", query = "SELECT r FROM RestaurantCapacitySettings r WHERE r.maxGuestsPerSlot = :maxGuestsPerSlot"),
    @NamedQuery(name = "RestaurantCapacitySettings.findByMaxBookingsPerDay", query = "SELECT r FROM RestaurantCapacitySettings r WHERE r.maxBookingsPerDay = :maxBookingsPerDay"),
    @NamedQuery(name = "RestaurantCapacitySettings.findByDefaultSlotDurationMin", query = "SELECT r FROM RestaurantCapacitySettings r WHERE r.defaultSlotDurationMin = :defaultSlotDurationMin"),
    @NamedQuery(name = "RestaurantCapacitySettings.findByCreatedAt", query = "SELECT r FROM RestaurantCapacitySettings r WHERE r.createdAt = :createdAt")})
public class RestaurantCapacitySettings implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CapacityId")
    private Long capacityId;
    @Column(name = "MaxGuestsPerSlot")
    private Integer maxGuestsPerSlot;
    @Column(name = "MaxBookingsPerDay")
    private Integer maxBookingsPerDay;
    @Column(name = "DefaultSlotDurationMin")
    private Integer defaultSlotDurationMin;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;

    public RestaurantCapacitySettings() {
    }

    public RestaurantCapacitySettings(Long capacityId) {
        this.capacityId = capacityId;
    }

    public RestaurantCapacitySettings(Long capacityId, Date createdAt) {
        this.capacityId = capacityId;
        this.createdAt = createdAt;
    }

    public Long getCapacityId() {
        return capacityId;
    }

    public void setCapacityId(Long capacityId) {
        this.capacityId = capacityId;
    }

    public Integer getMaxGuestsPerSlot() {
        return maxGuestsPerSlot;
    }

    public void setMaxGuestsPerSlot(Integer maxGuestsPerSlot) {
        this.maxGuestsPerSlot = maxGuestsPerSlot;
    }

    public Integer getMaxBookingsPerDay() {
        return maxBookingsPerDay;
    }

    public void setMaxBookingsPerDay(Integer maxBookingsPerDay) {
        this.maxBookingsPerDay = maxBookingsPerDay;
    }

    public Integer getDefaultSlotDurationMin() {
        return defaultSlotDurationMin;
    }

    public void setDefaultSlotDurationMin(Integer defaultSlotDurationMin) {
        this.defaultSlotDurationMin = defaultSlotDurationMin;
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

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (capacityId != null ? capacityId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RestaurantCapacitySettings)) {
            return false;
        }
        RestaurantCapacitySettings other = (RestaurantCapacitySettings) object;
        if ((this.capacityId == null && other.capacityId != null) || (this.capacityId != null && !this.capacityId.equals(other.capacityId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.RestaurantCapacitySettings[ capacityId=" + capacityId + " ]";
    }
    
}
