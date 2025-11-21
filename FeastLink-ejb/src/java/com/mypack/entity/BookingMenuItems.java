/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mypack.entity;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "BookingMenuItems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BookingMenuItems.findAll", query = "SELECT b FROM BookingMenuItems b"),
    @NamedQuery(name = "BookingMenuItems.findByBookingId", query = "SELECT b FROM BookingMenuItems b WHERE b.bookingMenuItemsPK.bookingId = :bookingId"),
    @NamedQuery(name = "BookingMenuItems.findByMenuItemId", query = "SELECT b FROM BookingMenuItems b WHERE b.bookingMenuItemsPK.menuItemId = :menuItemId"),
    @NamedQuery(name = "BookingMenuItems.findByUnitPrice", query = "SELECT b FROM BookingMenuItems b WHERE b.unitPrice = :unitPrice"),
    @NamedQuery(name = "BookingMenuItems.findByQuantity", query = "SELECT b FROM BookingMenuItems b WHERE b.quantity = :quantity"),
    @NamedQuery(name = "BookingMenuItems.findByTotalPrice", query = "SELECT b FROM BookingMenuItems b WHERE b.totalPrice = :totalPrice")})
public class BookingMenuItems implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected BookingMenuItemsPK bookingMenuItemsPK;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Basic(optional = false)
    @NotNull
    @Column(name = "UnitPrice")
    private BigDecimal unitPrice;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @Basic(optional = false)
    @NotNull
    @Column(name = "TotalPrice")
    private BigDecimal totalPrice;
    @JoinColumn(name = "BookingId", referencedColumnName = "BookingId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private Bookings bookings;
    @JoinColumn(name = "MenuItemId", referencedColumnName = "MenuItemId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private MenuItems menuItems;

    public BookingMenuItems() {
    }

    public BookingMenuItems(BookingMenuItemsPK bookingMenuItemsPK) {
        this.bookingMenuItemsPK = bookingMenuItemsPK;
    }

    public BookingMenuItems(BookingMenuItemsPK bookingMenuItemsPK, BigDecimal unitPrice, int quantity, BigDecimal totalPrice) {
        this.bookingMenuItemsPK = bookingMenuItemsPK;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public BookingMenuItems(long bookingId, long menuItemId) {
        this.bookingMenuItemsPK = new BookingMenuItemsPK(bookingId, menuItemId);
    }

    public BookingMenuItemsPK getBookingMenuItemsPK() {
        return bookingMenuItemsPK;
    }

    public void setBookingMenuItemsPK(BookingMenuItemsPK bookingMenuItemsPK) {
        this.bookingMenuItemsPK = bookingMenuItemsPK;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Bookings getBookings() {
        return bookings;
    }

    public void setBookings(Bookings bookings) {
        this.bookings = bookings;
    }

    public MenuItems getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(MenuItems menuItems) {
        this.menuItems = menuItems;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (bookingMenuItemsPK != null ? bookingMenuItemsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BookingMenuItems)) {
            return false;
        }
        BookingMenuItems other = (BookingMenuItems) object;
        if ((this.bookingMenuItemsPK == null && other.bookingMenuItemsPK != null) || (this.bookingMenuItemsPK != null && !this.bookingMenuItemsPK.equals(other.bookingMenuItemsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BookingMenuItems[ bookingMenuItemsPK=" + bookingMenuItemsPK + " ]";
    }
    
}
