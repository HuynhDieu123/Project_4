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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.io.Serializable;
import java.util.Collection;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "MenuCategories")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "MenuCategories.findAll", query = "SELECT m FROM MenuCategories m"),
    @NamedQuery(name = "MenuCategories.findByCategoryId", query = "SELECT m FROM MenuCategories m WHERE m.categoryId = :categoryId"),
    @NamedQuery(name = "MenuCategories.findByName", query = "SELECT m FROM MenuCategories m WHERE m.name = :name"),
    @NamedQuery(name = "MenuCategories.findByDescription", query = "SELECT m FROM MenuCategories m WHERE m.description = :description"),
    @NamedQuery(name = "MenuCategories.findBySortOrder", query = "SELECT m FROM MenuCategories m WHERE m.sortOrder = :sortOrder"),
    @NamedQuery(name = "MenuCategories.findByIsActive", query = "SELECT m FROM MenuCategories m WHERE m.isActive = :isActive")})
public class MenuCategories implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CategoryId")
    private Long categoryId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Name")
    private String name;
    @Size(max = 255)
    @Column(name = "Description")
    private String description;
    @Column(name = "SortOrder")
    private Integer sortOrder;
    @Basic(optional = false)
    @NotNull
    @Column(name = "IsActive")
    private boolean isActive;
    @JoinColumn(name = "RestaurantId", referencedColumnName = "RestaurantId")
    @ManyToOne(optional = false)
    private Restaurants restaurantId;
    @OneToMany(mappedBy = "categoryId")
    private Collection<MenuItems> menuItemsCollection;

    public MenuCategories() {
    }

    public MenuCategories(Long categoryId) {
        this.categoryId = categoryId;
    }

    public MenuCategories(Long categoryId, String name, boolean isActive) {
        this.categoryId = categoryId;
        this.name = name;
        this.isActive = isActive;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    public Restaurants getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Restaurants restaurantId) {
        this.restaurantId = restaurantId;
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
        hash += (categoryId != null ? categoryId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof MenuCategories)) {
            return false;
        }
        MenuCategories other = (MenuCategories) object;
        if ((this.categoryId == null && other.categoryId != null) || (this.categoryId != null && !this.categoryId.equals(other.categoryId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.MenuCategories[ categoryId=" + categoryId + " ]";
    }
    
}
