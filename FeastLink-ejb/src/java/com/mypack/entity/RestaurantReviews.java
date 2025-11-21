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
@Table(name = "RestaurantReviews")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "RestaurantReviews.findAll", query = "SELECT r FROM RestaurantReviews r"),
    @NamedQuery(name = "RestaurantReviews.findByReviewId", query = "SELECT r FROM RestaurantReviews r WHERE r.reviewId = :reviewId"),
    @NamedQuery(name = "RestaurantReviews.findByRating", query = "SELECT r FROM RestaurantReviews r WHERE r.rating = :rating"),
    @NamedQuery(name = "RestaurantReviews.findByComment", query = "SELECT r FROM RestaurantReviews r WHERE r.comment = :comment"),
    @NamedQuery(name = "RestaurantReviews.findByCreatedAt", query = "SELECT r FROM RestaurantReviews r WHERE r.createdAt = :createdAt"),
    @NamedQuery(name = "RestaurantReviews.findByIsApproved", query = "SELECT r FROM RestaurantReviews r WHERE r.isApproved = :isApproved"),
    @NamedQuery(name = "RestaurantReviews.findByIsDeleted", query = "SELECT r FROM RestaurantReviews r WHERE r.isDeleted = :isDeleted")})
public class RestaurantReviews implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ReviewId")
    private Long reviewId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Rating")
    private int rating;
    @Size(max = 2147483647)
    @Column(name = "Comment")
    private String comment;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsApproved")
    private boolean isApproved;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsDeleted")
    private boolean isDeleted;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @ManyToOne(optional = false)
    private Bookings bookingId;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @JoinColumn(name = "CustomerId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users customerId;

    public RestaurantReviews() {
    }

    public RestaurantReviews(Long reviewId) {
        this.reviewId = reviewId;
    }

    public RestaurantReviews(Long reviewId, int rating, Date createdAt, boolean isApproved, boolean isDeleted) {
        this.reviewId = reviewId;
        this.rating = rating;
        this.createdAt = createdAt;
        this.isApproved = isApproved;
        this.isDeleted = isDeleted;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean getIsApproved() {
        return isApproved;
    }

    public void setIsApproved(boolean isApproved) {
        this.isApproved = isApproved;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Users getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Users customerId) {
        this.customerId = customerId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (reviewId != null ? reviewId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof RestaurantReviews)) {
            return false;
        }
        RestaurantReviews other = (RestaurantReviews) object;
        if ((this.reviewId == null && other.reviewId != null) || (this.reviewId != null && !this.reviewId.equals(other.reviewId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.RestaurantReviews[ reviewId=" + reviewId + " ]";
    }
    
}
