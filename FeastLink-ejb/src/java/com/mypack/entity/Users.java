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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "Users")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Users.findAll", query = "SELECT u FROM Users u"),
    @NamedQuery(name = "Users.findByUserId", query = "SELECT u FROM Users u WHERE u.userId = :userId"),
    @NamedQuery(name = "Users.findByFullName", query = "SELECT u FROM Users u WHERE u.fullName = :fullName"),
    @NamedQuery(name = "Users.findByEmail", query = "SELECT u FROM Users u WHERE u.email = :email"),
    @NamedQuery(name = "Users.findByPhone", query = "SELECT u FROM Users u WHERE u.phone = :phone"),
    @NamedQuery(name = "Users.findByPassword", query = "SELECT u FROM Users u WHERE u.password = :password"),
    @NamedQuery(name = "Users.findByRole", query = "SELECT u FROM Users u WHERE u.role = :role"),
    @NamedQuery(name = "Users.findByStatus", query = "SELECT u FROM Users u WHERE u.status = :status"),
    @NamedQuery(name = "Users.findByAvatarUrl", query = "SELECT u FROM Users u WHERE u.avatarUrl = :avatarUrl"),
    @NamedQuery(name = "Users.findByAddress", query = "SELECT u FROM Users u WHERE u.address = :address"),
    @NamedQuery(name = "Users.findByCreatedAt", query = "SELECT u FROM Users u WHERE u.createdAt = :createdAt"),
    @NamedQuery(name = "Users.findByUpdatedAt", query = "SELECT u FROM Users u WHERE u.updatedAt = :updatedAt"),
    @NamedQuery(name = "Users.findByLastLoginAt", query = "SELECT u FROM Users u WHERE u.lastLoginAt = :lastLoginAt")})
public class Users implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "UserId")
    private Long userId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "FullName")
    private String fullName;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Email")
    private String email;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 20)
    @Column(name = "Phone")
    private String phone;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "Password")
    private String password;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Role")
    private String role;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 255)
    @Column(name = "AvatarUrl")
    private String avatarUrl;
    @Size(max = 255)
    @Column(name = "Address")
    private String address;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @Column(name = "LastLoginAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLoginAt;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<RestaurantReviews> restaurantReviewsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "userId")
    private Collection<RestaurantManagers> restaurantManagersCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "changedByUserId")
    private Collection<BookingStatusHistory> bookingStatusHistoryCollection;
    @JoinColumn(name = "CityId", referencedColumnName = "CityId")
    @ManyToOne
    private Cities cityId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "users")
    private Collection<FavoriteRestaurants> favoriteRestaurantsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "fromUserId")
    private Collection<Messages> messagesCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "toUserId")
    private Collection<Messages> messagesCollection1;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reporterUserId")
    private Collection<Feedbacks> feedbacksCollection;
    @OneToMany(mappedBy = "reportedUserId")
    private Collection<Feedbacks> feedbacksCollection1;
    @OneToMany(mappedBy = "adminHandlerId")
    private Collection<Feedbacks> feedbacksCollection2;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "customerId")
    private Collection<Bookings> bookingsCollection;

    public Users() {
    }

    public Users(Long userId) {
        this.userId = userId;
    }

    public Users(Long userId, String fullName, String email, String password, String role, String status, Date createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @XmlTransient
    public Collection<RestaurantReviews> getRestaurantReviewsCollection() {
        return restaurantReviewsCollection;
    }

    public void setRestaurantReviewsCollection(Collection<RestaurantReviews> restaurantReviewsCollection) {
        this.restaurantReviewsCollection = restaurantReviewsCollection;
    }

    @XmlTransient
    public Collection<RestaurantManagers> getRestaurantManagersCollection() {
        return restaurantManagersCollection;
    }

    public void setRestaurantManagersCollection(Collection<RestaurantManagers> restaurantManagersCollection) {
        this.restaurantManagersCollection = restaurantManagersCollection;
    }

    @XmlTransient
    public Collection<BookingStatusHistory> getBookingStatusHistoryCollection() {
        return bookingStatusHistoryCollection;
    }

    public void setBookingStatusHistoryCollection(Collection<BookingStatusHistory> bookingStatusHistoryCollection) {
        this.bookingStatusHistoryCollection = bookingStatusHistoryCollection;
    }

    public Cities getCityId() {
        return cityId;
    }

    public void setCityId(Cities cityId) {
        this.cityId = cityId;
    }

    @XmlTransient
    public Collection<FavoriteRestaurants> getFavoriteRestaurantsCollection() {
        return favoriteRestaurantsCollection;
    }

    public void setFavoriteRestaurantsCollection(Collection<FavoriteRestaurants> favoriteRestaurantsCollection) {
        this.favoriteRestaurantsCollection = favoriteRestaurantsCollection;
    }

    @XmlTransient
    public Collection<Messages> getMessagesCollection() {
        return messagesCollection;
    }

    public void setMessagesCollection(Collection<Messages> messagesCollection) {
        this.messagesCollection = messagesCollection;
    }

    @XmlTransient
    public Collection<Messages> getMessagesCollection1() {
        return messagesCollection1;
    }

    public void setMessagesCollection1(Collection<Messages> messagesCollection1) {
        this.messagesCollection1 = messagesCollection1;
    }

    @XmlTransient
    public Collection<Feedbacks> getFeedbacksCollection() {
        return feedbacksCollection;
    }

    public void setFeedbacksCollection(Collection<Feedbacks> feedbacksCollection) {
        this.feedbacksCollection = feedbacksCollection;
    }

    @XmlTransient
    public Collection<Feedbacks> getFeedbacksCollection1() {
        return feedbacksCollection1;
    }

    public void setFeedbacksCollection1(Collection<Feedbacks> feedbacksCollection1) {
        this.feedbacksCollection1 = feedbacksCollection1;
    }

    @XmlTransient
    public Collection<Feedbacks> getFeedbacksCollection2() {
        return feedbacksCollection2;
    }

    public void setFeedbacksCollection2(Collection<Feedbacks> feedbacksCollection2) {
        this.feedbacksCollection2 = feedbacksCollection2;
    }

    @XmlTransient
    public Collection<Bookings> getBookingsCollection() {
        return bookingsCollection;
    }

    public void setBookingsCollection(Collection<Bookings> bookingsCollection) {
        this.bookingsCollection = bookingsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (userId != null ? userId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Users)) {
            return false;
        }
        Users other = (Users) object;
        if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Users[ userId=" + userId + " ]";
    }
    
}
