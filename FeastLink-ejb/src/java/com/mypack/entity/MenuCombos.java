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
@Table(name = "MenuCombos")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MenuCombos.findAll", query = "SELECT m FROM MenuCombos m"),
    @NamedQuery(name = "MenuCombos.findByComboId", query = "SELECT m FROM MenuCombos m WHERE m.comboId = :comboId"),
    @NamedQuery(name = "MenuCombos.findByName", query = "SELECT m FROM MenuCombos m WHERE m.name = :name"),
    @NamedQuery(name = "MenuCombos.findByDescription", query = "SELECT m FROM MenuCombos m WHERE m.description = :description"),
    @NamedQuery(name = "MenuCombos.findByPriceTotal", query = "SELECT m FROM MenuCombos m WHERE m.priceTotal = :priceTotal"),
    @NamedQuery(name = "MenuCombos.findByMinGuests", query = "SELECT m FROM MenuCombos m WHERE m.minGuests = :minGuests"),
    @NamedQuery(name = "MenuCombos.findByStatus", query = "SELECT m FROM MenuCombos m WHERE m.status = :status"),
    @NamedQuery(name = "MenuCombos.findByIsDeleted", query = "SELECT m FROM MenuCombos m WHERE m.isDeleted = :isDeleted")})
public class MenuCombos implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ComboId")
    private Long comboId;
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
    @Column(name = "PriceTotal")
    private BigDecimal priceTotal;
    @Column(name = "MinGuests")
    private Integer minGuests;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "Status")
    private String status;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsDeleted")
    private boolean isDeleted;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "menuCombos")
    private Collection<BookingCombos> bookingCombosCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "menuCombos")
    private Collection<ComboItems> comboItemsCollection;

    public MenuCombos() {
    }

    public MenuCombos(Long comboId) {
        this.comboId = comboId;
    }

    public MenuCombos(Long comboId, String name, BigDecimal priceTotal, String status, boolean isDeleted) {
        this.comboId = comboId;
        this.name = name;
        this.priceTotal = priceTotal;
        this.status = status;
        this.isDeleted = isDeleted;
    }

    public Long getComboId() {
        return comboId;
    }

    public void setComboId(Long comboId) {
        this.comboId = comboId;
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

    public BigDecimal getPriceTotal() {
        return priceTotal;
    }

    public void setPriceTotal(BigDecimal priceTotal) {
        this.priceTotal = priceTotal;
    }

    public Integer getMinGuests() {
        return minGuests;
    }

    public void setMinGuests(Integer minGuests) {
        this.minGuests = minGuests;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
    }

    @XmlTransient
    public Collection<BookingCombos> getBookingCombosCollection() {
        return bookingCombosCollection;
    }

    public void setBookingCombosCollection(Collection<BookingCombos> bookingCombosCollection) {
        this.bookingCombosCollection = bookingCombosCollection;
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
        hash += (comboId != null ? comboId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MenuCombos)) {
            return false;
        }
        MenuCombos other = (MenuCombos) object;
        if ((this.comboId == null && other.comboId != null) || (this.comboId != null && !this.comboId.equals(other.comboId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.MenuCombos[ comboId=" + comboId + " ]";
    }
    
}
