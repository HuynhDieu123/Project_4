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
@Table(name = "BookingCombos")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "BookingCombos.findAll", query = "SELECT b FROM BookingCombos b"),
    @NamedQuery(name = "BookingCombos.findByBookingId", query = "SELECT b FROM BookingCombos b WHERE b.bookingCombosPK.bookingId = :bookingId"),
    @NamedQuery(name = "BookingCombos.findByComboId", query = "SELECT b FROM BookingCombos b WHERE b.bookingCombosPK.comboId = :comboId"),
    @NamedQuery(name = "BookingCombos.findByUnitPrice", query = "SELECT b FROM BookingCombos b WHERE b.unitPrice = :unitPrice"),
    @NamedQuery(name = "BookingCombos.findByQuantity", query = "SELECT b FROM BookingCombos b WHERE b.quantity = :quantity"),
    @NamedQuery(name = "BookingCombos.findByTotalPrice", query = "SELECT b FROM BookingCombos b WHERE b.totalPrice = :totalPrice")})
public class BookingCombos implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected BookingCombosPK bookingCombosPK;
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
    @JoinColumn(name = "ComboId", referencedColumnName = "ComboId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private MenuCombos menuCombos;

    public BookingCombos() {
    }

    public BookingCombos(BookingCombosPK bookingCombosPK) {
        this.bookingCombosPK = bookingCombosPK;
    }

    public BookingCombos(BookingCombosPK bookingCombosPK, BigDecimal unitPrice, int quantity, BigDecimal totalPrice) {
        this.bookingCombosPK = bookingCombosPK;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
    }

    public BookingCombos(long bookingId, long comboId) {
        this.bookingCombosPK = new BookingCombosPK(bookingId, comboId);
    }

    public BookingCombosPK getBookingCombosPK() {
        return bookingCombosPK;
    }

    public void setBookingCombosPK(BookingCombosPK bookingCombosPK) {
        this.bookingCombosPK = bookingCombosPK;
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

    public MenuCombos getMenuCombos() {
        return menuCombos;
    }

    public void setMenuCombos(MenuCombos menuCombos) {
        this.menuCombos = menuCombos;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (bookingCombosPK != null ? bookingCombosPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof BookingCombos)) {
            return false;
        }
        BookingCombos other = (BookingCombos) object;
        if ((this.bookingCombosPK == null && other.bookingCombosPK != null) || (this.bookingCombosPK != null && !this.bookingCombosPK.equals(other.bookingCombosPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.BookingCombos[ bookingCombosPK=" + bookingCombosPK + " ]";
    }
    
}
