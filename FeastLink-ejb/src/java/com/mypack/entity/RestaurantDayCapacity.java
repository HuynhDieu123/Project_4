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
@Table(name = "RestaurantDayCapacity")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RestaurantDayCapacity.findAll", query = "SELECT r FROM RestaurantDayCapacity r"),
    @NamedQuery(name = "RestaurantDayCapacity.findByDayCapacityId", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.dayCapacityId = :dayCapacityId"),
    @NamedQuery(name = "RestaurantDayCapacity.findByEventDate", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.eventDate = :eventDate"),
    @NamedQuery(name = "RestaurantDayCapacity.findBySlotCode", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.slotCode = :slotCode"),
    @NamedQuery(name = "RestaurantDayCapacity.findByMaxGuests", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.maxGuests = :maxGuests"),
    @NamedQuery(name = "RestaurantDayCapacity.findByMaxBookings", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.maxBookings = :maxBookings"),
    @NamedQuery(name = "RestaurantDayCapacity.findByCurrentGuestCount", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.currentGuestCount = :currentGuestCount"),
    @NamedQuery(name = "RestaurantDayCapacity.findByCurrentBookingCount", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.currentBookingCount = :currentBookingCount"),
    @NamedQuery(name = "RestaurantDayCapacity.findByIsFull", query = "SELECT r FROM RestaurantDayCapacity r WHERE r.isFull = :isFull")})
public class RestaurantDayCapacity implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "DayCapacityId")
    private Long dayCapacityId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "EventDate")
    @Temporal(TemporalType.DATE)
    private Date eventDate;
    @Size(max = 50)
    @Column(name = "SlotCode")
    private String slotCode;
    @Column(name = "MaxGuests")
    private Integer maxGuests;
    @Column(name = "MaxBookings")
    private Integer maxBookings;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CurrentGuestCount")
    private int currentGuestCount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CurrentBookingCount")
    private int currentBookingCount;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsFull")
    private boolean isFull;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;

    public RestaurantDayCapacity() {
    }

    public RestaurantDayCapacity(Long dayCapacityId) {
        this.dayCapacityId = dayCapacityId;
    }

    public RestaurantDayCapacity(Long dayCapacityId, Date eventDate, int currentGuestCount, int currentBookingCount, boolean isFull) {
        this.dayCapacityId = dayCapacityId;
        this.eventDate = eventDate;
        this.currentGuestCount = currentGuestCount;
        this.currentBookingCount = currentBookingCount;
        this.isFull = isFull;
    }

    public Long getDayCapacityId() {
        return dayCapacityId;
    }

    public void setDayCapacityId(Long dayCapacityId) {
        this.dayCapacityId = dayCapacityId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getSlotCode() {
        return slotCode;
    }

    public void setSlotCode(String slotCode) {
        this.slotCode = slotCode;
    }

    public Integer getMaxGuests() {
        return maxGuests;
    }

    public void setMaxGuests(Integer maxGuests) {
        this.maxGuests = maxGuests;
    }

    public Integer getMaxBookings() {
        return maxBookings;
    }

    public void setMaxBookings(Integer maxBookings) {
        this.maxBookings = maxBookings;
    }

    public int getCurrentGuestCount() {
        return currentGuestCount;
    }

    public void setCurrentGuestCount(int currentGuestCount) {
        this.currentGuestCount = currentGuestCount;
    }

    public int getCurrentBookingCount() {
        return currentBookingCount;
    }

    public void setCurrentBookingCount(int currentBookingCount) {
        this.currentBookingCount = currentBookingCount;
    }

    public boolean getIsFull() {
        return isFull;
    }

    public void setIsFull(boolean isFull) {
        this.isFull = isFull;
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
        hash += (dayCapacityId != null ? dayCapacityId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RestaurantDayCapacity)) {
            return false;
        }
        RestaurantDayCapacity other = (RestaurantDayCapacity) object;
        if ((this.dayCapacityId == null && other.dayCapacityId != null) || (this.dayCapacityId != null && !this.dayCapacityId.equals(other.dayCapacityId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.RestaurantDayCapacity[ dayCapacityId=" + dayCapacityId + " ]";
    }
    
}
