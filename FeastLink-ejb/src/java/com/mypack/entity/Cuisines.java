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
@Table(name = "Cuisines")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Cuisines.findAll", query = "SELECT c FROM Cuisines c"),
    @NamedQuery(name = "Cuisines.findByCuisineId", query = "SELECT c FROM Cuisines c WHERE c.cuisineId = :cuisineId"),
    @NamedQuery(name = "Cuisines.findByName", query = "SELECT c FROM Cuisines c WHERE c.name = :name")})
public class Cuisines implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CuisineId")
    private Integer cuisineId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Name")
    private String name;
    @OneToMany(mappedBy = "cuisineId")
    private Collection<MenuItems> menuItemsCollection;

    public Cuisines() {
    }

    public Cuisines(Integer cuisineId) {
        this.cuisineId = cuisineId;
    }

    public Cuisines(Integer cuisineId, String name) {
        this.cuisineId = cuisineId;
        this.name = name;
    }

    public Integer getCuisineId() {
        return cuisineId;
    }

    public void setCuisineId(Integer cuisineId) {
        this.cuisineId = cuisineId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        hash += (cuisineId != null ? cuisineId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Cuisines)) {
            return false;
        }
        Cuisines other = (Cuisines) object;
        if ((this.cuisineId == null && other.cuisineId != null) || (this.cuisineId != null && !this.cuisineId.equals(other.cuisineId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Cuisines[ cuisineId=" + cuisineId + " ]";
    }
    
}
