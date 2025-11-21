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
@Table(name = "BookingStatusHistory")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BookingStatusHistory.findAll", query = "SELECT b FROM BookingStatusHistory b"),
    @NamedQuery(name = "BookingStatusHistory.findByHistoryId", query = "SELECT b FROM BookingStatusHistory b WHERE b.historyId = :historyId"),
    @NamedQuery(name = "BookingStatusHistory.findByOldStatus", query = "SELECT b FROM BookingStatusHistory b WHERE b.oldStatus = :oldStatus"),
    @NamedQuery(name = "BookingStatusHistory.findByNewStatus", query = "SELECT b FROM BookingStatusHistory b WHERE b.newStatus = :newStatus"),
    @NamedQuery(name = "BookingStatusHistory.findByChangedAt", query = "SELECT b FROM BookingStatusHistory b WHERE b.changedAt = :changedAt"),
    @NamedQuery(name = "BookingStatusHistory.findByNote", query = "SELECT b FROM BookingStatusHistory b WHERE b.note = :note")})
public class BookingStatusHistory implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "HistoryId")
    private Long historyId;
    @Size(max = 20)
    @Column(name = "OldStatus")
    private String oldStatus;
    @Basic(optional = false)
    @NotNull
    @Size(min = 1, max = 20)
    @Column(name = "NewStatus")
    private String newStatus;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ChangedAt")
    @Temporal(TemporalType.TIMESTAMP)
    private Date changedAt;
    @Size(max = 2147483647)
    @Column(name = "Note")
    private String note;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId")
    @ManyToOne(optional = false)
    private Bookings bookingId;
    @JoinColumn(name = "ChangedByUserId", referencedColumnName = "UserId")
    @ManyToOne(optional = false)
    private Users changedByUserId;

    public BookingStatusHistory() {
    }

    public BookingStatusHistory(Long historyId) {
        this.historyId = historyId;
    }

    public BookingStatusHistory(Long historyId, String newStatus, Date changedAt) {
        this.historyId = historyId;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public Long getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Long historyId) {
        this.historyId = historyId;
    }

    public String getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus) {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(String newStatus) {
        this.newStatus = newStatus;
    }

    public Date getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(Date changedAt) {
        this.changedAt = changedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Bookings getBookingId() {
        return bookingId;
    }

    public void setBookingId(Bookings bookingId) {
        this.bookingId = bookingId;
    }

    public Users getChangedByUserId() {
        return changedByUserId;
    }

    public void setChangedByUserId(Users changedByUserId) {
        this.changedByUserId = changedByUserId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (historyId != null ? historyId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BookingStatusHistory)) {
            return false;
        }
        BookingStatusHistory other = (BookingStatusHistory) object;
        if ((this.historyId == null && other.historyId != null) || (this.historyId != null && !this.historyId.equals(other.historyId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BookingStatusHistory[ historyId=" + historyId + " ]";
    }
    
}
