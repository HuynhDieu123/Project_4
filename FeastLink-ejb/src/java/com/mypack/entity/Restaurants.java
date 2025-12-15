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
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "Restaurants")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Restaurants.findAll", query = "SELECT r FROM Restaurants r"),
    @NamedQuery(name = "Restaurants.findByRestaurantId", query = "SELECT r FROM Restaurants r WHERE r.restaurantId = :restaurantId"),
    @NamedQuery(name = "Restaurants.findByName", query = "SELECT r FROM Restaurants r WHERE r.name = :name"),
    @NamedQuery(name = "Restaurants.findByLogoUrl", query = "SELECT r FROM Restaurants r WHERE r.logoUrl = :logoUrl"),
    @NamedQuery(name = "Restaurants.findByDescription", query = "SELECT r FROM Restaurants r WHERE r.description = :description"),
    @NamedQuery(name = "Restaurants.findByAddress", query = "SELECT r FROM Restaurants r WHERE r.address = :address"),
    @NamedQuery(name = "Restaurants.findByPhone", query = "SELECT r FROM Restaurants r WHERE r.phone = :phone"),
    @NamedQuery(name = "Restaurants.findByEmail", query = "SELECT r FROM Restaurants r WHERE r.email = :email"),
    @NamedQuery(name = "Restaurants.findByContactPerson", query = "SELECT r FROM Restaurants r WHERE r.contactPerson = :contactPerson"),
    @NamedQuery(name = "Restaurants.findByOpenTime", query = "SELECT r FROM Restaurants r WHERE r.openTime = :openTime"),
    @NamedQuery(name = "Restaurants.findByCloseTime", query = "SELECT r FROM Restaurants r WHERE r.closeTime = :closeTime"),
    @NamedQuery(name = "Restaurants.findByMinGuestCount", query = "SELECT r FROM Restaurants r WHERE r.minGuestCount = :minGuestCount"),
    @NamedQuery(name = "Restaurants.findByMinDaysInAdvance", query = "SELECT r FROM Restaurants r WHERE r.minDaysInAdvance = :minDaysInAdvance"),
    @NamedQuery(name = "Restaurants.findByCancelFullRefundDays", query = "SELECT r FROM Restaurants r WHERE r.cancelFullRefundDays = :cancelFullRefundDays"),
    @NamedQuery(name = "Restaurants.findByCancelPartialRefundDays", query = "SELECT r FROM Restaurants r WHERE r.cancelPartialRefundDays = :cancelPartialRefundDays"),
    @NamedQuery(name = "Restaurants.findByDefaultDepositPercent", query = "SELECT r FROM Restaurants r WHERE r.defaultDepositPercent = :defaultDepositPercent"),
    @NamedQuery(name = "Restaurants.findByStatus", query = "SELECT r FROM Restaurants r WHERE r.status = :status"),
    @NamedQuery(name = "Restaurants.findByCreatedAt", query = "SELECT r FROM Restaurants r WHERE r.createdAt = :createdAt"),
    @NamedQuery(name = "Restaurants.findByUpdatedAt", query = "SELECT r FROM Restaurants r WHERE r.updatedAt = :updatedAt")})
