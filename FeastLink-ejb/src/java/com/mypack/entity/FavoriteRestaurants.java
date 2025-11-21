/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
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
@Table(name = "FavoriteRestaurants")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "FavoriteRestaurants.findAll", query = "SELECT f FROM FavoriteRestaurants f"),
    @NamedQuery(name = "FavoriteRestaurants.findByCustomerId", query = "SELECT f FROM FavoriteRestaurants f WHERE f.favoriteRestaurantsPK.customerId = :customerId"),
    @NamedQuery(name = "FavoriteRestaurants.findByRestaurantId", query = "SELECT f FROM FavoriteRestaurants f WHERE f.favoriteRestaurantsPK.restaurantId = :restaurantId"),
    @NamedQuery(name = "FavoriteRestaurants.findByCreatedAt", query = "SELECT f FROM FavoriteRestaurants f WHERE f.createdAt = :createdAt")})
public class FavoriteRestaurants implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected FavoriteRestaurantsPK favoriteRestaurantsPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Restaurants restaurants;
    @JoinColumn(name = "CustomerId", referencedColumnName = "UserId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Users users;

    public FavoriteRestaurants() {
    }

    public FavoriteRestaurants(FavoriteRestaurantsPK favoriteRestaurantsPK) {
        this.favoriteRestaurantsPK = favoriteRestaurantsPK;
    }

    public FavoriteRestaurants(FavoriteRestaurantsPK favoriteRestaurantsPK, Date createdAt) {
        this.favoriteRestaurantsPK = favoriteRestaurantsPK;
        this.createdAt = createdAt;
    }

    public FavoriteRestaurants(long customerId, long restaurantId) {
        this.favoriteRestaurantsPK = new FavoriteRestaurantsPK(customerId, restaurantId);
    }

    public FavoriteRestaurantsPK getFavoriteRestaurantsPK() {
        return favoriteRestaurantsPK;
    }

    public void setFavoriteRestaurantsPK(FavoriteRestaurantsPK favoriteRestaurantsPK) {
        this.favoriteRestaurantsPK = favoriteRestaurantsPK;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Restaurants getRestaurants() {
        return restaurants;
    }

    public void setRestaurants(Restaurants restaurants) {
        this.restaurants = restaurants;
    }

    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (favoriteRestaurantsPK != null ? favoriteRestaurantsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FavoriteRestaurants)) {
            return false;
        }
        FavoriteRestaurants other = (FavoriteRestaurants) object;
        if ((this.favoriteRestaurantsPK == null && other.favoriteRestaurantsPK != null) || (this.favoriteRestaurantsPK != null && !this.favoriteRestaurantsPK.equals(other.favoriteRestaurantsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.FavoriteRestaurants[ favoriteRestaurantsPK=" + favoriteRestaurantsPK + " ]";
    }
    
}
