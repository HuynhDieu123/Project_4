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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "MenuItems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MenuItems.findAll", query = "SELECT m FROM MenuItems m"),
    @NamedQuery(name = "MenuItems.findByMenuItemId", query = "SELECT m FROM MenuItems m WHERE m.menuItemId = :menuItemId"),
    @NamedQuery(name = "MenuItems.findByName", query = "SELECT m FROM MenuItems m WHERE m.name = :name"),
    @NamedQuery(name = "MenuItems.findByDescription", query = "SELECT m FROM MenuItems m WHERE m.description = :description"),
    @NamedQuery(name = "MenuItems.findByPricePerPerson", query = "SELECT m FROM MenuItems m WHERE m.pricePerPerson = :pricePerPerson"),
    @NamedQuery(name = "MenuItems.findByIsVegetarian", query = "SELECT m FROM MenuItems m WHERE m.isVegetarian = :isVegetarian"),
    @NamedQuery(name = "MenuItems.findByStatus", query = "SELECT m FROM MenuItems m WHERE m.status = :status"),
    @NamedQuery(name = "MenuItems.findByImageUrl", query = "SELECT m FROM MenuItems m WHERE m.imageUrl = :imageUrl"),
    @NamedQuery(name = "MenuItems.findByIsDeleted", query = "SELECT m FROM MenuItems m WHERE m.isDeleted = :isDeleted")})
public class MenuItems implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "MenuItemId")
    private Long menuItemId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 150)
    @Column(name = "Name")
    private String name;
    @Size(max = 2147483647)
    @Column(name = "Description")
    private String description;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "PricePerPerson")
    private BigDecimal pricePerPerson;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsVegetarian")
    private boolean isVegetarian;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Size(max = 255)
    @Column(name = "ImageUrl")
    private String imageUrl;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsDeleted")
    private boolean isDeleted;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "menuItemId")
    private Collection<MenuItemImages> menuItemImagesCollection;
    @JoinColumn(name = "CuisineId", referencedColumnName = "CuisineId")
    @ManyToOne
    private Cuisines cuisineId;
    @JoinColumn(name = "CategoryId", referencedColumnName = "CategoryId")
    @ManyToOne
    private MenuCategories categoryId;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "menuItems")
    private Collection<BookingMenuItems> bookingMenuItemsCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "menuItems")
    private Collection<ComboItems> comboItemsCollection;

    public MenuItems() {
    }

    public MenuItems(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public MenuItems(Long menuItemId, String name, BigDecimal pricePerPerson, boolean isVegetarian, String status, boolean isDeleted) {
        this.menuItemId = menuItemId;
        this.name = name;
        this.pricePerPerson = pricePerPerson;
        this.isVegetarian = isVegetarian;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    public Long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(Long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPricePerPerson() {
        return pricePerPerson;
    }

    public void setPricePerPerson(BigDecimal pricePerPerson) {
        this.pricePerPerson = pricePerPerson;
    }

    public boolean getIsVegetarian() {
        return isVegetarian;
    }

    public void setIsVegetarian(boolean isVegetarian) {
        this.isVegetarian = isVegetarian;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    @XmlTransient
    public Collection<MenuItemImages> getMenuItemImagesCollection() {
        return menuItemImagesCollection;
    }

    public void setMenuItemImagesCollection(Collection<MenuItemImages> menuItemImagesCollection) {
        this.menuItemImagesCollection = menuItemImagesCollection;
    }

    public Cuisines getCuisineId() {
        return cuisineId;
    }

    public void setCuisineId(Cuisines cuisineId) {
        this.cuisineId = cuisineId;
    }

    public MenuCategories getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(MenuCategories categoryId) {
        this.categoryId = categoryId;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    @XmlTransient
    public Collection<BookingMenuItems> getBookingMenuItemsCollection() {
        return bookingMenuItemsCollection;
    }

    public void setBookingMenuItemsCollection(Collection<BookingMenuItems> bookingMenuItemsCollection) {
        this.bookingMenuItemsCollection = bookingMenuItemsCollection;
    }

    @XmlTransient
    public Collection<ComboItems> getComboItemsCollection() {
        return comboItemsCollection;
    }

    public void setComboItemsCollection(Collection<ComboItems> comboItemsCollection) {
        this.comboItemsCollection = comboItemsCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (menuItemId != null ? menuItemId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MenuItems)) {
            return false;
        }
        MenuItems other = (MenuItems) object;
        if ((this.menuItemId == null && other.menuItemId != null) || (this.menuItemId != null && !this.menuItemId.equals(other.menuItemId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.MenuItems[ menuItemId=" + menuItemId + " ]";
    }
    
}