public class Restaurants implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "RestaurantId")
    private Long restaurantId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "Name")
    private String name;
    @Size(max = 255)
    @Column(name = "LogoUrl")
    private String logoUrl;
    @Size(max = 2147483647)
    @Column(name = "Description")
    private String description;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 255)
    @Column(name = "Address")
    private String address;
    // @Pattern(regexp="^\\(?(\\d{3})\\)?[- ]?(\\d{3})[- ]?(\\d{4})$", message="Invalid phone/fax format, should be as xxx-xxx-xxxx")//if the field contains phone or fax number consider using this annotation to enforce field validation
    @Size(max = 20)
    @Column(name = "Phone")
    private String phone;
    // @Pattern(regexp="[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?", message="Invalid email")//if the field contains email address consider using this annotation to enforce field validation
    @Size(max = 100)
    @Column(name = "Email")
    private String email;
    @Size(max = 100)
    @Column(name = "ContactPerson")
    private String contactPerson;
    @Column(name = "OpenTime")
    @Temporal(TemporalType.TIME)
    private Date openTime;
    @Column(name = "CloseTime")
    @Temporal(TemporalType.TIME)
    private Date closeTime;
    @Column(name = "MinGuestCount")
    private Integer minGuestCount;
    @Column(name = "MinDaysInAdvance")
    private Integer minDaysInAdvance;
    @Column(name = "CancelFullRefundDays")
    private Integer cancelFullRefundDays;
    @Column(name = "CancelPartialRefundDays")
    private Integer cancelPartialRefundDays;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Column(name = "DefaultDepositPercent")
    private BigDecimal defaultDepositPercent;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 30)
    @Column(name = "Status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "CreatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
    @Column(name = "UpdatedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<RestaurantReviews> restaurantReviewsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<RestaurantManagers> restaurantManagersCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<RestaurantDayCapacity> restaurantDayCapacityCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<RestaurantCapacitySettings> restaurantCapacitySettingsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurants")
    private Collection<FavoriteRestaurants> favoriteRestaurantsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<MenuCategories> menuCategoriesCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<RestaurantImages> restaurantImagesCollection;
    @OneToMany(mappedBy = "restaurantId")
    private Collection<Messages> messagesCollection;
    @JoinColumn(name = "AreaId", referencedColumnName = "AreaId")
    @ManyToOne
    private Areas areaId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<MenuCombos> menuCombosCollection;
    @OneToMany(mappedBy = "restaurantId")
    private Collection<Feedbacks> feedbacksCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<Bookings> bookingsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "restaurantId")
    private Collection<MenuItems> menuItemsCollection;

    public Restaurants() {
    }

    public Restaurants(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Restaurants(Long restaurantId, String name, String address, String status, Date createdAt) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.address = address;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Date getOpenTime() {
        return openTime;
    }

    public void setOpenTime(Date openTime) {
        this.openTime = openTime;
    }

    public Date getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }

    public Integer getMinGuestCount() {
        return minGuestCount;
    }

    public void setMinGuestCount(Integer minGuestCount) {
        this.minGuestCount = minGuestCount;
    }

    public Integer getMinDaysInAdvance() {
        return minDaysInAdvance;
    }

    public void setMinDaysInAdvance(Integer minDaysInAdvance) {
        this.minDaysInAdvance = minDaysInAdvance;
    }

    public Integer getCancelFullRefundDays() {
        return cancelFullRefundDays;
    }

    public void setCancelFullRefundDays(Integer cancelFullRefundDays) {
        this.cancelFullRefundDays = cancelFullRefundDays;
    }

    public Integer getCancelPartialRefundDays() {
        return cancelPartialRefundDays;
    }

    public void setCancelPartialRefundDays(Integer cancelPartialRefundDays) {
        this.cancelPartialRefundDays = cancelPartialRefundDays;
    }

    public BigDecimal getDefaultDepositPercent() {
        return defaultDepositPercent;
    }

    public void setDefaultDepositPercent(BigDecimal defaultDepositPercent) {
        this.defaultDepositPercent = defaultDepositPercent;
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

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
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
    public Collection<RestaurantDayCapacity> getRestaurantDayCapacityCollection() {
        return restaurantDayCapacityCollection;
    }

    public void setRestaurantDayCapacityCollection(Collection<RestaurantDayCapacity> restaurantDayCapacityCollection) {
        this.restaurantDayCapacityCollection = restaurantDayCapacityCollection;
    }

    @XmlTransient
    public Collection<RestaurantCapacitySettings> getRestaurantCapacitySettingsCollection() {
        return restaurantCapacitySettingsCollection;
    }

    public void setRestaurantCapacitySettingsCollection(Collection<RestaurantCapacitySettings> restaurantCapacitySettingsCollection) {
        this.restaurantCapacitySettingsCollection = restaurantCapacitySettingsCollection;
    }

    @XmlTransient
    public Collection<FavoriteRestaurants> getFavoriteRestaurantsCollection() {
        return favoriteRestaurantsCollection;
    }

    public void setFavoriteRestaurantsCollection(Collection<FavoriteRestaurants> favoriteRestaurantsCollection) {
        this.favoriteRestaurantsCollection = favoriteRestaurantsCollection;
    }

    @XmlTransient
    public Collection<MenuCategories> getMenuCategoriesCollection() {
        return menuCategoriesCollection;
    }

    public void setMenuCategoriesCollection(Collection<MenuCategories> menuCategoriesCollection) {
        this.menuCategoriesCollection = menuCategoriesCollection;
    }

    @XmlTransient
    public Collection<RestaurantImages> getRestaurantImagesCollection() {
        return restaurantImagesCollection;
    }

    public void setRestaurantImagesCollection(Collection<RestaurantImages> restaurantImagesCollection) {
        this.restaurantImagesCollection = restaurantImagesCollection;
    }

    @XmlTransient
    public Collection<Messages> getMessagesCollection() {
        return messagesCollection;
    }

    public void setMessagesCollection(Collection<Messages> messagesCollection) {
        this.messagesCollection = messagesCollection;
    }

    public Areas getAreaId() {
        return areaId;
    }

    public void setAreaId(Areas areaId) {
        this.areaId = areaId;
    }

    @XmlTransient
    public Collection<MenuCombos> getMenuCombosCollection() {
        return menuCombosCollection;
    }

    public void setMenuCombosCollection(Collection<MenuCombos> menuCombosCollection) {
        this.menuCombosCollection = menuCombosCollection;
    }

    @XmlTransient
    public Collection<Feedbacks> getFeedbacksCollection() {
        return feedbacksCollection;
    }

    public void setFeedbacksCollection(Collection<Feedbacks> feedbacksCollection) {
        this.feedbacksCollection = feedbacksCollection;
    }

    @XmlTransient
    public Collection<Bookings> getBookingsCollection() {
        return bookingsCollection;
    }

    public void setBookingsCollection(Collection<Bookings> bookingsCollection) {
        this.bookingsCollection = bookingsCollection;
    }

    @XmlTransient
    public Collection<MenuItems> getMenuItemsCollection() {
        return menuItemsCollection;
    }

    public void setMenuItemsCollection(Collection<MenuItems> menuItemsCollection) {
        this.menuItemsCollection = menuItemsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (restaurantId != null ? restaurantId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Restaurants)) {
            return false;
        }
        Restaurants other = (Restaurants) object;
        if ((this.restaurantId == null && other.restaurantId != null) || (this.restaurantId != null && !this.restaurantId.equals(other.restaurantId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Restaurants[ restaurantId=" + restaurantId + " ]";
    }
}
