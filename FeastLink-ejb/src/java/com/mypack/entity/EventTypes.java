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
@Table(name = "EventTypes")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "EventTypes.findAll", query = "SELECT e FROM EventTypes e"),
    @NamedQuery(name = "EventTypes.findByEventTypeId", query = "SELECT e FROM EventTypes e WHERE e.eventTypeId = :eventTypeId"),
    @NamedQuery(name = "EventTypes.findByName", query = "SELECT e FROM EventTypes e WHERE e.name = :name")})
public class EventTypes implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "Name")
    private String name;

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "EventTypeId")
    private Integer eventTypeId;
    @OneToMany(mappedBy = "eventTypeId")
    private Collection<Bookings> bookingsCollection;

    public EventTypes() {
    }

    public EventTypes(Integer eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public EventTypes(Integer eventTypeId, String name) {
        this.eventTypeId = eventTypeId;
        this.name = name;
    }

    public Integer getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(Integer eventTypeId) {
        this.eventTypeId = eventTypeId;
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
        hash += (eventTypeId != null ? eventTypeId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof EventTypes)) {
            return false;
        }
        EventTypes other = (EventTypes) object;
        if ((this.eventTypeId == null && other.eventTypeId != null) || (this.eventTypeId != null && !this.eventTypeId.equals(other.eventTypeId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.EventTypes[ eventTypeId=" + eventTypeId + " ]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
}
