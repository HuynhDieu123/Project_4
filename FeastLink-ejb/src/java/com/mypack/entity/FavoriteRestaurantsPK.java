/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author Laptop
 */
@Embeddable
public class FavoriteRestaurantsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "CustomerId")
    private long customerId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "RestaurantId")
    private long restaurantId;

    public FavoriteRestaurantsPK() {
    }

    public FavoriteRestaurantsPK(long customerId, long restaurantId) {
        this.customerId = customerId;
        this.restaurantId = restaurantId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(long customerId) {
        this.customerId = customerId;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) customerId;
        hash += (int) restaurantId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FavoriteRestaurantsPK)) {
            return false;
        }
        FavoriteRestaurantsPK other = (FavoriteRestaurantsPK) object;
        if (this.customerId != other.customerId) {
            return false;
        }
        if (this.restaurantId != other.restaurantId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.FavoriteRestaurantsPK[ customerId=" + customerId + ", restaurantId=" + restaurantId + " ]";
    }
    
}
