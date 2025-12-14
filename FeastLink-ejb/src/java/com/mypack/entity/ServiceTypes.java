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
@Table(name = "ServiceTypes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ServiceTypes.findAll", query = "SELECT s FROM ServiceTypes s"),
    @NamedQuery(name = "ServiceTypes.findByServiceTypeId", query = "SELECT s FROM ServiceTypes s WHERE s.serviceTypeId = :serviceTypeId"),
    @NamedQuery(name = "ServiceTypes.findByName", query = "SELECT s FROM ServiceTypes s WHERE s.name = :name")})
public class ServiceTypes implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Name")
    private String name;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ServiceTypeId")
    private Integer serviceTypeId;
    @OneToMany(mappedBy = "serviceTypeId")
    private Collection<Bookings> bookingsCollection;

    public ServiceTypes() {
    }

    public ServiceTypes(Integer serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
    }

    public ServiceTypes(Integer serviceTypeId, String name) {
        this.serviceTypeId = serviceTypeId;
        this.name = name;
    }

    public Integer getServiceTypeId() {
        return serviceTypeId;
    }

    public void setServiceTypeId(Integer serviceTypeId) {
        this.serviceTypeId = serviceTypeId;
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
        hash += (serviceTypeId != null ? serviceTypeId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ServiceTypes)) {
            return false;
        }
        ServiceTypes other = (ServiceTypes) object;
        if ((this.serviceTypeId == null && other.serviceTypeId != null) || (this.serviceTypeId != null && !this.serviceTypeId.equals(other.serviceTypeId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.ServiceTypes[ serviceTypeId=" + serviceTypeId + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
