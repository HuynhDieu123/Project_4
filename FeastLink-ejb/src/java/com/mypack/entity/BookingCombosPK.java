/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

/**
 *
 * @author Laptop
 */
@Embeddable
public class BookingCombosPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "BookingId")
    private long bookingId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "ComboId")
    private long comboId;

    public BookingCombosPK() {
    }

    public BookingCombosPK(long bookingId, long comboId) {
        this.bookingId = bookingId;
        this.comboId = comboId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getComboId() {
        return comboId;
    }

    public void setComboId(long comboId) {
        this.comboId = comboId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) bookingId;
        hash += (int) comboId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BookingCombosPK)) {
            return false;
        }
        BookingCombosPK other = (BookingCombosPK) object;
        if (this.bookingId != other.bookingId) {
            return false;
        }
        if (this.comboId != other.comboId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BookingCombosPK[ bookingId=" + bookingId + ", comboId=" + comboId + " ]";
    }
    
}
