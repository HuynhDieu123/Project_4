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

/**
 *
 * @author Laptop
 */
@Entity
@Table(name = "ComboItems")
@XmlRootElement
@NamedQueries({
    @NamedQuery(name = "ComboItems.findAll", query = "SELECT c FROM ComboItems c"),
    @NamedQuery(name = "ComboItems.findByComboId", query = "SELECT c FROM ComboItems c WHERE c.comboItemsPK.comboId = :comboId"),
    @NamedQuery(name = "ComboItems.findByMenuItemId", query = "SELECT c FROM ComboItems c WHERE c.comboItemsPK.menuItemId = :menuItemId"),
    @NamedQuery(name = "ComboItems.findByQuantity", query = "SELECT c FROM ComboItems c WHERE c.quantity = :quantity")})
public class ComboItems implements Serializable {

    private static final long serialVersionUID = 1L;
    @EmbeddedId
    protected ComboItemsPK comboItemsPK;
    @Basic(optional = false)
    @NotNull
    @Column(name = "Quantity")
    private int quantity;
    @JoinColumn(name = "ComboId", referencedColumnName = "ComboId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private MenuCombos menuCombos;
    @JoinColumn(name = "MenuItemId", referencedColumnName = "MenuItemId", insertable = false, updatable = false)
    @ManyToOne(optional = false)
    private MenuItems menuItems;

    public ComboItems() {
    }

    public ComboItems(ComboItemsPK comboItemsPK) {
        this.comboItemsPK = comboItemsPK;
    }

    public ComboItems(ComboItemsPK comboItemsPK, int quantity) {
        this.comboItemsPK = comboItemsPK;
        this.quantity = quantity;
    }

    public ComboItems(long comboId, long menuItemId) {
        this.comboItemsPK = new ComboItemsPK(comboId, menuItemId);
    }

    public ComboItemsPK getComboItemsPK() {
        return comboItemsPK;
    }

    public void setComboItemsPK(ComboItemsPK comboItemsPK) {
        this.comboItemsPK = comboItemsPK;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public MenuCombos getMenuCombos() {
        return menuCombos;
    }

    public void setMenuCombos(MenuCombos menuCombos) {
        this.menuCombos = menuCombos;
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
        hash += (comboItemsPK != null ? comboItemsPK.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof ComboItems)) {
            return false;
        }
        ComboItems other = (ComboItems) object;
        if ((this.comboItemsPK == null && other.comboItemsPK != null) || (this.comboItemsPK != null && !this.comboItemsPK.equals(other.comboItemsPK))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "com.mypack.entity.ComboItems[ comboItemsPK=" + comboItemsPK + " ]";
    }
    
}
