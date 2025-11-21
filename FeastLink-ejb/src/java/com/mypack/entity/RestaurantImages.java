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
@Table(name = "RestaurantImages")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RestaurantImages.findAll", query = "SELECT r FROM RestaurantImages r"),
    @NamedQuery(name = "RestaurantImages.findByImageId", query = "SELECT r FROM RestaurantImages r WHERE r.imageId = :imageId"),
    @NamedQuery(name = "RestaurantImages.findByImageUrl", query = "SELECT r FROM RestaurantImages r WHERE r.imageUrl = :imageUrl"),
    @NamedQuery(name = "RestaurantImages.findByCaption", query = "SELECT r FROM RestaurantImages r WHERE r.caption = :caption"),
    @NamedQuery(name = "RestaurantImages.findByIsPrimary", query = "SELECT r FROM RestaurantImages r WHERE r.isPrimary = :isPrimary"),
    @NamedQuery(name = "RestaurantImages.findBySortOrder", query = "SELECT r FROM RestaurantImages r WHERE r.sortOrder = :sortOrder"),
    @NamedQuery(name = "RestaurantImages.findByCreatedAt", query = "SELECT r FROM RestaurantImages r WHERE r.createdAt = :createdAt")})
public class RestaurantImages implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ImageId")
    private Long imageId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "ImageUrl")
    private String imageUrl;
    @Size(max = 255)
    @Column(name = "Caption")
    private String caption;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsPrimary")
    private boolean isPrimary;
    @Column(name = "SortOrder")
    private Integer sortOrder;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;

    public RestaurantImages() {
    }

    public RestaurantImages(Long imageId) {
        this.imageId = imageId;
    }

    public RestaurantImages(Long imageId, String imageUrl, boolean isPrimary, Date createdAt) {
        this.imageId = imageId;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary;
        this.createdAt = createdAt;
    }

    public Long getImageId() {
        return imageId;
    }

    public void setImageId(Long imageId) {
        this.imageId = imageId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
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
        hash += (imageId != null ? imageId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RestaurantImages)) {
            return false;
        }
        RestaurantImages other = (RestaurantImages) object;
        if ((this.imageId == null && other.imageId != null) || (this.imageId != null && !this.imageId.equals(other.imageId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.RestaurantImages[ imageId=" + imageId + " ]";
    }
    
}
