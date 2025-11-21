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
@Table(name = "Cities")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "Cities.findAll", query = "SELECT c FROM Cities c"),
    @NamedQuery(name = "Cities.findByCityId", query = "SELECT c FROM Cities c WHERE c.cityId = :cityId"),
    @NamedQuery(name = "Cities.findByName", query = "SELECT c FROM Cities c WHERE c.name = :name")})
public class Cities implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "CityId")
    private Integer cityId;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Name")
    private String name;
    @OneToMany(mappedBy = "cityId")
    private Collection<Users> usersCollection;
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cityId")
    private Collection<Areas> areasCollection;

    public Cities() {
    }

    public Cities(Integer cityId) {
        this.cityId = cityId;
    }

    public Cities(Integer cityId, String name) {
        this.cityId = cityId;
        this.name = name;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @XmlTransient
    public Collection<Users> getUsersCollection() {
        return usersCollection;
    }

    public void setUsersCollection(Collection<Users> usersCollection) {
        this.usersCollection = usersCollection;
    }

    @XmlTransient
    public Collection<Areas> getAreasCollection() {
        return areasCollection;
    }

    public void setAreasCollection(Collection<Areas> areasCollection) {
        this.areasCollection = areasCollection;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (cityId != null ? cityId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Cities)) {
            return false;
        }
        Cities other = (Cities) object;
        if ((this.cityId == null && other.cityId != null) || (this.cityId != null && !this.cityId.equals(other.cityId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.Cities[ cityId=" + cityId + " ]";
    }
    
}
