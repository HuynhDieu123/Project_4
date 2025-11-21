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
public class BookingMenuItemsPK implements Serializable {

    @Basic(optional = false)
    @NotNull
    @Column(name = "BookingId")
    private long bookingId;
    @Basic(optional = false)
    @NotNull
    @Column(name = "MenuItemId")
    private long menuItemId;

    public BookingMenuItemsPK() {
    }

    public BookingMenuItemsPK(long bookingId, long menuItemId) {
        this.bookingId = bookingId;
        this.menuItemId = menuItemId;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (int) bookingId;
        hash += (int) menuItemId;
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BookingMenuItemsPK)) {
            return false;
        }
        BookingMenuItemsPK other = (BookingMenuItemsPK) object;
        if (this.bookingId != other.bookingId) {
            return false;
        }
        if (this.menuItemId != other.menuItemId) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BookingMenuItemsPK[ bookingId=" + bookingId + ", menuItemId=" + menuItemId + " ]";
    }
    
}
